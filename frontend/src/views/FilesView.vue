<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import BaseModal from '../components/BaseModal.vue'
import ActionMenu from '../components/ActionMenu.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusPill from '../components/StatusPill.vue'
import PaginationBar from '../components/PaginationBar.vue'
import UploadDialog from '../components/UploadDialog.vue'
import { getKnowledgeBase } from '../api/knowledge'
import {
  createFolder,
  deleteFolder,
  folderTree,
  moveFolder,
  renameFolder,
} from '../api/folders'
import {
  deleteDocument,
  downloadDocument,
  getParseStatus,
  listDocuments,
  moveDocument,
  parseDocumentAsync,
  renameDocument,
} from '../api/documents'
import { errorMessage } from '../api/http'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import { askConfirm } from '../stores/confirm'
import { formatBytes, formatDateTime, saveBlob } from '../utils/format'

const route = useRoute()
const router = useRouter()
const workspace = useWorkspaceStore()
const toast = useToastStore()

const kbId = computed(() => Number(route.params.id))

const kb = ref(null)
const tree = ref([])
const loading = ref(true)
const docsLoading = ref(false)
const selectedFolderId = ref(null)
const filters = reactive({ keyword: '', processStatus: '' })

const docs = reactive({
  records: [],
  total: 0,
  page: 1,
  size: 10,
  pages: 1,
})

const uploadOpen = ref(false)
const folderModal = reactive({ open: false, mode: 'create', target: null, name: '' })
const folderError = ref('')
const folderSubmitting = ref(false)
const moveState = reactive({
  open: false,
  kind: 'document',
  target: null,
  targetId: null,
  error: '',
  submitting: false,
})
const busyId = ref(null)
const timers = new Map()
let keywordTimer = null

const flatFolders = computed(() => {
  const out = []
  const walk = (nodes, depth) => {
    nodes.forEach((node) => {
      out.push({ ...node, depth })
      if (node.children?.length) walk(node.children, depth + 1)
    })
  }
  walk(tree.value, 0)
  return out
})

const selectedFolder = computed(
  () => flatFolders.value.find((item) => item.id === selectedFolderId.value) || null
)

function clearTimers() {
  timers.forEach((timer) => window.clearInterval(timer))
  timers.clear()
}

function resetState() {
  clearTimers()
  kb.value = null
  tree.value = []
  docs.records = []
  docs.total = 0
  docs.page = 1
  docs.pages = 1
  selectedFolderId.value = null
  filters.keyword = ''
  filters.processStatus = ''
}

async function loadFolders() {
  tree.value = (await folderTree(kbId.value)) || []
}

async function loadDocs(page = 1) {
  if (!kbId.value) return
  docsLoading.value = true
  try {
    const data = await listDocuments({
      knowledgeBaseId: kbId.value,
      folderId: selectedFolderId.value || undefined,
      keyword: filters.keyword.trim() || undefined,
      processStatus: filters.processStatus || undefined,
      page,
      size: docs.size,
    })
    docs.records = data?.records || []
    docs.total = data?.total || 0
    docs.page = Number(data?.page) || 1
    docs.pages = Number(data?.pages) || 1
    docs.size = Number(data?.size) || docs.size
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    docsLoading.value = false
  }
}

async function loadAll() {
  if (!kbId.value) {
    router.replace({ name: 'knowledge-bases' })
    return
  }
  loading.value = true
  try {
    const [detail] = await Promise.all([getKnowledgeBase(kbId.value), loadFolders()])
    kb.value = detail
    workspace.selectKb(kbId.value)
    await loadDocs(1)
  } catch (error) {
    toast.error(errorMessage(error))
    if (error?.code === 2001) {
      router.replace({ name: 'knowledge-bases' })
    }
  } finally {
    loading.value = false
  }
}

function selectFolder(id) {
  selectedFolderId.value = id
  loadDocs(1)
}

function startKeywordWatch() {
  if (keywordTimer) window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(() => loadDocs(1), 320)
}

function parseAllowed(doc) {
  return doc.processStatus === 'UPLOADED' || doc.processStatus === 'FAILED'
}

function menuItems(doc) {
  return [
    { key: 'open', label: '查看详情', icon: 'arrow-right' },
    { key: 'rename', label: '重命名', icon: 'pencil' },
    { key: 'move', label: '移动到文件夹', icon: 'move' },
    { key: 'download', label: '下载原文件', icon: 'download' },
    { key: 'delete', label: '删除', icon: 'trash', danger: true },
  ]
}

function patchRow(id, patch) {
  docs.records = docs.records.map((item) => (item.id === id ? { ...item, ...patch } : item))
}

