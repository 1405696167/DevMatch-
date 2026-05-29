<template>
  <div class="page-container">
    <div class="publish-header">
      <h2 class="page-title">{{ isEditMode ? '编辑需求' : '发布需求' }}</h2>
      <div class="publish-mode">
        <el-radio-group v-model="mode" size="small">
          <el-radio-button label="form">表单模式</el-radio-button>
          <el-radio-button label="wizard">向导模式</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 向导模式 -->
    <template v-if="mode === 'wizard'">
      <div class="wizard-steps">
        <el-steps :active="wizardStep" finish-status="success" align-center>
          <el-step title="基本信息" />
          <el-step title="技术要求" />
          <el-step title="预算&周期" />
          <el-step title="确认发布" />
        </el-steps>
      </div>

      <div class="card wizard-card">
        <!-- Step 0: 基本信息 -->
        <div v-if="wizardStep === 0">
          <h3 class="wizard-step-title">描述您的需求</h3>
          <el-form ref="step0Ref" :model="form" label-position="top">
            <el-form-item label="需求标题" prop="title" :rules="[{required:true,message:'请输入需求标题'}]">
              <el-input v-model="form.title" placeholder="简洁描述您的项目，如：Vue3后台管理系统开发" size="large" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="项目类型" prop="category">
              <el-select v-model="form.category" placeholder="请选择项目类型" size="large" style="width:100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item label="详细描述" prop="description" :rules="[{required:true,message:'请填写详细描述',min:50}]">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="8"
                placeholder="请详细描述您的需求，包括功能模块、技术规范、交付要求等..."
                maxlength="5000"
                show-word-limit
              />
            </el-form-item>
          </el-form>
          <div class="wizard-btns">
            <div />
            <el-button type="primary" size="large" @click="nextWizardStep">下一步</el-button>
          </div>
        </div>

        <!-- Step 1: 技术要求 -->
        <div v-if="wizardStep === 1">
          <h3 class="wizard-step-title">技术要求</h3>
          <el-form :model="form" label-position="top">
            <el-form-item label="所需技术栈">
              <div class="skills-picker">
                <div class="picked-skills">
                  <el-tag
                    v-for="s in form.skills"
                    :key="s"
                    closable
                    @close="removeSkill(s)"
                  >{{ s }}</el-tag>
                  <el-input
                    v-if="inputVisible"
                    ref="skillInputRef"
                    v-model="skillInput"
                    size="small"
                    style="width:120px"
                    @keyup.enter="addSkill"
                    @blur="addSkill"
                  />
                  <el-button v-else size="small" @click="showSkillInput">+ 自定义</el-button>
                </div>
                <div class="suggest-skills">
                  <span class="suggest-label">快速选择：</span>
                  <span
                    v-for="s in suggestSkills"
                    :key="s"
                    class="suggest-item"
                    :class="{ selected: form.skills.includes(s) }"
                    @click="toggleSkill(s)"
                  >{{ s }}</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="经验要求">
              <el-radio-group v-model="form.experience">
                <el-radio label="NONE">不限经验</el-radio>
                <el-radio label="JUNIOR">1-3年</el-radio>
                <el-radio label="SENIOR">3-5年</el-radio>
                <el-radio label="EXPERT">5年以上</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="是否需要实名认证开发者">
              <el-switch v-model="form.requireKyc" />
            </el-form-item>
            <el-form-item label="需求附件（可选）">
              <el-upload
                :http-request="handleUpload"
                :on-success="onUploadSuccess"
                :on-error="onUploadError"
                :show-file-list="false"
                multiple
                accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.txt,.png,.jpg,.jpeg"
              >
                <el-button><el-icon><UploadFilled /></el-icon> 上传需求文档</el-button>
                <template #tip><div class="el-upload__tip">支持 PDF、Word、Excel、图片等格式</div></template>
              </el-upload>
              <div v-if="form.attachments.length" class="attachment-list" style="margin-top:8px">
                <div v-for="(f, idx) in form.attachments" :key="idx" class="attachment-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ f.name }}</span>
                  <el-button link type="danger" size="small" @click="removeAttachment(idx)">删除</el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
          <div class="wizard-btns">
            <el-button size="large" @click="wizardStep--">上一步</el-button>
            <el-button type="primary" size="large" @click="wizardStep++">下一步</el-button>
          </div>
        </div>

        <!-- Step 2: 预算&周期 -->
        <div v-if="wizardStep === 2">
          <h3 class="wizard-step-title">预算与周期</h3>
          <el-form :model="form" label-position="top">
            <el-form-item label="合同类型">
              <el-tag type="info" size="large">固定报价</el-tag>
              <span class="form-hint-inline">平台当前仅支持整包固定价格需求</span>
            </el-form-item>
            <el-form-item label="预算范围（元）">
              <div class="budget-inputs">
                <el-input-number v-model="form.budgetMin" :min="0" :step="1000" placeholder="最低预算" />
                <span>—</span>
                <el-input-number v-model="form.budgetMax" :min="form.budgetMin" :step="1000" placeholder="最高预算" />
              </div>
            </el-form-item>
            <el-form-item label="截止接单日期">
              <el-date-picker v-model="form.deadline" type="date" placeholder="选择截止日期" style="width:100%" :disabled-date="(d) => d < new Date()" />
            </el-form-item>
            <el-form-item label="预计项目周期（天）">
              <el-input-number v-model="form.durationDays" :min="1" :max="365" />
            </el-form-item>
            <el-form-item label="付款方式">
              <el-radio-group v-model="form.paymentType">
                <el-radio label="MILESTONE">里程碑分阶段付款（推荐）</el-radio>
                <el-radio label="ONCE">一次性付款</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.paymentType === 'MILESTONE'" label="里程碑由谁规划">
              <el-radio-group v-model="form.milestonePlanBy">
                <el-radio label="DEVELOPER">开发者自行拆分阶段（灵活迭代）</el-radio>
                <el-radio label="ENTERPRISE">企业统一规划验收节点（强管控）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <div class="wizard-btns">
            <el-button size="large" @click="wizardStep--">上一步</el-button>
            <el-button type="primary" size="large" @click="wizardStep++">下一步</el-button>
          </div>
        </div>

        <!-- Step 3: 确认发布 -->
        <div v-if="wizardStep === 3">
          <h3 class="wizard-step-title">确认需求信息</h3>
          <div class="preview-section">
            <div class="preview-item">
              <span class="preview-label">需求标题</span>
              <span>{{ form.title }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">项目类型</span>
              <span>{{ form.category }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">技术栈</span>
              <div class="preview-tags">
                <el-tag v-for="s in form.skills" :key="s" size="small">{{ s }}</el-tag>
              </div>
            </div>
            <div class="preview-item">
              <span class="preview-label">预算范围</span>
              <span class="preview-budget">¥{{ form.budgetMin?.toLocaleString() }} - ¥{{ form.budgetMax?.toLocaleString() }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">截止日期</span>
              <span>{{ formatDate(form.deadline) }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">项目周期</span>
              <span>{{ form.durationDays }} 天</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">付款与验收</span>
              <span>{{ form.paymentType === 'ONCE' ? '一次性整单验收付款' : '里程碑分阶段验收付款' }}</span>
            </div>
            <div v-if="form.paymentType === 'MILESTONE'" class="preview-item">
              <span class="preview-label">里程碑规划</span>
              <span>{{ form.milestonePlanBy === 'ENTERPRISE' ? '由企业规划节点' : '由开发者拆分阶段' }}</span>
            </div>
          </div>
          <el-alert v-if="depositPreview && Number(depositPreview.amount) > 0" type="warning" :closable="false" style="margin-bottom:12px">
            提交审核时将从企业钱包<strong>可用余额</strong>扣除发布押金约 <strong>¥{{ formatMoney(depositPreview.amount) }}</strong>
            （约为预算上限的 {{ depositRatePercent }}%）。请确保余额充足；审核驳回、关闭招募未选标、或选标立项后将自动退还该押金。
          </el-alert>
          <el-alert v-else-if="depositPreview" type="info" :closable="false" style="margin-bottom:12px">
            当前平台配置下，发布该需求无需支付押金（比例为 0 或金额低于分币进位）。
          </el-alert>
          <el-alert type="info" :closable="false" style="margin-bottom:16px">
            发布后需经平台审核，审核通过后将对开发者可见。预计1-2个工作日完成审核。
          </el-alert>
          <div class="wizard-btns">
            <el-button size="large" @click="wizardStep--">上一步</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="publishTask">发布需求</el-button>
            <el-button size="large" @click="saveDraft">保存草稿</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 表单模式（一页展示全部） -->
    <template v-else>
      <div class="card">
        <el-form ref="formRef" :model="form" label-position="top" :rules="formRules">
          <div class="form-grid">
            <el-form-item label="需求标题" prop="title">
              <el-input v-model="form.title" placeholder="简洁描述您的项目需求" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="项目类型" prop="category">
              <el-select v-model="form.category" style="width:100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="详细描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="6" maxlength="5000" show-word-limit placeholder="请详细描述您的需求，包括功能模块、技术规范、交付要求等..." />
          </el-form-item>
          <el-form-item label="所需技术栈">
            <div class="skills-picker">
              <div class="picked-skills">
                <el-tag v-for="s in form.skills" :key="s" closable @close="removeSkill(s)">{{ s }}</el-tag>
                <el-input v-if="inputVisible" ref="skillInputRef" v-model="skillInput" size="small" style="width:120px" @keyup.enter="addSkill" @blur="addSkill" />
                <el-button v-else size="small" @click="showSkillInput">+ 自定义</el-button>
              </div>
              <div class="suggest-skills">
                <span class="suggest-label">快速选择：</span>
                <span v-for="s in suggestSkills" :key="s" class="suggest-item" :class="{ selected: form.skills.includes(s) }" @click="toggleSkill(s)">{{ s }}</span>
              </div>
            </div>
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="经验要求">
              <el-select v-model="form.experience" style="width:100%">
                <el-option label="不限经验" value="NONE" />
                <el-option label="1-3年" value="JUNIOR" />
                <el-option label="3-5年" value="SENIOR" />
                <el-option label="5年以上" value="EXPERT" />
              </el-select>
            </el-form-item>
            <el-form-item label="合同类型">
              <el-tag type="info" size="large">固定报价</el-tag>
              <span class="form-hint-inline">平台当前仅支持整包固定价格需求</span>
            </el-form-item>
            <el-form-item label="预算下限（元）">
              <el-input-number v-model="form.budgetMin" :min="0" :step="1000" style="width:100%" />
            </el-form-item>
            <el-form-item label="预算上限（元）">
              <el-input-number v-model="form.budgetMax" :min="form.budgetMin || 0" :step="1000" style="width:100%" />
            </el-form-item>
            <el-form-item label="截止接单日期">
              <el-date-picker v-model="form.deadline" type="date" style="width:100%" :disabled-date="(d) => d < new Date()" />
            </el-form-item>
            <el-form-item label="预计项目周期（天）">
              <el-input-number v-model="form.durationDays" :min="1" :max="365" style="width:100%" />
            </el-form-item>
          </div>
          <el-form-item label="付款方式">
            <el-radio-group v-model="form.paymentType">
              <el-radio label="MILESTONE">里程碑分阶段付款（推荐）</el-radio>
              <el-radio label="ONCE">一次性付款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.paymentType === 'MILESTONE'" label="里程碑由谁规划">
            <el-radio-group v-model="form.milestonePlanBy">
              <el-radio label="DEVELOPER">开发者自行拆分阶段</el-radio>
              <el-radio label="ENTERPRISE">企业统一规划验收节点</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="是否需要实名认证开发者">
            <el-switch v-model="form.requireKyc" />
          </el-form-item>
          <el-form-item label="需求附件（可选）">
            <el-upload
              :http-request="handleUpload"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
              :show-file-list="false"
              multiple
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.txt,.png,.jpg,.jpeg"
            >
              <el-button><el-icon><UploadFilled /></el-icon> 上传需求文档</el-button>
              <template #tip><div class="el-upload__tip">支持 PDF、Word、Excel、图片等格式，单文件不超过20MB</div></template>
            </el-upload>
            <div v-if="form.attachments.length" class="attachment-list">
              <div v-for="(f, idx) in form.attachments" :key="idx" class="attachment-item">
                <el-icon><Document /></el-icon>
                <span>{{ f.name }}</span>
                <el-button link type="danger" size="small" @click="removeAttachment(idx)">删除</el-button>
              </div>
            </div>
          </el-form-item>
          <el-alert v-if="depositPreview && Number(depositPreview.amount) > 0" type="warning" :closable="false" style="margin-bottom:16px">
            提交审核将从钱包可用余额扣除发布押金约 <strong>¥{{ formatMoney(depositPreview.amount) }}</strong>（预算上限约 {{ depositRatePercent }}%）。
          </el-alert>
          <div class="form-actions">
            <el-button type="primary" size="large" :loading="submitting" @click="publishTask">发布需求</el-button>
            <el-button size="large" @click="saveDraft">保存草稿</el-button>
          </div>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { tasksApi } from '@/api/tasks'
import { formatDate, formatMoney, SKILL_TAGS } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const editTaskId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEditMode = computed(() => !!editTaskId.value)

const mode = ref('wizard')
const wizardStep = ref(0)
const submitting = ref(false)
const step0Ref = ref()
const skillInputRef = ref()
const inputVisible = ref(false)
const skillInput = ref('')

const categories = ['Web开发', '移动端开发', '小程序开发', '后端/API开发', 'UI/UX设计', '数据分析', '爬虫/自动化', '桌面应用', '其他']
const suggestSkills = SKILL_TAGS.slice(0, 20)

const form = reactive({
  title: '',
  category: '',
  description: '',
  skills: [],
  experience: 'NONE',
  requireKyc: false,
  attachments: [],
  contractType: 'FIXED',
  budgetMin: 5000,
  budgetMax: 20000,
  deadline: '',
  durationDays: 30,
  paymentType: 'MILESTONE',
  milestonePlanBy: 'DEVELOPER'
})

const formRules = {
  title: [{ required: true, message: '请输入需求标题' }],
  description: [{ required: true, message: '请填写详细描述', min: 50 }]
}

const depositPreview = ref(null)
const depositRatePercent = computed(() => {
  const r = depositPreview.value?.rate
  if (r == null || r === '') return '0'
  return (Number(r) * 100).toFixed(2)
})

async function refreshDepositPreview() {
  const max = form.budgetMax
  if (max == null || Number(max) <= 0) {
    depositPreview.value = null
    return
  }
  try {
    const res = await tasksApi.publishDepositPreview({ budgetMax: max })
    depositPreview.value = res.data || null
  } catch {
    depositPreview.value = null
  }
}

watch(() => form.budgetMax, () => { refreshDepositPreview() })
watch(() => wizardStep.value, (s) => { if (s === 3) refreshDepositPreview() })
watch(() => form.paymentType, (v) => {
  if (v === 'ONCE') form.milestonePlanBy = 'DEVELOPER'
})

function toggleSkill(s) {
  const idx = form.skills.indexOf(s)
  if (idx > -1) form.skills.splice(idx, 1)
  else form.skills.push(s)
}

function removeSkill(s) { form.skills = form.skills.filter(x => x !== s) }

function showSkillInput() {
  inputVisible.value = true
  nextTick(() => skillInputRef.value?.focus())
}

function addSkill() {
  if (skillInput.value && !form.skills.includes(skillInput.value)) {
    form.skills.push(skillInput.value)
  }
  skillInput.value = ''
  inputVisible.value = false
}

async function handleUpload({ file, onSuccess, onError }) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/tasks/upload-attachment', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const attachment = res.data
    form.attachments.push(attachment)
    onSuccess(attachment)
    ElMessage.success(`${file.name} 上传成功`)
  } catch (e) {
    onError(e)
    ElMessage.error(`${file.name} 上传失败`)
  }
}

function onUploadSuccess() {}
function onUploadError() {}

function removeAttachment(idx) {
  form.attachments.splice(idx, 1)
}

async function nextWizardStep() {
  await step0Ref.value?.validate()
  wizardStep.value++
}

async function publishTask() {
  form.contractType = 'FIXED'
  submitting.value = true
  try {
    if (isEditMode.value) {
      await tasksApi.update(editTaskId.value, { ...form, action: 'PUBLISH' })
      ElMessage.success('需求已修改并重新提交审核')
    } else {
      await tasksApi.create({ ...form, action: 'PUBLISH' })
      ElMessage.success('需求已提交审核，请等待平台审核通过')
    }
    router.push('/enterprise/tasks')
  } catch (e) {
    // 4xx/5xx 时 request 拦截器已提示；仅网络等无 response 时再提示
    if (!e?.response) ElMessage.error(e?.message || '提交失败，请重试')
  }
  finally { submitting.value = false }
}

async function saveDraft() {
  form.contractType = 'FIXED'
  if (isEditMode.value) {
    await tasksApi.update(editTaskId.value, { ...form, action: 'DRAFT' })
    ElMessage.success('修改已保存')
  } else {
    await tasksApi.create({ ...form, action: 'DRAFT' })
    ElMessage.success('已保存草稿')
  }
  router.push('/enterprise/tasks')
}

async function checkKyc() {
  // 先刷新用户信息以获取最新认证状态
  try { await userStore.refreshUserInfo() } catch {}
  const kyc = userStore.kycStatus
  if (kyc === 'VERIFIED') return true
  if (kyc === 'AUDITING') {
    await ElMessageBox.alert(
      '您的企业认证正在审核中，审核通过后即可发布需求。',
      '认证审核中',
      { confirmButtonText: '我知道了', type: 'warning' }
    )
    router.push('/enterprise/profile')
    return false
  }
  // NONE / REJECTED
  try {
    await ElMessageBox.confirm(
      '发布需求需要先完成企业认证，认证通过后才能发布需求。是否前往进行企业认证？',
      '需要企业认证',
      { confirmButtonText: '去认证', cancelButtonText: '取消', type: 'warning' }
    )
    router.push('/enterprise/profile?tab=kyc')
  } catch {}
  return false
}

onMounted(async () => {
  const passed = await checkKyc()
  if (!passed) return

  if (isEditMode.value) {
    mode.value = 'form'
    try {
      const res = await tasksApi.getDetail(editTaskId.value)
      const t = res.data
      if (t) {
        form.title = t.title || ''
        form.category = t.category || ''
        form.description = t.description || ''
        form.skills = t.skills || []
        form.experience = t.experience || 'NONE'
        form.requireKyc = t.requireKyc || false
        form.contractType = t.contractType === 'HOURLY' ? 'FIXED' : (t.contractType || 'FIXED')
        form.budgetMin = t.budgetMin || 5000
        form.budgetMax = t.budgetMax || 20000
        form.deadline = t.deadline || ''
        form.durationDays = t.durationDays || 30
        form.paymentType = t.paymentType || 'MILESTONE'
        form.milestonePlanBy = t.milestonePlanBy || 'DEVELOPER'
        form.attachments = t.attachments || []
      }
      await refreshDepositPreview()
    } catch { ElMessage.error('加载任务数据失败') }
  } else {
    await refreshDepositPreview()
  }
})
</script>

<style scoped lang="scss">
.publish-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.wizard-steps { margin-bottom: 24px; }
.wizard-card { min-height: 400px; }
.wizard-step-title { font-size: 18px; font-weight: 600; margin-bottom: 24px; }
.wizard-btns {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.skills-picker {
  width: 100%;
  .picked-skills {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
    min-height: 36px;
    padding: 8px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
  }
}

.suggest-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.suggest-label { font-size: 12px; color: var(--text-muted); }
.suggest-item {
  padding: 2px 10px;
  border-radius: 99px;
  border: 1px solid var(--border-color);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  &:hover { border-color: var(--primary-color); color: var(--primary-color); }
  &.selected { background: var(--primary-color); color: white; border-color: var(--primary-color); }
}

.budget-inputs {
  display: flex; align-items: center; gap: 12px;
  span { color: var(--text-muted); }
}

.preview-section { border: 1px solid var(--border-color); border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.preview-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid var(--border-color);
  &:last-child { border-bottom: none; }
}
.preview-label { width: 80px; flex-shrink: 0; color: var(--text-muted); font-size: 13px; }
.preview-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.preview-budget { font-weight: 600; color: var(--primary-color); }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-actions { display: flex; gap: 12px; margin-top: 8px; }
.form-hint-inline { margin-left: 10px; font-size: 12px; color: var(--text-muted); vertical-align: middle; }
</style>
