<template>
  <div class="auth-card">
    <div class="back-btn" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon> 返回
    </div>
    <h2 class="auth-title">找回密码</h2>
    <p class="auth-subtitle">通过手机号重置您的密码</p>

    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step title="验证身份" />
      <el-step title="重置密码" />
      <el-step title="完成" />
    </el-steps>

    <div v-if="step === 0" class="step-content">
      <el-form ref="step1Ref" :model="form" label-position="top">
        <el-form-item label="手机号" prop="phone" :rules="[{required:true,message:'请输入手机号'},{pattern:/^1[3-9]\d{9}$/,message:'格式不正确'}]">
          <el-input v-model="form.phone" placeholder="请输入注册时的手机号" size="large" />
        </el-form-item>
        <el-form-item label="验证码" prop="code" :rules="[{required:true,message:'请输入验证码'}]">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入6位验证码" size="large" />
            <el-button size="large" :disabled="codeCounting" @click="sendCode">
              {{ codeCounting ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <el-button type="primary" size="large" class="action-btn" :loading="loading" @click="verifyCode">下一步</el-button>
    </div>

    <div v-if="step === 1" class="step-content">
      <el-form ref="step2Ref" :model="form" label-position="top">
        <el-form-item label="新密码" prop="newPassword" :rules="[{required:true,message:'请输入新密码'},{min:8,message:'密码至少8位'}]">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码（8位以上）" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" :rules="[{required:true,message:'请确认密码'},{validator:confirmValidator}]">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" size="large" show-password />
        </el-form-item>
      </el-form>
      <el-button type="primary" size="large" class="action-btn" :loading="loading" @click="resetPassword">重置密码</el-button>
    </div>

    <div v-if="step === 2" class="success-step">
      <el-icon size="64" color="#10b981"><CircleCheckFilled /></el-icon>
      <h3>密码重置成功</h3>
      <p>您的密码已成功重置，请使用新密码登录</p>
      <el-button type="primary" size="large" class="action-btn" @click="router.push('/auth/login')">去登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const step1Ref = ref()
const step2Ref = ref()
const codeCounting = ref(false)
const countdown = ref(60)

const form = reactive({ phone: '', code: '', newPassword: '', confirmPassword: '' })

function confirmValidator(rule, value, callback) {
  if (value !== form.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

async function sendCode() {
  if (!form.phone) { ElMessage.warning('请先输入手机号'); return }
  codeCounting.value = true
  countdown.value = 60
  try {
    await authApi.sendSmsCode(form.phone)
    ElMessage.success('验证码已发送')
    const t = setInterval(() => { countdown.value--; if (countdown.value <= 0) { clearInterval(t); codeCounting.value = false } }, 1000)
  } catch { codeCounting.value = false }
}

async function verifyCode() {
  await step1Ref.value?.validate()
  step.value++
}

async function resetPassword() {
  await step2Ref.value?.validate()
  loading.value = true
  try {
    await authApi.resetPassword({ phone: form.phone, code: form.code, newPassword: form.newPassword })
    step.value++
  } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.auth-card {
  width: 440px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  margin-bottom: 20px;
  &:hover { color: var(--primary-color); }
}
.auth-title { font-size: 26px; font-weight: 700; margin-bottom: 6px; }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 24px; font-size: 14px; }
.steps { margin-bottom: 28px; }
.step-content { min-height: 200px; }
.code-row { display: flex; gap: 12px; .el-input { flex: 1; } }
.action-btn { width: 100%; height: 44px; font-size: 15px; font-weight: 600; margin-top: 8px; }
.success-step {
  text-align: center;
  padding: 20px 0;
  h3 { font-size: 18px; font-weight: 600; margin: 16px 0 8px; }
  p { color: var(--text-secondary); margin-bottom: 24px; }
}
</style>