function stopPoll(id) {
  const timer = timers.get(id)
  if (timer) {
    window.clearInterval(timer)
    timers.delete(id)
  }
}

function startPoll(id) {
  if (timers.has(id)) return
  const timer = window.setInterval(async () => {
    try {
      const status = await getParseStatus(id)
      patchRow(id, {
        processStatus: status.processStatus,
        errorMessage: status.errorMessage,
      })
      if (status.processStatus === 'SUCCESS') {
        stopPoll(id)
        toast.success('正文解析完成')
        loadDocs(docs.page)
      } else if (status.processStatus === 'FAILED') {
        stopPoll(id)
        toast.error(status.errorMessage || '解析失败')
      }
    } catch {
      stopPoll(id)
    }
  }, 1600)
  timers.set(id, timer)
}

async function startParse(doc) {
  busyId.value = doc.id
  try {
    const result = await parseDocumentAsync(doc.id)
    patchRow(doc.id, { processStatus: result?.processStatus || 'PROCESSING' })
    toast.success('已提交解析，正在处理')
    startPoll(doc.id)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    busyId.value = null
  }
}

async function onMenu(doc, key) {
  if (key === 'open') {
    router.push({ name: 'document-detail', params: { id: doc.id } })
    return
  }
  if (key === 'rename') {
    folderModal.mode = 'rename-doc'
    folderModal.open = true
    folderModal.target = doc
    folderModal.name = doc.originalName
    folderError.value = ''
    return
  }
  if (key === 'move') {
    openMove('document', doc)
    return
  }
  if (key === 'download') {
    await downloadDoc(doc)
    return
  }
  if (key === 'delete') {
    await removeDoc(doc)
  }
}

async function downloadDoc(doc) {
  busyId.value = doc.id
  try {
    const response = await downloadDocument(doc.id)
    saveBlob(response.data, doc.originalName)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    busyId.value = null
  }
}

async function removeDoc(doc) {
  const ok = await askConfirm({
    title: `删除「${doc.originalName}」`,
    message: '会同时删除磁盘上的原文件和已生成的向量分块，删除后不可恢复。',
    confirmText: '删除文档',
    tone: 'danger',
  })
  if (!ok) return

  busyId.value = doc.id
  try {
    await deleteDocument(doc.id)
    toast.success('文档已删除')
    if (docs.records.length === 1 && docs.page > 1) {
      await loadDocs(docs.page - 1)
    } else {
      await loadDocs(docs.page)
    }
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    busyId.value = null
  }
}

function openCreateFolder() {
  folderModal.mode = 'create'
  folderModal.open = true
  folderModal.target = null
  folderModal.name = ''
  folderError.value = ''
}

function openRenameFolder(folder) {
  folderModal.mode = 'rename-folder'
  folderModal.open = true
  folderModal.target = folder
  folderModal.name = folder.name
  folderError.value = ''
}

async function submitFolder() {
  folderError.value = ''
  const name = folderModal.name.trim()
  if (!name) {
    folderError.value = '请填写名称'
    return
  }
  if (name.length > 100) {
    folderError.value = '名称不能超过 100 个字符'
    return
  }

  folderSubmitting.value = true
  try {
    if (folderModal.mode === 'create') {
      await createFolder({
        knowledgeBaseId: kbId.value,
        parentId: selectedFolderId.value || 0,
        name,
      })
      toast.success('文件夹已创建')
    } else if (folderModal.mode === 'rename-folder') {
      await renameFolder(folderModal.target.id, name)
      toast.success('文件夹已重命名')
    } else {
      await renameDocument(folderModal.target.id, name)
      toast.success('文档已重命名')
      await loadDocs(docs.page)
    }
    await loadFolders()
    folderModal.open = false
  } catch (error) {
    folderError.value = errorMessage(error)
  } finally {
    folderSubmitting.value = false
  }
}

async function removeFolder(folder) {
  const ok = await askConfirm({
    title: `删除文件夹「${folder.name}」`,
    message: '文件夹里的文件会移到默认文件夹。存在子文件夹时不能删除。',
    confirmText: '删除文件夹',
    tone: 'danger',
  })
  if (!ok) return

  try {
    await deleteFolder(folder.id)
    toast.success('文件夹已删除')
    if (selectedFolderId.value === folder.id) selectedFolderId.value = null
    await loadFolders()
    await loadDocs(1)
  } catch (error) {
    toast.error(errorMessage(error))
  }
}

