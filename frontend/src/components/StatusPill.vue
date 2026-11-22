<script setup>
import { computed } from 'vue'
import { statusMeta } from '../utils/format'

const props = defineProps({
  status: { type: String, default: '' },
})

const meta = computed(() => statusMeta(props.status))
</script>

<template>
  <span class="chip" :class="meta.tone ? `chip--${meta.tone}` : ''">
    <span class="chip__dot" aria-hidden="true" />
    {{ meta.label }}
  </span>
</template>
