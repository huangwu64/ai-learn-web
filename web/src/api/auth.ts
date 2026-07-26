import request from './request'
import type { Result } from '@/types/api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  TokenRefreshResponse,
} from '@/types/user'

/** 用户登录 */
export function login(data: LoginRequest) {
  return request.post<Result<LoginResponse>>('/auth/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest) {
  return request.post<Result<null>>('/auth/register', data)
}

/** 刷新 Token */
export function refreshToken(refreshToken: string) {
  return request.post<Result<TokenRefreshResponse>>('/auth/refresh', { refreshToken })
}

/** 退出登录 */
export function logout() {
  return request.post<Result<null>>('/auth/logout')
}

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request.get<Result<any>>('/users/me')
}

/** 更新当前用户信息 */
export function updateUser(data: { nickname?: string; avatarUrl?: string }) {
  return request.patch<Result<any>>('/users/me', data)
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.patch<Result<null>>('/users/me/password', data)
}
