<template>
  <div class="page-container" v-loading="loading">
    <div v-if="project">
      <div class="project-header card">
        <div class="project-header-main">
          <div>
            <h2 class="project-name">{{ project.name }}</h2>
            <div class="project-meta">
              <el-tag :type="getStatusType(project.status)">{{ getStatusLabel(project.status) }}</el-tag>
              <span>开发者：{{ project.developer?.name }}</span>
              <span>合同金额：¥{{ formatMoney(project.amount) }}</span>
              <span>{{ formatDate(project.startDate) }} - {{ formatDate(project.endDate) }}</span>
            </div>
          </div>
        </div>
        <div class="project-progress-bar">
          <div class="progress-info"><span>项目进度</span><span>{{ project.progress }}%</span></div>
          <el-progress :percentage="project.progress" :stroke-width="10" color="#0ea5e9" />
        </div>
      </div>

      <el-alert
        v-if="project"
        class="mode-alert"
        :type="isOncePayment ? 'warning' : 'info'"
        :closable="false"
        show-icon
      >
        <template #title>{{ deliveryModeTitle }}</template>
        <div class="mode-alert-desc">{{ deliveryModeDesc }}</div>
      </el-alert>

      <el-card v-if="project && showEntMilestonePlanner" class="planner-card" shadow="never">
        <template #header>规划验收里程碑（企业强管控）</template>
        <el-form :inline="true" :model="newMs" class="new-ms-form" @submit.prevent="createMilestone">
          <el-form-item label="名称"><el-input v-model="newMs.name" placeholder="如：一期 UI 交付" style="width:160px" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="newMs.description" placeholder="可选" style="width:200px" /></el-form-item>
          <el-form-item label="金额"><el-input-number v-model="newMs.amount" :min="1" :step="500" /></el-form-item>
          <el-form-item label="截止"><el-date-picker v-model="newMs.deadline" type="date" value-format="YYYY-MM-DD" placeholder="可选" /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="creatingMs" @click="createMilestone">添加里程碑</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-tabs v-model="activeTab">
        <!-- 里程碑进度 -->
        <el-tab-pane label="进度跟踪" name="milestones">
          <div class="milestones-list">
            <div v-for="(m, index) in milestones" :key="m.id" class="milestone-row card">
              <div class="milestone-index">{{ index + 1 }}</div>
              <div class="milestone-content">
                <div class="milestone-top">
                  <h4 class="milestone-name">{{ m.name }}</h4>
                  <el-tag :type="getMilestoneStatusType(m.status)">{{ getMilestoneStatusLabel(m.status) }}</el-tag>
                </div>
                <p class="milestone-desc">{{ m.description }}</p>
                <div class="milestone-meta">
                  <span><el-icon><Calendar /></el-icon> 截止 {{ formatDate(m.deadline) }}</span>
                  <span class="milestone-amount">¥{{ formatMoney(m.amount) }}</span>
                </div>

                <!-- 验收操作 -->
                <div v-if="m.status === 'SUBMITTED'" class="acceptance-panel">
                  <div class="acceptance-title">{{ isOncePayment ? '开发者已提交整单成果，请一次性验收' : '开发者已提交本阶段交付物，请验收' }}</div>
                  <div class="deliverable-list" v-if="m.deliverables?.length">
                    <div v-for="d in m.deliverables" :key="d.id" class="deliverable-item">
                      <el-icon><Document /></el-icon>
                      <span>{{ d.name }}</span>
                      <el-button size="small" link type="primary" @click="downloadDeliverable(d)">下载</el-button>
                    </div>
                  </div>
                  <div class="acceptance-actions">
                    <el-button type="success" @click="acceptMilestone(m)">
                      <el-icon><Check /></el-icon> 验收通过
                    </el-button>
                    <el-button type="danger" @click="openRejectDialog(m)">
                      <el-icon><Close /></el-icon> 驳回
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 交付物 -->
        <el-tab-pane label="交付物" name="deliverables">
          <div class="deliverables-area">
            <div v-for="m in milestones" :key="m.id" class="deliverable-group">
              <div class="group-header">
                <span>{{ m.name }}</span>
                <el-tag :type="getMilestoneStatusType(m.status)" size="small">{{ getMilestoneStatusLabel(m.status) }}</el-tag>
              </div>
              <div v-if="m.deliverables?.length" class="deliverable-files">
                <div v-for="d in m.deliverables" :key="d.id" class="file-item">
                  <el-icon><Document /></el-icon>
                  <div class="file-info">
                    <div>{{ d.name }}</div>
                    <div class="file-meta">{{ formatFileSize(d.size) }} · {{ fromNow(d.uploadedAt || d.createdAt) }}</div>
                  </div>
                  <el-button size="small" type="primary" @click="downloadDeliverable(d)">下载</el-button>
                </div>
              </div>
              <div v-else class="empty-files">暂无交付物</div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 评价 -->
        <el-tab-pane label="项目评价" name="review" v-if="project.status === 'COMPLETED'">
          <div class="review-area card" style="padding:24px">
            <div v-if="!hasReviewed">
              <h3 style="margin-bottom:16px">对本次合作进行评价</h3>
              <el-form :model="reviewForm" label-position="top">
                <el-form-item label="总体评分">
                  <el-rate v-model="reviewForm.rating" :max="5" allow-half />
                </el-form-item>
                <el-form-item label="评价内容">
                  <el-input v-model="reviewForm.content" type="textarea" :rows="4" maxlength="500" show-word-limit />
                </el-form-item>
                <el-form-item label="评价标签">
                  <div class="review-tags">
                    <el-check-tag v-for="t in reviewTagOptions" :key="t" :checked="reviewForm.tags.includes(t)" @change="toggleTag(t)">{{ t }}</el-check-tag>
                  </div>
                </el-form-item>
              </el-form>
              <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
            </div>
            <div v-else>
              <el-result icon="success" title="评价已提交" sub-title="感谢您的评价" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回里程碑" width="440px">
      <el-form :model="rejectForm" label-position="top">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请详细说明驳回原因，以便开发者修改..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="rejectMilestone">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { projectsApi, triggerDeliverableDownload } from '@/api/projects'
