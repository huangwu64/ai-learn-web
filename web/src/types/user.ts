/** 用户基本信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  createdAt?: string
  lastLoginAt?: string | null
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  nickname?: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

/** Token 刷新响应 */
export interface TokenRefreshResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/** 更新用户信息请求 */
export interface UpdateUserRequest {
  nickname?: string
  avatarUrl?: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
