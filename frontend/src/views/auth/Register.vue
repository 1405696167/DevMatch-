<template>
  <div class="auth-card">
    <h2 class="auth-title">创建账号</h2>
    <p class="auth-subtitle">加入 DevMatch，开启合作之旅</p>

    <el-steps :active="step" finish-status="success" align-center class="reg-steps">
      <el-step title="选择身份" />
      <el-step title="填写信息" />
      <el-step title="验证账号" />
    </el-steps>

    <!-- Step 1: 选择身份 -->
    <div v-if="step === 0" class="step-content">
      <div class="role-cards">
        <div
          class="role-card"
          :class="{ selected: form.role === 'DEVELOPER' }"
          @click="form.role = 'DEVELOPER'"
        >
          <el-icon size="40" color="#4f46e5"><UserFilled /></el-icon>
          <div class="role-name">开发者 / 自由职业者</div>
          <div class="role-desc">承接软件开发项目，展示技能，获取收入</div>
        </div>
        <div
          class="role-card"
          :class="{ selected: form.role === 'ENTERPRISE' }"
          @click="form.role = 'ENTERPRISE'"
        >
          <el-icon size="40" color="#0ea5e9"><OfficeBuilding /></el-icon>
          <div class="role-name">企业 / 需求方</div>
          <div class="role-desc">发布开发需求，寻找优质开发者团队</div>
        </div>
      </div>
      <el-button type="primary" size="large" class="next-btn" :disabled="!form.role" @click="step++">
        下一步
      </el-button>
    </div>

    <!-- Step 2: 填写信息 -->
    <div v-if="step === 1" class="step-content">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item v-if="form.role === 'ENTERPRISE'" label="企业名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入企业名称" size="large" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱（可选）" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码（8位以上）" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
        </el-form-item>
      </el-form>
      <div class="step-btns">
        <el-button size="large" @click="step--">上一步</el-button>
        <el-button type="primary" size="large" @click="nextStep">下一步</el-button>
      </div>
    </div>

    <!-- Step 3: 验证 -->
    <div v-if="step === 2" class="step-content">
      <div class="verify-tip">
        <el-icon size="48" color="#4f46e5"><Message /></el-icon>
        <p>验证码已发送至手机 <strong>{{ form.phone }}</strong></p>
      </div>
      <el-form ref="codeFormRef" :model="form" label-position="top">
        <el-form-item label="短信验证码" prop="code" :rules="[{required:true,message:'请输入验证码'}]">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入6位验证码" size="large" maxlength="6" />
            <el-button size="large" :disabled="codeCounting" @click="sendCode">
              {{ codeCounting ? `${countdown}s` : '重新发送' }}
            </el-button>
          </div>
        </el-form-item>
        <el-checkbox v-model="form.agreed">
          我已阅读并同意
          <el-link type="primary">《用户协议》</el-link>
          和
          <el-link type="primary">《隐私政策》</el-link>
        </el-checkbox>
      </el-form>
      <div class="step-btns">
        <el-button size="large" @click="step--">上一步</el-button>
        <el-button type="primary" size="large" :loading="loading" :disabled="!form.agreed" @click="handleRegister">
          完成注册
        </el-button>
      </div>
    </div>

    <div class="auth-divider">
      <span>已有账号？</span>
      <router-link to="/auth/login">立即登录</router-link>
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
const formRef = ref()
const codeFormRef = ref()
const codeCounting = ref(false)
const countdown = ref(60)

const form = reactive({
  role: '',
  nickname: '',
  companyName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
  agreed: false
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称' }],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码' },
    { min: 8, message: '密码至少8位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次密码不一致'))
        else callback()
      }
    }
  ]
}

async function nextStep() {
  await formRef.value?.validate()
  step.value++
  sendCode()
}

async function sendCode() {
  if (codeCounting.value) return
  codeCounting.value = true
  countdown.value = 60
  try {
    await authApi.sendSmsCode(form.phone)
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

async function handleRegister() {
  await codeFormRef.value?.validate()
  loading.value = true
  try {
    await authApi.register({
      role: form.role,
      nickname: form.nickname,
      companyName: form.companyName,
      phone: form.phone,
      email: form.email,
      password: form.password,
      code: form.code
    })
    ElMessage.success('注册成功，请登录')
    router.push('/auth/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-card {
  width: 480px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}

.auth-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 6px;
}

.auth-subtitle {
  color: var(--text-secondary);
  margin-bottom: 24px;
  font-size: 14px;
}

.reg-steps {
  margin-bottom: 28px;
}

.step-content {
  min-height: 240px;
}

.role-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.role-card {
  border: 2px solid var(--border-color);
  border-radius: 12px;
  padding: 20px 16px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
  &:hover { border-color: var(--primary-light); background: #fafaf9; }
  &.selected { border-color: var(--primary-color); background: #ede9fe; }
  .role-name { font-weight: 600; margin: 10px 0 6px; font-size: 14px; }
  .role-desc { font-size: 12px; color: var(--text-secondary); }
}

.next-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

.step-btns {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  .el-button { flex: 1; height: 44px; }
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
  .el-input { flex: 1; }
}

.verify-tip {
  text-align: center;
  padding: 20px 0;
  color: var(--text-secondary);
  font-size: 14px;
  strong { color: var(--text-primary); }
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
