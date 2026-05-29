import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { useChatStore } from './chat'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role || '')
  const userId = computed(() => userInfo.value?.id || null)
  const userName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  const userAvatar = computed(() => userInfo.value?.avatar || '')
  const kycStatus = computed(() => userInfo.value?.kycStatus || 'NONE')

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function initUser() {
    const savedToken = localStorage.getItem('token')
    const savedInfo = localStorage.getItem('userInfo')
    if (savedToken) token.value = savedToken
    if (savedInfo) {
      try { userInfo.value = JSON.parse(savedInfo) } catch {}
    }
  }

  async function login(credentials) {
    const res = await authApi.login(credentials)
    const data = res.data
    setToken(data.accessToken || data.token)
    setUserInfo(data.userInfo || data.user)
    const chatStore = useChatStore()
    chatStore.connect()
    return data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      const chatStore = useChatStore()
      chatStore.disconnect()
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  async function refreshUserInfo() {
    const res = await authApi.getUserInfo()
    setUserInfo(res.data)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    userId,
    userName,
    userAvatar,
    kycStatus,
    setToken,
    setUserInfo,
    initUser,
    login,
    logout,
    refreshUserInfo
  }
})
