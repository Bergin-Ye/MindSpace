<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import BaseModal from '../components/BaseModal.vue'
import EmptyState from '../components/EmptyState.vue'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import { askConfirm } from '../stores/confirm'
import { createKnowledgeBase, deleteKnowledgeBase, updateKnowledgeBase } from '../api/knowledge'
import { errorMessage } from '../api/http'
import { formatDate } from '../utils/format'

const router = useRouter()
const workspace = useWorkspaceStore()
const toast = useToastStore()

const loading = ref(true)
const submitting = ref(false)
const deletingId = ref(null)

const modalOpen = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', description: '' })
const formError = ref('')

const list = computed(() => workspace.knowledgeBases)

function initialOf(name) {
  return (name || 'K').trim().slice(0, 1).toUpperCase()
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.description = ''
  formError.value = ''
  modalOpen.value = true
}

function openEdit(kb) {
  editingId.value = kb.id
  form.name = kb.name
  form.description = kb.description || ''
  formError.value = ''
  modalOpen.value = true
}

async function submit() {
  formError.value = ''
  const name = form.name.trim()
  if (!name) {
    formError.value = '请填写知识库名称'
    return
  }
  if (name.length > 100) {
    formError.value = '名称不能超过 100 个字符'
    return
  }
  if (form.description.length > 500) {
    formError.value = '描述不能超过 500 个字符'
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateKnowledgeBase(editingId.value, {
        name,
        description: form.description.trim(),
      })
      toast.success('知识库已更新')
    } else {
      const created = await createKnowledgeBase({
        name,
        description: form.description.trim(),
      })
      toast.success('知识库已创建')
      if (created?.id) workspace.selectKb(created.id)
    }
    await workspace.refresh()
    modalOpen.value = false
  } catch (error) {
    formError.value = errorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function remove(kb) {
  const ok = await askConfirm({
    title: `删除「${kb.name}」`,
    message: '知识库下的文件、文件夹和向量数据会一并不可见。至少需要保留一个知识库。',
    confirmText: '删除知识库',
    tone: 'danger',
  })
  if (!ok) return

  deletingId.value = kb.id
  try {
    await deleteKnowledgeBase(kb.id)
    toast.success('知识库已删除')
    await workspace.refresh()
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    deletingId.value = null
  }
}

function openKb(kb) {
  workspace.selectKb(kb.id)
  router.push({ name: 'kb-files', params: { id: kb.id } })
}

onMounted(async () => {
  try {
    await workspace.load(true)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <header class="page__head">
      <div>
        <h1 class="page__title">知识库</h1>
        <p class="page__desc">
          一个知识库放一类资料。解析、向量化、检索和问答都以知识库为单位。
        </p>
      </div>
      <div class="page__actions">
        <span class="mono muted">{{ list.length }} 个知识库</span>
        <button class="btn btn--primary" type="button" @click="openCreate">
          <Icon name="plus" :size="15" />
          新建知识库
        </button>
      </div>
    </header>

    <div v-if="loading" class="kb-grid">
      <div v-for="index in 4" :key="index" class="kb-card kb-card--skeleton">
        <div class="skeleton" style="height: 100%" />
      </div>
    </div>

    <EmptyState
      v-else-if="!list.length"
      icon="grid"
      title="还没有知识库"
      description="新建一个知识库，然后上传 PDF、DOCX、TXT 或 Markdown 文档。"
    >
      <button class="btn btn--primary" type="button" @click="openCreate">
        <Icon name="plus" :size="15" />
        新建知识库
      </button>
    </EmptyState>

    <div v-else class="kb-grid">
      <article
        v-for="kb in list"
        :key="kb.id"
        class="kb-card"
        @click="openKb(kb)"
      >
        <div class="kb-card__spine" aria-hidden="true">{{ initialOf(kb.name) }}</div>
        <div class="kb-card__body">
          <h2 class="kb-card__title">
            <RouterLink
              :to="{ name: 'kb-files', params: { id: kb.id } }"
              @click.stop
            >
              {{ kb.name }}
            </RouterLink>
          </h2>
          <p class="kb-card__desc">{{ kb.description || '暂无描述' }}</p>

          <div class="kb-card__foot">
            <span class="mono muted">创建于 {{ formatDate(kb.createTime) }}</span>
            <div class="row-actions">
              <button
                class="icon-btn"
                type="button"
                :aria-label="`重命名 ${kb.name}`"
                title="重命名"
                @click.stop="openEdit(kb)"
              >
                <Icon name="pencil" :size="15" />
              </button>
              <button
                class="icon-btn icon-btn--danger"
                type="button"
                :aria-label="`删除 ${kb.name}`"
                :title="list.length <= 1 ? '至少保留一个知识库' : '删除'"
                :disabled="list.length <= 1 || deletingId === kb.id"
                @click.stop="remove(kb)"
              >
                <Icon name="trash" :size="15" />
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <BaseModal
      :open="modalOpen"
      :title="editingId ? '重命名知识库' : '新建知识库'"
      :description="editingId ? '名称和描述会立即更新。' : '创建后可以继续上传文档。'"
      @close="modalOpen = false"
    >
      <form class="form-stack" @submit.prevent="submit">
        <label class="field">
          <span class="label">名称<span class="label__req">*</span></span>
          <input
            v-model="form.name"
            class="input"
            type="text"
            maxlength="100"
            placeholder="例如：Java 学习"
            :disabled="submitting"
          />
        </label>

        <label class="field">
          <span class="label">描述</span>
          <textarea
            v-model="form.description"
            class="textarea"
            maxlength="500"
            placeholder="这个知识库放什么资料"
            :disabled="submitting"
          />
          <span class="hint">{{ form.description.length }} / 500</span>
        </label>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      </form>

      <template #footer>
        <button class="btn" type="button" :disabled="submitting" @click="modalOpen = false">
          取消
        </button>
        <button class="btn btn--primary" type="button" :disabled="submitting" @click="submit">
          <span v-if="submitting" class="spinner" />
          {{ editingId ? '保存' : '创建' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.kb-card {
  display: flex;
  gap: 14px;
  min-height: 150px;
  padding: 18px;
  overflow: hidden;
  cursor: pointer;
  background-color: var(--glass);
  background-image: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.6),
    rgba(255, 255, 255, 0.12) 55%,
    rgba(255, 255, 255, 0.34)
  );
  -webkit-backdrop-filter: var(--glass-blur);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-island);
  transition: transform 180ms var(--ease), border-color 180ms var(--ease),
    box-shadow 180ms var(--ease);
}

.kb-card:hover {
  transform: translateY(-2px);
  border-color: rgba(217, 90, 12, 0.28);
  box-shadow: 0 24px 48px -30px rgba(24, 24, 28, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.kb-card--skeleton {
  display: block;
  cursor: default;
  height: 150px;
  padding: 0;
  background: rgba(255, 255, 255, 0.36);
  border-color: var(--glass-border);
}

.kb-card--skeleton:hover {
  transform: none;
  border-color: var(--glass-border);
  box-shadow: var(--shadow-island);
}

.kb-card__spine {
  display: grid;
  place-items: center;
  flex: none;
  width: 44px;
  height: 44px;
  color: var(--accent-strong);
  font-size: 17px;
  font-weight: 700;
  background: var(--accent-soft);
  border: 1px solid rgba(255, 255, 255, 0.85);
  border-radius: 14px;
  transition: background 180ms var(--ease), color 180ms var(--ease);
}

.kb-card:hover .kb-card__spine {
  color: #fff;
  background: linear-gradient(180deg, #ea6a17, var(--accent));
}

.kb-card__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 6px;
  min-width: 0;
  padding: 0;
}

.kb-card__title {
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.kb-card__title a {
  color: var(--ink);
}

.kb-card__title a:hover {
  color: var(--accent);
}

.kb-card__desc {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.65;
}

.kb-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}.kb-card__foot .icon-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

@media (max-width: 560px) {
  .kb-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
