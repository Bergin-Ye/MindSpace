<script setup>
import { computed, reactive, ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'
import Icon from './Icon.vue'
import { uploadDocument } from '../api/documents'
import { errorMessage } from '../api/http'
import { formatBytes } from '../utils/format'

const props = defineProps({
  open: { type: Boolean, default: false },
  knowledgeBaseId: { type: Number, default: null },
  folders: { type: Array, default: () => [] },
  defaultFolderId: { type: Number, default: null },
})

const emit = defineEmits(['close', 'uploaded'])

const ACCEPT = ['pdf', 'docx', 'txt', 'md']
const MAX_SIZE = 20 * 1024 * 1024

const fileInput = ref(null)
const folderId = ref(null)
const items = ref([])
const dragging = ref(false)
const uploading = ref(false)
const notice = ref('')
const seed = ref(0)

const pendingCount = computed(
  () => items.value.filter((item) => item.status === 'ready').length
)
const successCount = computed(
  () => items.value.filter((item) => item.status === 'done').length
)
const allDone = computed(
  () => items.value.length > 0 && items.value.every((item) => item.status === 'done')
)

watch(
  () => props.open,
  (open) => {
    if (open) {
      folderId.value = props.defaultFolderId ?? null
      items.value = []
      notice.value = ''
      uploading.value = false
    }
  }
)

function extensionOf(name) {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index + 1).toLowerCase() : ''
}

function addFiles(fileList) {
  notice.value = ''
  const rejected = []

  Array.from(fileList).forEach((file) => {
    const ext = extensionOf(file.name)
    if (!ACCEPT.includes(ext)) {
      rejected.push(`${file.name}：不支持的类型`)
      return
    }
    if (file.size > MAX_SIZE) {
      rejected.push(`${file.name}：超过 20MB`)
      return
    }
    if (file.size === 0) {
      rejected.push(`${file.name}：文件内容为空`)
      return
    }
    if (items.value.some((item) => item.file.name === file.name && item.file.size === file.size)) {
      rejected.push(`${file.name}：已在列表中`)
      return
    }
    seed.value += 1
    items.value.push({
      id: seed.value,
      file,
      progress: 0,
      status: 'ready',
      message: '',
    })
  })

  if (rejected.length) {
    notice.value = rejected.join('；')
  }
  if (fileInput.value) fileInput.value.value = ''
}

function onPick(event) {
  addFiles(event.target.files)
}

function onDrop(event) {
  dragging.value = false
  if (uploading.value) return
  addFiles(event.dataTransfer.files)
}

function removeItem(id) {
  if (uploading.value) return
  items.value = items.value.filter((item) => item.id !== id)
}

function updateItem(id, patch) {
  const target = items.value.find((item) => item.id === id)
  if (target) Object.assign(target, patch)
}

async function start() {
  if (!props.knowledgeBaseId) {
    notice.value = '没有可用的知识库'
    return
  }
  if (!pendingCount.value || uploading.value) return

  uploading.value = true
  notice.value = ''

  const queue = items.value.filter((item) => item.status === 'ready')
  for (const item of queue) {
    updateItem(item.id, { status: 'uploading', progress: 0 })
    const data = new FormData()
    data.append('knowledgeBaseId', String(props.knowledgeBaseId))
    if (folderId.value) data.append('folderId', String(folderId.value))
    data.append('file', item.file)

    try {
      await uploadDocument(data, (progress) => {
        updateItem(item.id, { progress })
      })
      updateItem(item.id, { status: 'done', progress: 100, message: '' })
    } catch (error) {
      updateItem(item.id, {
        status: 'error',
        message: errorMessage(error),
      })
    }
  }

  uploading.value = false
  if (successCount.value > 0) {
    emit('uploaded')
  }
}

function close() {
  if (uploading.value) return
  emit('close')
}
</script>

