import { reactive } from 'vue'

const state = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  tone: 'default',
})

let resolver = null

/**
 * 全局确认框。用法：
 *   const ok = await askConfirm({ title: '删除文档', message: '删除后不可恢复。' })
 */
export function askConfirm(options = {}) {
  if (resolver) {
    resolver(false)
    resolver = null
  }

  state.title = options.title || '确认操作'
  state.message = options.message || ''
  state.confirmText = options.confirmText || '确认'
  state.cancelText = options.cancelText || '取消'
  state.tone = options.tone || 'default'
  state.open = true

  return new Promise((resolve) => {
    resolver = resolve
  })
}

export function resolveConfirm(value) {
  state.open = false
  if (resolver) {
    resolver(value)
    resolver = null
  }
}

export const confirmState = state
