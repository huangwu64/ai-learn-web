<template>
  <div class="placeholder-view">
    <EmptyState description="知识库模块即将上线，敬请期待" icon="Collection" />
  </div>
</template>

<script setup lang="ts">
import EmptyState from '@/components/common/EmptyState.vue'
</script>

<style scoped lang="scss">
.placeholder-view {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
