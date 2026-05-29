<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">任务市场</h2>
      <span class="result-count">共找到 <strong>{{ total }}</strong> 个任务</span>
    </div>

    <div class="market-layout">
      <!-- 筛选栏 -->
      <aside class="filter-panel card">
        <div class="filter-title">筛选条件</div>

        <div class="filter-group">
          <div class="filter-label">技术栈</div>
          <el-checkbox-group v-model="filters.skills">
            <el-checkbox v-for="s in popularSkills" :key="s" :label="s">{{ s }}</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="filter-group">
          <div class="filter-label">预算范围</div>
          <el-slider
            v-model="filters.budgetRange"
            range
            :min="0"
            :max="100000"
            :step="1000"
            :format-tooltip="v => `¥${v}`"
          />
          <div class="budget-labels">
            <span>¥{{ filters.budgetRange[0].toLocaleString() }}</span>
            <span>¥{{ filters.budgetRange[1].toLocaleString() }}</span>
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-label">项目周期</div>
          <el-radio-group v-model="filters.duration" class="duration-group">
            <el-radio label="">不限</el-radio>
            <el-radio label="SHORT">1个月内</el-radio>
            <el-radio label="MEDIUM">1-3个月</el-radio>
            <el-radio label="LONG">3个月以上</el-radio>
          </el-radio-group>
        </div>

        <div class="filter-group">
          <div class="filter-label">排序方式</div>
          <el-select v-model="filters.sort" style="width:100%">
            <el-option label="最新发布" value="LATEST" />
            <el-option label="预算从高到低" value="BUDGET_DESC" />
            <el-option label="预算从低到高" value="BUDGET_ASC" />
            <el-option label="截止时间" value="DEADLINE" />
          </el-select>
        </div>

        <el-button @click="resetFilters" style="width:100%">重置筛选</el-button>
      </aside>

      <!-- 任务列表 -->
      <div class="task-list-area">
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索任务名称、技术栈..."
            size="large"
            clearable
            @keyup.enter="fetchTasks"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" size="large" @click="fetchTasks">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
        </div>

        <div v-loading="loading" class="task-cards">
          <div v-if="tasks.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无符合条件的任务" />
          </div>

          <div
            v-for="task in tasks"
            :key="task.id"
            class="task-card card"
            @click="router.push(`/developer/tasks/${task.id}`)"
          >
            <div class="task-card-header">
              <div class="task-title">{{ task.title }}</div>
              <div class="task-budget">¥{{ formatMoney(task.budgetMin) }} - ¥{{ formatMoney(task.budgetMax) }}</div>
            </div>
            <p class="task-desc">{{ task.description }}</p>
            <div class="task-tags">
              <span v-for="s in task.skills" :key="s" class="tag tag-primary">{{ s }}</span>
            </div>
            <div class="task-footer">
              <div class="task-meta-left">
                <el-avatar :size="24" :src="task.company?.avatar">{{ task.company?.name?.charAt(0) }}</el-avatar>
                <span class="company-name">{{ task.company?.name }}</span>
                <el-rate :model-value="task.company?.credit / 20" disabled :max="5" :colors="['#f59e0b','#f59e0b','#f59e0b']" />
              </div>
              <div class="task-meta-right">
                <el-icon><Timer /></el-icon>
                <span>截止 {{ formatDate(task.deadline) }}</span>
                <el-icon><User /></el-icon>
                <span>{{ task.bidCount }} 人投标</span>
              </div>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > 0"
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          class="pagination"
          @change="fetchTasks"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { tasksApi } from '@/api/tasks'
import { formatDate, formatMoney, SKILL_TAGS } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const filters = reactive({
  skills: [],
  budgetRange: [0, 100000],
  duration: '',
  sort: 'LATEST'
})

const popularSkills = SKILL_TAGS.slice(0, 15)

function resetFilters() {
  filters.skills = []
  filters.budgetRange = [0, 100000]
  filters.duration = ''
  filters.sort = 'LATEST'
  keyword.value = ''
  fetchTasks()
}

async function fetchTasks() {
  loading.value = true
  try {
    const res = await tasksApi.getList({
      keyword: keyword.value,
      skills: filters.skills.join(','),
      budgetMin: filters.budgetRange[0],
      budgetMax: filters.budgetRange[1],
      duration: filters.duration,
      sort: filters.sort,
      page: page.value,
      size: pageSize.value,
      status: 'PUBLISHED'
    })
    tasks.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    tasks.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchTasks)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  .result-count { font-size: 13px; color: var(--text-secondary); }
}

.market-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  align-items: start;
}

.filter-panel {
  position: sticky;
  top: calc(var(--header-height) + 24px);
}

.filter-title { font-weight: 600; margin-bottom: 20px; font-size: 15px; }

.filter-group {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  &:last-of-type { border-bottom: none; }
}

.filter-label { font-size: 13px; font-weight: 500; margin-bottom: 10px; color: var(--text-secondary); }

:deep(.el-checkbox) {
  display: flex;
  margin-bottom: 6px;
  margin-right: 0;
}

.budget-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

.duration-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  .el-input { flex: 1; }
}

.task-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.task-card {
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 16px rgba(79,70,229,0.12);
    border-color: var(--primary-light);
    transform: translateY(-1px);
  }
}

.task-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-title {
  font-size: 16px;
  font-weight: 600;
  flex: 1;
  margin-right: 16px;
}

.task-budget {
  font-size: 16px;
  font-weight: 700;
  color: var(--primary-color);
  white-space: nowrap;
}

.task-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.task-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.task-meta-left, .task-meta-right {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.company-name { font-weight: 500; }

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
