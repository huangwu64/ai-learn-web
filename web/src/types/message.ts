/** 消息角色 */
export type MessageRole = 'user' | 'assistant'

/** 消息响应 */
export interface MessageResponse {
  id: number
  role: MessageRole
  content: string
  tokenCount: number
  modelCode: string | null
  createdAt: string
}

/** 发送消息后的完整响应 */
export interface MessageSendResponse {
  userMessage: MessageResponse
  assistantMessage: MessageResponse
  sessionTitle: string
}

/** SSE 流式数据块 */
export interface SSEChunk {
  type: 'content' | 'reasoning' | 'done' | 'error'
  content?: string
  messageId?: number
  tokenCount?: number
  sessionTitle?: string
  message?: string
}

/** 分页响应 */
export interface PageResult<T> {
  list: T[]
  nextCursor: number | null
  hasMore: boolean
}
