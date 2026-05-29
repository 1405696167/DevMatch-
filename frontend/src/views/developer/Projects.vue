<template>
  <div class="page-container">
    <h2 class="page-title">我的项目</h2>

    <div class="filter-bar">
      <el-tabs v-model="status" @tab-change="fetchProjects">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="进行中" name="IN_PROGRESS" />
        <el-tab-pane label="待验收" name="PENDING_REVIEW" />
        <el-tab-pane label="已完成" name="COMPLETED" />
        <el-tab-pane label="争议中" name="DISPUTE" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="projects-grid">
      <div v-if="projects.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无项目" />
      </div>
      <div
        v-for="p in projects"
        :key="p.id"
        class="project-card card"
        @click="router.push(`/developer/projects/${p.id}`)"
      >
        <div class="project-card-header">
          <h3 class="project-name">{{ p.name }}</h3>
          <el-tag :type="getStatusType(p.status)">{{ getStatusLabel(p.status) }}</el-tag>
        </div>
        <div class="project-enterprise">
          <el-avatar :size="24" :src="p.enterprise?.avatar">{{ p.enterprise?.name?.charAt(0) }}</el-avatar>
          <span>{{ p.enterprise?.name }}</span>
        </div>
        <div class="project-progress-section">
          <div class="progress-label">
            <span>项目进度</span>
            <span>{{ p.progress }}%</span>
          </div>
          <el-progress :percentage="p.progress" :color="getProgressColor(p.status)" :stroke-width="8" />
        </div>
        <div class="project-milestones">
          <div class="milestone-label">里程碑</div>
          <div class="milestone-list">
            <div
              v-for="m in p.milestones?.slice(0, 4)"
              :key="m.id"
              class="milestone-dot"
              :class="m.status"
              :title="m.name"
            />
            <span v-if="p.milestones?.length > 4" class="milestone-more">+{{ p.milestones.length - 4 }}</span>
          </div>
        </div>
        <div class="project-card-footer">
          <div class="project-budget">¥{{ formatMoney(p.amount) }}</div>
          <div class="project-dates">
            <el-icon><Calendar /></el-icon>
            {{ formatDate(p.startDate) }} - {{ formatDate(p.endDate) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectsApi } from '@/api/projects'
import { formatDate, formatMoney, PROJECT_STATUS_MAP } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const projects = ref([])
const status = ref('')

function getStatusType(s) { return PROJECT_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return PROJECT_STATUS_MAP[s]?.label || s }
function getProgressColor(s) {
  return { IN_PROGRESS: '#4f46e5', PENDING_REVIEW: '#f59e0b', COMPLETED: '#10b981', DISPUTE: '#ef4444' }[s] || '#4f46e5'
}

async function fetchProjects() {
  loading.value = true
  try {
    const res = await projectsApi.getList({ status: status.value })
    projects.value = res.data?.list || []
  } catch {
    projects.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchProjects)
</script>

<style scoped lang="scss">
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.project-card {
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 16px rgba(0,0,0,0.1);
    transform: translateY(-2px);
  }
}

.project-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;
}

.project-name { font-size: 16px; font-weight: 600; flex: 1; margin-right: 12px; }

.project-enterprise {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.project-progress-section { margin-bottom: 14px; }
.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.project-milestones { margin-bottom: 14px; }
.milestone-label { font-size: 12px; color: var(--text-muted); margin-bottom: 6px; }
.milestone-list { display: flex; align-items: center; gap: 4px; }
.milestone-dot {
  width: 12px; height: 12px; border-radius: 50%;
  &.ACCEPTED { background: #10b981; }
  &.IN_PROGRESS { background: #4f46e5; }
  &.SUBMITTED { background: #f59e0b; }
  &.REJECTED { background: #ef4444; }
  &.PENDING { background: #e2e8f0; }
}
.milestone-more { font-size: 11px; color: var(--text-muted); }

.project-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.project-budget { font-size: 16px; font-weight: 700; color: var(--primary-color); }
.project-dates {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: var(--text-muted);
}
</style>
