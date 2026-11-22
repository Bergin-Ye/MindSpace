<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import ActionMenu from '../components/ActionMenu.vue'
import BaseModal from '../components/BaseModal.vue'
import {
  askQuestion,
  deleteSession,
  listMessages,
  listSessions,
  renameSession,
} from '../api/chat'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import { askConfirm } from '../stores/confirm'
import { formatDateTime, formatScore } from '../utils/format'

const auth = useAuthStore()
const workspace = useWorkspaceStore()
const toast = useToastStore()

const SESSION_PAGE_SIZE = 20
const MESSAGE_PAGE_SIZE = 100

const kb = computed(() => workspace.currentKb)

const sessions = ref([])
const sessionPage = ref(1)
const sessionPages = ref(1)
const sessionTotal = ref(0)
const sessionsLoading = ref(false)
const sessionsOpen = ref(false)
const activeSessionId = ref(null)

const messages = ref([])
const messagesLoading = ref(false)
const messagesTruncated = ref(false)

const question = ref('')
const sending = ref(false)
const sendError = ref('')
const expandedRefs = ref(new Set())

const scrollRef = ref(null)
const composerRef = ref(null)

const renameModal = reactive({ open: false, session: null, title: '', error: '', saving: false })

const activeSession = computed(
  () => sessions.value.find((item) => item.id === activeSessionId.value) || null
)

const sessionsHasMore = computed(() => sessionPage.value < sessionPages.value)

const prompts = [
  '这个知识库主要收录了哪些内容？',
  '把已上传的文档按主题归纳一下。',
  '有哪些概念在文档里出现了多次？',
]

const canSend = computed(() => Boolean(kb.value) && question.value.trim() && !sending.value)

function resetAll() {
  sessions.value = []
  sessionPage.value = 1
  sessionPages.value = 1
  sessionTotal.value = 0
  activeSessionId.value = null
  messages.value = []
  messagesTruncated.value = false
  sendError.value = ''
  expandedRefs.value = new Set()
}

async function loadSessions(page = 1, replace = false) {
  if (!kb.value) return
  sessionsLoading.value = true
  try {
    const data = await listSessions({
      knowledgeBaseId: kb.value.id,
      page,
      size: SESSION_PAGE_SIZE,
    })
    const records = data?.records || []
    sessions.value = replace || page === 1 ? records : [...sessions.value, ...records]
    sessionPage.value = Number(data?.page) || page
    sessionPages.value = Number(data?.pages) || 1
    sessionTotal.value = Number(data?.total) || 0
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    sessionsLoading.value = false
  }
}

function loadMoreSessions() {
  if (!sessionsHasMore.value || sessionsLoading.value) return
  loadSessions(sessionPage.value + 1)
}

async function loadMessages(sessionId) {
  messagesLoading.value = true
  messagesTruncated.value = false
  try {
    const first = await listMessages(sessionId, { page: 1, size: MESSAGE_PAGE_SIZE })
    let records = first?.records || []
    const pages = Number(first?.pages) || 1
    if (pages > 1) {
      const last = await listMessages(sessionId, { page: pages, size: MESSAGE_PAGE_SIZE })
      records = last?.records || []
    }
    messages.value = records
    messagesTruncated.value = Number(first?.total) > records.length
    await scrollToBottom()
  } catch (error) {
    toast.error(errorMessage(error))
    if (error?.code === 3008) {
      activeSessionId.value = null
      messages.value = []
    }
  } finally {
    messagesLoading.value = false
  }
}

async function openSession(session) {
  if (sending.value) return
  activeSessionId.value = session.id
  sessionsOpen.value = false
  sendError.value = ''
  expandedRefs.value = new Set()
  await loadMessages(session.id)
}

function newChat() {
  if (sending.value) return
  activeSessionId.value = null
  messages.value = []
  messagesTruncated.value = false
  sendError.value = ''
  expandedRefs.value = new Set()
  sessionsOpen.value = false
  nextTick(() => composerRef.value?.focus())
}

function usePrompt(text) {
  question.value = text
  nextTick(() => composerRef.value?.focus())
}

