/**
 * 管理员入口路径（V3）
 * 从后端公开接口动态获取，可在配置文件中替换
 */

let adminEntryPath = '/admin'

export function setAdminEntryPath(path: string) {
  const normalized = (path || '/admin').replace(/\/+$/, '')
  adminEntryPath = normalized || '/admin'
}

export function getAdminEntryPath(): string {
  return adminEntryPath
}
