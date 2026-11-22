export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes)) return '—'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  let value = bytes / 1024
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value >= 10 ? value.toFixed(0) : value.toFixed(1)} ${units[unit]}`
}

export function formatDateTime(value) {
  if (!value) return '—'
  const text = String(value).replace('T', ' ')
  return text.slice(0, 16)
}

export function formatDate(value) {
  if (!value) return '—'
  return String(value).slice(0, 10)
}

const STATUS_MAP = {
  UPLOADED: { label: '待解析', tone: '' },
  PROCESSING: { label: '解析中', tone: 'warn' },
  SUCCESS: { label: '已解析', tone: 'ok' },
  FAILED: { label: '解析失败', tone: 'danger' },
}

export function statusMeta(status) {
  return STATUS_MAP[status] || { label: status || '未知', tone: '' }
}

export const FILE_TYPES = ['pdf', 'docx', 'txt', 'md']
export const AI_CATEGORIES = ['技术', '学习', '工作', '生活', '其他']

export function parseKeywords(keywords) {
  if (!keywords) return []
  if (Array.isArray(keywords)) return keywords
  try {
    const parsed = JSON.parse(keywords)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return String(keywords)
      .split(/[,，\s]+/)
      .filter(Boolean)
  }
}

export function formatScore(score) {
  if (typeof score !== 'number') return '—'
  return score.toFixed(4)
}

export function clip(text, max = 80) {
  if (!text) return ''
  return text.length > max ? `${text.slice(0, max)}…` : text
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** 把一段文本按关键词切成片段，用于安全高亮（不注入 HTML）。 */
export function highlightParts(text, keyword) {
  if (!text) return []
  const word = (keyword || '').trim()
  if (!word) return [{ text, hit: false }]
  const pattern = new RegExp(`(${escapeRegExp(word)})`, 'gi')
  return text
    .split(pattern)
    .filter((part) => part !== '')
    .map((part) => ({
      text: part,
      hit: part.toLowerCase() === word.toLowerCase(),
    }))
}

export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
