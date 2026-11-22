<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
  stroke: { type: [Number, String], default: 1.7 },
})

const ICONS = {
  grid: '<rect x="3.5" y="3.5" width="7" height="7" rx="1"/><rect x="13.5" y="3.5" width="7" height="7" rx="1"/><rect x="3.5" y="13.5" width="7" height="7" rx="1"/><rect x="13.5" y="13.5" width="7" height="7" rx="1"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.5 4.5"/>',
  chat: '<path d="M20.5 12a8.5 8.5 0 0 1-12.4 7.5L3.5 20.5l1.2-4.4A8.5 8.5 0 1 1 20.5 12Z"/><path d="M8.5 11.5h7M8.5 14.5h4"/>',
  folder: '<path d="M3.5 7.5a2 2 0 0 1 2-2h3l2 2.2h8a2 2 0 0 1 2 2v7.8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2Z"/>',
  'folder-open': '<path d="M3.5 8V6.5a2 2 0 0 1 2-2h3l2 2.2h7a2 2 0 0 1 2 2V10"/><path d="M3.5 9.5h16.8l-2 8.2a2 2 0 0 1-2 1.5H5.2a2 2 0 0 1-2-1.9Z"/>',
  file: '<path d="M13.5 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9Z"/><path d="M13.5 3.5V9H19"/>',
  upload: '<path d="M12 16V4.5"/><path d="m7.5 9 4.5-4.5L16.5 9"/><path d="M4.5 15v3a1.5 1.5 0 0 0 1.5 1.5h12A1.5 1.5 0 0 0 19.5 18v-3"/>',
  download: '<path d="M12 4.5V16"/><path d="m7.5 11.5 4.5 4.5 4.5-4.5"/><path d="M4.5 15v3a1.5 1.5 0 0 0 1.5 1.5h12A1.5 1.5 0 0 0 19.5 18v-3"/>',
  trash: '<path d="M4.5 6.5h15"/><path d="M9 3.5h6"/><path d="M6.5 6.5 7.4 19a1.5 1.5 0 0 0 1.5 1.4h6.2a1.5 1.5 0 0 0 1.5-1.4l.9-12.5"/><path d="M10 10.5v6M14 10.5v6"/>',
  pencil: '<path d="M16.6 4.6a2 2 0 0 1 2.8 2.8L8.2 18.6 4 19.7l1.1-4.2Z"/><path d="m14.6 6.6 2.8 2.8"/>',
  move: '<path d="M12 3.5v17"/><path d="m8.5 7 3.5-3.5L15.5 7"/><path d="m8.5 17 3.5 3.5 3.5-3.5"/><path d="M3.5 12h17"/><path d="m7 8.5-3.5 3.5L7 15.5"/><path d="m17 8.5 3.5 3.5-3.5 3.5"/>',
  more: '<circle cx="5.5" cy="12" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="18.5" cy="12" r="1.2"/>',
  'chevron-down': '<path d="m6 9.5 6 6 6-6"/>',
  'chevron-right': '<path d="m9.5 6 6 6-6 6"/>',
  'chevron-left': '<path d="m14.5 6-6 6 6 6"/>',
  'arrow-right': '<path d="M4.5 12h15"/><path d="m14 6.5 6 5.5-6 5.5"/>',
  'arrow-left': '<path d="M19.5 12h-15"/><path d="m10 6.5-6 5.5 6 5.5"/>',
  x: '<path d="m6 6 12 12M18 6 6 18"/>',
  plus: '<path d="M12 4.5v15M4.5 12h15"/>',
  check: '<path d="m4.5 12.5 5 5 10-11"/>',
  alert: '<path d="M12 4.5 21 20H3Z"/><path d="M12 9.5v4.5"/><path d="M12 17.2v.1"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5"/><path d="M12 7.8v.1"/>',
  sparkle: '<path d="M12 3.5 13.9 9l5.6 2-5.6 1.9L12 18.5 10.1 12.9 4.5 11l5.6-2Z"/><path d="M18.5 4v2.5M17.2 5.2h2.6"/>',
  link: '<path d="M10 13.5a4 4 0 0 0 5.7 0l2.6-2.6a4 4 0 0 0-5.7-5.7L11.4 6.4"/><path d="M14 10.5a4 4 0 0 0-5.7 0l-2.6 2.6a4 4 0 0 0 5.7 5.7l1.2-1.2"/>',
  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',
  logout: '<path d="M10 4.5H6.5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2H10"/><path d="M15 8.5 18.5 12 15 15.5"/><path d="M18.5 12H9"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 14.5a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5v.2a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1h-.2a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3h.1a1.6 1.6 0 0 0 1-1.5v-.2a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8v.1a1.6 1.6 0 0 0 1.5 1h.2a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  layers: '<path d="m12 3.5 8 4.2-8 4.2-8-4.2Z"/><path d="m4 12 8 4.2 8-4.2"/><path d="m4 16.3 8 4.2 8-4.2"/>',
  send: '<path d="M20.5 3.5 3.8 10.2l6.2 2.4 2.4 6.2Z"/><path d="m10 13.5 4.5-4.5"/>',
  refresh: '<path d="M20 12a8 8 0 1 1-2.4-5.7"/><path d="M20 4.5V10h-5.5"/>',
  filter: '<path d="M4 6.5h16"/><path d="M7 12h10"/><path d="M10 17.5h4"/>',
  lock: '<rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"/>',
  mail: '<rect x="3.5" y="5.5" width="17" height="13" rx="2"/><path d="m4.5 7 7.5 6 7.5-6"/>',
  user: '<circle cx="12" cy="8.5" r="3.5"/><path d="M5 20a7 7 0 0 1 14 0"/>',
  'file-text': '<path d="M13.5 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9Z"/><path d="M13.5 3.5V9H19"/><path d="M8.5 13h7M8.5 16.5h5"/>',
  'corner-down-left': '<path d="M19 5v6.5a3 3 0 0 1-3 3H6"/><path d="m9.5 10.5-4 4 4 4"/>',
  copy: '<rect x="8.5" y="8.5" width="11" height="11" rx="2"/><path d="M15.5 8.5V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2.5"/>',
}

const content = computed(() => ICONS[props.name] || ICONS.file)
const px = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
</script>

<template>
  <svg
    :width="px"
    :height="px"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="stroke"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <g v-html="content" />
  </svg>
</template>
