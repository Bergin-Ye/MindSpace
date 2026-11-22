<script setup>
import { computed, reactive, ref } from 'vue'
import Icon from '../components/Icon.vue'
import { updatePassword, updateProfile } from '../api/auth'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { formatDateTime } from '../utils/format'

const auth = useAuthStore()
const toast = useToastStore()

const profile = reactive({
  username: auth.user?.username || '',
  avatar: auth.user?.avatar || '',
})
const profileSaving = ref(false)
const profileError = ref('')

const password = reactive({
  oldPassword: '',
  newPassword: '',
  confirm: '',
})
const passwordSaving = ref(false)
const passwordError = ref('')

const email = computed(() => auth.user?.email || '—')

async function saveProfile() {
  profileError.value = ''
  const username = profile.username.trim()
  if (username.length < 3 || username.length > 50) {
    profileError.value = '用户名长度需要在 3 到 50 个字符之间'
    return
  }

  profileSaving.value = true
  try {
    const user = await updateProfile({
      username,
      avatar: profile.avatar.trim(),
    })
    auth.updateUser(user)
    toast.success('资料已更新')
  } catch (error) {
    profileError.value = errorMessage(error)
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  passwordError.value = ''
  if (!password.oldPassword) {
    passwordError.value = '请填写当前密码'
    return
  }
  if (password.newPassword.length < 6 || password.newPassword.length > 50) {
    passwordError.value = '新密码长度需要在 6 到 50 个字符之间'
    return
  }
  if (password.newPassword !== password.confirm) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }

  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: password.oldPassword,
      newPassword: password.newPassword,
    })
    password.oldPassword = ''
    password.newPassword = ''
    password.confirm = ''
    toast.success('密码已修改，当前登录仍然有效')
  } catch (error) {
    passwordError.value = errorMessage(error)
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <div class="page">
    <header class="page__head">
      <div>
        <h1 class="page__title">账户设置</h1>
        <p class="page__desc">修改用户名、头像地址和密码。邮箱是登录账号，暂不支持修改。</p>
      </div>
    </header>

    <div class="settings">
      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">个人资料</span>
          <span class="mono muted">注册于 {{ formatDateTime(auth.user?.createTime) }}</span>
        </div>
        <form class="panel__body form-stack" @submit.prevent="saveProfile">
          <label class="field">
            <span class="label">邮箱</span>
            <input class="input" type="email" :value="email" disabled />
            <span class="hint">邮箱是登录账号，需要额外的验证流程才能修改。</span>
          </label>

          <label class="field">
            <span class="label">用户名</span>
            <input
              v-model="profile.username"
              class="input"
              type="text"
              maxlength="50"
              :disabled="profileSaving"
            />
          </label>

          <label class="field">
            <span class="label">头像地址</span>
            <input
              v-model="profile.avatar"
              class="input"
              type="url"
              maxlength="500"
              placeholder="https://example.com/avatar.png"
              :disabled="profileSaving"
            />
            <span class="hint">填图片 URL；留空表示不设置头像，界面会显示用户名首字。</span>
          </label>

          <p v-if="profileError" class="form-error" role="alert">{{ profileError }}</p>

          <div class="row">
            <button class="btn btn--primary" type="submit" :disabled="profileSaving">
              <span v-if="profileSaving" class="spinner" />
              保存资料
            </button>
          </div>
        </form>
      </section>

      <section class="panel">
        <div class="panel__head">
          <span class="panel__title">修改密码</span>
        </div>
        <form class="panel__body form-stack" @submit.prevent="savePassword">
          <label class="field">
            <span class="label">当前密码</span>
            <input
              v-model="password.oldPassword"
              class="input"
              type="password"
              autocomplete="current-password"
              :disabled="passwordSaving"
            />
          </label>

          <div class="form-grid">
            <label class="field">
              <span class="label">新密码</span>
              <input
                v-model="password.newPassword"
                class="input"
                type="password"
                autocomplete="new-password"
                placeholder="至少 6 位"
                :disabled="passwordSaving"
              />
            </label>

            <label class="field">
              <span class="label">确认新密码</span>
              <input
                v-model="password.confirm"
                class="input"
                type="password"
                autocomplete="new-password"
                :disabled="passwordSaving"
              />
            </label>
          </div>

          <p v-if="passwordError" class="form-error" role="alert">{{ passwordError }}</p>
          <p class="hint">
            修改密码后，之前签发的 token 仍会有效到过期时间。
          </p>

          <div class="row">
            <button class="btn btn--primary" type="submit" :disabled="passwordSaving">
              <span v-if="passwordSaving" class="spinner" />
              修改密码
            </button>
          </div>
        </form>
      </section>

      <section class="panel">
        <div class="panel__body tips">
          <div class="tips__item">
            <Icon name="layers" :size="16" />
            <div>
              <p class="tips__title">让文档先完成解析和向量化</p>
              <p class="tips__desc">摘要、语义检索和问答都依赖这一步；解析是异步的，状态会自己刷新。</p>
            </div>
          </div>
          <div class="tips__item">
            <Icon name="info" :size="16" />
            <div>
              <p class="tips__title">AI 功能需要后端配置</p>
              <p class="tips__desc">没有配置模型 key 时，摘要和问答会返回“AI 服务未配置”，其他功能不受影响。</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  max-width: 780px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tips {
  display: flex;
  flex-direction: column;
  gap: 14px;
  color: var(--muted);
}

.tips__item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.tips__title {
  color: var(--ink-2);
  font-size: 12px;
  font-weight: 500;
}

.tips__desc {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 680px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
