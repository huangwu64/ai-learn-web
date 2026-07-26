<template>
  <div class="welcome-screen">
    <div class="welcome-icon">
      <el-icon :size="64" color="#409eff"><ChatLineRound /></el-icon>
    </div>
    <h2>欢迎使用提示词训练系统</h2>
    <p class="welcome-desc">点击左侧「新建对话」开始与 AI 对话练习</p>
  </div>
</template>

<script setup lang="ts">
import { ChatLineRound } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #606266;

  h2 {
    margin: 16px 0 8px;
    font-size: 20px;
    font-weight: 600;
  }

  .welcome-desc {
    font-size: 14px;
    color: #909399;
  }
}
</style>
