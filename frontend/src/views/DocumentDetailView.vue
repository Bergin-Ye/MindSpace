<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import StatusPill from '../components/StatusPill.vue'
import EmptyState from '../components/EmptyState.vue'
import {
  analyzeDocument,
  deleteDocument,
  downloadDocument,
  embedDocument,
  getDocument,
  getParseStatus,
  parseDocumentAsync,
} from '../api/documents'
import { listFolders } from '../api/folders'
import { errorMessage } from '../api/http'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import { askConfirm } from '../stores/confirm'
import {
  formatBytes,
  formatDateTime,
  parseKeywords,
  saveBlob,
} from '../utils/format'

const route = useRoute()
const router = useRouter()
const workspace = useWorkspaceStore()
const toast = useToastStore()

const docId = computed(() => Number(route.params.id))

const detail = ref(null)
const folders = ref([])
const loading = ref(true)
const parsing = ref(false)
const analyzing = ref(false)
const embedding = ref(false)
const downloading = ref(false)
const deleting = ref(false)
const aiNotice = ref('')
const embedNotice = ref('')
const embedResult = ref(null)

let pollTimer = null

const kbName = computed(
  () =>
    workspace.knowledgeBases.find((item) => item.id === detail.value?.knowledgeBaseId)?.name ||
    '知识库'
)

const folderName = computed(() => {
  if (!detail.value) return '—'
  const folder = folders.value.find((item) => item.id === detail.value.folderId)
  return folder?.name || '未分类'
})

const keywords = computed(() => parseKeywords(detail.value?.keywords))

const parsed = computed(() => detail.value?.processStatus === 'SUCCESS')

const canParse = computed(
  () =>
    detail.value &&
    (detail.value.processStatus === 'UPLOADED' || detail.value.processStatus === 'FAILED')
)

const steps = computed(() => {
  const doc = detail.value
  if (!doc) return []

  let parseState = 'pending'
  if (doc.processStatus === 'SUCCESS') parseState = 'done'
  else if (doc.processStatus === 'PROCESSING') parseState = 'active'
  else if (doc.processStatus === 'FAILED') parseState = 'error'

  let aiState = 'pending'
  if (doc.summary) aiState = 'done'
  else if (analyzing.value) aiState = 'active'

  let vectorState = 'pending'
  if (embedResult.value) vectorState = 'done'
  else if (embedding.value) vectorState = 'active'

  return [
    {
      index: 1,
      title: '上传原文件',
      desc: `${(doc.fileType || '').toUpperCase()} · ${formatBytes(doc.fileSize)}`,
      state: 'done',
    },
    {
      index: 2,
      title: '解析正文',
      desc:
        doc.processStatus === 'PROCESSING'
          ? '正在后台提取文字'
          : doc.processStatus === 'FAILED'
            ? '解析失败，可以重试'
            : doc.extractedTextLength
              ? `${doc.extractedTextLength} 个字符`
              : '等待解析',
      state: parseState,
    },
    {
      index: 3,
      title: 'AI 分析',
      desc: doc.summary ? `${doc.aiCategory || '已分类'}` : '生成摘要、关键词和分类',
      state: aiState,
    },
    {
      index: 4,
      title: '切块向量化',
      desc: embedResult.value
        ? `${embedResult.value.chunkCount} 个分块 · ${embedResult.value.embeddingDim} 维`
        : '需要时执行，重复执行会覆盖旧分块',
      state: vectorState,
    },
  ]
})

function stopPoll() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPoll() {
  stopPoll()
  pollTimer = window.setInterval(async () => {
    try {
      const status = await getParseStatus(docId.value)
      if (detail.value) {
        detail.value.processStatus = status.processStatus
        detail.value.errorMessage = status.errorMessage
        detail.value.extractedTextLength = status.extractedTextLength
        if (status.textPreview) detail.value.textPreview = status.textPreview
      }
      if (status.processStatus === 'SUCCESS' || status.processStatus === 'FAILED') {
        stopPoll()
        if (status.processStatus === 'SUCCESS') {
          toast.success('正文解析完成')
          await loadDoc()
        } else {
          toast.error(status.errorMessage || '解析失败')
        }
      }
    } catch {
      stopPoll()
    }
  }, 1500)
}

async function loadDoc() {
  const doc = await getDocument(docId.value)
  detail.value = doc
  if (doc.processStatus === 'PROCESSING') startPoll()
  else stopPoll()
}

