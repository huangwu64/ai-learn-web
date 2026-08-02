import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router, { registerAdminPortal } from './router'
import { setAdminEntryPath } from './utils/adminEntry'
import { initAdminAuthState } from './utils/adminAuth'
import './styles/global.scss'

async function bootstrap() {
  const app = createApp(App)

  // 注册所有 Element Plus 图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })

  initAdminAuthState()

  // V3：管理员入口路径可配置，从后端公开接口获取并动态注册路由
  let adminPath = '/admin'
  try {
    const res = await fetch('/api/v1/public/admin-entry')
    const body = await res.json()
    if (body?.code === 200 && body.data?.path) {
      adminPath = body.data.path
    }
  } catch {
    // 获取失败则使用默认路径 /admin
  }
  setAdminEntryPath(adminPath)
  registerAdminPortal(adminPath)

  app.mount('#app')
}

bootstrap()
