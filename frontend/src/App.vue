<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(() => {
  userStore.initUser()
  if (userStore.isLoggedIn) {
    chatStore.connect()
  }
})
</script>