async function scrollToBottom() {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send() {
  if (!canSend.value) return
  const text = question.value.trim()
  if (text.length > 2000) {
    sendError.value = '问题不能超过 2000 个字符'
    return
  }

  const localId = `local-${Date.now()}`
  messages.value.push({
    id: localId,
    role: 'USER',
    content: text,
    references: [],
    local: true,
  })
  question.value = ''
  sendError.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const data = await askQuestion({
      knowledgeBaseId: kb.value.id,
      sessionId: activeSessionId.value || undefined,
      question: text,
    })

    activeSessionId.value = data.sessionId
    messages.value.push({
      id: data.messageId ?? `answer-${Date.now()}`,
      role: 'ASSISTANT',
      content: data.answer,
      references: data.references || [],
      local: true,
    })

    await loadSessions(1, true)
  } catch (error) {
    messages.value = messages.value.filter((item) => item.id !== localId)
    question.value = text
    sendError.value = errorMessage(error)
    toast.error(errorMessage(error))
    if (error?.code === 3008) {
      activeSessionId.value = null
    }
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function onComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    send()
  }
}

function sessionMenuItems() {
  return [
    { key: 'rename', label: '重命名对话', icon: 'pencil' },
    { key: 'delete', label: '删除对话', icon: 'trash', danger: true },
  ]
}

function openRename(session) {
  renameModal.open = true
  renameModal.session = session
  renameModal.title = session.title || ''
  renameModal.error = ''
}

async function submitRename() {
  const title = renameModal.title.trim()
  if (!title) {
    renameModal.error = '标题不能为空'
    return
  }
  if (title.length > 255) {
    renameModal.error = '标题不能超过 255 个字符'
    return
  }

  renameModal.saving = true
  try {
    await renameSession(renameModal.session.id, title)
    toast.success('对话已重命名')
    renameModal.open = false
    await loadSessions(1, true)
  } catch (error) {
    renameModal.error = errorMessage(error)
  } finally {
    renameModal.saving = false
  }
}

async function removeSession(session) {
  const ok = await askConfirm({
    title: `删除「${session.title || '未命名对话'}」`,
    message: '会话和其中的消息会一起删除。',
    confirmText: '删除对话',
    tone: 'danger',
  })
  if (!ok) return

  try {
    await deleteSession(session.id)
    toast.success('对话已删除')
    if (activeSessionId.value === session.id) newChat()
    await loadSessions(1, true)
  } catch (error) {
    toast.error(errorMessage(error))
  }
}

async function onSessionMenu(session, key) {
  if (key === 'rename') openRename(session)
  else if (key === 'delete') await removeSession(session)
}

function refKey(message, reference) {
  return `${message.id}-${reference.chunkId}`
}

