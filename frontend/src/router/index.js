import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true, title: '注册' },
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      {
        path: '',
        redirect: { name: 'knowledge-bases' },
      },
      {
        path: 'knowledge-bases',
        name: 'knowledge-bases',
        component: () => import('../views/KnowledgeBaseListView.vue'),
        meta: { title: '知识库' },
      },
      {
        path: 'knowledge-bases/:id/files',
        name: 'kb-files',
        component: () => import('../views/FilesView.vue'),
        meta: { title: '文件管理' },
      },
      {
        path: 'documents/:id',
        name: 'document-detail',
        component: () => import('../views/DocumentDetailView.vue'),
        meta: { title: '文档详情' },
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('../views/SearchView.vue'),
        meta: { title: '检索' },
      },
      {
        path: 'chat',
        name: 'chat',
        component: () => import('../views/ChatView.vue'),
        meta: { title: '智能问答' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('../views/SettingsView.vue'),
        meta: { title: '账户设置' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.public && auth.isLoggedIn && (to.name === 'login' || to.name === 'register')) {
    return { name: 'knowledge-bases' }
  }

  return true
})

router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} · MindSpace` : 'MindSpace · 文档知识库'
})

export default router
