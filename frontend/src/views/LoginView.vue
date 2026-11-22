<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthShell from '../layouts/AuthShell.vue'
import Icon from '../components/Icon.vue'
import { useAuthStore } from '../stores/auth'
import { errorMessage } from '../api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  email: route.query.email ? String(route.query.email) : '',
  password: '',
})

const loading = ref(false)
const error = ref('')
const isDev = import.meta.env.DEV

async function submit() {
  error.value = ''
  if (!form.email.trim() || !form.password) {
    error.value = '请填写邮箱和密码'
    return
  }

  loading.value = true
  try {
    await auth.login({ email: form.email.trim(), password: form.password })
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' && redirect ? redirect : { name: 'knowledge-bases' })
  } catch (err) {
    error.value = errorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
    <div class="auth-head">
      <h1>登录</h1>
      <p>进入你的文档知识库，继续搜索或提问。</p>
    </div>

    <form class="form-stack" @submit.prevent="submit">
      <label class="field">
        <span class="label">邮箱</span>
        <input
          v-model="form.email"
          class="input"
          type="email"
          autocomplete="email"
          placeholder="you@example.com"
          :disabled="loading"
        />
      </label>

      <label class="field">
        <span class="label">密码</span>
        <input
          v-model="form.password"
          class="input"
          type="password"
          autocomplete="current-password"
          placeholder="至少 6 位"
          :disabled="loading"
        />
      </label>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <button class="btn btn--primary btn--block" type="submit" :disabled="loading">
        <span v-if="loading" class="spinner" />
        <template v-else>
          登录
          <Icon name="arrow-right" :size="15" />
        </template>
      </button>
    </form>

    <p class="auth-switch">
      还没有账号？
      <RouterLink :to="{ name: 'register' }">注册一个</RouterLink>
    </p>

    <div v-if="isDev" class="auth-demo">
      <p class="label">本地测试账号</p>
      <p class="mono">test@mindspace.com / 123456</p>
    </div>
  </AuthShell>
</template>

<style scoped>
.auth-head {
  margin-bottom: 24px;
}

.auth-head h1 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.035em;
}

.auth-head p {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.auth-switch {
  margin-top: 18px;
  color: var(--muted);
  font-size: 13px;
}

.auth-demo {
  margin-top: 28px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.auth-demo .mono {
  margin-top: 4px;
  color: var(--ink-2);
}
</style>
