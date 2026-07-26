import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SessionItem } from '@/types/session'
import * as sessionApi from '@/api/session'

export const useSessionStore = defineStore('session', () => {
  const sessions = ref<SessionItem[]>([])
  const loading = ref(false)

  /** 加载会话列表 */
  async function loadSessions() {
    loading.value = true
    try {
      const res = await sessionApi.getSessionList()
      sessions.value = res.data.data
    } catch {
      console.error('加载会话列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 创建新会话 */
  async function createSession(): Promise<string | null> {
    try {
      const res = await sessionApi.createSession({ modelCode: 'deepseek-chat' })
      const newSession = res.data.data
      sessions.value.unshift({
        id: newSession.id,
        title: newSession.title,
        lastMessage: '',
        messageCount: 0,
        modelCode: newSession.modelCode,
        updatedAt: newSession.createdAt,
      })
      return newSession.id
    } catch {
      console.error('创建会话失败')
      return null
    }
  }

  /** 删除会话 */
  async function removeSession(id: string) {
    try {
      await sessionApi.deleteSession(id)
      sessions.value = sessions.value.filter(s => s.id !== id)
    } catch {
      console.error('删除会话失败')
    }
  }

  /** 更新会话信息（标题等） */
  function updateSessionItem(id: string, updates: Partial<SessionItem>) {
    const idx = sessions.value.findIndex(s => s.id === id)
    if (idx !== -1) {
      sessions.value[idx] = { ...sessions.value[idx], ...updates }
    }
  }

  return {
    sessions,
    loading,
    loadSessions,
    createSession,
    removeSession,
    updateSessionItem,
  }
})
