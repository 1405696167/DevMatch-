<template>
  <div class="app-layout">
    <aside class="sidebar sidebar-enterprise">
      <div class="sidebar-logo">
        <el-icon size="28" color="#0ea5e9"><Connection /></el-icon>
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
        <div class="user-card" @click="router.push('/enterprise/profile')">
          <el-avatar :src="userStore.userAvatar" :size="36">{{ userStore.userName?.charAt(0) }}</el-avatar>
          <div class="user-info">
            <div class="user-name">{{ userStore.userName }}</div>
            <div class="user-role">企业用户</div>
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
            <el-breadcrumb-item :to="{ path: '/enterprise/dashboard' }">首页</el-breadcrumb-item>
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
  { path: '/enterprise/dashboard', icon: 'HomeFilled', label: '工作台' },
  { path: '/enterprise/publish', icon: 'EditPen', label: '发布需求' },
  { path: '/enterprise/tasks', icon: 'List', label: '我的需求' },
  { path: '/enterprise/developers', icon: 'Search', label: '开发者检索' },
  { path: '/enterprise/projects', icon: 'FolderOpened', label: '项目管控' },
  { path: '/enterprise/wallet', icon: 'Wallet', label: '钱包' },
  { path: '/enterprise/profile', icon: 'OfficeBuilding', label: '企业信息' }
]

const pageTitleMap = {
  'EntDashboard': '工作台',
  'PublishTask': '发布需求',
  'MyTasks': '我的需求',
  'TaskBids': '投标列表',
  'DeveloperSearch': '开发者检索',
  'DeveloperProfile': '开发者档案',
  'EntProjects': '项目管控',
  'EntProjectWorkspace': '项目工作台',
  'EntWallet': '钱包',
  'EntProfile': '企业信息'
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
  color: #0ea5e9;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
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
    background: #f0f9ff;
    color: var(--text-primary);
  }

  &.active {
    background: #e0f2fe;
    color: #0ea5e9;
    .el-icon { color: #0ea5e9; }
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
