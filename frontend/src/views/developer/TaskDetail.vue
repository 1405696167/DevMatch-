<template>
  <div class="page-container" v-loading="loading">
    <div v-if="task" class="detail-layout">
      <!-- 主内容 -->
      <div class="detail-main">
        <div class="card">
          <div class="task-header">
            <div class="task-status-bar">
              <el-tag :type="headerStatusType(task)">{{ headerStatusLabel(task) }}</el-tag>
              <span class="task-category">{{ task.category }}</span>
            </div>
            <h1 class="task-title">{{ task.title }}</h1>
            <div class="task-meta">
              <span><el-icon><Timer /></el-icon> 发布于 {{ fromNow(task.createdAt) }}</span>
              <span><el-icon><User /></el-icon> {{ task.bidCount }} 人投标</span>
              <span><el-icon><View /></el-icon> {{ task.viewCount }} 次浏览</span>
            </div>
          </div>
          <el-divider />
          <div class="task-section">
            <h3>需求描述</h3>
            <div class="task-content" v-html="task.description" />
          </div>
          <el-divider />
          <div class="task-section">
            <h3>技术要求</h3>
            <div class="skill-tags">
              <span v-for="s in task.skills" :key="s" class="tag tag-primary">{{ s }}</span>
            </div>
          </div>
          <el-divider />
          <div class="task-section">
            <h3>项目附件</h3>
            <div v-if="task.attachments?.length" class="attachments">
              <div v-for="(f, idx) in task.attachments" :key="idx" class="attachment-item">
                <el-icon><Document /></el-icon>
                <span>{{ f.name }}</span>
                <el-button link size="small" type="primary" @click="downloadAttachment(f)">下载</el-button>
              </div>
            </div>
            <div v-else class="text-muted">无附件</div>
          </div>
        </div>

        <el-alert
          v-if="showBiddingClosedAlert(task)"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
          :title="biddingClosedAlertTitle(task)"
        />

        <!-- 投标表单 -->
        <div v-if="canShowBidForm(task)" class="card bid-form-card">
          <h3>提交投标</h3>
          <el-form ref="bidFormRef" :model="bidForm" label-position="top" :rules="bidRules">
            <el-form-item label="报价金额（元）" prop="amount">
              <el-input-number v-model="bidForm.amount" :min="0" :max="999999" :precision="2" style="width:200px" size="large" />
              <span class="budget-hint">预算范围：¥{{ formatMoney(task.budgetMin) }} - ¥{{ formatMoney(task.budgetMax) }}</span>
            </el-form-item>
            <el-form-item label="预计工期（天）" prop="days">
              <el-input-number v-model="bidForm.days" :min="1" :max="365" size="large" />
            </el-form-item>
            <el-form-item label="投标说明" prop="proposal">
              <el-input
                v-model="bidForm.proposal"
                type="textarea"
                :rows="5"
                placeholder="请介绍您的相关经验、解决方案，以及为什么您是最佳人选..."
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="submitBid">
              提交投标
            </el-button>
          </el-form>
        </div>
      </div>

      <!-- 侧边信息 -->
      <aside class="detail-side">
        <!-- 预算信息 -->
        <div class="card side-card">
          <div class="side-budget">
            <div class="budget-label">项目预算</div>
            <div class="budget-value">¥{{ formatMoney(task.budgetMin) }} - ¥{{ formatMoney(task.budgetMax) }}</div>
          </div>
          <el-divider />
          <div class="side-info-list">
            <div class="side-info-item">
              <span>截止日期</span>
              <strong>{{ formatDate(task.deadline) }}</strong>
            </div>
            <div class="side-info-item">
              <span>项目周期</span>
              <strong>{{ task.durationDays }} 天</strong>
            </div>
            <div class="side-info-item">
              <span>合同类型</span>
              <strong>{{ task.contractType === 'FIXED' ? '固定报价' : '按小时' }}</strong>
            </div>
          </div>
        </div>

        <!-- 发布方信息 -->
        <div class="card side-card">
          <h4 class="side-card-title">发布方信息</h4>
          <div class="company-card">
            <el-avatar :size="48" :src="task.company?.avatar">
              {{ task.company?.name?.charAt(0) }}
            </el-avatar>
            <div>
              <div class="company-name">{{ task.company?.name }}</div>
              <el-tag v-if="task.company?.verified" type="success" size="small">已认证</el-tag>
            </div>
          </div>
          <div class="side-info-list" style="margin-top:12px">
            <div class="side-info-item">
              <span>信用评分</span>
              <strong>{{ task.company?.credit }}/100</strong>
            </div>
            <div class="side-info-item">
              <span>历史项目</span>
              <strong>{{ task.company?.projectCount }} 个</strong>
            </div>
            <div class="side-info-item">
              <span>好评率</span>
              <strong>{{ task.company?.rateGood }}%</strong>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { tasksApi } from '@/api/tasks'
import { formatDate, fromNow, formatMoney, TASK_STATUS_MAP } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const bidFormRef = ref()
const task = ref(null)