async function loadFolders(knowledgeBaseId) {
  if (!knowledgeBaseId) return
  try {
    folders.value = (await listFolders(knowledgeBaseId)) || []
  } catch {
    folders.value = []
  }
}

async function load() {
  loading.value = true
  try {
    await loadDoc()
    await loadFolders(detail.value?.knowledgeBaseId)
  } catch (error) {
    toast.error(errorMessage(error))
    if (error?.code === 2013) {
      router.replace({ name: 'knowledge-bases' })
    }
  } finally {
    loading.value = false
  }
}

async function parse() {
  if (!canParse.value || parsing.value) return
  parsing.value = true
  try {
    const result = await parseDocumentAsync(docId.value)
    detail.value.processStatus = result?.processStatus || 'PROCESSING'
    detail.value.errorMessage = result?.errorMessage || null
    if (detail.value.processStatus === 'PROCESSING') {
      toast.success('已提交解析，正在后台处理')
      startPoll()
    } else {
      await loadDoc()
    }
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    parsing.value = false
  }
}

async function analyze() {
  if (!parsed.value || analyzing.value) return
  analyzing.value = true
  aiNotice.value = ''
  try {
    const updated = await analyzeDocument(docId.value)
    detail.value = updated
    toast.success('AI 分析完成')
  } catch (error) {
    aiNotice.value = errorMessage(error)
    toast.error(errorMessage(error))
  } finally {
    analyzing.value = false
  }
}

async function embed() {
  if (!parsed.value || embedding.value) return
  embedding.value = true
  embedNotice.value = ''
  try {
    embedResult.value = await embedDocument(docId.value)
    toast.success('向量化完成')
  } catch (error) {
    embedNotice.value = errorMessage(error)
    toast.error(errorMessage(error))
  } finally {
    embedding.value = false
  }
}

async function download() {
  if (!detail.value) return
  downloading.value = true
  try {
    const response = await downloadDocument(detail.value.id)
    saveBlob(response.data, detail.value.originalName)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    downloading.value = false
  }
}

async function remove() {
  if (!detail.value) return
  const ok = await askConfirm({
    title: `删除「${detail.value.originalName}」`,
    message: '会删除原文件、解析正文和向量分块，删除后不可恢复。',
    confirmText: '删除文档',
    tone: 'danger',
  })
  if (!ok) return

  deleting.value = true
  try {
    const knowledgeBaseId = detail.value.knowledgeBaseId
    await deleteDocument(detail.value.id)
    toast.success('文档已删除')
    await router.replace({ name: 'kb-files', params: { id: knowledgeBaseId } })
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    deleting.value = false
  }
}

watch(docId, load, { immediate: true })

onBeforeUnmount(stopPoll)
</script>

