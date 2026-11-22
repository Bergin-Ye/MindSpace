<script setup>
import { useToastStore } from '../stores/toast'
import Icon from './Icon.vue'

const toast = useToastStore()

const iconOf = (type) => (type === 'error' ? 'alert' : type === 'success' ? 'check' : 'info')
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="false">
    <div
      v-for="item in toast.items"
      :key="item.id"
      class="toast"
      :class="`toast--${item.type}`"
      role="status"
    >
      <Icon class="toast__icon" :name="iconOf(item.type)" :size="15" />
      <span>{{ item.message }}</span>
      <button class="toast__close" type="button" aria-label="关闭提示" @click="toast.remove(item.id)">
        <Icon name="x" :size="13" />
      </button>
    </div>
  </div>
</template>
