<template>
  <div class="page-container">
    <h2 class="page-title">企业信息</h2>
    <div class="profile-layout">
      <div class="card main-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form :model="form" label-width="120px">
              <el-form-item label="企业名称"><el-input v-model="form.companyName" /></el-form-item>
              <el-form-item label="企业简介"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
              <el-form-item label="所在城市"><el-input v-model="form.city" /></el-form-item>
              <el-form-item label="企业官网"><el-input v-model="form.website" /></el-form-item>
              <el-form-item label="联系邮箱"><el-input v-model="form.email" /></el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="企业认证" name="kyc">
            <div v-if="kycStatus === 'VERIFIED'" class="kyc-verified">
              <el-result icon="success" title="企业认证通过" sub-title="您的企业资质已通过认证" />
            </div>
            <div v-else-if="kycStatus === 'AUDITING'">
              <el-result icon="warning" title="认证审核中" sub-title="预计1-3个工作日完成审核" />
            </div>
            <div v-else>
              <el-alert type="info" :closable="false" style="margin-bottom:20px">
                企业认证后可发布需求、进行充值，提升平台信任度。请填写完整信息并上传相关证件。
              </el-alert>
              <el-form ref="kycFormRef" :model="kycForm" label-width="160px" :rules="kycRules">
                <el-divider content-position="left"><span class="form-section-title">企业基本信息</span></el-divider>
                <el-form-item label="企业名称" prop="companyName">
                  <el-input v-model="kycForm.companyName" placeholder="请输入营业执照上的企业全称" />
                </el-form-item>
                <el-form-item label="统一社会信用代码" prop="creditCode">
                  <el-input v-model="kycForm.creditCode" placeholder="18位统一社会信用代码" maxlength="18" show-word-limit />
                </el-form-item>
                <el-form-item label="营业执照" prop="licenseFile">
                  <el-upload
                    :auto-upload="false"
                    :show-file-list="false"
                    accept="image/*,.pdf"
                    :on-change="(f) => handleFileChange(f, 'license')"
                  >
                    <el-button><el-icon><UploadFilled /></el-icon> 上传营业执照</el-button>
                    <template #tip><div class="el-upload__tip">支持图片或 PDF，不超过 10MB</div></template>
                  </el-upload>
                  <div v-if="kycForm.licenseFileName" class="file-preview">
                    <el-icon><Document /></el-icon>
                    <span>{{ kycForm.licenseFileName }}</span>
                    <el-button link type="danger" size="small" @click="kycForm.licenseFile = null; kycForm.licenseFileName = ''">移除</el-button>
                  </div>
                </el-form-item>

                <el-divider content-position="left"><span class="form-section-title">法人信息</span></el-divider>
                <el-form-item label="法人姓名" prop="legalPersonName">
                  <el-input v-model="kycForm.legalPersonName" placeholder="请输入法定代表人姓名" />
                </el-form-item>
                <el-form-item label="法人身份证号" prop="legalPersonId">
                  <el-input v-model="kycForm.legalPersonId" placeholder="请输入法人身份证号码" maxlength="18" />
                </el-form-item>
                <el-form-item label="法人身份证正面">
                  <el-upload
                    :auto-upload="false"
                    :show-file-list="false"
                    accept="image/*"
                    :on-change="(f) => handleFileChange(f, 'idFront')"
                  >
                    <el-button><el-icon><UploadFilled /></el-icon> 上传身份证正面</el-button>
                    <template #tip><div class="el-upload__tip">仅支持图片格式，不超过 5MB</div></template>
                  </el-upload>
                  <div v-if="kycForm.idFrontFileName" class="file-preview">
                    <el-icon><Document /></el-icon>
                    <span>{{ kycForm.idFrontFileName }}</span>
                    <el-button link type="danger" size="small" @click="kycForm.idFrontFile = null; kycForm.idFrontFileName = ''">移除</el-button>
                  </div>
                </el-form-item>
                <el-form-item label="法人身份证背面">
                  <el-upload
                    :auto-upload="false"
                    :show-file-list="false"
                    accept="image/*"
                    :on-change="(f) => handleFileChange(f, 'idBack')"
                  >
                    <el-button><el-icon><UploadFilled /></el-icon> 上传身份证背面</el-button>
                    <template #tip><div class="el-upload__tip">仅支持图片格式，不超过 5MB</div></template>
                  </el-upload>
                  <div v-if="kycForm.idBackFileName" class="file-preview">
                    <el-icon><Document /></el-icon>
                    <span>{{ kycForm.idBackFileName }}</span>
                    <el-button link type="danger" size="small" @click="kycForm.idBackFile = null; kycForm.idBackFileName = ''">移除</el-button>
                  </div>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" :loading="kycSubmitting" @click="submitKyc">提交认证</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div class="side-info">
        <div class="card">
          <div class="ent-avatar-section">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              accept="image/*"
              :http-request="handleAvatarUpload"
            >
              <el-avatar :size="72" :src="form.avatar" style="background:#0ea5e9">
                {{ (form.companyName || form.nickname || '?').charAt(0) }}
              </el-avatar>
              <div class="avatar-overlay"><el-icon><Camera /></el-icon></div>
            </el-upload>
            <div class="ent-name">{{ form.companyName }}</div>
            <div class="avatar-tip">点击相机更换企业头像</div>
            <el-tag v-if="kycStatus === 'VERIFIED'" type="success">已认证企业</el-tag>
          </div>
          <el-divider />
          <div class="ent-stats">
            <div class="ent-stat"><strong>{{ entStats.taskCount }}</strong><span>发布需求</span></div>
            <div class="ent-stat"><strong>{{ entStats.projectCount }}</strong><span>进行中项目</span></div>
            <div class="ent-stat"><strong>{{ entStats.rating }}</strong><span>综合评分</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'
