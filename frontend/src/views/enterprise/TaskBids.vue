<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2 class="page-title" style="display:inline-block;margin-left:8px">投标列表</h2>
      </div>
      <div class="header-task-info" v-if="task">
        <el-tag>{{ task.title }}</el-tag>
        <span class="text-muted">共 {{ bids.length }} 人投标</span>
      </div>
    </div>

    <!-- 排序筛选 -->
    <div class="sort-bar card" style="display:flex;align-items:center;gap:16px;padding:14px 20px;margin-bottom:16px">
      <span class="text-muted">排序：</span>
      <el-radio-group v-model="sortBy" size="small">
        <el-radio-button label="LATEST">最新</el-radio-button>
        <el-radio-button label="AMOUNT_ASC">报价从低到高</el-radio-button>
        <el-radio-button label="CREDIT">信用分</el-radio-button>
      </el-radio-group>
    </div>

    <div class="bids-list">
      <div v-if="bids.length === 0 && !loading">
        <el-empty description="暂无投标" />
      </div>

      <div v-for="bid in sortedBids" :key="bid.id" class="bid-card card">
        <div class="bid-header">
          <div class="developer-info">
            <el-avatar :size="52" :src="bid.developer?.avatar">{{ bid.developer?.name?.charAt(0) }}</el-avatar>
            <div class="developer-details">
              <div class="developer-name">{{ bid.developer?.name }}</div>
              <div class="developer-meta">
                <el-tag v-if="bid.developer?.kycVerified" type="success" size="small">已实名</el-tag>
                <span>信用分 {{ bid.developer?.creditScore }}</span>
                <span>完成 {{ bid.developer?.completedProjects }} 个项目</span>
                <el-rate :model-value="bid.developer?.rating" disabled size="small" />
              </div>
              <div class="developer-skills">
                <span v-for="s in bid.developer?.skills?.slice(0, 5)" :key="s" class="tag tag-info">{{ s }}</span>
              </div>
            </div>
          </div>
          <div class="bid-price-info">
            <div class="bid-amount">¥{{ formatMoney(bid.amount) }}</div>
            <div class="bid-days">{{ bid.days }} 天完成</div>
            <div class="bid-time">{{ fromNow(bid.createdAt) }}</div>
          </div>
        </div>

        <div class="bid-proposal">
          <div class="proposal-label">投标说明</div>
          <p class="proposal-text" :class="{ expanded: bid.expanded }">{{ bid.proposal }}</p>
          <el-button v-if="bid.proposal?.length > 150" link size="small" @click="bid.expanded = !bid.expanded">
            {{ bid.expanded ? '收起' : '展开全文' }}
          </el-button>
        </div>

        <div class="bid-actions">
          <el-button
            type="primary"
            :disabled="task?.status !== 'PUBLISHED'"
            @click="selectBid(bid)"
          >
            选择此开发者
          </el-button>
          <el-button @click="viewDeveloper(bid.developer?.id)">查看详细档案</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tasksApi } from '@/api/tasks'
import { formatMoney, fromNow } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const task = ref(null)
const bids = ref([])
const sortBy = ref('LATEST')

const sortedBids = computed(() => {
  const list = [...bids.value]
  if (sortBy.value === 'AMOUNT_ASC') return list.sort((a, b) => a.amount - b.amount)
  if (sortBy.value === 'CREDIT') return list.sort((a, b) => b.developer.creditScore - a.developer.creditScore)
  return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

async function fetchBids() {
  loading.value = true
  try {
    const [taskRes, bidsRes] = await Promise.all([
      tasksApi.getDetail(route.params.id),
      tasksApi.getBids(route.params.id)
    ])
    task.value = taskRes.data
    bids.value = bidsRes.data || []
  } catch {
    task.value = { id: 1, title: 'Vue3 + Spring Boot 后台管理系统开发', status: 'PUBLISHED' }
    bids.value = [
      {
        id: 1, amount: 10000, days: 40, createdAt: new Date(),
        proposal: '本人有5年全栈开发经验，曾独立完成多个企业级Vue3+Spring Boot项目。熟悉Element Plus、Vite、MyBatis等主流技术栈，代码规范，注重性能优化。',
        developer: { id: 1, name: '张小明', avatar: '', kycVerified: true, creditScore: 98, completedProjects: 15, rating: 4.8, skills: ['Vue3', 'Spring Boot', 'MySQL'] },
        expanded: false
      },
      {
        id: 2, amount: 8500, days: 35, createdAt: new Date(),
        proposal: '3年Vue开发经验，参与过多个大型B端项目开发，擅长后台管理系统。',
        developer: { id: 2, name: '李开发', avatar: '', kycVerified: true, creditScore: 92, completedProjects: 8, rating: 4.5, skills: ['Vue3', 'Element Plus', 'TypeScript'] },
        expanded: false
      }
    ]
  } finally { loading.value = false }
}

async function selectBid(bid) {
  await ElMessageBox.confirm(
    `确定选择 ${bid.developer.name} 吗？选择后将自动生成项目并通知对方。`,
    '确认选择', { type: 'info' }
  )
  await tasksApi.selectBid(route.params.id, bid.id)
  ElMessage.success('已选择开发者，项目已自动创建')
  router.push('/enterprise/projects')
}

function viewDeveloper(id) { router.push(`/enterprise/developers/${id}`) }

onMounted(fetchBids)
</script>

<style scoped lang="scss">
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}
.header-task-info { display: flex; align-items: center; gap: 10px; }

.bids-list { display: flex; flex-direction: column; gap: 16px; }

.bid-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px;
}

.developer-info { display: flex; gap: 14px; flex: 1; }
.developer-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.developer-meta {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  font-size: 12px; color: var(--text-secondary); margin-bottom: 6px;
}
.developer-skills { display: flex; flex-wrap: wrap; gap: 4px; }

.bid-price-info { text-align: right; flex-shrink: 0; }
.bid-amount { font-size: 22px; font-weight: 700; color: var(--primary-color); }
.bid-days { font-size: 13px; color: var(--text-secondary); margin-bottom: 2px; }
.bid-time { font-size: 12px; color: var(--text-muted); }

.bid-proposal {
  background: var(--bg-color); border-radius: 8px; padding: 12px; margin-bottom: 14px;
  .proposal-label { font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }
  .proposal-text {
    font-size: 13px; color: var(--text-secondary); line-height: 1.6;
    display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
    &.expanded { display: block; -webkit-line-clamp: unset; }
  }
}

.bid-actions { display: flex; gap: 10px; }
.text-muted { color: var(--text-muted); font-size: 13px; }
</style>