<template>
  <BaseModal
    :open="open"
    title="上传文档"
    description="支持 PDF、DOCX、TXT、Markdown，单个文件最大 20MB。上传后需要解析正文。"
    wide
    @close="close"
  >
    <div class="form-stack">
      <label class="field">
        <span class="label">目标文件夹</span>
        <select v-model="folderId" class="select" :disabled="uploading">
          <option :value="null">未分类（默认文件夹）</option>
          <option v-for="folder in folders" :key="folder.id" :value="folder.id">
            {{ '　'.repeat(folder.depth) }}{{ folder.name }}
          </option>
        </select>
      </label>

      <label
        class="dropzone"
        :class="{ 'is-over': dragging, 'is-disabled': uploading }"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <Icon name="upload" :size="20" />
        <span class="dropzone__title">拖入文件，或点击选择</span>
        <span class="mono muted">PDF · DOCX · TXT · MD · 单个不超过 20MB</span>
        <input
          ref="fileInput"
          class="sr-only"
          type="file"
          multiple
          accept=".pdf,.docx,.txt,.md"
          :disabled="uploading"
          @change="onPick"
        />
      </label>

      <p v-if="notice" class="error-text" role="alert">{{ notice }}</p>

      <ul v-if="items.length" class="upload-list">
        <li v-for="item in items" :key="item.id" class="upload-item">
          <span class="upload-item__icon">
            <Icon name="file" :size="16" />
          </span>
          <div class="upload-item__body">
            <div class="upload-item__top">
              <span class="truncate">{{ item.file.name }}</span>
              <span class="mono muted">{{ formatBytes(item.file.size) }}</span>
            </div>
            <div class="upload-item__bar">
              <span
                class="upload-item__fill"
                :class="`is-${item.status}`"
                :style="{ width: `${item.status === 'ready' ? 0 : item.progress}%` }"
              />
            </div>
            <p v-if="item.status === 'error'" class="error-text">{{ item.message }}</p>
          </div>
          <span class="upload-item__state mono">
            <template v-if="item.status === 'ready'">等待</template>
            <template v-else-if="item.status === 'uploading'">{{ item.progress }}%</template>
            <template v-else-if="item.status === 'done'">完成</template>
            <template v-else>失败</template>
          </span>
          <button
            v-if="item.status !== 'uploading' && item.status !== 'done'"
            class="icon-btn"
            type="button"
            :aria-label="`移除 ${item.file.name}`"
            :disabled="uploading"
            @click="removeItem(item.id)"
          >
            <Icon name="x" :size="14" />
          </button>
        </li>
      </ul>

      <p v-if="allDone" class="hint">
        全部上传完成。关闭后可以继续解析文档，或批量提交异步解析。
      </p>
    </div>

    <template #footer>
      <button class="btn" type="button" :disabled="uploading" @click="close">
        {{ allDone ? '完成' : '取消' }}
      </button>
      <button
        v-if="!allDone"
        class="btn btn--primary"
        type="button"
        :disabled="!pendingCount || uploading"
        @click="start"
      >
        <span v-if="uploading" class="spinner" />
        {{ uploading ? '上传中' : `上传 ${pendingCount} 个文件` }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 30px 16px;
  color: var(--ink-2);
  text-align: center;
  background: rgba(255, 255, 255, 0.5);
  border: 1px dashed rgba(60, 60, 67, 0.18);
  border-radius: 18px;
  cursor: pointer;
  transition: border-color 160ms var(--ease), background 160ms var(--ease);
}

.dropzone:hover,
.dropzone.is-over {
  background: rgba(255, 255, 255, 0.78);
  border-color: var(--accent);
}

.dropzone.is-disabled {
  pointer-events: none;
  opacity: 0.6;
}

.dropzone__title {
  font-size: 13px;
  font-weight: 500;
}

.upload-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 11px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

.upload-item__icon {
  color: var(--muted);
}

.upload-item__body {
  flex: 1;
  min-width: 0;
}

.upload-item__top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.upload-item__bar {
  height: 3px;
  margin-top: 6px;
  overflow: hidden;
  background: rgba(120, 120, 128, 0.16);
  border-radius: 2px;
}

.upload-item__fill {
  display: block;
  height: 100%;
  background: var(--accent);
  transition: width 200ms var(--ease);
}

.upload-item__fill.is-error {
  background: var(--danger);
}

.upload-item__state {
  flex: none;
  color: var(--muted);
  font-size: 11px;
}
</style>