import { usersApi } from '@/api/users'
import { tasksApi } from '@/api/tasks'
import { projectsApi } from '@/api/projects'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const route = useRoute()
const activeTab = ref('basic')
const kycStatus = ref('NONE')
const kycSubmitting = ref(false)
const kycFormRef = ref()
const form = reactive({
  companyName: userStore.userName,
  nickname: userStore.userInfo?.nickname || '',
  avatar: userStore.userAvatar || '',
  description: '',
  city: '',
  website: '',
  email: ''
})
const kycForm = reactive({
  companyName: userStore.userInfo?.companyName || userStore.userName || '',
  creditCode: '',
  licenseFile: null, licenseFileName: '',
  legalPersonName: '',
  legalPersonId: '',
  idFrontFile: null, idFrontFileName: '',
  idBackFile: null, idBackFileName: ''
})
const kycRules = {
  companyName: [{ required: true, message: '请输入企业名称' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码' }, { len: 18, message: '统一社会信用代码为18位' }],
  legalPersonName: [{ required: true, message: '请输入法人姓名' }],
  legalPersonId: [{ required: true, message: '请输入法人身份证号' }, { len: 18, message: '身份证号为18位' }]
}
const entStats = reactive({ taskCount: 0, projectCount: 0, rating: '暂无' })

async function loadStats() {
  try {
    const [tasksRes, projectsRes] = await Promise.allSettled([
      tasksApi.getList({ page: 1, size: 1 }),
      projectsApi.getList({ status: 'IN_PROGRESS' })
    ])
    if (tasksRes.status === 'fulfilled') entStats.taskCount = tasksRes.value.data?.total || 0
    if (projectsRes.status === 'fulfilled') {
      const list = projectsRes.value.data?.list || projectsRes.value.data || []
      entStats.projectCount = Array.isArray(list) ? list.length : 0
    }
  } catch { /* 保持默认值 0 */ }
}

async function loadProfileFromApi() {
  try {
    const res = await usersApi.getMyProfile()
    const u = res.data || {}
    form.companyName = u.companyName || u.nickname || form.companyName
    form.nickname = u.nickname || form.nickname
    form.avatar = u.avatar || ''
    form.description = u.bio || form.description
    form.city = u.city || ''
    form.website = u.homepage || ''
    form.email = u.email || ''
    if (u.companyName) kycForm.companyName = u.companyName
    userStore.setUserInfo({
      ...userStore.userInfo,
      avatar: form.avatar,
      companyName: form.companyName || userStore.userInfo?.companyName,
      nickname: form.nickname || userStore.userInfo?.nickname
    })
  } catch { /* 使用本地/缓存信息 */ }
}

async function handleAvatarUpload({ file, onSuccess, onError }) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await usersApi.uploadAvatar(fd)
    const url = res.data
    form.avatar = url
    userStore.setUserInfo({ ...userStore.userInfo, avatar: url })
    onSuccess?.(res)
    ElMessage.success('企业头像已更新')
  } catch (e) {
    onError?.(e)
  }
}

