<template>
  <div class="page-container">
    <h2 class="page-title">认证审核</h2>

    <el-tabs v-model="activeTab" @tab-change="fetchKycList">
      <el-tab-pane label="待审核" name="AUDITING" />
      <el-tab-pane label="已通过" name="VERIFIED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
    </el-tabs>

    <div class="card">
      <el-empty v-if="!loading && kycList.length === 0" description="暂无认证记录" :image-size="60" />
      <el-table v-else :data="kycList" v-loading="loading" stripe>
        <el-table-column label="申请用户" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <el-avatar :size="30" :src="row.user?.avatar">{{ row.user?.nickname?.charAt(0) }}</el-avatar>
              <div>
                <div style="font-size:13px;font-weight:500">{{ row.user?.nickname }}</div>
                <div style="font-size:11px;color:#94a3b8">{{ row.user?.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="认证类型" width="110">
          <template #default="{ row }">
            <el-tag :type="isPersonal(row) ? 'primary' : 'success'" size="small">
              {{ isPersonal(row) ? '开发者实名' : '企业资质' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交信息" width="180">
          <template #default="{ row }">
            <div style="font-size:12px">
              <div>{{ row.realName }}</div>
              <div style="color:#94a3b8">{{ row.idNumber || row.creditCode }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="证件" width="120">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="viewImages(row)">查看证件</el-button>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'AUDITING'">
              <el-button size="small" type="success" @click="auditKyc(row, 'PASS')">通过</el-button>
              <el-button size="small" type="danger" @click="auditKyc(row, 'REJECT')">拒绝</el-button>
            </template>
            <el-tag v-else :type="row.status === 'VERIFIED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'VERIFIED' ? '已通过' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 证件查看弹窗 -->
    <el-dialog v-model="imagesVisible" :title="isPersonal(currentKyc) ? '开发者实名资料' : '企业认证资料'" width="680px">
      <el-descriptions :column="2" border size="small" style="margin-bottom:20px">
        <el-descriptions-item label="认证类型">
          <el-tag :type="isPersonal(currentKyc) ? 'primary' : 'success'" size="small">
            {{ isPersonal(currentKyc) ? '开发者实名' : '企业资质' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请用户">{{ currentKyc?.user?.nickname }}</el-descriptions-item>
        <template v-if="!isPersonal(currentKyc)">
          <el-descriptions-item label="企业名称">{{ currentKyc?.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码">{{ currentKyc?.creditCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="法人姓名">{{ currentKyc?.legalPersonName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="法人身份证号">{{ currentKyc?.legalPersonId || '—' }}</el-descriptions-item>
        </template>
        <template v-else>
          <el-descriptions-item label="真实姓名">{{ currentKyc?.realName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentKyc?.idNumber || '—' }}</el-descriptions-item>
        </template>
      </el-descriptions>
      <div class="kyc-images">
        <template v-if="currentImages.some(i => i.url)">
          <div v-for="img in currentImages.filter(i => i.url)" :key="img.type" class="kyc-image-item">
            <div class="img-label">{{ img.label }}</div>
            <el-image
              :src="img.url"
              fit="contain"
              class="kyc-img"
              :preview-src-list="currentImages.filter(i => i.url).map(i => i.url)"
              preview-teleported
            />
          </div>
        </template>
        <el-empty v-else description="暂无证件图片" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const kycList = ref([])
const activeTab = ref('AUDITING')
const imagesVisible = ref(false)
const currentImages = ref([])
const currentKyc = ref(null)

async function fetchKycList() {
  loading.value = true
  try {
    const res = await adminApi.getKycList({ status: activeTab.value, page: 1, size: 20 })
    kycList.value = res.data?.list || []
  } catch {
    kycList.value = []
  } finally { loading.value = false }
}

async function auditKyc(kyc, action) {
  const backendAction = action === 'PASS' ? 'APPROVE' : 'REJECT'
  await adminApi.auditKyc(kyc.id, { action: backendAction })
  ElMessage.success(action === 'PASS' ? '认证通过' : '已拒绝')
  fetchKycList()
}

function isPersonal(row) {
  return row?.type === 'PERSONAL' || row?.type === 'DEVELOPER'
}

function viewImages(kyc) {
  currentKyc.value = kyc
  const images = []
  if (!isPersonal(kyc)) {
    if (kyc.licenseUrl) images.push({ type: 'license', label: '营业执照', url: kyc.licenseUrl })
    if (kyc.idFrontUrl) images.push({ type: 'front', label: '法人身份证正面', url: kyc.idFrontUrl })
    if (kyc.idBackUrl) images.push({ type: 'back', label: '法人身份证背面', url: kyc.idBackUrl })
  } else {
    if (kyc.idFrontUrl) images.push({ type: 'front', label: '身份证正面', url: kyc.idFrontUrl })
    if (kyc.idBackUrl) images.push({ type: 'back', label: '身份证背面', url: kyc.idBackUrl })
  }
  currentImages.value = images
  imagesVisible.value = true
}

onMounted(fetchKycList)
</script>

<style scoped lang="scss">
.kyc-images { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.kyc-image-item { .img-label { font-size: 12px; color: var(--text-muted); margin-bottom: 6px; } }
.kyc-img { width: 100%; height: 160px; border: 1px solid var(--border-color); border-radius: 8px; }
</style>
