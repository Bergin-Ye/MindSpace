import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import './styles/tokens.css'
import './styles/app.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// token 失效时由 axios 拦截器触发：清理登录态并回到登录页。
setUnauthorizedHandler(() => {
  const auth = useAuthStore()
  auth.clearSession()
  const current = router.currentRoute.value
  if (current.name !== 'login' && current.name !== 'register') {
    router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

app.mount('#app')
