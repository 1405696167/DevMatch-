<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">我的需求</h2>
      <el-button type="primary" @click="router.push('/enterprise/publish')">
        <el-icon><Plus /></el-icon> 发布新需求
      </el-button>
    </div>

    <el-tabs v-model="activeStatus" @tab-change="fetchTasks">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="草稿" name="DRAFT" />
      <el-tab-pane label="审核中" name="AUDITING" />
      <el-tab-pane label="招募中" name="PUBLISHED" />
      <el-tab-pane label="已过期" name="EXPIRED" />
      <el-tab-pane label="进行中" name="IN_PROGRESS" />
      <el-tab-pane label="已完成" name="COMPLETED" />
    </el-tabs>

    <div v-loading="loading" class="tasks-list">
      <div v-if="tasks.length === 0 && !loading">
        <el-empty description="暂无需求">
          <el-button type="primary" @click="router.push('/enterprise/publish')">立即发布</el-button>
        </el-empty>
      </div>
      <div v-for="task in tasks" :key="task.id" class="task-card card">
        <div class="task-header">
          <div>
            <h3 class="task-title">{{ task.title }}</h3>
            <div class="task-meta">
              <el-tag :type="displayStatusType(task)">{{ displayStatusLabel(task) }}</el-tag>
              <span class="meta-item"><el-icon><Money /></el-icon> ¥{{ formatMoney(task.budgetMin) }}-¥{{ formatMoney(task.budgetMax) }}</span>
              <span class="meta-item"><el-icon><Timer /></el-icon> 截止 {{ formatDate(task.deadline) }}</span>
              <span class="meta-item"><el-icon><User /></el-icon> {{ task.bidCount }} 人投标</span>
            </div>
          </div>
          <div class="task-actions">
            <el-button
              v-if="['PUBLISHED','EXPIRED'].includes(task.status) && task.bidCount > 0"
              type="primary"
              @click="router.push(`/enterprise/tasks/${task.id}/bids`)"
            >
              查看投标 ({{ task.bidCount }})
            </el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, task)">
              <el-button>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :disabled="isEditDisabled(task)">编辑</el-dropdown-item>
                  <el-dropdown-item command="close" :disabled="task.status !== 'PUBLISHED'">关闭招募</el-dropdown-item>
                  <el-dropdown-item command="delete" :disabled="!['DRAFT','REJECTED'].includes(task.status)" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <el-alert
          v-if="task.status === 'REJECTED' && task.rejectReason"
          :title="`驳回原因：${task.rejectReason}`"
          type="error"
          :closable="false"
          style="margin-bottom:8px"
        />
        <p class="task-desc">{{ task.description }}</p>
        <div class="task-footer">
          <div class="skill-tags">
            <span v-for="s in task.skills" :key="s" class="tag tag-primary">{{ s }}</span>
          </div>
          <div class="task-created">发布于 {{ fromNow(task.createdAt) }}</div>
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
      @change="fetchTasks"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { tasksApi } from '@/api/tasks'
import { formatDate, formatMoney, fromNow, TASK_STATUS_MAP } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const total = ref(0)
const page = ref(1)
const activeStatus = ref('')

function getStatusType(s) { return TASK_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return TASK_STATUS_MAP[s]?.label || s }

/** 调度尚未跑完时仍为 PUBLISHED 但已过 deadline 的展示兜底 */
function isPastDeadline(deadline) {
  if (!deadline) return false
  return dayjs(deadline).isBefore(dayjs(), 'day')
}
function displayStatusLabel(task) {
  if (task.status === 'PUBLISHED' && isPastDeadline(task.deadline)) return '已过期'
  return getStatusLabel(task.status)
}
function displayStatusType(task) {
  if (task.status === 'PUBLISHED' && isPastDeadline(task.deadline)) return 'info'
  return getStatusType(task.status)
}
function isEditDisabled(task) {
  if (['IN_PROGRESS', 'COMPLETED', 'CLOSED', 'EXPIRED'].includes(task.status)) return true
  if (task.status === 'PUBLISHED' && isPastDeadline(task.deadline)) return true
  return false
}

async function fetchTasks() {
  loading.value = true
  try {
    const res = await tasksApi.getList({ status: activeStatus.value, page: page.value, size: 10, mine: true })
    tasks.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    tasks.value = []
    total.value = 0
  } finally { loading.value = false }
}

async function handleCommand(cmd, task) {
  if (cmd === 'edit') { router.push(`/enterprise/publish?id=${task.id}`) }
  else if (cmd === 'close') {
    await ElMessageBox.confirm('确定关闭该需求的招募吗？', '关闭招募', { type: 'warning' })
    await tasksApi.close(task.id)
    ElMessage.success('已关闭招募')
    fetchTasks()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm('确定删除该草稿吗？', '删除确认', { type: 'warning' })
    await tasksApi.delete(task.id)
    ElMessage.success('已删除')
    fetchTasks()
  }
}

onMounted(fetchTasks)
</script>

<style scoped lang="scss">
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
}

.tasks-list { display: flex; flex-direction: column; gap: 16px; min-height: 200px; }

.task-card {}

.task-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 10px;
}

.task-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.task-meta {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  font-size: 13px; color: var(--text-secondary);
}
.meta-item { display: flex; align-items: center; gap: 4px; }
.task-actions { display: flex; gap: 8px; flex-shrink: 0; }

.task-desc {
  font-size: 13px; color: var(--text-secondary);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  margin-bottom: 12px;
}

.task-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 12px; border-top: 1px solid var(--border-color);
}
.skill-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.task-created { font-size: 12px; color: var(--text-muted); }

.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