function descendantsOf(folder) {
  const ids = new Set([folder.id])
  const walk = (nodes) => {
    nodes.forEach((node) => {
      if (ids.has(node.parentId)) {
        ids.add(node.id)
        if (node.children?.length) walk(node.children)
      } else if (node.children?.length) {
        walk(node.children)
      }
    })
  }
  walk(tree.value)
  return ids
}

const moveOptions = computed(() => {
  if (moveState.kind === 'document') return flatFolders.value
  if (!moveState.target) return flatFolders.value
  const blocked = descendantsOf(moveState.target)
  return flatFolders.value.filter((folder) => !blocked.has(folder.id))
})

function openMove(kind, target) {
  moveState.open = true
  moveState.kind = kind
  moveState.target = target
  moveState.error = ''
  moveState.targetId = kind === 'document' ? target.folderId : target.parentId
}

async function submitMove() {
  moveState.error = ''
  if (moveState.targetId === null || moveState.targetId === undefined) {
    moveState.error = '请选择目标文件夹'
    return
  }

  moveState.submitting = true
  try {
    if (moveState.kind === 'document') {
      await moveDocument(moveState.target.id, moveState.targetId)
      toast.success('文档已移动')
      await loadDocs(docs.page)
    } else {
      await moveFolder(moveState.target.id, moveState.targetId)
      toast.success('文件夹已移动')
      await loadFolders()
      await loadDocs(1)
    }
    moveState.open = false
  } catch (error) {
    moveState.error = errorMessage(error)
  } finally {
    moveState.submitting = false
  }
}

async function onUploaded() {
  await loadDocs(1)
  toast.success('上传完成，下一步解析正文')
}

watch(kbId, () => {
  resetState()
  loadAll()
}, { immediate: true })

watch(
  () => filters.processStatus,
  () => {
    if (!kb.value) return
    loadDocs(1)
  }
)

onBeforeUnmount(() => {
  clearTimers()
  if (keywordTimer) window.clearTimeout(keywordTimer)
})
</script>

