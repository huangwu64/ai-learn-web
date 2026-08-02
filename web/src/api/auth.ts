import request from './request'
import type { Result } from '@/types/api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  TokenRefreshResponse,
} from '@/types/user'
import type { ProfileChangeRequest, ProfileChangeRequestSubmit } from '@/types/admin'

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

/** 上传头像（V3.2：上传后进入管理员审核） */
export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<Result<{ requestId: number; url: string; status: string }>>(
    '/upload/avatar',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

/** 提交资料变更审核（V3.2：昵称/用户名等） */
export function submitProfileChange(data: ProfileChangeRequestSubmit) {
  return request.post<Result<ProfileChangeRequest>>('/users/me/profile-request', data)
}

/** 查看我的资料变更审核记录（V3.2） */
export function listMyProfileChanges() {
  return request.get<Result<ProfileChangeRequest[]>>('/users/me/profile-requests')
}
