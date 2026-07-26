/** 统一响应结构 */
export interface Result<T> {
  code: number
  message: string
  data: T
}
