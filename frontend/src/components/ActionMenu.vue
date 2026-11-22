<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Icon from './Icon.vue'

defineProps({
  items: { type: Array, default: () => [] },
  label: { type: String, default: '更多操作' },
})

const emit = defineEmits(['select'])

const open = ref(false)
const rootRef = ref(null)

function toggle() {
  open.value = !open.value
}

function choose(item) {
  if (item.disabled) return
  open.value = false
  emit('select', item.key)
}

function onDocumentClick(event) {
  if (!open.value) return
  if (rootRef.value && !rootRef.value.contains(event.target)) {
    open.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="rootRef" class="menu">
    <button
      class="icon-btn"
      type="button"
      :aria-label="label"
      :aria-expanded="open"
      aria-haspopup="menu"
      @click.stop="toggle"
    >
      <Icon name="more" :size="16" />
    </button>

    <div v-if="open" class="menu__pop" role="menu">
      <button
        v-for="item in items"
        :key="item.key"
        class="menu__item"
        :class="{ 'is-danger': item.danger, 'is-disabled': item.disabled }"
        type="button"
        role="menuitem"
        :disabled="item.disabled"
        @click.stop="choose(item)"
      >
        <Icon v-if="item.icon" :name="item.icon" :size="14" />
        {{ item.label }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.menu {
  position: relative;
  display: inline-flex;
}

.menu__pop {
  position: absolute;
  z-index: 25;
  top: calc(100% + 4px);
  right: 0;
  min-width: 148px;
  padding: 5px;
  background: rgba(255, 255, 255, 0.86);
  -webkit-backdrop-filter: saturate(180%) blur(26px);
  backdrop-filter: saturate(180%) blur(26px);
  border: 1px solid var(--glass-border);
  border-radius: 14px;
  box-shadow: var(--shadow-2);
}

.menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 9px;
  color: var(--ink-2);
  font-size: 12px;
  text-align: left;
  background: none;
  border: 0;
  border-radius: 9px;
}

.menu__item:hover {
  color: var(--ink);
  background: rgba(120, 120, 128, 0.1);
}

.menu__item.is-danger {
  color: var(--danger);
}

.menu__item.is-danger:hover {
  background: var(--danger-soft);
}

.menu__item.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