import { formatDate, fromNow, formatMoney, formatFileSize, PROJECT_STATUS_MAP, MILESTONE_STATUS_MAP } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const activeTab = ref('milestones')
const project = ref(null)
const milestones = ref([])
const rejectDialogVisible = ref(false)
const currentMilestone = ref(null)
const rejectForm = ref({ reason: '' })
const hasReviewed = ref(false)
const submittingReview = ref(false)
const reviewForm = ref({ rating: 5, content: '', tags: [] })
const reviewTagOptions = ['专业可靠', '沟通顺畅', '交付及时', '质量优秀', '超出预期']

const isOncePayment = computed(() => project.value?.paymentType === 'ONCE')
const enterprisePlansMilestones = computed(() =>
  project.value?.paymentType === 'MILESTONE' && project.value?.milestonePlanBy === 'ENTERPRISE')
const showEntMilestonePlanner = computed(() =>
  enterprisePlansMilestones.value && !isOncePayment.value && project.value?.status === 'IN_PROGRESS')

const deliveryModeTitle = computed(() => {
  if (isOncePayment.value) return '一次性整单验收'
  if (enterprisePlansMilestones.value) return '企业规划里程碑（分阶段验收）'
  return '开发者拆分里程碑（分阶段验收）'
})
const deliveryModeDesc = computed(() => {
  if (isOncePayment.value) {
    return '仅有一个交付节点：验收通过后按合同金额一次性向开发者付款并结项。'
  }
  if (enterprisePlansMilestones.value) {
    return '请在上方添加各阶段里程碑；开发者按节点上传并提交后，您在此验收。'
  }
  return '里程碑由开发者拆分；开发者提交后您在此逐阶段验收付款。'
})

const newMs = reactive({ name: '', description: '', amount: 1000, deadline: '' })
const creatingMs = ref(false)

function getStatusType(s) { return PROJECT_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return PROJECT_STATUS_MAP[s]?.label || s }
function getMilestoneStatusType(s) { return MILESTONE_STATUS_MAP[s]?.type || 'info' }
function getMilestoneStatusLabel(s) { return MILESTONE_STATUS_MAP[s]?.label || s }

function toggleTag(t) {
  const idx = reviewForm.value.tags.indexOf(t)
  if (idx > -1) reviewForm.value.tags.splice(idx, 1)
  else reviewForm.value.tags.push(t)
}

function normalizeParty(u, isEnt) {
  if (!u) return null
  return { ...u, name: isEnt ? (u.companyName || u.nickname) : (u.nickname || u.username) }
}

function applyProjectDetail(d) {
  if (!d?.project) return
  const p = d.project
  project.value = {
    ...p,
    paymentType: p.paymentType || 'MILESTONE',
    milestonePlanBy: p.milestonePlanBy || 'DEVELOPER',
    enterprise: normalizeParty(d.enterprise, true),
    developer: normalizeParty(d.developer, false)
  }
  milestones.value = (d.milestones || []).map(m => ({
    ...m,
    deliverables: (m.deliverables || []).map(x => ({ ...x, uploadedAt: x.createdAt }))
  }))
}

