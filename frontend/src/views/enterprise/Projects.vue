<template>
  <div class="page-container">
    <h2 class="page-title">项目管控</h2>
    <el-tabs v-model="status" @tab-change="fetchProjects">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="进行中" name="IN_PROGRESS" />
      <el-tab-pane label="待验收" name="PENDING_REVIEW" />
      <el-tab-pane label="已完成" name="COMPLETED" />
    </el-tabs>
    <div v-loading="loading">
      <el-empty v-if="!loading && projects.length === 0" description="暂无项目，发布需求并选定开发者后将自动创建项目" />
    </div>
    <div v-loading="loading" class="projects-grid">
      <div v-for="p in projects" :key="p.id" class="project-card card" @click="router.push(`/enterprise/projects/${p.id}`)">
        <div class="project-card-header">
          <h3 class="project-name">{{ p.name }}</h3>
          <el-tag :type="getStatusType(p.status)">{{ getStatusLabel(p.status) }}</el-tag>
        </div>
        <div class="project-developer">
          <el-avatar :size="24" :src="p.developer?.avatar">{{ p.developer?.name?.charAt(0) }}</el-avatar>
          <span>开发者：{{ p.developer?.name }}</span>
        </div>
        <div class="project-progress-section">
          <div class="progress-label"><span>项目进度</span><span>{{ p.progress }}%</span></div>
          <el-progress :percentage="p.progress" :stroke-width="8" color="#0ea5e9" />
        </div>
        <div class="project-card-footer">
          <div class="project-budget">¥{{ formatMoney(p.amount) }}</div>
          <div class="project-dates"><el-icon><Calendar /></el-icon> {{ formatDate(p.endDate) }} 到期</div>
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

async function fetchProjects() {
  loading.value = true
  try {
    const res = await projectsApi.getList({ status: status.value })
    projects.value = res.data?.list || []
  } catch {
    projects.value = []
  } finally { loading.value = false }
}

onMounted(fetchProjects)
</script>

<style scoped lang="scss">
.projects-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }
.project-card { cursor: pointer; transition: all 0.2s; &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); transform: translateY(-2px); } }
.project-card-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 10px; }
.project-name { font-size: 16px; font-weight: 600; flex: 1; margin-right: 12px; }
.project-developer { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); margin-bottom: 14px; }
.project-progress-section { margin-bottom: 14px; }
.progress-label { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
.project-card-footer { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; border-top: 1px solid var(--border-color); }
.project-budget { font-size: 16px; font-weight: 700; color: #0ea5e9; }
.project-dates { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--text-muted); }
</style>