<template>
  <div class="page">
    <nav class="crumbs mono" aria-label="面包屑">
      <RouterLink :to="{ name: 'knowledge-bases' }">知识库</RouterLink>
      <span aria-hidden="true">/</span>
      <RouterLink
        v-if="detail"
        :to="{ name: 'kb-files', params: { id: detail.knowledgeBaseId } }"
      >
        {{ kbName }}
      </RouterLink>
      <span v-else aria-hidden="true">…</span>
      <span aria-hidden="true">/</span>
      <span class="truncate">文档详情</span>
    </nav>

    <div v-if="loading" class="loading-block">
      <span class="spinner spinner--lg" />
      正在读取文档…
    </div>

    <template v-else-if="detail">
      <header class="page__head">
        <div class="doc-head">
          <div class="row wrap">
            <h1 class="page__title">{{ detail.originalName }}</h1>
            <StatusPill :status="detail.processStatus" />
          </div>
          <p class="page__desc">
            {{ folderName }} · {{ (detail.fileType || '').toUpperCase() }} ·
            {{ formatBytes(detail.fileSize) }} · 上传于 {{ formatDateTime(detail.createTime) }}
          </p>
        </div>
        <div class="page__actions">
          <button class="btn" type="button" :disabled="downloading" @click="download">
            <span v-if="downloading" class="spinner" />
            <Icon v-else name="download" :size="15" />
            下载原文件
          </button>
          <button class="btn btn--danger" type="button" :disabled="deleting" @click="remove">
            <Icon name="trash" :size="15" />
            删除
          </button>
        </div>
      </header>

      <div class="split">
        <div class="stack">
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">处理进度</span>
              <span class="mono muted">{{ formatDateTime(detail.updateTime) }} 更新</span>
            </div>
            <div class="panel__body">
              <ol class="steps">
                <li v-for="step in steps" :key="step.index" class="step" :class="`is-${step.state}`">
                  <span class="step__index">
                    <Icon v-if="step.state === 'done'" name="check" :size="14" :stroke="2.3" />
                    <span v-else-if="step.state === 'active'" class="step__pulse" />
                    <template v-else>{{ step.index }}</template>
                  </span>
                  <div class="step__text">
                    <p class="step__title">{{ step.title }}</p>
                    <p class="step__desc">{{ step.desc }}</p>
                  </div>
                </li>
              </ol>

              <p v-if="detail.errorMessage" class="form-error" role="alert">
                {{ detail.errorMessage }}
              </p>

              <div class="panel__action">
                <button
                  v-if="canParse"
                  class="btn btn--primary"
                  type="button"
                  :disabled="parsing"
                  @click="parse"
                >
                  <span v-if="parsing" class="spinner" />
                  <Icon v-else name="refresh" :size="15" />
                  {{ detail.processStatus === 'FAILED' ? '重新解析' : '解析正文' }}
                </button>
                <span v-else-if="detail.processStatus === 'PROCESSING'" class="processing">
                  <span class="spinner" />
                  正在后台解析，页面会自动刷新
                </span>
                <span v-else class="hint">正文已解析，可以进行 AI 分析或向量化。</span>
              </div>
            </div>
          </section>

          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">AI 分析</span>
              <button
                class="btn btn--sm"
                type="button"
                :disabled="!parsed || analyzing"
                :title="parsed ? '' : '需要先解析正文'"
                @click="analyze"
              >
                <span v-if="analyzing" class="spinner" />
                <Icon v-else name="sparkle" :size="14" />
                {{ detail.summary ? '重新分析' : '生成摘要' }}
              </button>
            </div>

            <div v-if="detail.summary" class="panel__body stack" style="gap: 16px">
              <div>
                <p class="field-title">摘要</p>
                <p class="summary">{{ detail.summary }}</p>
              </div>

              <div class="analysis-row">
                <div>
                  <p class="field-title">关键词</p>
                  <div v-if="keywords.length" class="row wrap" style="gap: 6px">
                    <span v-for="word in keywords" :key="word" class="chip">{{ word }}</span>
                  </div>
                  <p v-else class="hint">没有提取到关键词</p>
                </div>

                <div>
                  <p class="field-title">分类</p>
                  <div class="row wrap">
                    <span class="chip chip--accent">{{ detail.aiCategory || '未分类' }}</span>
                    <span v-if="detail.confidence !== null" class="mono muted">
                      置信度 {{ Math.round(detail.confidence * 100) }}%
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="panel__body">
              <div class="inline-empty">
                <Icon name="sparkle" :size="18" />
                <div>
                  <p class="inline-empty__title">还没有分析结果</p>
                  <p class="inline-empty__desc">
                    解析正文后，可以让模型生成一句话摘要、关键词和分类。
                  </p>
                </div>
              </div>
              <p v-if="aiNotice" class="form-error" role="alert" style="margin-top: 12px">
                {{ aiNotice }}
              </p>
            </div>
          </section>

          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">切块与向量化</span>
              <button
                class="btn btn--sm"
                type="button"
                :disabled="!parsed || embedding"
                :title="parsed ? '' : '需要先解析正文'"
                @click="embed"
              >
                <span v-if="embedding" class="spinner" />
                <Icon v-else name="layers" :size="14" />
                切块并向量化
              </button>
            </div>
            <div class="panel__body stack" style="gap: 14px">
              <p class="hint" style="max-width: 62ch">
                按 500 个字符切块、相邻重叠 100 个字符，逐块生成向量。语义检索和问答只会用到已向量化的分块；重复执行会覆盖旧分块。
              </p>
              <p class="hint">
                详情接口不返回向量化状态，所以这里只在本次执行后显示结果。
              </p>

              <div v-if="embedResult" class="stats">
                <div class="stat">
                  <span class="stat__value mono">{{ embedResult.chunkCount }}</span>
                  <span class="stat__label">分块数</span>
                </div>
                <div class="stat">
                  <span class="stat__value mono">{{ embedResult.embeddingDim }}</span>
                  <span class="stat__label">向量维度</span>
                </div>
                <div class="stat">
                  <span class="stat__value mono">{{ embedResult.elapsedMs }}</span>
                  <span class="stat__label">耗时（毫秒）</span>
                </div>
              </div>

              <p v-if="embedNotice" class="form-error" role="alert">{{ embedNotice }}</p>
            </div>
          </section>

          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">正文预览</span>
              <span v-if="detail.extractedTextLength" class="mono muted">
                {{ detail.extractedTextLength }} 个字符
              </span>
            </div>
            <div class="panel__body">
              <pre v-if="detail.textPreview" class="preview">{{ detail.textPreview }}</pre>
              <EmptyState
                v-else
                icon="file-text"
                title="还没有正文"
                description="解析完成后，这里会显示正文开头。"
              />
              <p v-if="detail.textPreview" class="hint" style="margin-top: 12px">
                详情接口只返回正文前 200 个字符；完整内容请下载原文件。
              </p>
            </div>
          </section>
        </div>

        <aside class="stack">
          <section class="panel">
            <div class="panel__head">
              <span class="panel__title">文档信息</span>
            </div>
            <div class="panel__body">
              <dl class="kv">
                <dt>知识库</dt>
                <dd>
                  <RouterLink :to="{ name: 'kb-files', params: { id: detail.knowledgeBaseId } }">
                    {{ kbName }}
                  </RouterLink>
                </dd>
                <dt>文件夹</dt>
                <dd>{{ folderName }}</dd>
                <dt>文件类型</dt>
                <dd class="mono">{{ (detail.fileType || '').toUpperCase() }}</dd>
                <dt>文件大小</dt>
                <dd class="mono">{{ formatBytes(detail.fileSize) }}</dd>
                <dt>正文长度</dt>
                <dd class="mono">
                  {{ detail.extractedTextLength ?? '—' }}
                </dd>
                <dt>上传时间</dt>
                <dd class="mono">{{ formatDateTime(detail.createTime) }}</dd>
                <dt>更新时间</dt>
                <dd class="mono">{{ formatDateTime(detail.updateTime) }}</dd>
              </dl>
            </div>
          </section>

          <section class="panel">
            <div class="panel__body">
              <p class="note">
                回答和检索都只使用你当前知识库中已完成解析的文档。向量化之后，才能命中语义检索和问答引用。
              </p>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.crumbs {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 10px;
  color: var(--muted);
  font-size: 11px;
}

