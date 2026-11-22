<script setup>
import BaseModal from './BaseModal.vue'
import { confirmState, resolveConfirm } from '../stores/confirm'

function cancel() {
  resolveConfirm(false)
}

function accept() {
  resolveConfirm(true)
}
</script>

<template>
  <BaseModal
    :open="confirmState.open"
    :title="confirmState.title"
    @close="cancel"
  >
    <p class="ink-2" style="font-size: 13px">{{ confirmState.message }}</p>

    <template #footer>
      <button class="btn" type="button" @click="cancel">{{ confirmState.cancelText }}</button>
      <button
        class="btn"
        :class="confirmState.tone === 'danger' ? 'btn--danger' : 'btn--primary'"
        type="button"
        @click="accept"
      >
        {{ confirmState.confirmText }}
      </button>
    </template>
  </BaseModal>
</template>
