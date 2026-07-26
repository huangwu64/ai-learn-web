<template>
  <div class="placeholder-view">
    <EmptyState description="训练模块即将上线，敬请期待" icon="Trophy" />
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
