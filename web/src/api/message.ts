import request from './request'
import type { MessageResponse, MessageSendResponse, PageResult } from '@/types/message'
import type { Result } from '@/types/api'

/** 同步发送消息 */
export function sendMessage(sessionId: string, content: string) {
  return request.post<Result<MessageSendResponse>>(`/sessions/${sessionId}/messages`, { content })
}

/** 获取消息历史（游标分页） */
export function getMessages(sessionId: string, cursor?: number, limit = 50) {
  const params: Record<string, any> = { limit }
  if (cursor) params.cursor = cursor
  return request.get<Result<PageResult<MessageResponse>>>(`/sessions/${sessionId}/messages`, { params })
}

/** 同步重新生成 AI 回复 */
export function regenerateMessage(sessionId: string, messageId: number) {
  return request.post<Result<MessageSendResponse>>(`/sessions/${sessionId}/messages/${messageId}/regenerate`)
}

/** 删除单条消息 */
export function deleteMessage(sessionId: string, messageId: number) {
  return request.delete<Result<null>>(`/sessions/${sessionId}/messages/${messageId}`)
}

/** 清空会话所有消息 */
export function deleteMessages(sessionId: string) {
  return request.delete<Result<{ deletedCount: number }>>(`/sessions/${sessionId}/messages`)
}
