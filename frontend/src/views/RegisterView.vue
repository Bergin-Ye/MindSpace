<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthShell from '../layouts/AuthShell.vue'
import Icon from '../components/Icon.vue'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { errorMessage } from '../api/http'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirm: '',
})

const loading = ref(false)
const error = ref('')

function validate() {
  if (form.username.trim().length < 3 || form.username.trim().length > 50) {
    return '用户名长度需要在 3 到 50 个字符之间'
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    return '邮箱格式不正确'
  }
  if (form.password.length < 6 || form.password.length > 50) {
    return '密码长度需要在 6 到 50 个字符之间'
  }
  if (form.password !== form.confirm) {
    return '两次输入的密码不一致'
  }
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) return

  loading.value = true
  try {
    await auth.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
    })
    toast.success('注册成功，请登录')
    await router.replace({ name: 'login', query: { email: form.email.trim() } })
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
      <h1>创建账号</h1>
      <p>注册后会自动获得一个默认知识库。</p>
    </div>

    <form class="form-stack" @submit.prevent="submit">
      <label class="field">
        <span class="label">用户名</span>
        <input
          v-model="form.username"
          class="input"
          type="text"
          autocomplete="username"
          placeholder="3 到 50 个字符"
          :disabled="loading"
        />
      </label>

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
          autocomplete="new-password"
          placeholder="至少 6 位"
          :disabled="loading"
        />
      </label>

      <label class="field">
        <span class="label">确认密码</span>
        <input
          v-model="form.confirm"
          class="input"
          type="password"
          autocomplete="new-password"
          placeholder="再输入一次"
          :disabled="loading"
        />
      </label>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <button class="btn btn--primary btn--block" type="submit" :disabled="loading">
        <span v-if="loading" class="spinner" />
        <template v-else>
          创建账号
          <Icon name="arrow-right" :size="15" />
        </template>
      </button>
    </form>

    <p class="auth-switch">
      已有账号？
      <RouterLink :to="{ name: 'login' }">去登录</RouterLink>
    </p>
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
</style>
