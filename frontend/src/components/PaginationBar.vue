<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pages: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  size: { type: Number, default: 10 },
  unit: { type: String, default: '条' },
})

const emit = defineEmits(['change'])

const range = computed(() => {
  const pages = Math.max(1, props.pages)
  const start = Math.max(1, Math.min(props.page - 2, pages - 4))
  const end = Math.min(pages, start + 4)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

function go(next) {
  if (next < 1 || next > props.pages || next === props.page) return
  emit('change', next)
}
</script>

<template>
  <div class="pagination">
    <span>共 {{ total }} {{ unit }}，第 {{ page }} / {{ Math.max(pages, 1) }} 页</span>
    <div class="pagination__pages">
      <button
        class="pagination__page"
        type="button"
        aria-label="上一页"
        :disabled="page <= 1"
        @click="go(page - 1)"
      >
        <Icon name="chevron-left" :size="14" />
      </button>
      <button
        v-for="item in range"
        :key="item"
        class="pagination__page"
        :class="{ 'is-active': item === page }"
        type="button"
        :aria-current="item === page ? 'page' : undefined"
        @click="go(item)"
      >
        {{ item }}
      </button>
      <button
        class="pagination__page"
        type="button"
        aria-label="下一页"
        :disabled="page >= pages"
        @click="go(page + 1)"
      >
        <Icon name="chevron-right" :size="14" />
      </button>
    </div>
  </div>
</template>
