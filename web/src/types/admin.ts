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
