import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/types/user'
import * as authApi from '@/api/auth'
import * as authUtils from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo | null>(authUtils.getUserInfo())
  const isLoggedIn = ref(authUtils.hasToken())

  /** 登录 */
  async function loginAction(username: string, password: string) {
    const res = await authApi.login({ username, password })
    const data = res.data.data
    authUtils.setTokens(data.accessToken, data.refreshToken)
    authUtils.saveUserInfo(data.user)
    user.value = data.user
    isLoggedIn.value = true
    return data
  }

  /** 注册 */
  async function registerAction(username: string, password: string, nickname?: string) {
    await authApi.register({ username, password, nickname })
  }

  /** 退出登录 */
  async function logoutAction() {
    try {
      await authApi.logout()
    } catch {
      // 即使接口失败也清除本地状态
    }
    authUtils.clearTokens()
    user.value = null
    isLoggedIn.value = false
  }

  /** 从 localStorage 恢复用户状态 */
  function restoreUser() {
    if (authUtils.hasToken()) {
      const cached = authUtils.getUserInfo()
      if (cached) {
        user.value = cached
        isLoggedIn.value = true
      }
    }
  }

  /** 更新用户信息 */
  function updateUserInfo(info: Partial<UserInfo>) {
    if (user.value) {
      user.value = { ...user.value, ...info }
      authUtils.saveUserInfo(user.value)
    }
  }

  /** 清除状态（Token 过期等场景） */
  function clearState() {
    authUtils.clearTokens()
    user.value = null
    isLoggedIn.value = false
  }

  return {
    user,
    isLoggedIn,
    loginAction,
    registerAction,
    logoutAction,
    restoreUser,
    updateUserInfo,
    clearState,
  }
})
