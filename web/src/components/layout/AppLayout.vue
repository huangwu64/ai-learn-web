<template>
  <div class="app-layout">
    <Sidebar />
    <div class="main-area">
      <div class="top-nav">
        <el-menu
          mode="horizontal"
          :default-active="activeNav"
          @select="handleNavSelect"
          class="nav-menu"
        >
          <el-menu-item index="/">对话</el-menu-item>
          <!-- [V3 移除] 训练模块已合并入管理员初始提示词，用户端不再展示 -->
          <!-- <el-menu-item index="/training">训练</el-menu-item> -->
          <!-- [V3 启用] 知识库模块相关代码，V2 版本暂时注释 -->
          <!-- <el-menu-item index="/knowledge">知识库</el-menu-item> -->
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-menu>
        <div class="nav-right">
          <span class="user-tag" @click="$router.push('/profile')">
            {{ userStore.user?.nickname || userStore.user?.username || '未登录' }}
          </span>
        </div>
      </div>
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Sidebar from './Sidebar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeNav = computed(() => {
  const path = route.path
  if (path.startsWith('/training')) return '/training'
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/profile')) return '/profile'
  return '/'
})

function handleNavSelect(index: string) {
  router.push(index)
}
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  padding-right: 20px;
  background: #fff;

  .nav-menu {
    border-bottom: none;
  }

  .nav-right {
    .user-tag {
      color: #409eff;
      font-size: 14px;
      cursor: pointer;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.main-content {
  flex: 1;
  overflow: hidden;
}
</style>
