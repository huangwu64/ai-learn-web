<template>
  <div class="empty-state">
    <el-icon class="empty-icon"><component :is="icon || 'ChatDotRound'" /></el-icon>
    <p class="empty-desc">{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  description?: string
  icon?: string
}>()
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;

  .empty-icon {
    font-size: 48px;
    color: #c0c4cc;
  }

  .empty-desc {
    margin-top: 12px;
    color: #909399;
    font-size: 14px;
  }
}
</style>
