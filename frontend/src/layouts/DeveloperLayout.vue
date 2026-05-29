<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <el-icon size="28" color="#4f46e5"><Connection /></el-icon>
        <span>DevMatch</span>
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
          <el-badge v-if="item.badge" :value="item.badge" :max="99" class="nav-badge" />
        </router-link>
      </nav>
      <div class="sidebar-bottom">
        <div class="user-card" @click="router.push('/developer/profile')">
          <el-avatar :src="userStore.userAvatar" :size="36">{{ userStore.userName?.charAt(0) }}</el-avatar>
          <div class="user-info">
            <div class="user-name">{{ userStore.userName }}</div>
            <div class="user-role">开发者</div>
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
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/developer/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-bar-right">
          <NotificationBell />
        </div>
      </header>
      <main class="content">
        <!-- route.name 相同但 params/query 变化时组件不重建，导致需刷新才能生效 -->
        <router-view :key="route.fullPath" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import NotificationBell from '@/components/common/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = [
  { path: '/developer/dashboard', icon: 'HomeFilled', label: '工作台' },
  { path: '/developer/tasks', icon: 'List', label: '任务市场' },
  { path: '/developer/orders', icon: 'DocumentChecked', label: '我的投标' },
  { path: '/developer/projects', icon: 'FolderOpened', label: '我的项目' },
  { path: '/developer/wallet', icon: 'Wallet', label: '钱包' },
  { path: '/developer/credit', icon: 'Medal', label: '信用' },
  { path: '/developer/profile', icon: 'UserFilled', label: '个人中心' }
]

const pageTitleMap = {
  'DevDashboard': '工作台',
  'TaskMarket': '任务市场',
  'TaskDetail': '任务详情',
  'MyOrders': '我的投标',
  'DevProjects': '我的项目',
  'DevProjectWorkspace': '项目工作台',
  'DevWallet': '钱包',
  'DevCredit': '信用',
  'DevProfile': '个人中心'
}

const currentPageTitle = computed(() => pageTitleMap[route.name] || '')

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
  background: white;
  border-right: 1px solid var(--border-color);
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
  padding: 20px 20px 16px;
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
}

.sidebar-nav {
  flex: 1;
  padding: 12px 12px;
  overflow-y: auto;
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
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  text-decoration: none;
  position: relative;

  &:hover {
    background: #f1f5f9;
    color: var(--text-primary);
  }

  &.active {
    background: #ede9fe;
    color: var(--primary-color);
    .el-icon { color: var(--primary-color); }
  }

  .nav-badge {
    margin-left: auto;
  }
}

.sidebar-bottom {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
  &:hover { opacity: 0.8; }
}

.user-info {
  min-width: 0;
  .user-name {
    font-size: 13px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .user-role {
    font-size: 11px;
    color: var(--text-muted);
  }
}

.logout-btn {
  color: var(--text-muted) !important;
  &:hover { color: var(--danger-color) !important; }
}

.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
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

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
  overflow: auto;
}
</style>
