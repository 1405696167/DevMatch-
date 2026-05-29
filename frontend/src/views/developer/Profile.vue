<template>
  <div class="page-container">
    <div class="profile-layout">
      <!-- 左侧基本信息 -->
      <aside class="profile-side">
        <div class="card avatar-card">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              accept="image/*"
              :http-request="handleAvatarUpload"
            >
              <el-avatar :size="80" :src="form.avatar">{{ form.nickname?.charAt(0) }}</el-avatar>
              <div class="avatar-overlay"><el-icon><Camera /></el-icon></div>
            </el-upload>
            <div class="user-name">{{ form.nickname }}</div>
            <div class="user-role-tag">
              <el-tag type="success">开发者</el-tag>
              <el-tag v-if="kycStatus === 'VERIFIED'" type="success" size="small">已实名</el-tag>
              <el-tag v-else type="warning" size="small">未实名</el-tag>
            </div>
          </div>
          <el-divider />
          <div class="profile-stats">
            <div class="profile-stat">
              <div class="stat-num">{{ profileData.completedProjects }}</div>
              <div class="stat-lbl">完成项目</div>
            </div>
            <div class="profile-stat">
              <div class="stat-num">{{ profileData.creditScore }}</div>
              <div class="stat-lbl">信用分</div>
            </div>
            <div class="profile-stat">
              <div class="stat-num">{{ profileData.rateGood }}%</div>
              <div class="stat-lbl">好评率</div>
            </div>
          </div>
        </div>

        <!-- 实名认证 -->
        <div class="card">
          <div class="card-section-title">实名认证</div>
          <div v-if="kycStatus === 'VERIFIED'" class="kyc-verified">
            <el-icon color="#10b981" size="20"><CircleCheckFilled /></el-icon>
            <span>已通过实名认证</span>
          </div>
          <div v-else-if="kycStatus === 'AUDITING'" class="kyc-pending">
            <el-icon color="#f59e0b" size="20"><Clock /></el-icon>
            <span>认证审核中</span>
          </div>
          <div v-else>
            <p class="kyc-tip">实名认证可提升信任度，获得更多订单机会</p>
            <el-button type="primary" style="width:100%" @click="kycDialogVisible = true">立即认证</el-button>
          </div>
        </div>
      </aside>

      <!-- 右侧详细信息 -->
      <div class="profile-main">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <div class="card">
              <el-form :model="form" label-width="100px">
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" />
                </el-form-item>
                <el-form-item label="个人简介">
                  <el-input v-model="form.bio" type="textarea" :rows="4" placeholder="介绍一下自己..." maxlength="500" show-word-limit />
                </el-form-item>
                <el-form-item label="所在城市">
                  <el-input v-model="form.city" placeholder="如：北京" />
                </el-form-item>
                <el-form-item label="个人主页">
                  <el-input v-model="form.homepage" placeholder="GitHub/Portfolio 链接" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveBasicInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 技能标签 -->
          <el-tab-pane label="技能标签" name="skills">
            <div class="card">
              <div class="section-header">
                <h3>我的技能</h3>
                <el-button type="primary" size="small" @click="skillDialogVisible = true">+ 添加技能</el-button>
              </div>
              <div class="skills-list">
                <div v-for="s in skills" :key="s.id" class="skill-item">
                  <span class="skill-name">{{ s.name }}</span>
                  <div class="skill-level">
                    <el-rate v-model="s.level" :max="5" @change="() => onSkillLevelChange(s)" />
                  </div>
                  <el-button type="danger" link size="small" @click="deleteSkill(s.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 项目经验 -->
          <el-tab-pane label="项目经验" name="projects">
            <div class="card">
              <div class="section-header">
                <h3>项目经验</h3>
                <el-button type="primary" size="small" @click="openAddProject">+ 添加项目</el-button>
              </div>
              <div class="projects-list">
                <div v-for="p in portfolioProjects" :key="p.id" class="portfolio-item">
                  <div class="portfolio-header">
                    <h4>{{ p.name }}</h4>
                    <div class="portfolio-actions">
                      <el-button link size="small" @click="openEditProject(p)"><el-icon><Edit /></el-icon></el-button>
                      <el-button link size="small" type="danger" @click="deleteProject(p.id)"><el-icon><Delete /></el-icon></el-button>
                    </div>
                  </div>
                  <p class="portfolio-desc">{{ p.description }}</p>
                  <div class="portfolio-tags">
                    <span v-for="t in p.tags" :key="t" class="tag tag-primary">{{ t }}</span>
                  </div>
                  <el-link v-if="p.link" :href="p.link" target="_blank" type="primary">查看项目</el-link>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 账户安全 -->
          <el-tab-pane label="账户安全" name="security">
            <div class="card">
              <div class="security-item">
                <div class="security-info">
                  <div class="security-name">登录密码</div>
                  <div class="security-desc">已设置，建议定期更换密码</div>
                </div>
                <el-button @click="changePwdVisible = true">修改密码</el-button>
              </div>
              <el-divider />
              <div class="security-item">
                <div class="security-info">
                  <div class="security-name">手机号</div>
                  <div class="security-desc">{{ maskPhone(form.phone) }}</div>
                </div>
                <el-button @click="hintBindChange('手机')">更换手机号</el-button>
              </div>
              <el-divider />
              <div class="security-item">
                <div class="security-info">
                  <div class="security-name">邮箱</div>
                  <div class="security-desc">{{ form.email || '未绑定' }}</div>
                </div>
                <el-button @click="hintBindChange('邮箱')">{{ form.email ? '更换邮箱' : '绑定邮箱' }}</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>

  <!-- 实名认证对话框 -->
  <el-dialog v-model="kycDialogVisible" title="实名认证" width="480px" :close-on-click-modal="false">
    <el-form :model="kycForm" :rules="kycRules" ref="kycFormRef" label-width="100px">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
        实名认证信息仅用于身份核验，平台将严格保密
      </el-alert>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="kycForm.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idNumber">
        <el-input v-model="kycForm.idNumber" placeholder="请输入18位身份证号码" maxlength="18" />
      </el-form-item>
      <el-form-item label="身份证正面" required>
        <div class="kyc-upload-box" @click="triggerFileInput('front')">
          <img v-if="kycFrontPreview" :src="kycFrontPreview" class="kyc-preview" />
          <div v-else class="kyc-placeholder">
            <el-icon size="24"><Picture /></el-icon>
            <span>点击上传正面照片</span>
          </div>
        </div>
        <input ref="frontInputRef" type="file" accept="image/*" style="display:none" @change="onFileInputChange($event, 'front')" />
      </el-form-item>
      <el-form-item label="身份证背面" required>
        <div class="kyc-upload-box" @click="triggerFileInput('back')">
          <img v-if="kycBackPreview" :src="kycBackPreview" class="kyc-preview" />
          <div v-else class="kyc-placeholder">
            <el-icon size="24"><Picture /></el-icon>
            <span>点击上传背面照片</span>
          </div>
        </div>
        <input ref="backInputRef" type="file" accept="image/*" style="display:none" @change="onFileInputChange($event, 'back')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="kycDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="kycSubmitting" @click="submitKyc">提交认证</el-button>
    </template>
  </el-dialog>

  <!-- 添加技能 -->
  <el-dialog v-model="skillDialogVisible" title="添加技能" width="420px" destroy-on-close @closed="skillForm.name = ''; skillForm.level = 3">
    <el-form :model="skillForm" label-width="88px">
      <el-form-item label="技能名称" required>
        <el-input v-model="skillForm.name" placeholder="如：Vue3、Java" maxlength="64" show-word-limit />
      </el-form-item>
      <el-form-item label="熟练度">
        <el-rate v-model="skillForm.level" :max="5" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="skillDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="skillSaving" @click="submitAddSkill">保存</el-button>
    </template>
  </el-dialog>

  <!-- 项目经验 -->
  <el-dialog v-model="projectDialogVisible" :title="editingPortfolioId ? '编辑项目经验' : '添加项目经验'" width="520px" destroy-on-close>
    <el-form :model="portfolioForm" label-width="96px">
      <el-form-item label="项目名称" required>
        <el-input v-model="portfolioForm.name" maxlength="128" show-word-limit />
      </el-form-item>
      <el-form-item label="项目描述">
        <el-input v-model="portfolioForm.description" type="textarea" :rows="4" maxlength="2000" show-word-limit />
      </el-form-item>
      <el-form-item label="技术标签">
        <el-input v-model="portfolioForm.tagsStr" placeholder="多个标签用英文或中文逗号分隔" />
      </el-form-item>
      <el-form-item label="项目链接">
        <el-input v-model="portfolioForm.link" placeholder="https://..." />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="projectDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="portfolioSaving" @click="submitPortfolio">保存</el-button>
    </template>
  </el-dialog>

  <!-- 修改密码 -->
  <el-dialog v-model="changePwdVisible" title="修改密码" width="440px" destroy-on-close @closed="pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''">
    <el-form :model="pwdForm" label-width="96px">
      <el-form-item label="原密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="changePwdVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdSaving" @click="submitChangePassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { usersApi } from '@/api/users'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'

