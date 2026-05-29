<template>
  <div class="page-container">
    <h2 class="page-title">我的投标</h2>

    <div class="tab-bar">
      <el-tabs v-model="activeTab" @tab-change="fetchBids">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="审核中" name="PENDING" />
        <el-tab-pane label="已中标" name="SELECTED" />
        <el-tab-pane label="未入选" name="REJECTED" />
        <el-tab-pane label="已撤回" name="CANCELLED" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="orders-list">
      <div v-if="bids.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无投标记录" />
      </div>
      <div v-for="bid in bids" :key="bid.id" class="order-card card">
        <div class="order-header">
          <div class="order-task-info">
            <h3 class="order-task-title" @click="router.push(`/developer/tasks/${bid.taskId}`)">
              {{ bid.taskTitle }}
            </h3>
            <div class="order-tags">
              <span v-for="s in bid.skills" :key="s" class="tag tag-primary">{{ s }}</span>
            </div>
          </div>
          <el-tag :type="getStatusType(bid.status)" size="large">{{ getStatusLabel(bid.status) }}</el-tag>
        </div>
        <el-divider style="margin: 12px 0" />
        <div class="order-meta">
          <div class="meta-item">
            <el-icon><Money /></el-icon>
            <span>我的报价</span>
            <strong>¥{{ formatMoney(bid.amount) }}</strong>
          </div>
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>预计工期</span>
            <strong>{{ bid.days }} 天</strong>
          </div>
          <div class="meta-item">
            <el-icon><Timer /></el-icon>
            <span>投标时间</span>
            <strong>{{ formatDateTime(bid.createdAt) }}</strong>
          </div>
        </div>
        <div class="order-proposal">
          <div class="proposal-label">投标说明</div>
          <p>{{ bid.proposal }}</p>
        </div>
        <div class="order-actions">
          <el-button v-if="bid.status === 'SELECTED'" type="primary" @click="router.push(`/developer/projects/${bid.projectId}`)">
            进入项目工作台
          </el-button>
          <el-button v-if="bid.status === 'PENDING'" @click="cancelBid(bid)">撤回投标</el-button>
          <el-button @click="router.push(`/developer/tasks/${bid.taskId}`)">查看任务</el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      :total="total"
      layout="total, prev, pager, next"
      background
      class="pagination"
      @change="fetchBids"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tasksApi } from '@/api/tasks'
import { formatMoney, formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const bids = ref([])
const total = ref(0)
const page = ref(1)
const activeTab = ref('')

function getStatusType(s) {
  return { PENDING: 'warning', SELECTED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[s] || 'info'
}
function getStatusLabel(s) {
  return { PENDING: '审核中', SELECTED: '已中标', REJECTED: '未入选', CANCELLED: '已撤回' }[s] || s
}

async function fetchBids() {
  loading.value = true
  try {
    const res = await tasksApi.getMyBids({ status: activeTab.value || undefined, page: page.value, size: 10 })
    // 后端返回 { list, total } 或直接是数组（兼容两种情况）
    const data = res.data
    bids.value = Array.isArray(data) ? data : (data?.list || [])
    total.value = Array.isArray(data) ? data.length : (data?.total || 0)
  } catch {
    bids.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function cancelBid(bid) {
  await ElMessageBox.confirm('确定撤回该投标吗？', '撤回投标', { type: 'warning' })
  await tasksApi.cancelBid(bid.taskId, bid.id)
  ElMessage.success('已撤回投标')
  fetchBids()
}

onMounted(fetchBids)
</script>

<style scoped lang="scss">
.orders-list { display: flex; flex-direction: column; gap: 16px; }

.order-card {}

.order-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.order-task-title {
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 8px;
  &:hover { color: var(--primary-color); }
}

.order-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.order-meta {
  display: flex;
  gap: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  strong { color: var(--text-primary); }
}

.order-proposal {
  background: var(--bg-color);
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  .proposal-label { font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }
  p { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
}

.order-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
