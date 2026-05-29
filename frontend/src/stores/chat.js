import { defineStore } from 'pinia'
import { ref } from 'vue'
import wsClient from '@/utils/websocket'

/**
 * 实时连接与站内通知（原私信会话已下线，仅保留 WebSocket 通知通道）
 */
export const useChatStore = defineStore('chat', () => {
  const isConnected = ref(false)
  const notifications = ref([])

  function connect() {
    wsClient.connect({
      onMessage: handleWsMessage,
      onOpen: () => { isConnected.value = true },
      onClose: () => { isConnected.value = false }
    })
  }

  function disconnect() {
    wsClient.disconnect()
    isConnected.value = false
  }

  function handleWsMessage(data) {
    switch (data.type) {
      case 'NOTIFICATION':
        if (data?.data) {
          const payload = data.data || {}
          if (payload.id == null) break
          notifications.value.unshift({
            id: payload.id,
            type: payload.notifyType ?? payload.type,
            content: payload.content ?? '',
            link: payload.link ?? '',
            createdAt: payload.createdAt,
            isRead: payload.isRead ?? false,
            read: payload.isRead ?? false
          })
        }
        break
      default:
        break
    }
  }

  function setNotifications(list) {
    notifications.value = Array.isArray(list) ? [...list] : []
  }

  function clearNotifications() {
    notifications.value = []
  }

  return {
    isConnected,
    notifications,
    connect,
    disconnect,
    handleWsMessage,
    setNotifications,
    clearNotifications
  }
})
