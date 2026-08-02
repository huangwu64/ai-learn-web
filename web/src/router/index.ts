import { createRouter, createWebHistory } from 'vue-router'
import { hasToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { noAuth: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { noAuth: true },
    },
    {
      path: '/',
      name: 'portal',
      component: () => import('@/views/PortalView.vue'),
      meta: { standalone: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    // [V3 移除] 训练模块已合并入管理员初始提示词，用户端不再展示
    // {
    //   path: '/training',
    //   name: 'training',
    //   component: () => import('@/views/TrainingView.vue'),
    //   meta: { requiresAuth: true },
    // },
    // [V3 启用] 知识库模块相关代码，V2 版本暂时注释
    // {
    //   path: '/knowledge',
    //   name: 'knowledge',
    //   component: () => import('@/views/KnowledgeView.vue'),
    //   meta: { requiresAuth: true },
    // },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isAuthenticated = hasToken()

  // 目标路由需要认证但用户未登录 → 重定向到登录页
  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录用户访问登录/注册页 → 重定向到用户端对话
  if (to.meta.noAuth && isAuthenticated) {
    next({ path: '/chat' })
    return
  }

  next()
})

/**
 * 动态注册管理员门户路由（V3）
 * @param path 管理员入口路径（可配置，默认 /admin）
 */
export function registerAdminPortal(path: string) {
  router.addRoute({
    path,
    name: 'adminPortal',
    component: () => import('@/views/admin/AdminPortalView.vue'),
  })
}

export default router
