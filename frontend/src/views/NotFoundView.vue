<script setup>
import AuthShell from '../layouts/AuthShell.vue'
</script>

<template>
  <AuthShell>
    <div class="not-found">
      <p class="mono muted">404</p>
      <h1>这个页面不存在</h1>
      <p class="muted">地址可能写错了，或者页面已经被移动。</p>
      <RouterLink class="btn btn--primary" :to="{ name: 'knowledge-bases' }">
        回到知识库
      </RouterLink>
    </div>
  </AuthShell>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.not-found h1 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.035em;
}

.not-found .btn {
  margin-top: 10px;
}
</style>
