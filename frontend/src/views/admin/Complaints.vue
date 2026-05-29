<template>
  <div class="page-container">
    <h2 class="page-title">申诉处理</h2>
    <el-tabs v-model="statusFilter" @tab-change="fetchComplaints">
      <el-tab-pane label="待处理" name="PENDING" />
      <el-tab-pane label="处理中" name="PROCESSING" />
      <el-tab-pane label="已解决" name="RESOLVED" />
    </el-tabs>
    <div class="card">
      <el-empty v-if="!loading && complaints.length === 0" description="暂无申诉记录" :image-size="60" />
      <el-table v-else :data="complaints" v-loading="loading" stripe>
        <el-table-column prop="title" label="申诉标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="申诉方" width="130">
          <template #default="{ row }">{{ row.complainant?.name }}</template>
        </el-table-column>
        <el-table-column label="被申诉方" width="130">
          <template #default="{ row }">{{ row.respondent?.name }}</template>
        </el-table-column>
        <el-table-column label="涉及项目" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.projectName }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleComplaint(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="申诉详情" width="600px">
      <div v-if="currentComplaint">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申诉标题">{{ currentComplaint.title }}</el-descriptions-item>
          <el-descriptions-item label="涉及项目">{{ currentComplaint.projectName }}</el-descriptions-item>
          <el-descriptions-item label="申诉方">{{ currentComplaint.complainant?.name }}</el-descriptions-item>
          <el-descriptions-item label="被申诉方">{{ currentComplaint.respondent?.name }}</el-descriptions-item>
        </el-descriptions>
        <div class="complaint-content">
          <div class="content-label">申诉内容</div>
          <p>{{ currentComplaint.content }}</p>
        </div>
        <el-form :model="handleForm" label-position="top" style="margin-top:16px">
          <el-form-item label="处理结果">
            <el-radio-group v-model="handleForm.result">
              <el-radio label="COMPLAINANT_WIN">支持申诉方</el-radio>
              <el-radio label="RESPONDENT_WIN">支持被申诉方</el-radio>
              <el-radio label="COMPROMISE">调解处理</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input v-model="handleForm.remark" type="textarea" :rows="3" placeholder="请说明处理依据和结果..." />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const complaints = ref([])
const statusFilter = ref('PENDING')
const dialogVisible = ref(false)
const currentComplaint = ref(null)
const handleForm = reactive({ result: 'COMPROMISE', remark: '' })

async function fetchComplaints() {
  loading.value = true
  try {
    const res = await adminApi.getComplaints({ status: statusFilter.value })
    complaints.value = res.data?.list || []
  } catch {
    complaints.value = []
  } finally { loading.value = false }
}

function handleComplaint(c) {
  currentComplaint.value = c
  handleForm.result = 'COMPROMISE'
  handleForm.remark = ''
  dialogVisible.value = true
}

async function submitHandle() {
  if (!handleForm.remark) { ElMessage.warning('请填写处理说明'); return }
  await adminApi.handleComplaint(currentComplaint.value.id, handleForm)
  ElMessage.success('处理成功')
  dialogVisible.value = false
  fetchComplaints()
}

onMounted(fetchComplaints)
</script>

<style scoped lang="scss">
.complaint-content { margin-top: 16px; .content-label { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; } p { font-size: 13px; color: var(--text-secondary); } }
</style>
