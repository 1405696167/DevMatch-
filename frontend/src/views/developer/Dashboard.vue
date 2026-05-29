<template>
  <div class="page-container">
    <div class="welcome-banner">
      <div>
        <h2>你好，{{ userStore.userName }} 👋</h2>
        <p>今天是 {{ today }}，查看你的最新动态</p>
      </div>
      <el-button type="primary" @click="router.push('/developer/tasks')">
        <el-icon><Search /></el-icon> 浏览任务市场
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg }">
          <el-icon :size="22" :color="s.color"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <!-- 进行中的项目 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">进行中的项目</span>
          <el-button link @click="router.push('/developer/projects')">查看全部</el-button>
        </div>
        <div v-if="activeProjects.length === 0" class="empty-tip">暂无进行中的项目</div>
        <div v-for="p in activeProjects" :key="p.id" class="project-item" @click="router.push(`/developer/projects/${p.id}`)">
          <div class="project-info">
            <div class="project-name">{{ p.name }}</div>
            <div class="project-meta">
              <span class="tag tag-primary">{{ projectProgressTag(p) }}</span>
              <span class="text-muted">截止 {{ formatDate(p.endDate) || '待定' }}</span>
            </div>
          </div>
          <el-progress :percentage="p.progress" :stroke-width="6" style="width:100px" />
        </div>
      </div>

      <!-- 待处理事项 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">待处理事项</span>
          <el-badge :value="pendingTodos.length" type="danger" />
        </div>
        <div v-if="pendingTodos.length === 0" class="empty-tip">暂无待处理事项</div>
        <div v-for="t in pendingTodos" :key="t.id" class="todo-item">
          <el-icon :color="t.urgent ? '#ef4444' : '#f59e0b'"><Warning /></el-icon>
          <div class="todo-content">
            <div class="todo-text">{{ t.content }}</div>
            <div class="todo-time">{{ fromNow(t.createdAt) }}</div>
          </div>
          <el-button size="small" type="primary" @click="router.push(t.link)">处理</el-button>
        </div>
      </div>

      <!-- 最近投标 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">最近投标</span>
          <el-button link @click="router.push('/developer/orders')">查看全部</el-button>
        </div>
        <div v-if="recentBids.length === 0" class="empty-tip">暂无投标记录</div>
        <div v-for="b in recentBids" :key="b.id" class="bid-item">
          <div class="bid-task">{{ b.taskTitle }}</div>
          <div class="bid-meta">
            <el-tag :type="getBidStatusType(b.status)" size="small">{{ getBidStatusLabel(b.status) }}</el-tag>
            <span class="text-muted">¥{{ formatMoney(b.amount) }}</span>
          </div>
        </div>
      </div>

      <!-- 收益概览 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">收益概览</span>
          <el-button link @click="router.push('/developer/wallet')">查看钱包</el-button>
        </div>
        <div class="earnings-info">
          <div class="earnings-main">
            <div class="earnings-label">本月收益</div>
            <div class="earnings-value">¥{{ formatMoney(earnings.monthly) }}</div>
          </div>
          <div class="earnings-sub-grid">
            <div class="earnings-sub">
              <div>总收益</div>
              <strong>¥{{ formatMoney(earnings.total) }}</strong>
            </div>
            <div class="earnings-sub">
              <div>可提现</div>
              <strong>¥{{ formatMoney(earnings.available) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { projectsApi } from '@/api/projects'
import { tasksApi } from '@/api/tasks'
import { walletApi } from '@/api/wallet'
import { usersApi } from '@/api/users'
import { formatDate, fromNow, formatMoney, PROJECT_STATUS_MAP } from '@/utils/format'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const today = dayjs().format('YYYY年MM月DD日 dddd')

const activeProjects = ref([])
const recentBids = ref([])
const wallet = ref({ balance: 0, totalIncome: 0, monthlyIncome: 0 })
const creditScore = ref('--')

const stats = computed(() => [
  { label: '进行中项目', value: activeProjects.value.length, icon: 'FolderOpened', color: '#4f46e5', bg: '#ede9fe' },
  { label: '待处理事项', value: pendingTodos.value.length, icon: 'Warning', color: '#f59e0b', bg: '#fef3c7' },
  { label: '信用分', value: creditScoreDisplay.value, icon: 'Medal', color: '#10b981', bg: '#d1fae5' },
  { label: '账户余额(元)', value: formatMoney(wallet.value.balance), icon: 'CircleCheck', color: '#0ea5e9', bg: '#e0f2fe' }
])

// 从进行中项目派生待处理事项
const pendingTodos = computed(() => {
  const items = []
  activeProjects.value.forEach(p => {
    if (p.status === 'PENDING_REVIEW') {
      items.push({ id: p.id, content: `「${p.name}」有里程碑等待企业验收`, urgent: false, createdAt: new Date(), link: `/developer/projects/${p.id}` })
    }
  })
  return items
})

const creditScoreDisplay = computed(() => {
  const v = creditScore.value
  if (v === null || v === undefined || v === '') return '--'
  return v
})

const earnings = computed(() => ({
  monthly: Number(wallet.value.monthlyIncome) || 0,
  total: wallet.value.totalIncome || 0,
  available: wallet.value.balance || 0
}))

function projectProgressTag(p) {
  const label = PROJECT_STATUS_MAP[p.status]?.label || p.status || '进行中'
  const pct = p.progress != null && p.progress !== '' ? `${p.progress}%` : ''
  return pct ? `${label} · ${pct}` : label
}

function getBidStatusType(s) {
  return { PENDING: 'warning', SELECTED: 'success', REJECTED: 'danger' }[s] || 'info'
}
function getBidStatusLabel(s) {
  return { PENDING: '审核中', SELECTED: '已中标', REJECTED: '未入选' }[s] || s
}

onMounted(async () => {
  const [projectsRes, bidsRes, walletRes, resumeRes] = await Promise.allSettled([
    projectsApi.getList({ status: 'IN_PROGRESS' }),
    tasksApi.getMyBids({ page: 1, size: 5 }),
    walletApi.getBalance(),
    usersApi.getResume()
  ])

  if (projectsRes.status === 'fulfilled') {
    activeProjects.value = projectsRes.value.data?.list || projectsRes.value.data || []
  }
  if (bidsRes.status === 'fulfilled') {
    recentBids.value = bidsRes.value.data?.list || []
  }
  if (walletRes.status === 'fulfilled') {
    const w = walletRes.value.data || {}
    wallet.value = {
      balance: Number(w.balance) || 0,
      totalIncome: Number(w.totalIncome) || 0,
      monthlyIncome: Number(w.monthlyIncome) || 0
    }
  }
  if (resumeRes.status === 'fulfilled') {
    const bag = resumeRes.value.data || resumeRes.value
    const cs = bag?.user?.creditScore
    creditScore.value = cs != null && cs !== '' ? cs : '--'
  }
})
</script>

<style scoped lang="scss">
.welcome-banner {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 16px;
  padding: 28px 32px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  h2 { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
  p { opacity: 0.85; font-size: 14px; }
  .el-button { background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: white; }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  .card-title { font-weight: 600; font-size: 15px; }
}

.empty-tip {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}

.project-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  &:last-child { border-bottom: none; }
  &:hover { opacity: 0.8; }
}

.project-name { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.project-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.text-muted { color: var(--text-muted); }

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
  &:last-child { border-bottom: none; }
}

.todo-content { flex: 1; }
.todo-text { font-size: 13px; margin-bottom: 3px; }
.todo-time { font-size: 11px; color: var(--text-muted); }

.bid-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
  &:last-child { border-bottom: none; }
}

.bid-task { font-size: 13px; font-weight: 500; flex: 1; margin-right: 12px; }
.bid-meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.earnings-info { padding: 8px 0; }
.earnings-main {
  text-align: center;
  padding: 16px 0;
  .earnings-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 6px; }
  .earnings-value { font-size: 32px; font-weight: 700; color: var(--primary-color); }
}
.earnings-sub-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.earnings-sub {
  background: var(--bg-color);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  div { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
  strong { font-size: 16px; color: var(--text-primary); }
}
</style>
