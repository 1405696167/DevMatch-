<template>
  <div class="page-container">
    <h2 class="page-title">系统配置</h2>

    <div class="config-layout">
      <div class="card config-section">
        <h3 class="section-title">平台费率配置</h3>
        <el-form :model="feeConfig" label-width="160px">
          <el-form-item label="平台服务费率（%）">
            <el-input-number v-model="feeConfig.commissionRate" :min="0" :max="30" :precision="1" :step="0.5" />
            <span class="form-tip">当前：{{ feeConfig.commissionRate }}%</span>
          </el-form-item>
          <el-form-item label="提现手续费率（%）">
            <el-input-number v-model="feeConfig.withdrawRate" :min="0" :max="10" :precision="1" :step="0.1" />
          </el-form-item>
          <el-form-item label="最低提现金额（元）">
            <el-input-number v-model="feeConfig.minWithdraw" :min="1" :step="10" />
          </el-form-item>
          <el-form-item label="最大单次提现（元）">
            <el-input-number v-model="feeConfig.maxWithdraw" :min="100" :step="1000" />
          </el-form-item>
          <el-form-item label="需求发布押金（占预算上限%）">
            <el-input-number v-model="feeConfig.taskPublishDepositRate" :min="0" :max="50" :precision="2" :step="0.5" />
            <span class="form-tip">例如 5 表示按预算上限的 5% 从企业钱包扣除；0 表示不收取</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingFee" @click="saveFeeConfig">保存费率配置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card config-section">
        <h3 class="section-title">审核配置</h3>
        <el-form :model="auditConfig" label-width="160px">
          <el-form-item label="任务自动审核">
            <el-switch v-model="auditConfig.autoAuditTask" />
            <span class="form-tip">开启后符合条件的任务自动通过</span>
          </el-form-item>
          <el-form-item label="KYC审核方式">
            <el-radio-group v-model="auditConfig.kycMode">
              <el-radio label="MANUAL">人工审核</el-radio>
              <el-radio label="AI">AI辅助审核</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核SLA（小时）">
            <el-input-number v-model="auditConfig.slaDuration" :min="1" :max="72" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveAuditConfig">保存审核配置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card config-section">
        <h3 class="section-title">敏感词管理</h3>
        <div class="sensitive-words">
          <el-tag v-for="w in sensitiveWords" :key="w" closable @close="removeSensitiveWord(w)" style="margin:4px">{{ w }}</el-tag>
        </div>
        <div class="add-word-row">
          <el-input v-model="newWord" placeholder="添加敏感词..." size="small" style="width:200px" @keyup.enter="addSensitiveWord" />
          <el-button size="small" type="primary" @click="addSensitiveWord">添加</el-button>
        </div>
      </div>

      <div class="card config-section">
        <div class="section-header">
          <h3 class="section-title">公告管理</h3>
          <el-button size="small" type="primary" @click="openAnnouncementDialog()">+ 新增公告</el-button>
        </div>
        <el-table :data="announcements" size="small">
          <el-table-column prop="title" label="公告标题" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-switch v-model="row.active" size="small" @change="toggleAnnouncement(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" link @click="openAnnouncementDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="deleteAnnouncement(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="announcementDialogVisible" :title="editingAnnouncement ? '编辑公告' : '新增公告'" width="500px">
      <el-form :model="announcementForm" label-position="top">
        <el-form-item label="公告标题">
          <el-input v-model="announcementForm.title" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="announcementForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="announcementForm.type" style="width:100%">
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="维护通知" value="MAINTENANCE" />
            <el-option label="活动公告" value="ACTIVITY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { ElMessage } from 'element-plus'

const savingFee = ref(false)
const feeConfig = reactive({ commissionRate: 5, withdrawRate: 0, minWithdraw: 100, maxWithdraw: 50000, taskPublishDepositRate: 5 })
const auditConfig = reactive({ autoAuditTask: false, kycMode: 'MANUAL', slaDuration: 48 })
const sensitiveWords = ref([])
const newWord = ref('')
const announcements = ref([])
const announcementDialogVisible = ref(false)
const editingAnnouncement = ref(null)
const announcementForm = reactive({ title: '', content: '', type: 'SYSTEM' })

async function saveFeeConfig() {
  savingFee.value = true
  try {
    await adminApi.updateConfig('commission_rate', (feeConfig.commissionRate / 100).toFixed(4))
    await adminApi.updateConfig('withdraw_rate', (feeConfig.withdrawRate / 100).toFixed(4))
    await adminApi.updateConfig('min_withdraw', feeConfig.minWithdraw)
    await adminApi.updateConfig('max_withdraw', feeConfig.maxWithdraw)
    await adminApi.updateConfig('task_publish_deposit_rate', (feeConfig.taskPublishDepositRate / 100).toFixed(4))
    ElMessage.success('费率配置已保存')
  } finally { savingFee.value = false }
}

async function saveAuditConfig() {
  await adminApi.updateConfig('auto_audit_task', auditConfig.autoAuditTask)
  await adminApi.updateConfig('kyc_mode', auditConfig.kycMode)
  await adminApi.updateConfig('sla_duration', auditConfig.slaDuration)
  ElMessage.success('审核配置已保存')
}

function addSensitiveWord() {
  if (newWord.value && !sensitiveWords.value.includes(newWord.value)) {
    sensitiveWords.value.push(newWord.value)
    newWord.value = ''
  }
}

function removeSensitiveWord(w) {
  sensitiveWords.value = sensitiveWords.value.filter(x => x !== w)
}

function openAnnouncementDialog(a) {
  editingAnnouncement.value = a || null
  announcementForm.title = a?.title || ''
  announcementForm.content = a?.content || ''
  announcementForm.type = a?.type || 'SYSTEM'
  announcementDialogVisible.value = true
}

async function saveAnnouncement() {
  if (editingAnnouncement.value) {
    await adminApi.updateAnnouncement(editingAnnouncement.value.id, announcementForm)
  } else {
    await adminApi.createAnnouncement(announcementForm)
  }
  ElMessage.success('保存成功')
  announcementDialogVisible.value = false
  await loadAnnouncements()
}

async function loadAnnouncements() {
  try {
    const res = await adminApi.getAnnouncements()
    announcements.value = res.data || []
  } catch {
    announcements.value = []
  }
}

async function deleteAnnouncement(a) {
  await adminApi.deleteAnnouncement(a.id)
  ElMessage.success('已删除')
  await loadAnnouncements()
}

function toggleAnnouncement(a) {}

onMounted(async () => {
  try {
    const [configsRes] = await Promise.allSettled([
      adminApi.getConfigs()
    ])
    if (configsRes.status === 'fulfilled') {
      const configs = configsRes.value.data || []
      configs.forEach(item => {
        const v = item.configValue
        if (item.configKey === 'commission_rate') feeConfig.commissionRate = Math.round(parseFloat(v) * 100 * 10) / 10 || 5
        if (item.configKey === 'withdraw_rate') feeConfig.withdrawRate = Math.round(parseFloat(v) * 100 * 10) / 10 || 0
        if (item.configKey === 'min_withdraw') feeConfig.minWithdraw = parseFloat(v) || 100
        if (item.configKey === 'max_withdraw') feeConfig.maxWithdraw = parseFloat(v) || 50000
        if (item.configKey === 'task_publish_deposit_rate') feeConfig.taskPublishDepositRate = Math.round(parseFloat(v) * 100 * 100) / 100 || 5
        if (item.configKey === 'auto_audit_task') auditConfig.autoAuditTask = v === 'true'
        if (item.configKey === 'kyc_mode') auditConfig.kycMode = v || 'MANUAL'
        if (item.configKey === 'sla_duration') auditConfig.slaDuration = parseInt(v) || 48
      })
    }
  } catch {}
  await loadAnnouncements()
})
</script>

<style scoped lang="scss">
.config-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.config-section {}
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; }
.form-tip { margin-left: 12px; font-size: 12px; color: var(--text-muted); }

.sensitive-words { display: flex; flex-wrap: wrap; min-height: 40px; margin-bottom: 12px; }
.add-word-row { display: flex; gap: 8px; align-items: center; }
</style>