function toggleRef(key) {
  const next = new Set(expandedRefs.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedRefs.value = next
}

function messageTime(message) {
  return message.local ? '刚刚' : formatDateTime(message.createTime)
}

watch(
  () => kb.value?.id,
  async (id) => {
    resetAll()
    if (id) await loadSessions(1, true)
  }
)

watch(
  () => messages.value.length,
  () => scrollToBottom()
)

onMounted(async () => {
  if (!workspace.loaded) {
    try {
      await workspace.load()
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }
  if (kb.value) await loadSessions(1, true)
  nextTick(() => composerRef.value?.focus())
})
</script>

<template>
  <div class="chat">
    <aside class="chat__sessions" :class="{ 'is-open': sessionsOpen }">
      <div class="sessions-head">
        <span class="panel__title">对话</span>
        <button class="btn btn--sm btn--primary" type="button" :disabled="sending" @click="newChat">
          <Icon name="plus" :size="13" />
          新建
        </button>
      </div>

      <div class="sessions-body">
        <p class="sessions-label mono">
          {{ sessionTotal }} 个会话
        </p>

        <div v-if="sessionsLoading && !sessions.length" class="loading-block">
          <span class="spinner" />
          正在读取…
        </div>

        <EmptyState
          v-else-if="!sessions.length"
          icon="chat"
          title="还没有对话"
          description="提出第一个问题后，会话会出现在这里。"
        />

        <ul v-else class="session-list">
          <li v-for="session in sessions" :key="session.id">
            <div class="session" :class="{ 'is-active': session.id === activeSessionId }">
              <button class="session__main" type="button" @click="openSession(session)">
                <span class="session__title truncate">
                  {{ session.title || '未命名对话' }}
                </span>
                <span class="session__time mono">{{ formatDateTime(session.updateTime) }}</span>
              </button>
              <ActionMenu :items="sessionMenuItems()" @select="onSessionMenu(session, $event)" />
            </div>
          </li>
        </ul>

        <button
          v-if="sessionsHasMore"
          class="btn btn--ghost btn--sm btn--block"
          type="button"
          :disabled="sessionsLoading"
          @click="loadMoreSessions"
        >
          <span v-if="sessionsLoading" class="spinner" />
          加载更多
        </button>
      </div>

      <div class="sessions-foot">
        <Icon name="grid" :size="14" />
        <span class="truncate">{{ kb?.name || '未选择知识库' }}</span>
      </div>
    </aside>

    <div v-if="sessionsOpen" class="rail-backdrop" @click="sessionsOpen = false" />

    <section class="thread">
      <header class="thread__head">
        <button
          class="icon-btn thread__history"
          type="button"
          aria-label="打开历史会话"
          @click="sessionsOpen = true"
        >
          <Icon name="menu" :size="17" />
        </button>
        <div class="thread__meta">
          <p class="thread__name truncate">{{ activeSession?.title || '新对话' }}</p>
          <p class="thread__sub mono">{{ kb?.name || '未选择知识库' }}</p>
        </div>
        <span class="spacer" />
        <button
          class="icon-btn"
          type="button"
          aria-label="新建对话"
          title="新建对话"
          :disabled="sending"
          @click="newChat"
        >
          <Icon name="plus" :size="17" />
        </button>
      </header>

      <div ref="scrollRef" class="thread__body">
        <div v-if="!kb" class="thread__center">
          <EmptyState
            icon="grid"
            title="还没有可用的知识库"
            description="先创建知识库、上传并向量化文档，再来提问。"
          >
            <RouterLink class="btn btn--primary" :to="{ name: 'knowledge-bases' }">
              去创建知识库
            </RouterLink>
          </EmptyState>
        </div>

        <div v-else-if="messagesLoading" class="loading-block">
          <span class="spinner" />
          正在读取对话…
        </div>

        <div v-else-if="!messages.length && !sending" class="thread__center">
          <div class="intro">
            <span class="intro__mark" aria-hidden="true">M</span>
            <h2 class="intro__title">问一个关于这个知识库的问题</h2>
            <p class="intro__desc">
              回答只使用已解析并完成向量化的文档。引用会附在回答下面，可以点开核对原文。
            </p>
            <div class="intro__prompts">
              <button
                v-for="prompt in prompts"
                :key="prompt"
                class="prompt"
                type="button"
                @click="usePrompt(prompt)"
              >
                <span>{{ prompt }}</span>
                <Icon name="arrow-right" :size="14" />
              </button>
            </div>
          </div>
        </div>

        <div v-else class="messages">
          <p v-if="messagesTruncated" class="thread__note">
            只显示最近 {{ messages.length }} 条消息，更早的记录未加载。
          </p>

          <article
            v-for="message in messages"
            :key="message.id"
            class="msg"
            :class="`msg--${message.role.toLowerCase()}`"
          >
            <span class="msg__avatar" :class="message.role === 'ASSISTANT' ? 'is-assistant' : ''">
              {{ message.role === 'ASSISTANT' ? 'M' : auth.initial }}
            </span>

            <div class="msg__main">
              <div class="msg__meta">
                <span>{{ message.role === 'ASSISTANT' ? 'MindSpace' : auth.displayName }}</span>
                <span class="mono">{{ messageTime(message) }}</span>
              </div>

              <div class="msg__bubble">
                <p>{{ message.content }}</p>
              </div>

              <div v-if="message.references?.length" class="refs">
                <p class="refs__title">引用 {{ message.references.length }} 段</p>
                <ol class="refs__list">
                  <li
                    v-for="(reference, index) in message.references"
                    :key="reference.chunkId"
                    class="ref"
                  >
                    <span class="ref__index mono">{{ index + 1 }}</span>
                    <div class="ref__body">
                      <div class="ref__top">
                        <RouterLink
                          class="ref__name"
                          :to="{ name: 'document-detail', params: { id: reference.documentId } }"
                        >
                          {{ reference.originalName }}
                        </RouterLink>
                        <span class="mono muted">
                          第 {{ reference.chunkIndex + 1 }} 块 · {{ formatScore(reference.score) }}
                        </span>
                      </div>
                      <p
                        class="ref__content"
                        :class="{ 'is-open': expandedRefs.has(refKey(message, reference)) }"
                      >
                        {{ reference.content }}
                      </p>
                      <button
                        v-if="reference.content.length > 150"
                        class="link-btn"
                        type="button"
                        @click="toggleRef(refKey(message, reference))"
                      >
                        {{ expandedRefs.has(refKey(message, reference)) ? '收起引用' : '展开引用' }}
                      </button>
                    </div>
                  </li>
                </ol>
              </div>
            </div>
          </article>

          <article v-if="sending" class="msg msg--assistant">
            <span class="msg__avatar is-assistant">M</span>
            <div class="msg__main">
              <div class="msg__meta">
                <span>MindSpace</span>
                <span class="mono">检索中</span>
              </div>
              <div class="typing" aria-label="正在生成回答">
                <span /><span /><span />
              </div>
              <p class="typing__hint">
                正在做向量检索并组织回答。嵌入和模型调用可能需要几十秒。
              </p>
            </div>
          </article>
        </div>
      </div>

      <footer class="composer">
        <p v-if="sendError" class="form-error composer__error" role="alert">{{ sendError }}</p>

        <div class="composer__box">
          <textarea
            ref="composerRef"
            v-model="question"
            class="composer__input"
            rows="3"
            maxlength="2000"
            placeholder="输入问题，基于当前知识库回答"
            aria-label="输入问题"
            :disabled="sending || !kb"
            @keydown="onComposerKeydown"
          />
          <div class="composer__bar">
            <span class="hint">Enter 发送，Shift + Enter 换行</span>
            <span class="spacer" />
            <span v-if="question.length > 1800" class="mono muted">{{ question.length }}/2000</span>
            <button class="btn btn--primary btn--sm" type="button" :disabled="!canSend" @click="send">
              <span v-if="sending" class="spinner" />
              <Icon v-else name="send" :size="14" />
              发送
            </button>
          </div>
        </div>
      </footer>
    </section>

    <BaseModal
      :open="renameModal.open"
      title="重命名对话"
      @close="renameModal.open = false"
    >
      <form class="form-stack" @submit.prevent="submitRename">
        <label class="field">
          <span class="label">标题<span class="label__req">*</span></span>
          <input
            v-model="renameModal.title"
            class="input"
            type="text"
            maxlength="255"
            :disabled="renameModal.saving"
          />
        </label>
        <p v-if="renameModal.error" class="form-error" role="alert">{{ renameModal.error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" :disabled="renameModal.saving" @click="renameModal.open = false">
          取消
        </button>
        <button class="btn btn--primary" type="button" :disabled="renameModal.saving" @click="submitRename">
          <span v-if="renameModal.saving" class="spinner" />
          保存
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.chat {
  display: grid;
  grid-template-columns: 262px minmax(0, 1fr);
  gap: var(--gap);
  height: calc(100vh - var(--gap) * 2);
  overflow: hidden;
}

.chat__sessions {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background-color: var(--glass);
  background-image: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.56),
    rgba(255, 255, 255, 0.1)
  );
  -webkit-backdrop-filter: var(--glass-blur);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-island);
}

.sessions-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 14px 12px;
  border-bottom: 1px solid var(--line);
}

