<template>
  <div class="auth-card">
    <h2 class="auth-title">欢迎登录</h2>
    <p class="auth-subtitle">登录您的 DevMatch 账号</p>

    <el-tabs v-model="loginType" class="login-tabs">
      <el-tab-pane label="账号密码登录" name="password" />
      <el-tab-pane label="验证码登录" name="sms" />
    </el-tabs>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
      <el-form-item label="手机号 / 邮箱" prop="account">
        <el-input v-model="form.account" placeholder="请输入手机号或邮箱" size="large" clearable>
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>

      <template v-if="loginType === 'password'">
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入验证码" size="large">
              <template #prefix><el-icon><Key /></el-icon></template>
            </el-input>
            <el-button size="large" :disabled="codeCounting" @click="sendCode">
              {{ codeCounting ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </template>

      <div class="login-actions">
        <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        <router-link to="/auth/forgot-password">忘记密码？</router-link>
      </div>

      <el-button
        type="primary"
        size="large"
        class="submit-btn"
        :loading="loading"
        @click="handleLogin"
      >
        登录
      </el-button>
    </el-form>

    <div class="auth-divider">
      <span>还没有账号？</span>
      <router-link to="/auth/register">立即注册</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const loginType = ref('password')
const rememberMe = ref(false)
const codeCounting = ref(false)
const countdown = ref(60)

const form = reactive({
  account: '',
  password: '',
  code: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function sendCode() {
  if (!form.account) {
    ElMessage.warning('请先输入手机号或邮箱')
    return
  }
  codeCounting.value = true
  countdown.value = 60
  try {
    if (form.account.includes('@')) {
      await authApi.sendEmailCode(form.account)
    } else {
      await authApi.sendSmsCode(form.account)
    }
    ElMessage.success('验证码已发送')
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        codeCounting.value = false
      }
    }, 1000)
  } catch {
    codeCounting.value = false
  }
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const payload = loginType.value === 'password'
      ? { username: form.account, password: form.password, loginType: 'PASSWORD' }
      : { username: form.account, smsCode: form.code, loginType: 'SMS' }
    const data = await userStore.login(payload)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
      const roleMap = {
        DEVELOPER: '/developer/dashboard',
        ENTERPRISE: '/enterprise/dashboard',
        ADMIN: '/admin/dashboard'
      }
      router.push(roleMap[(data.userInfo || data.user)?.role] || '/')
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err?.data?.message || err?.message || '登录失败，请检查账号或密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-card {
  width: 420px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}

.auth-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.auth-subtitle {
  color: var(--text-secondary);
  margin-bottom: 24px;
  font-size: 14px;
}

.login-tabs {
  margin-bottom: 8px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
  .el-input { flex: 1; }
  .el-button { flex-shrink: 0; }
}

.login-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
}

.auth-divider {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  a {
    font-weight: 600;
    color: var(--primary-color);
    margin-left: 4px;
  }
}
</style>
