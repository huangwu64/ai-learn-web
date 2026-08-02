/**
 * Token 管理工具
 */

import axios from 'axios'

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const USER_INFO_KEY = 'user_info'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export function saveUserInfo(user: any) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))
}

export function getUserInfo(): any | null {
  const data = localStorage.getItem(USER_INFO_KEY)
  if (!data) return null
  try {
    return JSON.parse(data)
  } catch {
    return null
  }
}

export function hasToken(): boolean {
  return !!getAccessToken()
}

/**
 * 认证失效：清除本地登录状态并跳转登录页。
 * 使用整页跳转以清空内存中的 Pinia 状态和进行中的 SSE 流。
 */
export function redirectToLogin() {
  clearTokens()
  const path = window.location.pathname
  if (path !== '/login' && path !== '/register') {
    window.location.href = '/login'
  }
}

// 全局共享的刷新锁：并发多处 401 时只发起一次 /auth/refresh
let refreshingPromise: Promise<string | null> | null = null

/**
 * 刷新 Access Token（带并发锁）。
 * 成功返回新 Access Token 并已同步更新本地 Token 存储；失败返回 null。
 */
export function refreshAccessToken(): Promise<string | null> {
  if (!refreshingPromise) {
    refreshingPromise = doRefreshAccessToken().finally(() => {
      refreshingPromise = null
    })
  }
  return refreshingPromise
}

async function doRefreshAccessToken(): Promise<string | null> {
  const refreshTokenValue = getRefreshToken()
  if (!refreshTokenValue) return null
  try {
    // 使用原生 axios，避免再经过业务拦截器造成循环
    const res = await axios.post('/api/v1/auth/refresh', { refreshToken: refreshTokenValue })
    const data = res.data
    if (data && data.code === 200 && data.data?.accessToken) {
      setTokens(data.data.accessToken, data.data.refreshToken)
      return data.data.accessToken
    }
    return null
  } catch {
    return null
  }
}
