<script setup>
import { computed, reactive, ref, watch } from 'vue'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusPill from '../components/StatusPill.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { searchDocuments, vectorSearch } from '../api/documents'
import { errorMessage } from '../api/http'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import {
  AI_CATEGORIES,
  FILE_TYPES,
  clip,
  formatBytes,
  formatDateTime,
  formatScore,
  highlightParts,
} from '../utils/format'

const workspace = useWorkspaceStore()
const toast = useToastStore()

const kb = computed(() => workspace.currentKb)

const mode = ref('keyword')
const loading = ref(false)
const notice = ref('')
const expanded = ref(new Set())

const keywordForm = reactive({
  keyword: '',
  fileType: '',
  processStatus: '',
  aiCategory: '',
  startTime: '',
  endTime: '',
  sort: 'relevance',
})

const semanticForm = reactive({
  query: '',
  topK: 5,
})

const keywordResult = reactive({
  records: [],
  total: 0,
  page: 1,
  size: 10,
  pages: 1,
})

const semanticResult = ref([])
const searched = reactive({ keyword: false, semantic: false })
const lastKeyword = ref('')

function resetResults() {
  keywordResult.records = []
  keywordResult.total = 0
  keywordResult.page = 1
  keywordResult.pages = 1
  semanticResult.value = []
  searched.keyword = false
  searched.semantic = false
  notice.value = ''
  expanded.value = new Set()
}