.crumbs a {
  color: var(--muted);
}

.crumbs a:hover {
  color: var(--accent);
}

.doc-head {
  min-width: 0;
}

.doc-head .page__title {
  overflow-wrap: anywhere;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 16px;
}

.step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
}

.step:last-child {
  border-bottom: 0;
}

.step__index {
  display: grid;
  place-items: center;
  flex: none;
  width: 24px;
  height: 24px;
  color: var(--muted);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--line);
  border-radius: 50%;
}

.step__pulse {
  width: 7px;
  height: 7px;
  background: var(--accent);
  border-radius: 50%;
  animation: step-pulse 1.4s infinite ease-in-out;
}

@keyframes step-pulse {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(0.78);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.step__text {
  min-width: 0;
}

.step__title {
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
}

.step__desc {
  color: var(--muted);
  font-size: 12px;
}

.step.is-done .step__index {
  color: #fff;
  background: linear-gradient(180deg, #ea6a17, var(--accent));
  border-color: transparent;
  box-shadow: 0 6px 14px -10px var(--accent-glow);
}

.step.is-done .step__title {
  color: var(--ink);
}

.step.is-active .step__index {
  color: var(--accent-strong);
  background: var(--accent-soft);
  border-color: var(--accent-line);
}

.step.is-active .step__title {
  color: var(--accent-strong);
}

.step.is-error .step__index {
  color: var(--danger);
  background: var(--danger-soft);
  border-color: var(--danger-line);
}

.step.is-error .step__title {
  color: var(--danger);
}

.panel__action {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.processing {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--warn);
  font-size: 12px;
}

.field-title {
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 11px;
}

.summary {
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink);
}

.analysis-row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 20px;
}

.inline-empty {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 16px;
}

.inline-empty__title {
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
}

.inline-empty__desc {
  margin-top: 2px;
  font-size: 12px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 104px;
  padding: 10px 13px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 14px;
}

.stat__value {
  color: var(--ink);
  font-size: 16px;
  font-weight: 500;
}

.stat__label {
  color: var(--muted);
  font-size: 11px;
}

.preview {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  padding: 16px;
  color: var(--ink-2);
  font-family: var(--font-sans);
  font-size: 13px;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
}

.note {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

@media (max-width: 680px) {
  .analysis-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