const userStore = useUserStore()
const route = useRoute()
const activeTab = ref('basic')
const saving = ref(false)
const kycStatus = ref('NONE')
const kycDialogVisible = ref(false)
const kycSubmitting = ref(false)
const kycFormRef = ref()
const kycFrontPreview = ref('')
const kycBackPreview = ref('')
const kycFrontFile = ref(null)
const kycBackFile = ref(null)
const frontInputRef = ref()
const backInputRef = ref()
const kycForm = reactive({ realName: '', idNumber: '' })
const kycRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idNumber: [
    { required: true, message: '请输入身份证号码', trigger: 'blur' },
    { pattern: /^\d{17}[\dX]$/, message: '请输入正确的18位身份证号码', trigger: 'blur' }
  ]
}
const skillDialogVisible = ref(false)
const skillSaving = ref(false)
const skillForm = reactive({ name: '', level: 3 })
const changePwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const projectDialogVisible = ref(false)
const portfolioSaving = ref(false)
const editingPortfolioId = ref(null)
const portfolioForm = reactive({ name: '', description: '', link: '', tagsStr: '' })

const form = reactive({
  nickname: userStore.userName,
  bio: '',
  city: '',
  homepage: '',
  phone: '',
  email: '',
  avatar: userStore.userAvatar
})

