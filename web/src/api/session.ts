import request from './request'
import type { SessionItem, SessionDetail, SessionCreateRequest } from '@/types/session'
import type { Result } from '@/types/api'

/** 创建新会话 */
export function createSession(data: SessionCreateRequest) {
  return request.post<Result<SessionDetail>>('/sessions', data)
}

/** 获取会话列表 */
export function getSessionList() {
  return request.get<Result<SessionItem[]>>('/sessions')
}

/** 获取会话详情 */
export function getSessionDetail(id: string) {
  return request.get<Result<SessionDetail>>(`/sessions/${id}`)
}

/** 更新会话标题 */
export function updateSession(id: string, title: string) {
  return request.patch<Result<null>>(`/sessions/${id}`, { title })
}

/** 删除会话 */
export function deleteSession(id: string) {
  return request.delete<Result<null>>(`/sessions/${id}`)
}

/** 搜索会话 */
export function searchSessions(keyword: string) {
  return request.get<Result<SessionItem[]>>('/sessions/search', { params: { keyword } })
}

/** 批量删除会话 */
export function batchDeleteSessions(ids: string[]) {
  return request.post<Result<{ deletedCount: number }>>('/sessions/batch-delete', { ids })
}
