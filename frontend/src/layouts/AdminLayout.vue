<template>
  <div class="app-layout">
    <aside class="sidebar sidebar-admin">
      <div class="sidebar-logo">
        <el-icon size="28" color="#dc2626"><Setting /></el-icon>
        <span>后台管理</span>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <el-badge v-if="item.badge" :value="item.badge" :max="99" type="danger" class="nav-badge" />
        </router-link>
      </nav>
      <div class="sidebar-bottom">
        <div class="user-card">
          <el-avatar :src="userStore.userAvatar" :size="36" color="#dc2626">
            {{ userStore.userName?.charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name">{{ userStore.userName }}</div>
            <div class="user-role">系统管理员</div>
          </div>
        </div>
        <el-button link @click="handleLogout" class="logout-btn">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>
    <div class="main-area">
      <header class="top-bar">
        <div class="top-bar-left">
          <h2 class="page-heading">{{ currentPageTitle }}</h2>
        </div>
        <div class="top-bar-right">
          <el-text type="info" size="small">{{ currentTime }}</el-text>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { path: '/admin/dashboard', icon: 'DataAnalysis', label: '仪表盘' },
  { path: '/admin/tasks', icon: 'DocumentChecked', label: '任务审核' },
  { path: '/admin/kyc', icon: 'Postcard', label: '认证审核' },
  { path: '/admin/users', icon: 'UserFilled', label: '用户管理' },
  { path: '/admin/complaints', icon: 'Warning', label: '申诉处理' },
  { path: '/admin/config', icon: 'Setting', label: '系统配置' }
]

const pageTitleMap = {
  'AdminDashboard': '仪表盘',
  'AdminTasks': '任务审核',
  'AdminKyc': '认证审核',
  'AdminUsers': '用户管理',
  'AdminComplaints': '申诉处理',
  'AdminConfig': '系统配置'
}

const currentPageTitle = computed(() => pageTitleMap[route.name] || '')

const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let timeTimer = null
onMounted(() => { timeTimer = setInterval(() => { currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss') }, 1000) })
onUnmounted(() => clearInterval(timeTimer))

function isActive(path) {
  return route.path.startsWith(path)
}

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await userStore.logout()
  router.push('/auth/login')
}
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: var(--sidebar-width);
  background: #1e1e2e;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 100;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px;
  font-size: 18px;
  font-weight: 700;
  color: white;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: rgba(255,255,255,0.6);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  text-decoration: none;

  &:hover {
    background: rgba(255,255,255,0.08);
    color: rgba(255,255,255,0.9);
  }

  &.active {
    background: rgba(220,38,38,0.2);
    color: #fca5a5;
  }

  .nav-badge {
    margin-left: auto;
  }
}

.sidebar-bottom {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.user-info {
  min-width: 0;
  .user-name {
    font-size: 13px;
    font-weight: 600;
    color: white;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .user-role {
    font-size: 11px;
    color: rgba(255,255,255,0.4);
  }
}

.logout-btn {
  color: rgba(255,255,255,0.4) !important;
  &:hover { color: #fca5a5 !important; }
}

.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
}

.top-bar {
  height: var(--header-height);
  background: white;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-heading {
  font-size: 18px;
  font-weight: 600;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content {
  flex: 1;
  background: var(--bg-color);
}
</style>
