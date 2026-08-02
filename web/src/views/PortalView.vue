<template>
  <div class="portal-view">
    <div class="portal-container">
      <div class="portal-logo">💬</div>
      <h1 class="portal-title">提示词工程实战训练系统</h1>
      <p class="portal-subtitle">通过真实对话掌握提示词工程 · 综合入口</p>

      <!-- 服务状态 -->
      <div class="service-status">
        <span class="status-dot" :class="{ online: backendOnline }"></span>
        后端服务：{{ backendOnline ? '在线' : '检测中 / 离线' }}
      </div>

      <div class="portal-cards">
        <div class="portal-card" @click="goChat">
          <div class="card-icon">💬</div>
          <div class="card-title">用户端 · 对话</div>
          <div class="card-desc">与 AI 对话、学习提示词、管理个人中心</div>
          <el-button type="primary" round>进入对话</el-button>
        </div>

        <div class="portal-card admin-card" @click="goAdmin">
          <div class="card-icon">⚙️</div>
          <div class="card-title">管理后台</div>
          <div class="card-desc">AI 模型配置 · 用户管理 · 资料审核</div>
          <el-button type="warning" round>进入管理</el-button>
        </div>
      </div>

      <div class="portal-footer">
        <el-link type="info" @click="goChat">用户端（/chat）</el-link>
        <span class="sep">·</span>
        <el-link type="info" @click="goAdmin">管理后台（{{ adminPath }}）</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminEntryPath } from '@/utils/adminEntry'

const router = useRouter()
const adminPath = getAdminEntryPath()
const backendOnline = ref(false)

function goChat() {
  router.push('/chat')
}

function goAdmin() {
  router.push(adminPath)
}

onMounted(async () => {
  // 检测后端服务是否在线
  try {
    const res = await fetch('/api/v1/public/admin-entry', { signal: AbortSignal.timeout(5000) })
    backendOnline.value = res.ok
  } catch {
    backendOnline.value = false
  }
})
</script>

<style scoped lang="scss">
.portal-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.portal-container {
  text-align: center;
  padding: 40px 24px;
}

.portal-logo {
  font-size: 56px;
}

.portal-title {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  margin: 12px 0 6px;
}

.portal-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 24px;
}

.service-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.15);
  padding: 6px 14px;
  border-radius: 20px;
  margin-bottom: 36px;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f56c6c;
    transition: background 0.3s;

    &.online {
      background: #67c23a;
    }
  }
}

.portal-cards {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;
}

.portal-card {
  width: 260px;
  padding: 32px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 14px 40px rgba(0, 0, 0, 0.22);
  }

  .card-icon {
    font-size: 40px;
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 12px 0 6px;
  }

  .card-desc {
    font-size: 13px;
    color: #909399;
    margin-bottom: 18px;
    min-height: 36px;
  }
}

.portal-footer {
  margin-top: 32px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);

  .sep {
    margin: 0 8px;
  }
}
</style>
