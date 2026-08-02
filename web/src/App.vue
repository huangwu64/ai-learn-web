<template>
  <div v-if="isStandalonePage" class="app-auth">
    <router-view />
  </div>
  <AppLayout v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import { getAdminEntryPath } from '@/utils/adminEntry'

const route = useRoute()

/** 独立页面（登录/注册/管理员门户）不套用户布局 */
const isStandalonePage = computed(() => {
  const path = route.path
  return (
    path === '/login' ||
    path === '/register' ||
    path === getAdminEntryPath() ||
    path.startsWith(getAdminEntryPath() + '/')
  )
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
