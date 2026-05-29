<template>
  <div class="page-container">
    <h2 class="page-title">任务审核</h2>

    <div class="filter-bar card" style="padding:16px 20px;margin-bottom:16px">
      <el-input v-model="keyword" placeholder="搜索任务名称..." size="small" clearable style="width:200px" />
      <el-select v-model="statusFilter" size="small" style="width:150px" clearable placeholder="状态筛选">
        <el-option label="待审核" value="AUDITING" />
        <el-option label="已通过（全部）" value="APPROVED_ALL" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" size="small" @click="fetchTasks">搜索</el-button>
    </div>

    <div class="card">
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务数据" :image-size="60" />
      <el-table v-else :data="tasks" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
                <el-descriptions-item label="项目类型">{{ row.category || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="合同类型">{{ row.contractType === 'FIXED' ? '固定报价' : row.contractType === 'HOURLY' ? '按小时计费' : '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="经验要求">{{ { NONE: '不限', JUNIOR: '1-3年', SENIOR: '3-5年', EXPERT: '5年以上' }[row.experience] || '不限' }}</el-descriptions-item>
                <el-descriptions-item label="付款方式">{{ row.paymentType === 'MILESTONE' ? '里程碑付款' : row.paymentType === 'ONCE' ? '一次性付款' : '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="预算范围">¥{{ formatMoney(row.budgetMin) }} - ¥{{ formatMoney(row.budgetMax) }}</el-descriptions-item>
                <el-descriptions-item label="截止日期">{{ row.deadline || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="项目周期">{{ row.durationDays ? row.durationDays + ' 天' : '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="需要实名认证">{{ row.requireKyc ? '是' : '否' }}</el-descriptions-item>
              </el-descriptions>
              <div class="expand-label">需求描述</div>
              <p style="white-space:pre-wrap;line-height:1.7;color:var(--text-secondary);margin-bottom:12px">{{ row.description }}</p>
              <div class="expand-label">技术栈</div>
              <div class="skill-tags" style="margin-bottom:12px">
                <el-tag v-for="s in row.skills" :key="s" size="small">{{ s }}</el-tag>
                <span v-if="!row.skills?.length" style="color:var(--text-muted);font-size:12px">无</span>
              </div>
              <div class="expand-label">需求附件</div>
              <div v-if="row.attachments?.length" class="attachment-list">
                <div v-for="(f, idx) in row.attachments" :key="idx" class="attachment-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ f.name }}</span>
                  <el-button link size="small" type="primary" @click="downloadFile(f)">下载</el-button>
                </div>
              </div>
              <span v-else style="color:var(--text-muted);font-size:12px">无附件</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="发布方" width="130">
          <template #default="{ row }">{{ row.company?.name || row.companyId }}</template>
        </el-table-column>
        <el-table-column label="预算" width="160">
          <template #default="{ row }">¥{{ formatMoney(row.budgetMin) }} - ¥{{ formatMoney(row.budgetMax) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'AUDITING'">
              <el-button size="small" type="success" @click="auditTask(row, 'PASS')">通过</el-button>
              <el-button size="small" type="danger" @click="openRejectDialog(row)">驳回</el-button>
            </template>
            <el-button v-else size="small" link @click="viewTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
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

    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="400px">
      <el-form :model="rejectForm" label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请说明驳回原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { tasksApi } from '@/api/tasks'
import { formatMoney, formatDateTime, TASK_STATUS_MAP } from '@/utils/format'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tasks = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const statusFilter = ref('AUDITING')
const rejectDialogVisible = ref(false)
const currentTask = ref(null)
const rejectForm = reactive({ reason: '' })

function getStatusType(s) { return TASK_STATUS_MAP[s]?.type || 'info' }
function getStatusLabel(s) { return TASK_STATUS_MAP[s]?.label || s }

async function fetchTasks() {
  loading.value = true
  try {
    const res = await tasksApi.getList({ keyword: keyword.value, status: statusFilter.value, page: page.value, size: 10 })
    tasks.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    tasks.value = []
    total.value = 0
  } finally { loading.value = false }
}

async function auditTask(task, action) {
  // 后端期望 APPROVE / REJECT
  const backendAction = action === 'PASS' ? 'APPROVE' : 'REJECT'
  await tasksApi.auditTask(task.id, { action: backendAction, reason: rejectForm.reason || '' })
  ElMessage.success(action === 'PASS' ? '审核通过' : '已驳回')
  fetchTasks()
}

function openRejectDialog(task) {
  currentTask.value = task
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectForm.reason) { ElMessage.warning('请填写驳回原因'); return }
  await auditTask(currentTask.value, 'REJECT')
  rejectDialogVisible.value = false
}

function viewTask(task) {}

async function downloadFile(f) {
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

onMounted(fetchTasks)
</script>

<style scoped lang="scss">
.filter-bar { display: flex; align-items: center; gap: 12px; }
.expand-content { padding: 12px 16px 16px 48px; }
.expand-label { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; margin-top: 4px; }
.skill-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.attachment-list { display: flex; flex-direction: column; gap: 6px; }
.attachment-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-secondary); }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>
