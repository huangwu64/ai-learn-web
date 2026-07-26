<template>
  <div class="chat-view">
    <WelcomeScreen v-if="!chatStore.activeSessionId" />
    <ChatPanel v-else :key="chatStore.activeSessionId" />
  </div>
</template>

<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import WelcomeScreen from '@/components/chat/WelcomeScreen.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const chatStore = useChatStore()
</script>

<style scoped lang="scss">
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