const bidForm = ref({ amount: 0, days: 30, proposal: '' })
const bidRules = {
  amount: [{ required: true, type: 'number', min: 1, message: '请输入报价金额' }],
  days: [{ required: true, type: 'number', min: 1, message: '请输入预计工期' }],
  proposal: [{ required: true, message: '请填写投标说明', min: 50, message: '投标说明至少50字' }]
}

function getStatusType(s) { return TASK_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return TASK_STATUS_MAP[s]?.label || s }

function isPastDeadline(deadline) {
  if (!deadline) return false
  return dayjs(deadline).isBefore(dayjs(), 'day')
}
/** 与后端 detail 的 biddingClosed 一致；旧接口未返回时按 deadline 兜底 */
function isBiddingClosed(t) {
  if (t.biddingClosed === true) return true
  if (t.biddingClosed === false) return false
  return isPastDeadline(t.deadline)
}
function canShowBidForm(t) {
  return t.status === 'PUBLISHED' && !isBiddingClosed(t)
}
function headerStatusLabel(t) {
  if (t.status === 'EXPIRED' || (t.status === 'PUBLISHED' && isBiddingClosed(t))) return '已过期'
  return getStatusLabel(t.status)
}
function headerStatusType(t) {
  if (t.status === 'EXPIRED' || (t.status === 'PUBLISHED' && isBiddingClosed(t))) return 'info'
  return getStatusType(t.status)
}
function showBiddingClosedAlert(t) {
  return t.status === 'EXPIRED' || (t.status === 'PUBLISHED' && isBiddingClosed(t))
}
function biddingClosedAlertTitle(t) {
  if (t.status === 'EXPIRED') return '该任务已过期，无法再投标。'
  return '已超过接单截止日期，无法再投标。'
}

async function fetchTask() {
  loading.value = true
  try {
    const res = await tasksApi.getDetail(route.params.id)
    task.value = res.data
  } catch {
    task.value = {
      id: 1, title: 'Vue3 + Spring Boot 后台管理系统开发', status: 'PUBLISHED',
      category: 'Web开发', createdAt: new Date(), bidCount: 12, viewCount: 328,
      description: '<p>需要开发一套完整的企业后台管理系统...</p>',
      skills: ['Vue3', 'Spring Boot', 'MySQL', 'Redis'],
      attachments: [], budgetMin: 8000, budgetMax: 15000,
      deadline: '2026-05-01', durationDays: 60, contractType: 'FIXED',
      company: { name: '科技有限公司', avatar: '', verified: true, credit: 95, projectCount: 8, rateGood: 98 }
    }
    bidForm.value.amount = task.value.budgetMin
  } finally {
    loading.value = false }
}

async function submitBid() {
  if (!canShowBidForm(task.value)) {
    ElMessage.warning('当前任务不可投标')
    return
  }
  if (userStore.kycStatus !== 'VERIFIED') {
    await ElMessageBox.confirm(
      '投标前需要先完成实名认证，是否前往个人资料页面进行认证？',
      '需要实名认证',
      { type: 'warning', confirmButtonText: '去认证', cancelButtonText: '取消' }
    )
    router.push('/developer/profile?tab=kyc')
    return
  }
  await bidFormRef.value?.validate()
  await ElMessageBox.confirm('确定提交投标吗？', '确认投标', { type: 'info' })
  submitting.value = true
  try {
    await tasksApi.bid(task.value.id, bidForm.value)
    ElMessage.success('投标成功，等待发布方审核')
    router.push('/developer/orders')
  } finally { submitting.value = false }
}

async function downloadAttachment(f) {
  if (!f.url) return
  try {
    const res = await fetch(f.url)
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = f.name || 'attachment'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('文件下载失败，请重试')
  }
}

onMounted(fetchTask)
</script>

<style scoped lang="scss">
.detail-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
  align-items: start;
}

.task-header { margin-bottom: 4px; }
.task-status-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.task-category { font-size: 13px; color: var(--text-muted); }
.task-title { font-size: 22px; font-weight: 700; margin-bottom: 10px; }
.task-meta {
  display: flex; align-items: center; gap: 16px; font-size: 13px; color: var(--text-secondary);
  span { display: flex; align-items: center; gap: 4px; }
}

.task-section {
  h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
}

.task-content { line-height: 1.8; font-size: 14px; color: var(--text-secondary); }
.skill-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.attachments { display: flex; flex-direction: column; gap: 8px; }
.attachment-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.text-muted { color: var(--text-muted); font-size: 13px; }

.bid-form-card h3 { font-size: 16px; font-weight: 600; margin-bottom: 16px; }
.budget-hint { margin-left: 12px; font-size: 13px; color: var(--text-muted); }

.side-card { margin-bottom: 16px; }
.side-budget { text-align: center; padding: 8px 0; }
.budget-label { font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }
.budget-value { font-size: 20px; font-weight: 700; color: var(--primary-color); }

.side-info-list { display: flex; flex-direction: column; gap: 8px; }
.side-info-item {
  display: flex; justify-content: space-between; font-size: 13px;
  span { color: var(--text-secondary); }
}

.side-card-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.company-card { display: flex; align-items: center; gap: 10px; }
.company-name { font-weight: 600; margin-bottom: 4px; }
</style>