.sessions-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 10px 8px;
}

.sessions-label {
  padding: 0 8px 8px;
  color: var(--muted);
  font-size: 11px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.session {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-right: 4px;
  border-radius: 12px;
}

.session:hover {
  background: rgba(255, 255, 255, 0.52);
}

.session.is-active {
  background: var(--accent-soft);
  box-shadow: inset 0 0 0 1px var(--accent-line);
}

.session__main {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
  padding: 8px 9px;
  text-align: left;
  background: none;
  border: 0;
}

.session__title {
  color: var(--ink-2);
  font-size: 12px;
}

.session.is-active .session__title {
  color: var(--accent-strong);
  font-weight: 500;
}

.session__time {
  color: var(--muted);
  font-size: 10px;
}

.sessions-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  color: var(--muted);
  font-size: 11px;
  border-top: 1px solid var(--line);
}

.thread {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background-color: var(--glass);
  background-image: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.58),
    rgba(255, 255, 255, 0.12)
  );
  -webkit-backdrop-filter: var(--glass-blur);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-island);
}

.thread__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line);
}

.thread__history {
  display: none;
}

.thread__meta {
  min-width: 0;
}

.thread__name {
  font-size: 13px;
  font-weight: 600;
}

.thread__sub {
  color: var(--muted);
  font-size: 10px;
}

.thread__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 22px 20px 8px;
}

.thread__center {
  display: grid;
  place-items: center;
  min-height: 100%;
}

.thread__note {
  max-width: 760px;
  margin: 0 auto 14px;
  color: var(--muted);
  font-size: 11px;
  text-align: center;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 22px;
  max-width: 820px;
  margin: 0 auto;
}

.msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.msg__avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 30px;
  height: 30px;
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 700;
  background: var(--accent-soft);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 10px;
}

.msg__avatar.is-assistant {
  color: #fff;
  background: linear-gradient(180deg, #ea6a17, var(--accent));
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 6px 14px -10px var(--accent-glow);
}

.msg__main {
  flex: 1;
  min-width: 0;
}

.msg__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 5px;
  color: var(--muted);
  font-size: 11px;
}

