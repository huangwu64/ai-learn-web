import adminRequest from './adminRequest'
import type { Result } from '@/types/api'
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AiConfig,
  AiConfigUpdateRequest,
  AdminUser,
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  AdminResetPasswordRequest,
  ProfileChangeRequest,
} from '@/types/admin'

/** 管理员登录 */
export function adminLogin(data: AdminLoginRequest) {
  return adminRequest.post<Result<AdminLoginResponse>>('/admin/auth/login', data)
}

/** 管理员退出 */
export function adminLogout() {
  return adminRequest.post<Result<null>>('/admin/auth/logout')
}

/** 获取 AI 配置（Key 脱敏） */
export function getAiConfig() {
  return adminRequest.get<Result<AiConfig>>('/admin/ai-config')
}

/** 更新 AI 配置 */
export function updateAiConfig(data: AiConfigUpdateRequest) {
  return adminRequest.put<Result<null>>('/admin/ai-config', data)
}

/** 测试 AI 连接 */
export function testAiConfig(data: AiConfigUpdateRequest) {
  return adminRequest.post<Result<{ ok: boolean }>>('/admin/ai-config/test', data)
}

/** 获取可用模型列表 */
export function listAiModels() {
  return adminRequest.get<Result<{ models: string[] }>>('/admin/ai-config/models')
}

/** 用户列表 */
export function adminListUsers() {
  return adminRequest.get<Result<AdminUser[]>>('/admin/users')
}

/** 创建用户 */
export function adminCreateUser(data: AdminCreateUserRequest) {
  return adminRequest.post<Result<AdminUser>>('/admin/users', data)
}

/** 删除用户 */
export function adminDeleteUser(id: number) {
  return adminRequest.delete<Result<null>>(`/admin/users/${id}`)
}

/** 更新用户信息（管理员直接生效） */
export function adminUpdateUser(id: number, data: AdminUpdateUserRequest) {
  return adminRequest.put<Result<AdminUser>>(`/admin/users/${id}`, data)
}

/** 重置用户密码 */
export function adminResetPassword(id: number, password: string) {
  return adminRequest.put<Result<null>>(`/admin/users/${id}/password`, { password })
}

/** 资料变更审核列表 */
export function adminListReviews(status?: number) {
  return adminRequest.get<Result<ProfileChangeRequest[]>>('/admin/reviews', {
    params: status !== undefined ? { status } : {},
  })
}

/** 通过审核 */
export function adminApproveReview(id: number) {
  return adminRequest.post<Result<null>>(`/admin/reviews/${id}/approve`)
}

/** 拒绝审核 */
export function adminRejectReview(id: number, remark?: string) {
  return adminRequest.post<Result<null>>(`/admin/reviews/${id}/reject`, { remark })
}
