<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { useAuthStore } from '../stores/auth'
import { useWorkspaceStore } from '../stores/workspace'
import { useToastStore } from '../stores/toast'
import { errorMessage } from '../api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const workspace = useWorkspaceStore()
const toast = useToastStore()

const drawerOpen = ref(false)
const kbMenuOpen = ref(false)
const kbMenuRef = ref(null)

const currentKb = computed(() => workspace.currentKb)

const isKbSection = computed(() =>
  ['knowledge-bases', 'kb-files', 'document-detail'].includes(route.name)
)

const navItems = [
  { name: 'knowledge-bases', label: '知识库', icon: 'grid' },
  { name: 'search', label: '检索', icon: 'search' },
  { name: 'chat', label: '智能问答', icon: 'chat' },
]

function toggleKbMenu() {
  if (!workspace.hasKb) {
    router.push({ name: 'knowledge-bases' })
    return
  }
  kbMenuOpen.value = !kbMenuOpen.value
}

function chooseKb(kb) {
  workspace.selectKb(kb.id)
  kbMenuOpen.value = false
  if (route.name === 'kb-files') {
    router.push({ name: 'kb-files', params: { id: kb.id } })
  }
}

function onDocumentClick(event) {
  if (!kbMenuOpen.value) return
  if (kbMenuRef.value && !kbMenuRef.value.contains(event.target)) {
    kbMenuOpen.value = false
  }
}

async function logout() {
  auth.logout()
  workspace.reset()
  drawerOpen.value = false
  await router.push({ name: 'login' })
}

watch(
  () => route.fullPath,
  () => {
    drawerOpen.value = false
    kbMenuOpen.value = false
  }
)

watch(
  () => route.params.id,
  (id) => {
    if (route.name !== 'kb-files') return
    const numeric = Number(id)
    if (Number.isFinite(numeric) && numeric > 0 && numeric !== workspace.currentKbId) {
      workspace.selectKb(numeric)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  document.addEventListener('click', onDocumentClick)
  try {
    await Promise.all([workspace.load(), auth.refreshMe()])
  } catch (error) {
    toast.error(errorMessage(error))
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})
</script>

<template>
  <div class="shell">
    <aside class="rail" :class="{ 'is-open': drawerOpen }">
      <div class="rail__brand">
        <RouterLink class="brand" :to="{ name: 'knowledge-bases' }">
          MindSpace<span class="brand__dot" aria-hidden="true" />
        </RouterLink>
        <span class="brand__sub">文档知识库</span>
      </div>

      <div ref="kbMenuRef" class="kb-menu">
        <button
          class="kb-switch"
          type="button"
          :aria-expanded="kbMenuOpen"
          aria-haspopup="listbox"
          @click.stop="toggleKbMenu"
        >
          <span class="kb-switch__label">
            当前知识库
            <Icon name="chevron-down" :size="12" />
          </span>
          <span class="kb-switch__name">
            <span v-if="currentKb" class="chip chip--accent mono" style="padding: 0 5px">KB</span>
            <span>{{ currentKb?.name || '还没有知识库' }}</span>
          </span>
        </button>

        <div v-if="kbMenuOpen" class="kb-menu__pop" role="listbox">
          <button
            v-for="kb in workspace.knowledgeBases"
            :key="kb.id"
            class="kb-menu__item"
            :class="{ 'is-current': kb.id === workspace.currentKbId }"
            type="button"
            role="option"
            :aria-selected="kb.id === workspace.currentKbId"
            @click="chooseKb(kb)"
          >
            <span class="truncate">{{ kb.name }}</span>
            <Icon
              v-if="kb.id === workspace.currentKbId"
              class="spacer"
              name="check"
              :size="14"
            />
          </button>
          <div class="kb-menu__foot">
            <RouterLink :to="{ name: 'knowledge-bases' }">管理全部知识库</RouterLink>
          </div>
        </div>
      </div>

      <nav class="nav" aria-label="主导航">
        <p class="nav__label">工作区</p>
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          class="nav__link"
          :class="{ 'is-active': isKbSection ? item.name === 'knowledge-bases' : route.name === item.name }"
          :to="{ name: item.name }"
        >
          <span class="nav__mark" aria-hidden="true" />
          <Icon :name="item.icon" :size="17" />
          <span class="nav__text">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="rail__foot">
        <div class="user">
          <img
            v-if="auth.user?.avatar"
            class="avatar avatar--img"
            :src="auth.user.avatar"
            :alt="`${auth.displayName} 的头像`"
          />
          <span v-else class="avatar" aria-hidden="true">{{ auth.initial }}</span>
          <div class="user__meta">
            <p class="user__name truncate">{{ auth.displayName }}</p>
            <p class="user__mail">{{ auth.user?.email || '' }}</p>
          </div>
        </div>
        <div class="row">
          <RouterLink class="btn btn--ghost btn--sm" :to="{ name: 'settings' }">
            <Icon name="settings" :size="14" />
            账户
          </RouterLink>
          <span class="spacer" />
          <button class="icon-btn" type="button" aria-label="退出登录" @click="logout">
            <Icon name="logout" :size="16" />
          </button>
        </div>
      </div>
    </aside>

    <div v-if="drawerOpen" class="rail-backdrop" @click="drawerOpen = false" />

    <div class="main">
      <header class="mobile-bar">
        <button class="icon-btn" type="button" aria-label="打开导航" @click="drawerOpen = true">
          <Icon name="menu" :size="18" />
        </button>
        <RouterLink class="brand" :to="{ name: 'knowledge-bases' }">
          MindSpace<span class="brand__dot" aria-hidden="true" />
        </RouterLink>
        <span class="spacer" />
        <span class="chip chip--accent truncate" style="max-width: 42vw">
          {{ currentKb?.name || '未选择知识库' }}
        </span>
      </header>

      <RouterView />
    </div>
  </div>
</template>
