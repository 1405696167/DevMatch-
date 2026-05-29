<template>
  <div class="page-container">
    <div class="welcome-banner enterprise-banner">
      <div>
        <h2>欢迎回来，{{ userStore.userName }} 👋</h2>
        <p>今天是 {{ today }}，管理您的开发项目</p>
      </div>
      <el-button @click="router.push('/enterprise/publish')" style="background:rgba(255,255,255,0.2);border-color:rgba(255,255,255,0.4);color:white">
        <el-icon><EditPen /></el-icon> 发布新需求
      </el-button>
    </div>

    <div class="stats-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg }">
          <el-icon :size="22" :color="s.color"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <!-- 我的需求 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">我的需求</span>
          <el-button link @click="router.push('/enterprise/tasks')">查看全部</el-button>
        </div>
        <div v-if="recentTasks.length === 0" class="empty-hint">暂无需求，<el-link type="primary" @click="router.push('/enterprise/publish')">立即发布</el-link></div>
        <div v-for="t in recentTasks" :key="t.id" class="task-row">
          <div class="task-row-info">
            <div class="task-row-name" @click="router.push('/enterprise/tasks')">{{ t.title }}</div>
            <div class="task-row-meta">
              <el-tag :type="getTaskStatusType(t.status)" size="small">{{ getTaskStatusLabel(t.status) }}</el-tag>
              <span class="text-muted">{{ t.bidCount }} 人投标</span>
            </div>
          </div>
          <el-button size="small" @click="router.push(`/enterprise/tasks/${t.id}/bids`)">查看投标</el-button>
        </div>
      </div>

      <!-- 进行中项目 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">进行中项目</span>
          <el-button link @click="router.push('/enterprise/projects')">查看全部</el-button>
        </div>
        <div v-if="activeProjects.length === 0" class="empty-hint">暂无进行中的项目</div>
        <div v-for="p in activeProjects" :key="p.id" class="project-row">
          <div class="project-row-info">
            <div class="project-row-name">{{ p.name }}</div>
            <el-progress :percentage="p.progress" :stroke-width="5" style="width:120px" />
          </div>
          <el-button size="small" type="primary" @click="router.push(`/enterprise/projects/${p.id}`)">工作台</el-button>
        </div>
      </div>

      <!-- 待处理事项 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">待处理事项</span>
          <el-badge v-if="todos.length > 0" :value="todos.length" type="danger" />
        </div>
        <div v-if="todos.length === 0" class="empty-hint">暂无待处理事项</div>
        <div v-for="t in todos" :key="t.id" class="todo-row">
          <el-icon :color="t.urgent ? '#ef4444' : '#f59e0b'"><Warning /></el-icon>
          <div class="todo-row-content">
            <div>{{ t.content }}</div>
          </div>
          <el-button size="small" type="primary" @click="router.push(t.link)">处理</el-button>
        </div>
      </div>

      <!-- 资金状态 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">资金状态</span>
          <el-button link @click="router.push('/enterprise/wallet')">钱包详情</el-button>
        </div>
        <div class="wallet-overview">
          <div class="wallet-item">
            <div class="wallet-label">账户余额</div>
            <div class="wallet-value">¥{{ formatMoney(wallet.balance) }}</div>
          </div>
          <div class="wallet-item">
            <div class="wallet-label">冻结资金</div>
            <div class="wallet-value warning">¥{{ formatMoney(wallet.frozen) }}</div>
          </div>
          <div class="wallet-item">
            <div class="wallet-label">累计支出</div>
            <div class="wallet-value">¥{{ formatMoney(wallet.totalExpense) }}</div>
          </div>
        </div>
        <el-button type="primary" style="width:100%;margin-top:16px" @click="router.push('/enterprise/wallet')">充值</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { tasksApi } from '@/api/tasks'
import { projectsApi } from '@/api/projects'
import { walletApi } from '@/api/wallet'
import { formatMoney, TASK_STATUS_MAP } from '@/utils/format'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const today = dayjs().format('YYYY年MM月DD日')

const recentTasks = ref([])
const activeProjects = ref([])
const wallet = ref({ balance: 0, frozen: 0, totalExpense: 0 })

const stats = computed(() => [
  { label: '发布需求', value: recentTasks.value.length, icon: 'EditPen', color: '#0ea5e9', bg: '#e0f2fe' },
  { label: '进行中项目', value: activeProjects.value.length, icon: 'FolderOpened', color: '#4f46e5', bg: '#ede9fe' },
  { label: '账户余额(元)', value: formatMoney(wallet.value.balance), icon: 'Wallet', color: '#10b981', bg: '#d1fae5' },
  { label: '冻结资金(元)', value: formatMoney(wallet.value.frozen), icon: 'Lock', color: '#f59e0b', bg: '#fef3c7' }
])

// 从任务列表中派生待处理事项（有投标未处理的任务）
const todos = computed(() => {
  const items = []
  recentTasks.value.forEach(t => {
    if (t.status === 'PUBLISHED' && t.bidCount > 0) {
      items.push({ id: t.id, content: `「${t.title}」有 ${t.bidCount} 个投标待审阅`, urgent: false, link: `/enterprise/tasks/${t.id}/bids` })
    }
  })
  return items
})

function getTaskStatusType(s) { return TASK_STATUS_MAP[s]?.type || 'info' }
function getTaskStatusLabel(s) { return TASK_STATUS_MAP[s]?.label || s }

async function loadDashboard() {
  try {
    const [tasksRes, projectsRes, walletRes] = await Promise.allSettled([
      tasksApi.getList({ page: 1, size: 5, mine: true }),
      projectsApi.getList({ status: 'IN_PROGRESS' }),
      walletApi.getBalance()
    ])

    if (tasksRes.status === 'fulfilled') {
      recentTasks.value = tasksRes.value.data?.list || []
    }
    if (projectsRes.status === 'fulfilled') {
      activeProjects.value = projectsRes.value.data?.list || projectsRes.value.data || []
    }
    if (walletRes.status === 'fulfilled') {
      wallet.value = walletRes.value.data || { balance: 0, frozen: 0, totalExpense: 0 }
    }
  } catch { /* 静默失败，展示空状态 */ }
}

onMounted(loadDashboard)
</script>

<style scoped lang="scss">
.welcome-banner {
  border-radius: 16px;
  padding: 28px 32px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  h2 { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
  p { opacity: 0.85; font-size: 14px; }
}

.enterprise-banner {
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
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
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { font-size: 12px; color: var(--text-muted); }

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;
  .card-title { font-weight: 600; font-size: 15px; }
}

.empty-hint {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 24px 0;
}

.task-row, .project-row, .todo-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid var(--border-color);
  &:last-child { border-bottom: none; }
}

.task-row-name, .project-row-name {
  font-size: 14px; font-weight: 500; margin-bottom: 4px; cursor: pointer;
  &:hover { color: var(--primary-color); }
}
.task-row-meta { display: flex; align-items: center; gap: 8px; }
.project-row-info { flex: 1; margin-right: 12px; }

.todo-row { align-items: flex-start; gap: 10px; }
.todo-row-content { flex: 1; font-size: 13px; }
.text-muted { color: var(--text-muted); }

.wallet-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 4px;
}
.wallet-item { text-align: center; }
.wallet-label { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.wallet-value { font-size: 18px; font-weight: 700; color: var(--text-primary); &.warning { color: #f59e0b; } }
</style>
