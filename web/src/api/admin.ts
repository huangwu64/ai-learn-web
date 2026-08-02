import adminRequest from './adminRequest'
import type { Result } from '@/types/api'
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AiConfig,
  AiConfigUpdateRequest,
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
