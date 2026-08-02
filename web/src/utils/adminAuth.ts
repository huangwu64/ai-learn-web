/**
 * 管理员 Token 管理（V3）
 * 与用户 Token 隔离存储，互不影响
 */
import { ref } from 'vue'

const ADMIN_TOKEN_KEY = 'admin_token'

/** 响应式：管理员是否已登录（供管理界面切换登录/配置视图） */
export const isAdminLoggedIn = ref(false)

export function getAdminToken(): string | null {
  return localStorage.getItem(ADMIN_TOKEN_KEY)
}

export function setAdminToken(token: string) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
  isAdminLoggedIn.value = true
}

export function clearAdminToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  isAdminLoggedIn.value = false
}

export function hasAdminToken(): boolean {
  return !!getAdminToken()
}

/** 初始化响应式登录状态 */
export function initAdminAuthState() {
  isAdminLoggedIn.value = hasAdminToken()
}
