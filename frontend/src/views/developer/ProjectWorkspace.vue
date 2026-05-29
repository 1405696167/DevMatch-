<template>
  <div class="page-container" v-loading="loading">
    <div v-if="project">
      <!-- 项目头部 -->
      <div class="project-header card">
        <div class="project-header-main">
          <div>
            <h2 class="project-name">{{ project.name }}</h2>
            <div class="project-meta">
              <el-tag :type="getStatusType(project.status)">{{ getStatusLabel(project.status) }}</el-tag>
              <span>发布方：{{ project.enterprise?.name }}</span>
              <span>合同金额：¥{{ formatMoney(project.amount) }}</span>
              <span>{{ formatDate(project.startDate) }} - {{ formatDate(project.endDate) }}</span>
            </div>
          </div>
        </div>
        <div class="project-progress-bar">
          <div class="progress-info">
            <span>项目进度</span>
            <span>{{ project.progress }}%</span>
          </div>
          <el-progress :percentage="project.progress" :stroke-width="10" :color="getProgressColor(project.status)" />
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

      <el-card v-if="project && showDevMilestonePlanner" class="planner-card" shadow="never">
        <template #header>拆分里程碑（仅分阶段 + 开发者规划）</template>
        <el-form :inline="true" :model="newMs" class="new-ms-form" @submit.prevent="createMilestone">
          <el-form-item label="名称"><el-input v-model="newMs.name" placeholder="如：接口联调" style="width:160px" /></el-form-item>
          <el-form-item label="说明"><el-input v-model="newMs.description" placeholder="可选" style="width:200px" /></el-form-item>
          <el-form-item label="金额"><el-input-number v-model="newMs.amount" :min="1" :step="500" /></el-form-item>
          <el-form-item label="截止"><el-date-picker v-model="newMs.deadline" type="date" value-format="YYYY-MM-DD" placeholder="可选" /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="creatingMs" @click="createMilestone">添加里程碑</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-tabs v-model="activeTab" class="workspace-tabs">
        <!-- 里程碑看板 -->
        <el-tab-pane label="里程碑看板" name="milestones">
          <div class="milestones-board">
            <div
              v-for="col in milestoneCols"
              :key="col.status"
              class="milestone-col"
            >
              <div class="col-header" :style="{ borderTopColor: col.color }">
                <span class="col-title">{{ col.title }}</span>
                <el-badge :value="getMilestonesByStatus(col.status).length" type="primary" />
              </div>
              <div class="col-items">
                <div
                  v-for="m in getMilestonesByStatus(col.status)"
                  :key="m.id"
                  class="milestone-card"
                >
                  <div class="milestone-name">{{ m.name }}</div>
                  <div class="milestone-desc">{{ m.description }}</div>
                  <div class="milestone-footer">
                    <span class="milestone-deadline">{{ formatDate(m.deadline) }}</span>
                    <span class="milestone-amount">¥{{ formatMoney(m.amount) }}</span>
                  </div>
                  <div class="milestone-actions">
                    <el-button
                      v-if="m.status === 'PENDING' && isDevRole"
                      type="success"
                      size="small"
                      @click="startMilestoneRow(m)"
                    >
                      开始
                    </el-button>
                    <el-button
                      v-if="(m.status === 'PENDING' || m.status === 'IN_PROGRESS') && isDevRole"
                      type="primary"
                      size="small"
                      @click="openDeliverableUpload(m)"
                    >
                      上传交付物
                    </el-button>
                    <el-button size="small" @click="viewDeliverables(m)">
                      查看交付物
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 交付物 -->
        <el-tab-pane label="交付物管理" name="deliverables">
          <div class="deliverables-panel">
            <div v-for="m in milestones" :key="m.id" class="deliverable-group">
              <div class="deliverable-group-header">
                <span class="group-name">{{ m.name }}</span>
                <el-tag :type="getMilestoneStatusType(m.status)" size="small">{{ getMilestoneStatusLabel(m.status) }}</el-tag>
              </div>
              <div v-if="m.deliverables?.length" class="deliverable-list">
                <div v-for="d in m.deliverables" :key="d.id" class="deliverable-item">
                  <el-icon><Document /></el-icon>
                  <div class="deliverable-info">
                    <div class="deliverable-name">{{ d.name }}</div>
                    <div class="deliverable-meta">{{ formatFileSize(d.size) }} · {{ fromNow(d.uploadedAt || d.createdAt) }}</div>
                  </div>
                  <el-button size="small" type="primary" link @click="downloadDeliverable(d)">下载</el-button>
                </div>
              </div>
              <div v-else class="empty-tip">该里程碑暂无交付物</div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 项目动态 -->
        <el-tab-pane label="项目动态" name="timeline">
          <div class="timeline-panel card" style="padding:20px">
            <el-timeline>
              <el-timeline-item
                v-for="event in timeline"
                :key="event.id"
                :timestamp="formatDateTime(event.createdAt)"
                :type="event.type"
              >
                {{ event.content }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 上传交付物对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传交付物" width="500px">
      <el-upload
        drag
        multiple
        :http-request="handleDeliverableUpload"
        :show-file-list="true"
      >
        <el-icon size="48"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持任意格式文件，单个文件不超过100MB</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMilestone">提交里程碑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { projectsApi, triggerDeliverableDownload } from '@/api/projects'
import request from '@/api/request'
import { formatDate, formatDateTime, fromNow, formatMoney, formatFileSize, PROJECT_STATUS_MAP, MILESTONE_STATUS_MAP } from '@/utils/format'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('milestones')
const project = ref(null)
const milestones = ref([])
const timeline = ref([])
const uploadDialogVisible = ref(false)
const currentMilestone = ref(null)
const isDevRole = computed(() => userStore.userRole === 'DEVELOPER')
const isOncePayment = computed(() => project.value?.paymentType === 'ONCE')
const enterprisePlansMilestones = computed(() =>
  project.value?.paymentType === 'MILESTONE' && project.value?.milestonePlanBy === 'ENTERPRISE')
const developerPlansMilestones = computed(() =>
  project.value?.paymentType === 'MILESTONE' && project.value?.milestonePlanBy === 'DEVELOPER')
const showDevMilestonePlanner = computed(() =>
  isDevRole.value && developerPlansMilestones.value && !isOncePayment.value && project.value?.status === 'IN_PROGRESS')

const deliveryModeTitle = computed(() => {
  if (isOncePayment.value) return '一次性整单交付'
  if (enterprisePlansMilestones.value) return '企业规划里程碑（分阶段验收）'
  return '开发者拆分里程碑（分阶段验收）'
})
const deliveryModeDesc = computed(() => {
  if (isOncePayment.value) {
    return '本项目已自动生成唯一交付节点：上传全部成果后提交验收，企业通过后一次性结算全款。不可再拆分里程碑。'
  }
  if (enterprisePlansMilestones.value) {
    return '由企业在工作台添加验收节点；您可对「待开始」节点点击「开始」，上传交付物后提交验收。'
  }
  return '您可在下方自行添加里程碑；对每个节点点击「开始」→上传交付物→在弹窗中提交验收。'
})

const newMs = reactive({ name: '', description: '', amount: 1000, deadline: '' })
const creatingMs = ref(false)

const milestoneCols = [
  { status: 'PENDING', title: '待开始', color: '#94a3b8' },
  { status: 'IN_PROGRESS', title: '进行中', color: '#4f46e5' },
  { status: 'SUBMITTED', title: '已提交', color: '#f59e0b' },
  { status: 'ACCEPTED', title: '已验收', color: '#10b981' },
  { status: 'REJECTED', title: '已驳回', color: '#ef4444' }
]

function getStatusType(s) { return PROJECT_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return PROJECT_STATUS_MAP[s]?.label || s }
function getMilestoneStatusType(s) { return MILESTONE_STATUS_MAP[s]?.type || 'info' }
function getMilestoneStatusLabel(s) { return MILESTONE_STATUS_MAP[s]?.label || s }
function getProgressColor(s) {
  return { IN_PROGRESS: '#4f46e5', COMPLETED: '#10b981', DISPUTE: '#ef4444' }[s] || '#4f46e5'
}
function getMilestonesByStatus(status) {
  return milestones.value.filter(m => m.status === status)
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
  } finally {
    loading.value = false
  }
}