async function runKeyword(page = 1) {
  notice.value = ''
  if (!kb.value) return

  if (keywordForm.startTime && keywordForm.endTime && keywordForm.startTime > keywordForm.endTime) {
    notice.value = '开始时间不能晚于结束时间'
    return
  }

  loading.value = true
  try {
    const data = await searchDocuments({
      knowledgeBaseId: kb.value.id,
      keyword: keywordForm.keyword.trim() || undefined,
      fileType: keywordForm.fileType || undefined,
      processStatus: keywordForm.processStatus || undefined,
      aiCategory: keywordForm.aiCategory || undefined,
      startTime: keywordForm.startTime ? `${keywordForm.startTime}T00:00:00` : undefined,
      endTime: keywordForm.endTime ? `${keywordForm.endTime}T23:59:59` : undefined,
      sort: keywordForm.keyword.trim() ? keywordForm.sort : undefined,
      page,
      size: 10,
    })

    keywordResult.records = data?.records || []
    keywordResult.total = data?.total || 0
    keywordResult.page = Number(data?.page) || 1
    keywordResult.pages = Number(data?.pages) || 1
    keywordResult.size = Number(data?.size) || 10
    lastKeyword.value = keywordForm.keyword.trim()
    searched.keyword = true
  } catch (error) {
    notice.value = errorMessage(error)
    toast.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function runSemantic() {
  notice.value = ''
  if (!kb.value) return
  if (!semanticForm.query.trim()) {
    notice.value = '请输入要检索的问题或描述'
    return
  }

  loading.value = true
  try {
    const data = await vectorSearch({
      knowledgeBaseId: kb.value.id,
      query: semanticForm.query.trim(),
      topK: semanticForm.topK,
    })
    semanticResult.value = Array.isArray(data) ? data : []
    searched.semantic = true
  } catch (error) {
    notice.value = errorMessage(error)
    toast.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function submit() {
  if (mode.value === 'keyword') runKeyword(1)
  else runSemantic()
}

function toggleExpand(id) {
  const next = new Set(expanded.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expanded.value = next
}

function contentOf(item) {
  if (expanded.value.has(item.chunkId)) return item.content
  return clip(item.content, 260)
}

watch(
  () => kb.value?.id,
  () => resetResults()
)
</script>

<template>
  <div class="page">
    <header class="page__head">
      <div>
        <h1 class="page__title">检索</h1>
        <p class="page__desc">
          关键词检索匹配文件名、正文和 AI 摘要；语义检索按意思找段落，需要先向量化文档。
        </p>
      </div>
      <div class="page__actions">
        <div class="segmented" role="tablist" aria-label="检索方式">
          <button
            class="segmented__btn"
            :class="{ 'is-active': mode === 'keyword' }"
            type="button"
            role="tab"
            :aria-selected="mode === 'keyword'"
            @click="mode = 'keyword'"
          >
            关键词
          </button>
          <button
            class="segmented__btn"
            :class="{ 'is-active': mode === 'semantic' }"
            type="button"
            role="tab"
            :aria-selected="mode === 'semantic'"
            @click="mode = 'semantic'"
          >
            语义
          </button>
        </div>
      </div>
    </header>

    <div v-if="!workspace.loaded" class="loading-block">
      <span class="spinner" />
      正在读取知识库…
    </div>

    <EmptyState
      v-else-if="!kb"
      icon="grid"
      title="还没有可检索的知识库"
      description="先在知识库页创建一个知识库，再上传并解析文档。"
    >
      <RouterLink class="btn btn--primary" :to="{ name: 'knowledge-bases' }">
        去创建知识库
      </RouterLink>
    </EmptyState>

    <template v-else>
      <section class="panel search-panel">
        <form v-if="mode === 'keyword'" class="search-form" @submit.prevent="submit">
          <div class="search-main">
            <div class="search-input">
              <Icon name="search" :size="16" />
              <input
                v-model="keywordForm.keyword"
                class="input"
                type="search"
                placeholder="搜索文件名、正文或摘要"
                aria-label="搜索关键词"
                maxlength="100"
              />
            </div>
            <button class="btn btn--primary" type="submit" :disabled="loading">
              <span v-if="loading" class="spinner" />
              <Icon v-else name="search" :size="15" />
              搜索
            </button>
          </div>

          <div class="filter-grid">
            <label class="field">
              <span class="label">文件类型</span>
              <select v-model="keywordForm.fileType" class="select">
                <option value="">全部类型</option>
                <option v-for="type in FILE_TYPES" :key="type" :value="type">
                  {{ type.toUpperCase() }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="label">解析状态</span>
              <select v-model="keywordForm.processStatus" class="select">
                <option value="">全部状态</option>
                <option value="UPLOADED">待解析</option>
                <option value="PROCESSING">解析中</option>
                <option value="SUCCESS">已解析</option>
                <option value="FAILED">解析失败</option>
              </select>
            </label>

            <label class="field">
              <span class="label">AI 分类</span>
              <select v-model="keywordForm.aiCategory" class="select">
                <option value="">全部分类</option>
                <option v-for="category in AI_CATEGORIES" :key="category" :value="category">
                  {{ category }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="label">开始日期</span>
              <input v-model="keywordForm.startTime" class="input" type="date" />
            </label>

            <label class="field">
              <span class="label">结束日期</span>
              <input v-model="keywordForm.endTime" class="input" type="date" />
            </label>

            <label class="field">
              <span class="label">排序</span>
              <select
                v-model="keywordForm.sort"
                class="select"
                :disabled="!keywordForm.keyword.trim()"
              >
                <option value="relevance">相关度</option>
                <option value="createTime">上传时间</option>
              </select>
              <span class="hint">没有关键词时按上传时间倒序。</span>
            </label>
          </div>
        </form>

        <form v-else class="search-form" @submit.prevent="submit">
          <div class="search-main">
            <div class="search-input">
              <Icon name="search" :size="16" />
              <input
                v-model="semanticForm.query"
                class="input"
                type="search"
                placeholder="例如：自动配置的生效条件是什么"
                aria-label="语义检索问题"
              />
            </div>
            <select v-model.number="semanticForm.topK" class="select select--topk" aria-label="返回数量">
              <option :value="5">前 5 段</option>
              <option :value="10">前 10 段</option>
              <option :value="20">前 20 段</option>
            </select>
            <button class="btn btn--primary" type="submit" :disabled="loading">
              <span v-if="loading" class="spinner" />
              <Icon v-else name="sparkle" :size="15" />
              语义检索
            </button>
          </div>
          <p class="hint">
            先用自然语言描述想找的内容。只检索当前知识库中已完成解析和向量化的文档。
          </p>
        </form>

        <p v-if="notice" class="form-error" role="alert" style="margin: 0 16px 16px">
          {{ notice }}
        </p>
      </section>

      <section v-if="mode === 'keyword'" class="panel results-panel">
        <div class="panel__head">
          <span class="panel__title">
            结果
            <span v-if="searched.keyword" class="mono muted" style="font-weight: 400">
              {{ keywordResult.total }} 条
            </span>
          </span>
          <span class="mono muted">{{ kb.name }}</span>
        </div>

        <div v-if="loading" class="loading-block">
          <span class="spinner" />
          正在搜索…
        </div>

        <EmptyState
          v-else-if="!searched.keyword"
          icon="search"
          title="输入关键词开始检索"
          description="可以只按筛选条件查询，查看某个时间段或某个状态下上传的文档。"
        />

        <EmptyState
          v-else-if="!keywordResult.records.length"
          icon="search"
          title="没有匹配的文档"
          description="换一个关键词，或放宽文件类型、状态和时间范围。"
        />

        <template v-else>
          <ul class="result-list">
            <li v-for="item in keywordResult.records" :key="item.id" class="result">
              <span class="type-badge mono">{{ (item.fileType || '?').toUpperCase() }}</span>

              <div class="result__body">
                <div class="result__top">
                  <RouterLink
                    class="result__title"
                    :to="{ name: 'document-detail', params: { id: item.id } }"
                  >
                    <template v-for="(part, index) in highlightParts(item.originalName, lastKeyword)" :key="index">
                      <mark v-if="part.hit">{{ part.text }}</mark>
                      <template v-else>{{ part.text }}</template>
                    </template>
                  </RouterLink>
                  <StatusPill :status="item.processStatus" />
                  <span v-if="item.aiCategory" class="chip">{{ item.aiCategory }}</span>
                </div>

                <p v-if="item.textSnippet" class="result__snippet">
                  <template v-for="(part, index) in highlightParts(item.textSnippet, lastKeyword)" :key="index">
                    <mark v-if="part.hit">{{ part.text }}</mark>
                    <template v-else>{{ part.text }}</template>
                  </template>
                </p>

                <div class="result__meta mono">
                  <span>{{ formatBytes(item.fileSize) }}</span>
                  <span>{{ formatDateTime(item.createTime) }}</span>
                </div>
              </div>

              <RouterLink
                class="icon-btn"
                :to="{ name: 'document-detail', params: { id: item.id } }"
                :aria-label="`打开 ${item.originalName}`"
              >
                <Icon name="arrow-right" :size="15" />
              </RouterLink>
            </li>
          </ul>

          <PaginationBar
            :page="keywordResult.page"
            :pages="keywordResult.pages"
            :total="keywordResult.total"
            :size="keywordResult.size"
            unit="条"
            @change="runKeyword"
          />
        </template>
      </section>

      <section v-else class="panel results-panel">
        <div class="panel__head">
          <span class="panel__title">
            语义结果
            <span v-if="searched.semantic" class="mono muted" style="font-weight: 400">
              {{ semanticResult.length }} 段
            </span>
          </span>
          <span class="mono muted">余弦相似度</span>
        </div>

        <div v-if="loading" class="loading-block">
          <span class="spinner" />
          正在把问题转成向量并比对…
        </div>

        <EmptyState
          v-else-if="!searched.semantic"
          icon="sparkle"
          title="用一句话描述你要找的内容"
          description="语义检索会把问题和文档分块都转成向量，再按相似度排序。"
        />

        <EmptyState
          v-else-if="!semanticResult.length"
          icon="sparkle"
          title="没有命中任何分块"
          description="确认相关文档已完成解析和向量化，或换一种说法再试。"
        />

        <ul v-else class="result-list">
          <li v-for="(item, index) in semanticResult" :key="item.chunkId" class="result">
            <span class="chunk-index mono">{{ index + 1 }}</span>

            <div class="result__body">
              <div class="result__top">
                <RouterLink
                  class="result__title"
                  :to="{ name: 'document-detail', params: { id: item.documentId } }"
                >
                  {{ item.originalName }}
                </RouterLink>
                <span class="chip mono">第 {{ item.chunkIndex + 1 }} 块</span>
              </div>

              <p class="result__snippet result__snippet--chunk">{{ contentOf(item) }}</p>

              <div class="result__meta">
                <button
                  v-if="item.content.length > 260"
                  class="link-btn"
                  type="button"
                  @click="toggleExpand(item.chunkId)"
                >
                  {{ expanded.has(item.chunkId) ? '收起' : '展开全文' }}
                </button>
                <span class="spacer" />
                <span class="score" :title="`相似度 ${formatScore(item.score)}`">
                  <span class="score__bar">
                    <span class="score__fill" :style="{ width: `${Math.max(0, Math.min(1, item.score)) * 100}%` }" />
                  </span>
                  <span class="mono">{{ formatScore(item.score) }}</span>
                </span>
              </div>
            </div>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
.search-panel {
  margin-bottom: 16px;
  overflow: hidden;
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
}

.search-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-main .search-input {
  flex: 1;
}

.search-input {
  position: relative;
  color: var(--muted);
}

.search-input svg {
  position: absolute;
  top: 50%;
  left: 11px;
  transform: translateY(-50%);
  pointer-events: none;
}

.search-input .input {
  min-height: 40px;
  padding-left: 34px;
}

.select--topk {
  width: auto;
  min-width: 110px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.results-panel {
  overflow: hidden;
}

.result-list {
  display: flex;
  flex-direction: column;
}

.result {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--line);
  transition: background 120ms var(--ease);
}

.result:hover {
  background: rgba(255, 255, 255, 0.5);
}

.result:last-child {
  border-bottom: 0;
}

.type-badge {
  display: grid;
  place-items: center;
  flex: none;
  width: 38px;
  height: 38px;
  color: var(--accent-strong);
  font-size: 10px;
  font-weight: 700;
  background: var(--accent-soft);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

.chunk-index {
  display: grid;
  place-items: center;
  flex: none;
  width: 24px;
  height: 24px;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  background: var(--accent);
  border-radius: 50%;
}

.result__body {
  flex: 1;
  min-width: 0;
}

.result__top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.result__title {
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.result__title:hover {
  color: var(--accent);
}

.result__snippet {
  margin-top: 6px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.75;
  overflow-wrap: anywhere;
}

.result__snippet--chunk {
  white-space: pre-wrap;
}

.result__meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
  color: var(--muted);
  font-size: 11px;
}

.link-btn {
  padding: 0;
  color: var(--accent);
  font-size: 11px;
  background: none;
  border: 0;
}

.link-btn:hover {
  text-decoration: underline;
}

.score {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-2);
}

.score__bar {
  display: block;
  width: 72px;
  height: 4px;
  overflow: hidden;
  background: var(--surface-3);
  border-radius: 2px;
}

.score__fill {
  display: block;
  height: 100%;
  background: var(--accent);
}

@media (max-width: 1080px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-main {
    flex-wrap: wrap;
  }

  .search-main .search-input {
    flex-basis: 100%;
  }
}
</style>
