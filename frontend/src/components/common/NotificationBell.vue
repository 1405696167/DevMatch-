<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" v-model:visible="visible">
    <template #reference>
      <el-badge :value="unreadCount" :max="99" :hidden="!unreadCount">
        <el-button circle>
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div class="notify-panel">
      <div class="notify-header">
        <span class="notify-title">通知</span>
        <el-button link size="small" @click="markAllRead">全部已读</el-button>
      </div>
      <el-scrollbar max-height="400px">
        <div v-if="notifications.length === 0" class="empty-notify">
          <el-empty description="暂无通知" :image-size="80" />
        </div>
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notify-item"
          :class="{ unread: !isRead(n) }"
          @click="handleClick(n)"
        >
          <div class="notify-dot" v-if="!isRead(n)" />
          <el-icon class="notify-icon" :color="getTypeColor(n.type)">
            <component :is="getTypeIcon(n.type)" />
          </el-icon>
          <div class="notify-content">
            <div class="notify-text">{{ n.content }}</div>
            <div class="notify-time">{{ fromNow(n.createdAt) }}</div>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { messagesApi } from '@/api/messages'
import { fromNow } from '@/utils/format'

const chatStore = useChatStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const visible = ref(false)

const notifications = computed(() => chatStore.notifications)
const unreadCount = computed(() => notifications.value.filter(n => !isRead(n)).length)

function isRead(n) {
  return n.isRead === true || n.read === true
}

async function loadNotifications() {
  try {
    const res = await messagesApi.getNotifications({ limit: 20 })
    // R<List<Notification>> → data 为数组，不是 data.list
    const list = Array.isArray(res?.data) ? res.data : (res?.data?.list || [])
    chatStore.setNotifications(list)
  } catch {
    chatStore.setNotifications([])
  }
}

onMounted(loadNotifications)

watch(
  () => route.fullPath,
  () => {
    if (visible.value) visible.value = false
  }
)

function getTypeIcon(type) {
  const map = {
    PROJECT: 'FolderOpened',
    PAYMENT: 'Wallet',
    AUDIT: 'DocumentChecked',
    SYSTEM: 'InfoFilled',
    CHAT: 'ChatDotRound',
    TASK: 'Timer'
  }
  return map[type] || 'Bell'
}

function getTypeColor(type) {
  const map = {
    PROJECT: '#4f46e5',
    PAYMENT: '#10b981',
    AUDIT: '#f59e0b',
    SYSTEM: '#6b7280',
    CHAT: '#0ea5e9',
    TASK: '#ea580c'
  }
  return map[type] || '#6b7280'
}

async function markAllRead() {
  try {
    await messagesApi.markAllNotificationsRead()
    chatStore.notifications.forEach(n => {
      n.isRead = true
      n.read = true
    })
  } catch {}
}

function handleClick(n) {
  if (!isRead(n)) {
    messagesApi.markNotificationRead(n.id).catch(() => {})
    n.isRead = true
    n.read = true
  }
  const target = normalizeNotificationLink(n.link)
  if (target) router.push(target)
  visible.value = false
}

function normalizeNotificationLink(link) {
  if (!link || typeof link !== 'string') return ''

  // 已经是前端路由的完整路径（含角色前缀/管理员）则直接使用
  if (link.startsWith('/developer') || link.startsWith('/enterprise') || link.startsWith('/admin')) return link

  const base = userStore.userRole === 'DEVELOPER' ? '/developer' : '/enterprise'

  // 统一把后端通知中的“裸路径”补上角色前缀
  if (link === '/profile') return `${base}/profile`
  if (link === '/wallet') return `${base}/wallet`
  if (link.startsWith('/projects')) return `${base}${link}`
  if (link.startsWith('/tasks')) {
    // 后端可能发送 `/tasks/manage` 给企业用户，前端没有对应管理员页面则跳回“我的需求”
    if (link.startsWith('/tasks/manage')) {
      return userStore.userRole === 'ADMIN' ? '/admin/tasks' : `${base}/tasks`
    }
    return `${base}${link}`
  }

  return link
}
</script>

<style scoped lang="scss">
.notify-panel {
  padding: 0;
}
.notify-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  .notify-title {
    font-weight: 600;
    font-size: 14px;
  }
}
.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  position: relative;
  transition: background 0.15s;
  &:hover { background: #f8fafc; }
  &.unread { background: #fafaf9; }
}
.notify-dot {
  position: absolute;
  top: 14px;
  left: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--danger-color);
}
.notify-icon {
  margin-top: 2px;
  flex-shrink: 0;
}
.notify-content {
  flex: 1;
  min-width: 0;
}
.notify-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-primary);
}
.notify-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 3px;
}
.empty-notify {
  padding: 20px;
}
</style>
