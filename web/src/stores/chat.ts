import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MessageResponse } from '@/types/message'

export const useChatStore = defineStore('chat', () => {
  /** 当前活跃会话 ID */
  const activeSessionId = ref<string | null>(null)

  /** 当前会话的消息列表 */
  const messages = ref<MessageResponse[]>([])

  /** 是否正在生成回复 */
  const isStreaming = ref(false)

  /** 当前流式回复的临时内容 */
  const streamingContent = ref('')

  /** 当前流式回复的推理内容（deepseek-v4-flash 等推理模型） */
  const streamingReasoning = ref('')

  /** 消息游标分页 */
  const nextCursor = ref<number | null>(null)
  const hasMore = ref(false)

  function setActiveSession(sessionId: string | null) {
    activeSessionId.value = sessionId
    messages.value = []
    streamingContent.value = ''
    streamingReasoning.value = ''
    isStreaming.value = false
    nextCursor.value = null
    hasMore.value = false
  }

  function setMessages(list: MessageResponse[], cursor: number | null, more: boolean) {
    messages.value = list
    nextCursor.value = cursor
    hasMore.value = more
  }

  function prependMessages(list: MessageResponse[], cursor: number | null, more: boolean) {
    messages.value = [...list, ...messages.value]
    nextCursor.value = cursor
    hasMore.value = more
  }

  function appendUserMessage(msg: MessageResponse) {
    messages.value.push(msg)
  }

  function appendAssistantMessage(msg: MessageResponse) {
    messages.value.push(msg)
  }

  function startStreaming() {
    isStreaming.value = true
    streamingContent.value = ''
    streamingReasoning.value = ''
  }

  function appendStreamChunk(chunk: string) {
    streamingContent.value += chunk
  }

  function appendReasoningChunk(chunk: string) {
    streamingReasoning.value += chunk
  }

  function finishStreaming(messageId: number, tokenCount: number) {
    isStreaming.value = false
    messages.value.push({
      id: messageId,
      role: 'assistant',
      content: streamingContent.value,
      tokenCount,
      modelCode: 'deepseek-chat',
      createdAt: new Date().toISOString(),
    })
    streamingContent.value = ''
    streamingReasoning.value = ''
  }

  function stopStreaming() {
    isStreaming.value = false
    if (streamingContent.value) {
      messages.value.push({
        id: 0,
        role: 'assistant',
        content: streamingContent.value + '\n\n（已停止生成）',
        tokenCount: 0,
        modelCode: 'deepseek-chat',
        createdAt: new Date().toISOString(),
      })
    }
    streamingContent.value = ''
    streamingReasoning.value = ''
  }

  /** 清除所有状态（退出登录时调用） */
  function resetAll() {
    activeSessionId.value = null
    messages.value = []
    streamingContent.value = ''
    streamingReasoning.value = ''
    isStreaming.value = false
    nextCursor.value = null
    hasMore.value = false
  }

  return {
    activeSessionId,
    messages,
    isStreaming,
    streamingContent,
    streamingReasoning,
    nextCursor,
    hasMore,
    setActiveSession,
    setMessages,
    prependMessages,
    appendUserMessage,
    appendAssistantMessage,
    startStreaming,
    appendStreamChunk,
    appendReasoningChunk,
    finishStreaming,
    stopStreaming,
    resetAll,
  }
})