onMounted(() => {
  loadStats()
  loadProfileFromApi()
  // 支持 ?tab=kyc 直接跳到认证 tab
  if (route.query.tab) activeTab.value = route.query.tab
  // 从 userStore 同步最新 kycStatus
  kycStatus.value = userStore.kycStatus || 'NONE'
})

async function saveProfile() {
  await usersApi.updateProfile({
    companyName: form.companyName,
    bio: form.description,
    city: form.city,
    homepage: form.website,
    email: form.email
  })
  userStore.setUserInfo({
    ...userStore.userInfo,
    companyName: form.companyName,
    nickname: form.nickname || userStore.userInfo?.nickname,
    email: form.email
  })
  ElMessage.success('保存成功')
}

const FILE_LIMITS = { license: 10, idFront: 5, idBack: 5 }
const FILE_KEYS = {
  license: ['licenseFile', 'licenseFileName'],
  idFront: ['idFrontFile', 'idFrontFileName'],
  idBack: ['idBackFile', 'idBackFileName']
}

function handleFileChange(uploadFile, type) {
  const file = uploadFile.raw
  if (!file) return
  const limitMB = FILE_LIMITS[type] || 10
  if (file.size / 1024 / 1024 > limitMB) {
    ElMessage.error(`文件大小不能超过 ${limitMB}MB`)
    return
  }
  const [fileKey, nameKey] = FILE_KEYS[type]
  kycForm[fileKey] = file
  kycForm[nameKey] = file.name
}

async function submitKyc() {
  await kycFormRef.value?.validate()
  kycSubmitting.value = true
  try {
    const fd = new FormData()
    fd.append('companyName', kycForm.companyName)
    fd.append('creditCode', kycForm.creditCode)
    fd.append('legalPersonName', kycForm.legalPersonName)
    fd.append('legalPersonId', kycForm.legalPersonId)
    if (kycForm.licenseFile) fd.append('licenseFile', kycForm.licenseFile)
    if (kycForm.idFrontFile) fd.append('idFrontFile', kycForm.idFrontFile)
    if (kycForm.idBackFile) fd.append('idBackFile', kycForm.idBackFile)
    await usersApi.submitEnterpriseKyc(fd)
    kycStatus.value = 'AUDITING'
    userStore.setUserInfo({ ...userStore.userInfo, kycStatus: 'AUDITING' })
    ElMessage.success('已提交认证，请等待审核（预计1-3个工作日）')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.data?.message || '提交失败，请重试')
  } finally {
    kycSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-layout { display: grid; grid-template-columns: 1fr 260px; gap: 20px; }
.main-card {}
.ent-avatar-section { text-align: center; padding: 16px 0; }
.avatar-uploader {
  display: inline-block;
  position: relative;
  cursor: pointer;
  &:hover .avatar-overlay { opacity: 1; }
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: white;
  font-size: 22px;
}
.avatar-tip { font-size: 11px; color: var(--text-muted); margin: 6px 0 10px; }
.ent-name { font-size: 16px; font-weight: 600; margin: 10px 0 6px; }
.ent-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; text-align: center; }
.ent-stat { strong { display: block; font-size: 20px; font-weight: 700; color: #0ea5e9; } span { font-size: 11px; color: var(--text-muted); } }
.file-preview {
  display: flex; align-items: center; gap: 8px;
  margin-top: 8px; font-size: 13px; color: var(--text-secondary);
}
.form-section-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
</style>