async function fetchProject() {
  loading.value = true
  try {
    const pRes = await projectsApi.getDetail(route.params.id)
    applyProjectDetail(pRes.data)
  } catch {
    project.value = null
    milestones.value = []
    ElMessage.error('加载项目失败')
  } finally { loading.value = false }
}

async function createMilestone() {
  if (!newMs.name?.trim()) {
    ElMessage.warning('请填写里程碑名称')
    return
  }
  creatingMs.value = true
  try {
    await projectsApi.createMilestone(project.value.id, {
      name: newMs.name.trim(),
      description: newMs.description || '',
      amount: newMs.amount,
      deadline: newMs.deadline || undefined
    })
    ElMessage.success('已添加里程碑')
    newMs.name = ''
    newMs.description = ''
    newMs.amount = 1000
    newMs.deadline = ''
    fetchProject()
  } finally {
    creatingMs.value = false
  }
}

async function acceptMilestone(m) {
  const msg = isOncePayment.value
    ? '确认整单验收通过？通过后合同全款（扣除平台费）将一次性转入开发者账户。'
    : '确认验收通过？验收通过后，该阶段款项将自动转入开发者账户。'
  await ElMessageBox.confirm(msg, '确认验收', { type: 'success' })
  await projectsApi.acceptMilestone(project.value.id, m.id)
  ElMessage.success('验收通过')
  await fetchProject()
}

function openRejectDialog(m) {
  currentMilestone.value = m
  rejectForm.value.reason = ''
  rejectDialogVisible.value = true
}

async function rejectMilestone() {
  if (!rejectForm.value.reason) { ElMessage.warning('请填写驳回原因'); return }
  await projectsApi.rejectMilestone(project.value.id, currentMilestone.value.id, { reason: rejectForm.value.reason })
  rejectDialogVisible.value = false
  ElMessage.success('已驳回，等待开发者修改')
  await fetchProject()
}

async function submitReview() {
  submittingReview.value = true
  try {
    await projectsApi.submitReview(project.value.id, reviewForm.value)
    hasReviewed.value = true
    ElMessage.success('评价提交成功')
  } catch (e) {
    const msg = e?.response?.data?.message || e?.data?.message || e?.message || '提交失败'
    ElMessage.error(msg)
  } finally { submittingReview.value = false }
}

/** 原生打开下载链接，避免 fetch 被 IDM 拦截为 204 导致空文件 */
function downloadDeliverable(d) {
  if (!d?.id) return
  triggerDeliverableDownload(d.id)
}

onMounted(fetchProject)
</script>

<style scoped lang="scss">
.mode-alert { margin-bottom: 16px; }
.mode-alert-desc { margin-top: 6px; font-size: 13px; line-height: 1.5; }
.planner-card { margin-bottom: 16px; }
.new-ms-form { flex-wrap: wrap; }

.project-header { margin-bottom: 20px; }
.project-header-main { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.project-name { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.project-meta { display: flex; align-items: center; gap: 12px; font-size: 13px; color: var(--text-secondary); }
.progress-info { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }

.milestones-list { display: flex; flex-direction: column; gap: 12px; }
.milestone-row { display: flex; gap: 16px; }
.milestone-index {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--primary-color); color: white;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.milestone-content { flex: 1; }
.milestone-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.milestone-name { font-size: 15px; font-weight: 600; }
.milestone-desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.milestone-meta { display: flex; align-items: center; gap: 16px; font-size: 12px; color: var(--text-muted); }
.milestone-amount { color: var(--primary-color); font-weight: 500; font-size: 14px; }

.acceptance-panel {
  margin-top: 12px; padding: 12px; background: #fef3c7; border-radius: 8px;
  .acceptance-title { font-size: 13px; font-weight: 500; color: #92400e; margin-bottom: 10px; }
}
.acceptance-actions { display: flex; gap: 10px; margin-top: 10px; }

.deliverable-list, .deliverable-files { display: flex; flex-direction: column; gap: 6px; }
.deliverable-item, .file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; background: white; border-radius: 6px;
  font-size: 13px;
  .el-icon { color: var(--text-muted); }
}
.file-info { flex: 1; }
.file-meta { font-size: 11px; color: var(--text-muted); }

.deliverables-area { display: flex; flex-direction: column; gap: 16px; }
.deliverable-group { background: white; border-radius: 10px; border: 1px solid var(--border-color); overflow: hidden; }
.group-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: var(--bg-color); border-bottom: 1px solid var(--border-color); font-weight: 600; }
.empty-files { padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px; }

.review-tags { display: flex; flex-wrap: wrap; gap: 8px; }
</style>
