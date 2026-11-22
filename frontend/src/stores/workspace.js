import { defineStore } from 'pinia'
import * as kbApi from '../api/knowledge'

const CURRENT_KB_KEY = 'mindspace.currentKbId'

function readCurrentId() {
  const raw = localStorage.getItem(CURRENT_KB_KEY)
  const id = Number(raw)
  return Number.isFinite(id) && id > 0 ? id : null
}

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    knowledgeBases: [],
    loaded: false,
    loading: false,
    currentKbId: readCurrentId(),
  }),

  getters: {
    currentKb: (state) =>
      state.knowledgeBases.find((item) => item.id === state.currentKbId) || null,
    hasKb: (state) => state.knowledgeBases.length > 0,
  },

  actions: {
    async load(force = false) {
      if (this.loading) return this.knowledgeBases
      if (this.loaded && !force) return this.knowledgeBases

      this.loading = true
      try {
        const list = await kbApi.listKnowledgeBases()
        this.knowledgeBases = Array.isArray(list) ? list : []
        this.normalizeCurrent()
        this.loaded = true
        return this.knowledgeBases
      } finally {
        this.loading = false
      }
    },

    normalizeCurrent() {
      const exists = this.knowledgeBases.some((item) => item.id === this.currentKbId)
      if (!exists) {
        this.currentKbId = this.knowledgeBases[0]?.id ?? null
        this.persistCurrent()
      }
    },

    selectKb(id) {
      this.currentKbId = id
      this.persistCurrent()
    },

    persistCurrent() {
      if (this.currentKbId) {
        localStorage.setItem(CURRENT_KB_KEY, String(this.currentKbId))
      } else {
        localStorage.removeItem(CURRENT_KB_KEY)
      }
    },

    async refresh() {
      return this.load(true)
    },

    reset() {
      this.knowledgeBases = []
      this.loaded = false
      this.currentKbId = null
      this.persistCurrent()
    },
  },
})
