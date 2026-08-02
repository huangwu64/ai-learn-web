import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAdminToken, clearAdminToken } from '@/utils/adminAuth'

/**
 * 管理员专用 Axios 实例（V3）
 * - 自动携带管理员 Token
 * - Token 过期时清除登录态（isAdminLoggedIn → false），管理界面自动回到登录视图
 */
const adminRequest = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

adminRequest.interceptors.request.use(
  (config) => {
    const token = getAdminToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

adminRequest.interceptors.response.use(
  (response) => {
    const data = response.data
    if (!data) return response

    if (data.code === 401) {
      clearAdminToken()
      ElMessage.error(data.message || '管理员登录已过期，请重新登录')
      return Promise.reject(new Error(data.message || '登录已过期'))
    }

    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAdminToken()
      ElMessage.error('管理员登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default adminRequest
