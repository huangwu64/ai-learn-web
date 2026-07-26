/** 会话信息 */
export interface SessionItem {
  id: string
  title: string
  lastMessage: string
  messageCount: number
  modelCode: string
  updatedAt: string
}

/** 会话详情 */
export interface SessionDetail {
  id: string
  title: string
  modelCode: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

/** 创建会话请求 */
export interface SessionCreateRequest {
  modelCode: string
}
