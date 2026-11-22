<script setup>
import Icon from './Icon.vue'

defineProps({
  icon: { type: String, default: 'file-text' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <div class="empty">
    <div class="empty__icon">
      <Icon :name="icon" :size="20" />
    </div>
    <p class="empty__title">{{ title }}</p>
    <p v-if="description" class="empty__desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty__actions">
      <slot />
    </div>
  </div>
</template>