const profileData = ref({ completedProjects: 0, creditScore: 0, rateGood: 0 })
const skills = ref([])
const portfolioProjects = ref([])

function maskPhone(p) {
  if (!p) return '未绑定'
  return p.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

async function saveBasicInfo() {
  saving.value = true
  try {
    await usersApi.updateProfile({
      nickname: form.nickname,
      bio: form.bio,
      city: form.city,
      homepage: form.homepage
    })
    userStore.setUserInfo({
      ...userStore.userInfo,
      nickname: form.nickname,
      bio: form.bio,
      city: form.city,
      homepage: form.homepage
    })
    ElMessage.success('保存成功')
  } catch { /* 拦截器已提示 */ } finally { saving.value = false }
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
    ElMessage.success('头像更新成功')
  } catch (e) {
    onError?.(e)
  }
}

function normalizePortfolio(p) {
  let tags = p.tags
  if (!Array.isArray(tags)) {
    if (typeof tags === 'string') {
      try { tags = JSON.parse(tags) } catch { tags = [] }
    } else {
      tags = []
    }
  }
  return { ...p, tags }
}

async function loadProfileData() {
  try {
    const kycRes = await usersApi.getKycStatus()
    const rec = kycRes.data
    kycStatus.value = rec?.status || userStore.userInfo?.kycStatus || 'NONE'
  } catch {
    kycStatus.value = userStore.userInfo?.kycStatus || 'NONE'
  }

  try {
    const res = await usersApi.getResume()
    const bag = res.data || {}
    const u = bag.user || {}
    Object.assign(form, {
      nickname: u.nickname ?? form.nickname,
      bio: u.bio ?? '',
      city: u.city ?? '',
      homepage: u.homepage ?? '',
      phone: u.phone ?? '',
      email: u.email ?? '',
      avatar: u.avatar ?? form.avatar
    })
    skills.value = (bag.skills || []).map(s => ({ ...s, level: Number(s.level) || 3 }))
    portfolioProjects.value = (bag.portfolios || []).map(normalizePortfolio)
    profileData.value = {
      completedProjects: bag.completedProjects ?? 0,
      creditScore: u.creditScore ?? 100,
      rateGood: bag.rateGood ?? 0
    }
  } catch {
    ElMessage.error('加载简历数据失败')
  }
}

async function onSkillLevelChange(s) {
  try {
    await usersApi.updateSkill(s.id, { level: s.level })
    ElMessage.success('熟练度已保存')
  } catch {
    await loadProfileData()
  }
}

async function submitAddSkill() {
  if (!skillForm.name?.trim()) {
    ElMessage.warning('请输入技能名称')
    return
  }
  skillSaving.value = true
  try {
    await usersApi.addSkill({ name: skillForm.name.trim(), level: skillForm.level })
    ElMessage.success('已添加技能')
    skillDialogVisible.value = false
    await loadProfileData()
  } finally {
    skillSaving.value = false
  }
}

async function deleteSkill(id) {
  try {
    await usersApi.deleteSkill(id)
    skills.value = skills.value.filter(s => s.id !== id)
    ElMessage.success('已删除')
  } catch { /* 拦截器已提示 */ }
}

function openAddProject() {
  editingPortfolioId.value = null
  portfolioForm.name = ''
  portfolioForm.description = ''
  portfolioForm.link = ''
  portfolioForm.tagsStr = ''
  projectDialogVisible.value = true
}

function openEditProject(p) {
  editingPortfolioId.value = p.id
  portfolioForm.name = p.name || ''
  portfolioForm.description = p.description || ''
  portfolioForm.link = p.link || ''
  portfolioForm.tagsStr = (p.tags || []).join(', ')
  projectDialogVisible.value = true
}