.msg__meta span:first-child {
  color: var(--ink-2);
  font-weight: 500;
}

.msg__bubble {
  display: inline-block;
  max-width: 100%;
  padding: 11px 15px;
  color: var(--ink);
  font-size: 13px;
  line-height: 1.75;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(24, 24, 28, 0.03);
}

.msg__bubble p {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.msg--user .msg__avatar {
  display: none;
}

.msg--user .msg__main {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg--user .msg__meta {
  justify-content: flex-end;
}

.msg--user .msg__bubble {
  color: #fff;
  background: linear-gradient(180deg, #e9681a, var(--accent));
  border-color: rgba(255, 255, 255, 0.18);
  box-shadow: 0 12px 26px -18px var(--accent-glow);
}

.refs {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.refs__title {
  color: var(--muted);
  font-size: 11px;
}

.refs__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.ref {
  display: flex;
  gap: 9px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.76);
  border-radius: 14px;
}

.ref__index {
  display: grid;
  place-items: center;
  flex: none;
  width: 19px;
  height: 19px;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  background: var(--accent);
  border-radius: 50%;
}

.ref__body {
  min-width: 0;
}

.ref__top {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}

.ref__name {
  font-size: 12px;
  font-weight: 500;
}

.ref__content {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  margin-top: 3px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ref__content.is-open {
  display: block;
  overflow: visible;
  -webkit-line-clamp: initial;
}

.link-btn {
  margin-top: 4px;
  padding: 0;
  color: var(--accent);
  font-size: 11px;
  background: none;
  border: 0;
}

.link-btn:hover {
  text-decoration: underline;
}

.typing {
  display: inline-flex;
  gap: 4px;
  padding: 13px 15px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
}

.typing span {
  width: 5px;
  height: 5px;
  background: var(--accent);
  border-radius: 50%;
  animation: blink 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

.typing__hint {
  margin-top: 6px;
  color: var(--muted);
  font-size: 11px;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.intro {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 520px;
  padding: 24px 0;
  text-align: center;
}

.intro__mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  color: #fff;
  font-size: 19px;
  font-weight: 700;
  background: linear-gradient(180deg, #ea6a17, var(--accent));
  border-radius: 15px;
  box-shadow: 0 14px 30px -18px var(--accent-glow);
}

.intro__title {
  font-size: 23px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.intro__desc {
  max-width: 44ch;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.75;
}

.intro__prompts {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  margin-top: 10px;
}

.prompt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 11px 14px;
  color: var(--ink-2);
  font-size: 12.5px;
  text-align: left;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 14px;
  transition: border-color 160ms var(--ease), background 160ms var(--ease),
    transform 160ms var(--ease);
}

.prompt:hover {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.86);
  border-color: var(--accent-line);
  transform: translateY(-1px);
}

.composer {
  padding: 12px 18px 16px;
  border-top: 1px solid var(--line);
  background: transparent;
}

.composer__error {
  max-width: 820px;
  margin: 0 auto 8px;
}

.composer__box {
  max-width: 820px;
  margin: 0 auto;
  padding: 9px 12px 9px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(24, 24, 28, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: border-color 160ms var(--ease), box-shadow 160ms var(--ease);
}

.composer__box:focus-within {
  border-color: rgba(217, 90, 12, 0.36);
  box-shadow: 0 0 0 3px rgba(242, 112, 27, 0.1);
}

.composer__input {
  width: 100%;
  padding: 4px 2px;
  color: var(--ink);
  font-size: 13px;
  line-height: 1.7;
  background: transparent;
  border: 0;
  outline: none;
  resize: none;
}

.composer__input::placeholder {
  color: #a1a1a6;
}

.composer__bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
}

@media (max-width: 900px) {
  .chat {
    grid-template-columns: minmax(0, 1fr);
    height: calc(100dvh - 82px);
    gap: 10px;
  }

  .chat__sessions {
    position: fixed;
    z-index: 45;
    top: 10px;
    bottom: 10px;
    left: 10px;
    width: 290px;
    transform: translateX(calc(-100% - 16px));
    transition: transform 240ms var(--ease);
    box-shadow: var(--shadow-2);
  }

  .chat__sessions.is-open {
    transform: translateX(0);
  }

  .thread__history {
    display: inline-grid;
  }

  .thread__body {
    padding: 16px 14px 8px;
  }

  .composer {
    padding: 8px 12px 12px;
  }

  .composer__bar .hint {
    display: none;
  }
}
</style>