async function startMilestoneRow(m) {
  try {
    await projectsApi.startMilestone(project.value.id, m.id)
    ElMessage.success('已开始该里程碑')
    fetchProject()
  } catch { /* 拦截器已提示 */ }
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

function openDeliverableUpload(m) {
  currentMilestone.value = m
  uploadDialogVisible.value = true
}

function viewDeliverables(m) {
  activeTab.value = 'deliverables'
}

/** 与企业端发布需求附件一致：走 axios，自动带 Token、与 /api 代理一致 */
async function handleDeliverableUpload({ file, onSuccess, onError }) {
  const mid = currentMilestone.value?.id
  if (!mid) {
    ElMessage.error('未选择里程碑')
    onError?.(new Error('no milestone'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post(`/projects/milestones/${mid}/deliverables`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code !== 200) {
      ElMessage.error(res.message || '上传失败')
      onError?.(new Error(res.message))
      return
    }
    onSuccess(res.data)
    ElMessage.success(`${file.name} 上传成功`)
    fetchProject()
  } catch (e) {
    onError?.(e)
    if (!e?.response) ElMessage.error('文件上传失败')
  }
}

async function submitMilestone() {
  try {
    await projectsApi.submitMilestone(project.value.id, currentMilestone.value.id)
    ElMessage.success('已提交里程碑，等待发布方验收')
    uploadDialogVisible.value = false
    fetchProject()
  } catch { /* 拦截器已提示 */ }
}

function downloadDeliverable(d) {
  if (!d?.id) return
  triggerDeliverableDownload(d.id)
}

onMounted(fetchProject)
</script>

<style scoped lang="scss">
.project-header { margin-bottom: 20px; }
.project-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}
.project-name { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.project-meta {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  font-size: 13px; color: var(--text-secondary);
}
.progress-info {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--text-muted); margin-bottom: 6px;
}

.mode-alert { margin-bottom: 16px; }
.mode-alert-desc { margin-top: 6px; font-size: 13px; line-height: 1.5; }
.planner-card { margin-bottom: 16px; }
.new-ms-form { flex-wrap: wrap; }

.workspace-tabs { margin-top: 16px; }

.milestones-board {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  align-items: start;
}

.milestone-col {
  background: var(--bg-color);
  border-radius: 10px;
  overflow: hidden;
  min-height: 200px;
}

.col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: white;
  border-top: 3px solid;
  border-bottom: 1px solid var(--border-color);
}
.col-title { font-size: 13px; font-weight: 600; }

.col-items {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.milestone-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.milestone-name { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.milestone-desc { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; line-height: 1.5; }
.milestone-footer {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--text-muted); margin-bottom: 8px;
}
.milestone-amount { color: var(--primary-color); font-weight: 500; }
.milestone-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.deliverables-panel { display: flex; flex-direction: column; gap: 16px; }
.deliverable-group {
  background: white;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}
.deliverable-group-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px;
  background: var(--bg-color);
  border-bottom: 1px solid var(--border-color);
  .group-name { font-weight: 600; font-size: 14px; }
}
.deliverable-list { padding: 8px 0; }
.deliverable-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px;
  &:hover { background: #f8fafc; }
  .el-icon { color: var(--text-muted); }
}
.deliverable-info { flex: 1; }
.deliverable-name { font-size: 13px; font-weight: 500; }
.deliverable-meta { font-size: 11px; color: var(--text-muted); }
.empty-tip { padding: 16px; font-size: 13px; color: var(--text-muted); text-align: center; }
</style>
