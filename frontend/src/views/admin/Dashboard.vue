<template>
  <div class="page-container">
    <div class="stats-grid">
      <div v-for="s in statsCards" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg }">
          <el-icon :size="24" :color="s.color"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <!-- 用户增长趋势 -->
      <div class="card chart-card">
        <div class="chart-header">
          <span class="chart-title">近7日用户注册</span>
        </div>
        <div ref="userChartRef" class="chart-container" />
      </div>

      <!-- 交易额趋势 -->
      <div class="card chart-card">
        <div class="chart-header">
          <span class="chart-title">近7日交易笔数</span>
        </div>
        <div ref="txChartRef" class="chart-container" />
      </div>
    </div>

    <div class="bottom-grid">
      <!-- 待审核（任务 + 认证提示） -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">待审核</span>
          <el-button link type="primary" @click="router.push('/admin/tasks')">任务审核</el-button>
        </div>
        <el-alert type="info" :closable="false" show-icon class="pending-hint">
          <template #title>认证审核</template>
          下表为待平台审核的<strong>任务需求</strong>。开发者实名、企业资质等请到
          <el-link type="primary" @click="router.push('/admin/kyc')">认证审核</el-link>
          处理。
          <span v-if="pendingKycTotal > 0">当前有 <strong>{{ pendingKycTotal }}</strong> 条认证申请待处理。</span>
          <span v-else>当前暂无待处理的认证申请。</span>
        </el-alert>
        <el-empty v-if="pendingTasks.length === 0 && !loadingTasks" description="暂无待审核的任务" :image-size="60" />
        <el-table v-else :data="pendingTasks" v-loading="loadingTasks" size="small">
          <el-table-column prop="title" label="任务名称" show-overflow-tooltip />
          <el-table-column label="发布方" width="100">
            <template #default="{ row }">{{ row.company?.name || row.companyName }}</template>
          </el-table-column>
          <el-table-column label="预算" width="140">
            <template #default="{ row }">¥{{ formatMoney(row.budgetMin) }}-¥{{ formatMoney(row.budgetMax) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="quickAudit(row, 'PASS')">通过</el-button>
              <el-button size="small" type="danger" @click="quickAudit(row, 'REJECT')">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 平台数据摘要 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">平台概览</span>
        </div>
        <div class="platform-summary">
          <div v-for="item in platformSummary" :key="item.label" class="summary-item">
            <div class="summary-label">{{ item.label }}</div>
            <div class="summary-value" :style="{ color: item.color }">{{ item.value }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api/admin'
import { tasksApi } from '@/api/tasks'
import { formatMoney } from '@/utils/format'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()
const userChartRef = ref()
const txChartRef = ref()
const userChart = shallowRef(null)
const txChart = shallowRef(null)
const loadingTasks = ref(false)
const pendingKycTotal = ref(0)

const dashboardStats = ref({ totalUsers: 0, totalTasks: 0, totalProjects: 0, pendingAudit: 0 })
const pendingTasks = ref([])

const statsCards = computed(() => [
  { label: '总用户数', value: dashboardStats.value.totalUsers, icon: 'UserFilled', color: '#4f46e5', bg: '#ede9fe' },
  { label: '总任务数', value: dashboardStats.value.totalTasks, icon: 'EditPen', color: '#10b981', bg: '#d1fae5' },
  { label: '总项目数', value: dashboardStats.value.totalProjects, icon: 'FolderOpened', color: '#0ea5e9', bg: '#e0f2fe' },
  { label: '待审任务', value: dashboardStats.value.pendingAudit, icon: 'Warning', color: '#f59e0b', bg: '#fef3c7' }
])

const platformSummary = computed(() => [
  { label: '总用户数', value: dashboardStats.value.totalUsers, color: '#4f46e5' },
  { label: '总任务数', value: dashboardStats.value.totalTasks, color: '#0ea5e9' },
  { label: '总项目数', value: dashboardStats.value.totalProjects, color: '#10b981' },
  { label: '待审任务', value: dashboardStats.value.pendingAudit, color: '#f59e0b' }
])

function initChart(ref, chart, data, label, color) {
  if (!ref.value) return
  chart.value = echarts.init(ref.value)
  const xData = data.map(d => d.date?.slice(5) || d.date)
  const yData = data.map(d => d.count ?? 0)
  chart.value.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: label,
      type: 'line',
      smooth: true,
      data: yData,
      itemStyle: { color },
      areaStyle: { color: color + '20' }
    }]
  })
}

async function quickAudit(task, action) {
  await tasksApi.auditTask(task.id, { action, reason: '' })
  ElMessage.success(action === 'PASS' ? '已通过审核' : '已驳回')
  pendingTasks.value = pendingTasks.value.filter(t => t.id !== task.id)
  dashboardStats.value.pendingAudit = Math.max(0, dashboardStats.value.pendingAudit - 1)
}

onMounted(async () => {
  try {
    const [statsRes, pendingRes, kycRes, userChartRes, txChartRes] = await Promise.allSettled([
      adminApi.getDashboard(),
      (() => { loadingTasks.value = true; return tasksApi.getList({ status: 'AUDITING', page: 1, size: 5 }) })(),
      adminApi.getKycList({ status: 'AUDITING', page: 1, size: 1 }),
      adminApi.getUserGrowthChart(),
      adminApi.getTransactionChart()
    ])

    if (statsRes.status === 'fulfilled') {
      dashboardStats.value = statsRes.value.data || dashboardStats.value
    }
    if (pendingRes.status === 'fulfilled') {
      pendingTasks.value = pendingRes.value.data?.list || []
    }
    if (kycRes.status === 'fulfilled') {
      pendingKycTotal.value = Number(kycRes.value.data?.total) || 0
    }
    loadingTasks.value = false

    setTimeout(() => {
      const ugData = userChartRes.status === 'fulfilled' ? (userChartRes.value.data || []) : []
      const txData = txChartRes.status === 'fulfilled' ? (txChartRes.value.data || []) : []
      initChart(userChartRef, userChart, ugData, '新增用户', '#4f46e5')
      initChart(txChartRef, txChart, txData, '交易笔数', '#10b981')
    }, 100)
  } catch {
    loadingTasks.value = false
  }
})
</script>

<style scoped lang="scss">
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.stat-value { font-size: 22px; font-weight: 700; margin-bottom: 3px; }
.stat-label { font-size: 12px; color: var(--text-muted); }

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card { }
.chart-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  .chart-title { font-size: 15px; font-weight: 600; }
}
.chart-container { height: 240px; }

.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

.card-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  .card-title { font-size: 15px; font-weight: 600; }
}

.pending-hint { margin-bottom: 12px; }
.pending-hint :deep(.el-alert__title) { font-weight: 600; }

.platform-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.summary-item {
  padding: 12px;
  background: var(--bg-color);
  border-radius: 8px;
  .summary-label { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
  .summary-value { font-size: 20px; font-weight: 700; }
}
</style>
