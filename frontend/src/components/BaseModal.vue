<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  wide: { type: Boolean, default: false },
})

const emit = defineEmits(['close'])
const dialogRef = ref(null)

function close() {
  emit('close')
}

function onKeydown(event) {
  if (event.key === 'Escape') close()
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', onKeydown)
      await nextTick()
      dialogRef.value?.focus()
    } else {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKeydown)
    }
  }
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @mousedown.self="close">
      <div
        ref="dialogRef"
        class="modal"
        :class="{ 'modal--wide': wide }"
        role="dialog"
        aria-modal="true"
        :aria-label="title || '对话框'"
        tabindex="-1"
      >
        <div class="modal__head">
          <div>
            <h2 class="modal__title">{{ title }}</h2>
            <p v-if="description" class="modal__desc">{{ description }}</p>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="close">
            <Icon name="x" :size="16" />
          </button>
        </div>

        <div class="modal__body">
          <slot />
        </div>

        <div v-if="$slots.footer" class="modal__foot">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>
