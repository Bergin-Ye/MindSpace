import { defineStore } from 'pinia'
import { TOKEN_KEY } from '../api/http'
import * as authApi from '../api/auth'

const USER_KEY = 'mindspace.user'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persist(token, user) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)

  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.username || '未登录',
    initial: (state) => (state.user?.username || 'M').slice(0, 1).toUpperCase(),
  },

  actions: {
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token
      this.user = data.user
      persist(this.token, this.user)
      return data
    },

    async register(payload) {
      return authApi.register(payload)
    },

    async refreshMe() {
      const user = await authApi.fetchMe()
      this.user = user
      persist(this.token, this.user)
      return user
    },

    updateUser(user) {
      this.user = user
      persist(this.token, this.user)
    },

    clearSession() {
      this.token = ''
      this.user = null
      persist('', null)
    },

    logout() {
      this.clearSession()
    },
  },
})
