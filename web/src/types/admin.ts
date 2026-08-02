/** 管理员登录请求 */
export interface AdminLoginRequest {
  username: string
  password: string
}

/** 管理员登录响应 */
export interface AdminLoginResponse {
  token: string
  expiresIn: number
}

/** 管理员入口路径 */
export interface AdminEntryResponse {
  path: string
}

/** AI 动态配置 */
export interface AiConfig {
  providerCode: string
  apiBaseUrl: string
  apiKeyMasked: string
  hasApiKey: boolean
  modelCode: string
  maxTokens: number | null
  temperature: number | null
  topP: number | null
  presencePenalty: number | null
  frequencyPenalty: number | null
  systemPrompt: string
  updatedAt: string
}

/** AI 配置更新请求（apiKey 留空表示不修改） */
export interface AiConfigUpdateRequest {
  apiBaseUrl?: string
  apiKey?: string
  modelCode?: string
  maxTokens?: number | null
  temperature?: number | null
  topP?: number | null
  presencePenalty?: number | null
  frequencyPenalty?: number | null
  systemPrompt?: string
}

/** 管理员视图的用户信息 */
export interface AdminUser {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  status: number
  createdAt: string
  lastLoginAt: string | null
}

/** 管理员创建用户请求 */
export interface AdminCreateUserRequest {
  username: string
  password: string
  nickname?: string
}

/** 管理员更新用户请求 */
export interface AdminUpdateUserRequest {
  nickname?: string
  avatarUrl?: string
  status?: number
}

/** 管理员重置密码请求 */
export interface AdminResetPasswordRequest {
  password: string
}

/** 资料变更审核记录 */
export interface ProfileChangeRequest {
  id: number
  userId: number
  username?: string
  nickname?: string
  fieldName: 'avatar' | 'nickname' | 'username'
  oldValue?: string | null
  newValue: string
  status: number
  reviewRemark?: string | null
  reviewedAt?: string | null
  createdAt: string
}

/** 提交资料变更请求 */
export interface ProfileChangeRequestSubmit {
  fieldName: 'avatar' | 'nickname' | 'username'
  newValue: string
}