<template>
  <div class="page page--wide">
    <nav class="crumbs mono" aria-label="面包屑">
      <RouterLink :to="{ name: 'knowledge-bases' }">知识库</RouterLink>
      <span aria-hidden="true">/</span>
      <span class="truncate">{{ kb?.name || '…' }}</span>
    </nav>

    <header class="page__head">
      <div>
        <h1 class="page__title">{{ kb?.name || '文件管理' }}</h1>
        <p class="page__desc">
          {{ kb?.description || '文件夹、原文件和解析状态都在这里管理。' }}
        </p>
      </div>
      <div class="page__actions">
        <button class="btn" type="button" @click="openCreateFolder">
          <Icon name="folder" :size="15" />
          新建文件夹
        </button>
        <button class="btn btn--primary" type="button" @click="uploadOpen = true">
          <Icon name="upload" :size="15" />
          上传文档
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-block">
      <span class="spinner spinner--lg" />
      正在读取文件夹…
    </div>

    <div v-else class="files-layout">
      <aside class="panel folder-panel">
        <div class="panel__head">
          <span class="panel__title">文件夹</span>
          <button
            class="icon-btn"
            type="button"
            aria-label="新建文件夹"
            title="在当前文件夹下新建"
            @click="openCreateFolder"
          >
            <Icon name="plus" :size="15" />
          </button>
        </div>

        <ul class="folder-list">
          <li>
            <div class="folder-row" :class="{ 'is-active': selectedFolderId === null }">
              <button class="folder-row__main" type="button" @click="selectFolder(null)">
                <Icon name="layers" :size="15" />
                <span class="truncate">全部文件</span>
              </button>
            </div>
          </li>

          <li v-for="folder in flatFolders" :key="folder.id">
            <div
              class="folder-row"
              :class="{ 'is-active': selectedFolderId === folder.id }"
              :style="{ paddingLeft: `${Math.min(folder.depth, 4) * 14}px` }"
            >
              <button class="folder-row__main" type="button" @click="selectFolder(folder.id)">
                <Icon :name="selectedFolderId === folder.id ? 'folder-open' : 'folder'" :size="15" />
                <span class="truncate">{{ folder.name }}</span>
                <span v-if="folder.isDefault" class="chip" style="padding: 0 5px">默认</span>
              </button>

              <div v-if="!folder.isDefault" class="folder-row__tools">
                <button
                  class="icon-btn"
                  type="button"
                  :aria-label="`重命名 ${folder.name}`"
                  title="重命名"
                  @click.stop="openRenameFolder(folder)"
                >
                  <Icon name="pencil" :size="13" />
                </button>
                <button
                  class="icon-btn"
                  type="button"
                  :aria-label="`移动 ${folder.name}`"
                  title="移动"
                  @click.stop="openMove('folder', folder)"
                >
                  <Icon name="move" :size="13" />
                </button>
                <button
                  class="icon-btn icon-btn--danger"
                  type="button"
                  :aria-label="`删除 ${folder.name}`"
                  title="删除"
                  @click.stop="removeFolder(folder)"
                >
                  <Icon name="trash" :size="13" />
                </button>
              </div>
            </div>
          </li>
        </ul>

        <p class="folder-note">
          新建文件夹会放在
          <strong>{{ selectedFolder?.name || '根目录' }}</strong>
          下。
        </p>
      </aside>

      <section class="panel doc-panel">
        <div class="doc-toolbar">
          <div class="search-input">
            <Icon name="search" :size="15" />
            <input
              v-model="filters.keyword"
              class="input"
              type="search"
              placeholder="按文件名筛选"
              aria-label="按文件名筛选"
              @input="startKeywordWatch"
            />
          </div>

          <select
            v-model="filters.processStatus"
            class="select select--compact"
            aria-label="按解析状态筛选"
          >
            <option value="">全部状态</option>
            <option value="UPLOADED">待解析</option>
            <option value="PROCESSING">解析中</option>
            <option value="SUCCESS">已解析</option>
            <option value="FAILED">解析失败</option>
          </select>

          <button
            class="icon-btn"
            type="button"
            aria-label="刷新列表"
            title="刷新"
            @click="loadDocs(docs.page)"
          >
            <Icon name="refresh" :size="15" />
          </button>

          <span class="spacer" />
          <span class="mono muted">
            {{ selectedFolder?.name || '全部文件' }} · {{ docs.total }} 个文件
          </span>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>文件名</th>
                <th>解析状态</th>
                <th>大小</th>
                <th>上传时间</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="docsLoading">
                <td colspan="5">
                  <div class="loading-block">
                    <span class="spinner" />
                    正在加载文件…
                  </div>
                </td>
              </tr>

              <tr v-else-if="!docs.records.length">
                <td colspan="5">
                  <EmptyState
                    icon="file-text"
                    :title="filters.keyword || filters.processStatus ? '没有匹配的文件' : '这个文件夹还是空的'"
                    :description="
                      filters.keyword || filters.processStatus
                        ? '换一个关键词，或清空筛选条件。'
                        : '上传 PDF、DOCX、TXT 或 Markdown，然后解析正文。'
                    "
                  >
                    <button class="btn btn--primary" type="button" @click="uploadOpen = true">
                      <Icon name="upload" :size="15" />
                      上传文档
                    </button>
                  </EmptyState>
                </td>
              </tr>

              <template v-else>
                <tr v-for="doc in docs.records" :key="doc.id">
                <td>
                  <div class="file-cell">
                    <span class="type-badge mono">{{ (doc.fileType || '?').toUpperCase() }}</span>
                    <div class="file-cell__text">
                      <RouterLink
                        class="file-cell__name"
                        :to="{ name: 'document-detail', params: { id: doc.id } }"
                      >
                        {{ doc.originalName }}
                      </RouterLink>
                      <span v-if="doc.errorMessage" class="file-cell__meta error-text">
                        {{ doc.errorMessage }}
                      </span>
                    </div>
                  </div>
                </td>
                <td><StatusPill :status="doc.processStatus" /></td>
                <td class="num">{{ formatBytes(doc.fileSize) }}</td>
                <td class="num">{{ formatDateTime(doc.createTime) }}</td>
                <td class="col-actions">
                  <div class="row-actions">
                    <button
                      v-if="parseAllowed(doc)"
                      class="btn btn--sm"
                      type="button"
                      :disabled="busyId === doc.id"
                      @click="startParse(doc)"
                    >
                      <Icon name="refresh" :size="13" />
                      {{ doc.processStatus === 'FAILED' ? '重试' : '解析' }}
                    </button>
                    <ActionMenu :items="menuItems(doc)" @select="onMenu(doc, $event)" />
                  </div>
                </td>
              </tr>
              </template>
            </tbody>
          </table>
        </div>

        <PaginationBar
          v-if="docs.total > docs.size"
          :page="docs.page"
          :pages="docs.pages"
          :total="docs.total"
          :size="docs.size"
          unit="个文件"
          @change="loadDocs"
        />
      </section>
    </div>

    <UploadDialog
      :open="uploadOpen"
      :knowledge-base-id="kbId"
      :folders="flatFolders"
      :default-folder-id="selectedFolderId"
      @close="uploadOpen = false"
      @uploaded="onUploaded"
    />

    <BaseModal
      :open="folderModal.open"
      :title="
        folderModal.mode === 'create'
          ? '新建文件夹'
          : folderModal.mode === 'rename-folder'
            ? '重命名文件夹'
            : '重命名文档'
      "
      @close="folderModal.open = false"
    >
      <form class="form-stack" @submit.prevent="submitFolder">
        <label class="field">
          <span class="label">名称<span class="label__req">*</span></span>
          <input
            v-model="folderModal.name"
            class="input"
            type="text"
            maxlength="100"
            :disabled="folderSubmitting"
          />
        </label>
        <p v-if="folderError" class="form-error" role="alert">{{ folderError }}</p>
      </form>

      <template #footer>
        <button class="btn" type="button" :disabled="folderSubmitting" @click="folderModal.open = false">
          取消
        </button>
        <button class="btn btn--primary" type="button" :disabled="folderSubmitting" @click="submitFolder">
          <span v-if="folderSubmitting" class="spinner" />
          保存
        </button>
      </template>
    </BaseModal>

    <BaseModal
      :open="moveState.open"
      :title="moveState.kind === 'document' ? '移动文档' : '移动文件夹'"
      :description="`目标：${moveState.target?.originalName || moveState.target?.name || ''}`"
      @close="moveState.open = false"
    >
      <div class="form-stack">
        <label class="field">
          <span class="label">目标文件夹</span>
          <select v-model="moveState.targetId" class="select">
            <option v-if="moveState.kind === 'folder'" :value="0">根目录</option>
            <option v-for="folder in moveOptions" :key="folder.id" :value="folder.id">
              {{ '　'.repeat(folder.depth) }}{{ folder.name }}
            </option>
          </select>
        </label>
        <p v-if="moveState.error" class="form-error" role="alert">{{ moveState.error }}</p>
      </div>

      <template #footer>
        <button class="btn" type="button" :disabled="moveState.submitting" @click="moveState.open = false">
          取消
        </button>
        <button class="btn btn--primary" type="button" :disabled="moveState.submitting" @click="submitMove">
          <span v-if="moveState.submitting" class="spinner" />
          移动
        </button>
      </template>
    </BaseModal>
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