async function submitPortfolio() {
  if (!portfolioForm.name?.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  const tags = portfolioForm.tagsStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
  const payload = {
    name: portfolioForm.name.trim(),
    description: portfolioForm.description || '',
    link: portfolioForm.link || '',
    tags
  }
  portfolioSaving.value = true
  try {
    if (editingPortfolioId.value) {
      await usersApi.updateProject(editingPortfolioId.value, payload)
      ElMessage.success('已更新')
    } else {
      await usersApi.addProject(payload)
      ElMessage.success('已添加')
    }
    projectDialogVisible.value = false
    await loadProfileData()
  } finally {
    portfolioSaving.value = false
  }
}

async function deleteProject(id) {
  try {
    await ElMessageBox.confirm('确定删除该项目经验？', '提示', { type: 'warning' })
    await usersApi.deleteProject(id)
    portfolioProjects.value = portfolioProjects.value.filter(p => p.id !== id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') { /* api 错误由拦截器提示 */ }
  }
}

function hintBindChange(label) {
  ElMessage.info(`${label}绑定/换绑功能当前版本未开放，如需修改请联系管理员或通过注册账号处理`)
}

async function submitChangePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await usersApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已修改，请牢记新密码')
    changePwdVisible.value = false
  } catch { /* 拦截器已提示 */ } finally {
    pwdSaving.value = false
  }
}

function triggerFileInput(side) {
  if (side === 'front') frontInputRef.value?.click()
  else backInputRef.value?.click()
}

function onFileInputChange(event, side) {
  const file = event.target.files?.[0]
  if (!file) return
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.warning('图片大小不能超过5MB')
    event.target.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    if (side === 'front') {
      kycFrontPreview.value = e.target.result
      kycFrontFile.value = file
    } else {
      kycBackPreview.value = e.target.result
      kycBackFile.value = file
    }
  }
  reader.readAsDataURL(file)
}

async function submitKyc() {
  await kycFormRef.value?.validate()
  if (!kycFrontFile.value) { ElMessage.warning('请上传身份证正面照片'); return }
  if (!kycBackFile.value) { ElMessage.warning('请上传身份证背面照片'); return }
  kycSubmitting.value = true
  try {
    const fd = new FormData()
    fd.append('realName', kycForm.realName)
    fd.append('idNumber', kycForm.idNumber)
    fd.append('idFrontFile', kycFrontFile.value)
    fd.append('idBackFile', kycBackFile.value)
    await usersApi.submitKyc(fd)
    ElMessage.success('实名认证申请已提交，请等待审核')
    kycDialogVisible.value = false
    kycStatus.value = 'AUDITING'
    userStore.setUserInfo({ ...userStore.userInfo, kycStatus: 'AUDITING' })
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    kycSubmitting.value = false
  }
}

onMounted(async () => {
  if (route.query.tab === 'kyc') {
    kycDialogVisible.value = true
  }
  await loadProfileData()
})
</script>

<style scoped lang="scss">
.profile-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  align-items: start;
}

.avatar-card { text-align: center; margin-bottom: 16px; }
.avatar-section { padding: 8px 0 16px; }

.avatar-uploader {
  display: inline-block;
  position: relative;
  cursor: pointer;
  &:hover .avatar-overlay { opacity: 1; }
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: white;
}

.user-name { font-size: 18px; font-weight: 700; margin: 10px 0 8px; }
.user-role-tag { display: flex; justify-content: center; gap: 6px; }

.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 4px;
}
.profile-stat { text-align: center; }
.stat-num { font-size: 20px; font-weight: 700; color: var(--primary-color); }
.stat-lbl { font-size: 11px; color: var(--text-muted); }

.card-section-title { font-weight: 600; margin-bottom: 12px; }
.kyc-verified, .kyc-pending {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px;
  .kyc-verified { color: #10b981; }
  .kyc-pending { color: #f59e0b; }
}
.kyc-tip { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }

.section-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
  h3 { font-size: 15px; font-weight: 600; }
}

.skills-list { display: flex; flex-direction: column; gap: 12px; }
.skill-item {
  display: flex; align-items: center; gap: 12px;
  .skill-name { width: 100px; font-size: 14px; font-weight: 500; }
  .skill-level { flex: 1; }
}

.projects-list { display: flex; flex-direction: column; gap: 16px; }
.portfolio-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.portfolio-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
  h4 { font-size: 14px; font-weight: 600; }
}
.portfolio-desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.portfolio-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }

.security-item {
  display: flex; align-items: center; justify-content: space-between; padding: 8px 0;
}
.security-name { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.security-desc { font-size: 12px; color: var(--text-muted); }

.kyc-upload-box {
  width: 160px; height: 100px;
  border: 1px dashed var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  &:hover { border-color: var(--primary-color); }
}
.kyc-preview { width: 100%; height: 100%; object-fit: cover; }
.kyc-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  color: var(--text-muted); font-size: 12px;
}
</style>
