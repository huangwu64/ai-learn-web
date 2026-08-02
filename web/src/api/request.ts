import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAccessToken, refreshAccessToken, redirectToLogin } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：自动携带 Token
request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 统一处理认证过期（HTTP 401 或业务码 body code=401）：
 * 尝试刷新 Token 并重放原请求；刷新失败则清除登录态并跳转登录页。
 */
async function handleAuthExpired(originalRequest: any): Promise<any> {
  // 已用新 Token 重放过仍失败 → 直接退出，避免死循环
  if (originalRequest._retry) {
    forceLogout()
    return Promise.reject(new Error('登录已过期'))
  }

  originalRequest._retry = true
  const newToken = await refreshAccessToken()

  if (newToken) {
    originalRequest.headers = originalRequest.headers || {}
    originalRequest.headers.Authorization = `Bearer ${newToken}`
    return request(originalRequest)
  }

  forceLogout()
  return Promise.reject(new Error('登录已过期'))
}

/** 认证彻底失效：提示并跳转登录页 */
function forceLogout(message = '登录已过期，请重新登录') {
  ElMessage.error(message)
  redirectToLogin()
}

// 响应拦截器：统一错误处理 + Token 自动刷新
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (!data) return response

    // 后端以 HTTP 200 + body code=401 表达认证过期（见 SecurityConfig / GlobalExceptionHandler）
    if (data.code === 401) {
      const url = response.config?.url || ''
      // 登录接口的 401 是"用户名/密码错误"，仅提示、不触发退出流程
      if (url.includes('/auth/login')) {
        ElMessage.error(data.message || '登录失败')
        return Promise.reject(new Error(data.message || '登录失败'))
      }
      return handleAuthExpired(response.config)
    }

    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const originalRequest = error.config

    // 真实 HTTP 401（兼容后端改为标准状态码的情况）
    if (error.response?.status === 401 && originalRequest) {
      return handleAuthExpired(originalRequest)
    }

    // 其他 HTTP 错误
    if (error.response) {
      switch (error.response.status) {
        case 403:
          ElMessage.error('无权访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.message || '网络错误')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请重试')
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default request