.files-layout {
  display: grid;
  grid-template-columns: 244px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.folder-panel {
  position: sticky;
  top: var(--gap);
  overflow: hidden;
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 6px;
  max-height: 56vh;
  overflow: auto;
}

.folder-row {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-right: 4px;
  border-radius: var(--radius-xs);
}

.folder-row:hover {
  background: rgba(255, 255, 255, 0.52);
}

.folder-row.is-active {
  background: var(--accent-soft);
  box-shadow: inset 0 0 0 1px var(--accent-line);
}

.folder-row__main {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 1;
  min-width: 0;
  padding: 7px 6px;
  color: var(--ink-2);
  font-size: 12px;
  text-align: left;
  background: none;
  border: 0;
}

.folder-row.is-active .folder-row__main {
  color: var(--accent-strong);
  font-weight: 500;
}

.folder-row__tools {
  display: flex;
  gap: 0;
  opacity: 0;
  transition: opacity 120ms var(--ease);
}

.folder-row:hover .folder-row__tools,
.folder-row:focus-within .folder-row__tools {
  opacity: 1;
}

.folder-row__tools .icon-btn {
  width: 24px;
  height: 24px;
}

.folder-note {
  padding: 10px 14px 14px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.6;
  border-top: 1px solid var(--line);
}

.folder-note strong {
  color: var(--ink-2);
  font-weight: 500;
}

.doc-panel {
  min-width: 0;
  overflow: hidden;
}

.doc-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
}

.search-input {
  position: relative;
  width: min(280px, 100%);
  color: var(--muted);
}

.search-input svg {
  position: absolute;
  top: 50%;
  left: 10px;
  transform: translateY(-50%);
  pointer-events: none;
}

.search-input .input {
  padding-left: 32px;
}

.select--compact {
  width: auto;
  min-width: 108px;
}

.file-cell__text {
  min-width: 0;
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
  letter-spacing: 0.02em;
  background: var(--accent-soft);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

@media (max-width: 1080px) {
  .files-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .folder-panel {
    position: static;
  }

  .folder-list {
    max-height: none;
  }

  .folder-row__tools {
    opacity: 1;
  }
}

@media (max-width: 560px) {
  .doc-toolbar {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }

  .doc-toolbar .spacer {
    display: none;
  }
}

@media (hover: none) {
  .folder-row__tools {
    opacity: 1;
  }
}
</style>
