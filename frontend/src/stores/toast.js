import { defineStore } from 'pinia'

let seed = 0

export const useToastStore = defineStore('toast', {
  state: () => ({
    items: [],
  }),

  actions: {
    push(message, type = 'info', duration = 4200) {
      if (!message) return
      seed += 1
      const id = seed
      this.items.push({ id, message, type })
      if (duration > 0) {
        window.setTimeout(() => this.remove(id), duration)
      }
    },

    success(message) {
      this.push(message, 'success')
    },

    error(message) {
      this.push(message, 'error', 6000)
    },

    remove(id) {
      this.items = this.items.filter((item) => item.id !== id)
    },
  },
})
