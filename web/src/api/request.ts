import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求锁：并发 401 时仅发起一次刷新
let isRefreshing = false
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (err: any) => void
}> = []

function addPendingRequest(resolve: (token: string) => void, reject: (err: any) => void) {
  pendingRequests.push({ resolve, reject })
}

function resolvePendingRequests(token: string) {
  pendingRequests.forEach(({ resolve }) => resolve(token))
  pendingRequests = []
}

function rejectPendingRequests(err: any) {
  pendingRequests.forEach(({ reject }) => reject(err))
  pendingRequests = []
}

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

// 响应拦截器：统一错误处理 + Token 自动刷新
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    // Token 过期 (401)，尝试自动刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshTokenValue = getRefreshToken()
      if (!refreshTokenValue) {
        // 没有 Refresh Token，直接跳转登录页
        clearTokens()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 已有刷新请求在进行中，将当前请求加入等待队列
        return new Promise((resolve, reject) => {
          addPendingRequest(
            (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              originalRequest._retry = true
              resolve(request(originalRequest))
            },
            reject
          )
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/v1/auth/refresh', { refreshToken: refreshTokenValue })
        const data = res.data.data
        if (data && data.accessToken) {
          setTokens(data.accessToken, data.refreshToken)
          // 通知所有等待的请求
          resolvePendingRequests(data.accessToken)
          // 重放原请求
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
          return request(originalRequest)
        }
      } catch (refreshError) {
        rejectPendingRequests(refreshError)
        clearTokens()
        ElMessage.error('登录已过期，请重新登录')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
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
