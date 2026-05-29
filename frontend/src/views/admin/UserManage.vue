<template>
  <div class="page-container">
    <h2 class="page-title">用户管理</h2>

    <div class="filter-bar card" style="padding:16px 20px;margin-bottom:16px">
      <el-input v-model="keyword" placeholder="搜索用户名、手机号..." size="small" clearable style="width:200px" />
      <el-select v-model="roleFilter" size="small" style="width:120px" clearable placeholder="角色筛选">
        <el-option label="开发者" value="DEVELOPER" />
        <el-option label="企业用户" value="ENTERPRISE" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-select v-model="statusFilter" size="small" style="width:120px" clearable placeholder="状态筛选">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已禁用" value="DISABLED" />
      </el-select>
      <el-button type="primary" size="small" @click="fetchUsers">搜索</el-button>
    </div>

    <div class="card">
      <el-empty v-if="!loading && users.length === 0" description="暂无用户数据" :image-size="60" />
      <el-table v-else :data="users" v-loading="loading" stripe>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
              <div>
                <div class="user-nickname">{{ row.nickname }}</div>
                <div class="user-account">{{ row.phone || row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'DEVELOPER' ? 'primary' : row.role === 'ENTERPRISE' ? 'success' : 'danger'" size="small">
              {{ { DEVELOPER: '开发者', ENTERPRISE: '企业用户', ADMIN: '管理员' }[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.kycStatus === 'VERIFIED' ? 'success' : 'info'" size="small">
              {{ row.kycStatus === 'VERIFIED' ? '已认证' : '未认证' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信用分" width="80" align="center">
          <template #default="{ row }">{{ row.creditScore }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" link @click="viewDetail(row)">详情</el-button>
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
        @change="fetchUsers"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usersApi } from '@/api/users'
import { formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const roleFilter = ref('')
const statusFilter = ref('')

async function fetchUsers() {
  loading.value = true
  try {
    const res = await usersApi.getAdminList({ keyword: keyword.value, role: roleFilter.value, status: statusFilter.value, page: page.value, size: 10 })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    users.value = []
    total.value = 0
  } finally { loading.value = false }
}

async function toggleStatus(user) {
  const action = user.status === 'ACTIVE' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}用户 ${user.nickname} 吗？`, '操作确认', { type: 'warning' })
  const newStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await usersApi.toggleUserStatus(user.id, newStatus)
  user.status = newStatus
  ElMessage.success(`已${action}`)
}

function viewDetail(user) {}

onMounted(fetchUsers)
</script>

<style scoped lang="scss">
.filter-bar { display: flex; align-items: center; gap: 12px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.user-nickname { font-size: 13px; font-weight: 500; }
.user-account { font-size: 11px; color: var(--text-muted); }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>
