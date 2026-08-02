<template>
  <div class="chat-panel" ref="panelRef">
    <!-- 顶部操作栏 -->
    <div class="chat-top-bar">
      <span class="session-title">{{ currentTitle }}</span>
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button text :icon="MoreFilled" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="clear">清空对话</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesRef">
      <div v-if="chatStore.messages.length === 0 && !chatStore.isStreaming" class="empty-chat">
        <EmptyState description="开始你的第一次对话吧" />
      </div>

      <MessageBubble
        v-for="msg in chatStore.messages"
        :key="msg.id"
        :role="msg.role"
        :content="msg.content"
        :tokenCount="msg.tokenCount"
        :messageId="msg.id"
        :createdAt="msg.createdAt"
        @regenerate="handleRegenerate"
      />

      <!-- 推理过程（推理模型的思考内容，如 deepseek-v4-flash） -->
      <div v-if="chatStore.isStreaming && chatStore.streamingReasoning" class="reasoning-box">
        <div class="reasoning-title">🤔 深度思考中</div>
        <div class="reasoning-content">{{ chatStore.streamingReasoning }}</div>
      </div>

      <!-- 流式回复的临时气泡 -->
      <MessageBubble
        v-if="chatStore.isStreaming && chatStore.streamingContent"
        role="assistant"
        :content="chatStore.streamingContent"
      />

      <!-- 加载中的等待动画 -->
      <div v-if="chatStore.isStreaming && !chatStore.streamingContent" class="stream-loading">
        <LoadingSpinner />
      </div>

      <!-- 加载更多的提示 -->
      <div v-if="chatStore.hasMore && !loadingMore" class="load-more" @click="loadMore">
        加载更早的消息
      </div>
      <div v-if="loadingMore" class="load-more">
        <LoadingSpinner />
      </div>
    </div>

    <!-- 输入框 -->
    <MessageInput
      :disabled="chatStore.isStreaming"
      @send="handleSend"
      @stop="handleStop"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useSessionStore } from '@/stores/session'
import * as messageApi from '@/api/message'
import * as sessionApi from '@/api/session'
import { getAccessToken, refreshAccessToken, redirectToLogin } from '@/utils/auth'
import type { SSEChunk } from '@/types/message'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const chatStore = useChatStore()
const sessionStore = useSessionStore()
const messagesRef = ref<HTMLElement | null>(null)
const loadingMore = ref(false)

/** 截取文本前 N 字 */
function truncateText(text: string, maxLen: number): string {
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

/** 同步侧边栏会话信息 */
function syncSessionInfo(sessionId: string, extra?: Record<string, any>) {
  const lastMsg = chatStore.messages[chatStore.messages.length - 1]
  sessionStore.updateSessionItem(sessionId, {
    messageCount: chatStore.messages.length,
    lastMessage: lastMsg ? truncateText(lastMsg.content, 50) : '',
    ...extra,
  })
}

/** 当前会话标题 */
const currentTitle = computed(() => {
  if (!chatStore.activeSessionId) return ''
  const session = sessionStore.sessions.find(s => s.id === chatStore.activeSessionId)
  return session?.title || '新对话'
})

/** 加载消息历史 */
function loadMessages(sessionId: string) {
  messageApi.getMessages(sessionId).then(res => {
    const data = res.data.data
    chatStore.setMessages(data.list, data.nextCursor, data.hasMore)
    scrollToBottom()
  })
}

/** 滚动到底部 */
function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

/** 加载更早的消息 */
async function loadMore() {
  if (loadingMore.value || !chatStore.activeSessionId) return
  loadingMore.value = true
  try {
    const res = await messageApi.getMessages(chatStore.activeSessionId, chatStore.nextCursor ?? undefined)
    const data = res.data.data
    chatStore.prependMessages(data.list, data.nextCursor, data.hasMore)
  } finally {
    loadingMore.value = false
  }
}

/** 当前活跃的 AbortController（用于停止生成） */
let currentAbortController: AbortController | null = null

/**
 * 携带 Token 的流式请求封装：
 * - 自动附带 Authorization 请求头
 * - 检测到认证过期（HTTP 401 或 HTTP 200 + JSON body code=401）时，
 *   刷新 Token 并用新 Token 重试一次；仍失败则清除登录态并跳转登录页，返回 null。
 * - 其他非 401 的 JSON 错误直接抛出，由调用方走同步请求降级。
 */
async function streamFetchWithAuth(
  url: string,
  body: unknown,
  signal: AbortSignal
): Promise<Response | null> {
  const token = getAccessToken()
  const baseHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  }
  const init: RequestInit = {
    method: 'POST',
    headers: baseHeaders,
    body: JSON.stringify(body),
    signal,
  }

  let response = await fetch(url, init)

  // 认证过期判断：HTTP 401，或 HTTP 200 + JSON body code=401（后端统一返回 200）
  const isAuthError =
    response.status === 401 ||
    (response.headers.get('content-type') || '').includes('application/json')

  if (!isAuthError) return response

  let bodyJson: any = null
  try {
    bodyJson = await response.clone().json()
  } catch {
    bodyJson = null
  }
  const authExpired = response.status === 401 || bodyJson?.code === 401

  if (!authExpired) {
    // 非 401 的 JSON 错误（如服务器 500）→ 抛出让调用方降级
    throw new Error(bodyJson?.message || '请求失败')
  }

  // 认证过期：刷新 Token 并重试一次
  const newToken = await refreshAccessToken()
  if (!newToken) {
    chatStore.stopStreaming()
    ElMessage.error('登录已过期，请重新登录')
    redirectToLogin()
    return null
  }

  response = await fetch(url, {
    ...init,
    headers: { ...baseHeaders, Authorization: `Bearer ${newToken}` },
  })

  // 重试后仍为认证错误 → 直接退出登录
  const retryAuthError =
    response.status === 401 ||
    (response.headers.get('content-type') || '').includes('application/json')
  if (retryAuthError) {
    chatStore.stopStreaming()
    ElMessage.error('登录已过期，请重新登录')
    redirectToLogin()
    return null
  }
  return response
}

/** 发送消息 */
async function handleSend(content: string) {
  if (!chatStore.activeSessionId || chatStore.isStreaming) return

  const sessionId = chatStore.activeSessionId
  const tempUserMsg = {
    id: Date.now(),
    role: 'user' as const,
    content,
    tokenCount: 0,
    modelCode: null,
    createdAt: new Date().toISOString(),
  }
  chatStore.appendUserMessage(tempUserMsg)
  scrollToBottom()

  chatStore.startStreaming()
  scrollToBottom()

  currentAbortController = new AbortController()

  try {
    const response = await streamFetchWithAuth(
      `/api/v1/sessions/${sessionId}/messages/stream`,
      { content },
      currentAbortController.signal
    )
    if (!response) return // 已跳转登录页

    if (!response.ok) throw new Error('请求失败')

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    const SSE_TIMEOUT_MS = 60_000

    while (true) {
      let timeoutId: ReturnType<typeof setTimeout> | null = null
      const readPromise = reader.read()
      const timeoutPromise = new Promise<{ timedOut: true }>((resolve) => {
        timeoutId = setTimeout(() => resolve({ timedOut: true }), SSE_TIMEOUT_MS)
      })

      const result = await Promise.race([readPromise, timeoutPromise])

      if ('timedOut' in result && result.timedOut) {
        console.warn('SSE 流读取超时')
        reader.cancel()
        if (chatStore.streamingContent) {
          chatStore.stopStreaming()
        } else {
          chatStore.stopStreaming()
          ElMessage.warning('AI 响应超时，正在重试...')
        }
        break
      }

      if (timeoutId !== null) clearTimeout(timeoutId)

      const { done, value } = result as ReadableStreamReadResult<Uint8Array>
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const dataPrefix = 'data:'
        if (line.startsWith(dataPrefix)) {
          const jsonStr = line.substring(dataPrefix.length).trim()
          if (!jsonStr) continue
          try {
            const data: SSEChunk = JSON.parse(jsonStr)
            if (data.type === 'content' && data.content) {
              chatStore.appendStreamChunk(data.content)
              scrollToBottom()
            } else if (data.type === 'reasoning' && data.content) {
              chatStore.appendReasoningChunk(data.content)
            } else if (data.type === 'done') {
              chatStore.finishStreaming(data.messageId ?? 0, data.tokenCount ?? 0)
              syncSessionInfo(sessionId, data.sessionTitle ? { title: data.sessionTitle } : {})
              scrollToBottom()
            } else if (data.type === 'error') {
              chatStore.stopStreaming()
              ElMessage.error(data.message || 'AI 响应出错，请重试')
            }
          } catch {
            // 忽略解析错误
          }
        }
      }
    }
  } catch (error: any) {
    if (error?.name === 'AbortError') {
      // 用户主动停止，已在 handleStop 处理
      return
    }
    console.error('流式请求失败:', error)
    try {
      chatStore.stopStreaming()
      const res = await messageApi.sendMessage(sessionId, content)
      const data = res.data.data
      chatStore.appendAssistantMessage(data.assistantMessage)
      syncSessionInfo(sessionId, data.sessionTitle ? { title: data.sessionTitle } : {})
      scrollToBottom()
    } catch (syncError) {
      chatStore.stopStreaming()
      console.error('同步请求也失败了:', syncError)
      ElMessage.error('AI 服务暂不可用，请稍后重试')
    }
  } finally {
    currentAbortController = null
  }
}

/** 停止生成 */
function handleStop() {
  if (currentAbortController) {
    currentAbortController.abort()
    currentAbortController = null
  }
  chatStore.stopStreaming()
  if (chatStore.activeSessionId) {
    syncSessionInfo(chatStore.activeSessionId)
  }
}

/** 重新生成 AI 回复 */
async function handleRegenerate(messageId: number) {
  if (!chatStore.activeSessionId || chatStore.isStreaming) return
  const sessionId = chatStore.activeSessionId

  // 从消息列表中移除该 AI 回复
  const idx = chatStore.messages.findIndex(m => m.id === messageId)
  if (idx !== -1) {
    chatStore.messages.splice(idx, 1)
  }

  chatStore.startStreaming()
  scrollToBottom()

  currentAbortController = new AbortController()

  try {
    const response = await streamFetchWithAuth(
      `/api/v1/sessions/${sessionId}/messages/${messageId}/regenerate/stream`,
      {},
      currentAbortController.signal
    )
    if (!response) return // 已跳转登录页

    if (!response.ok) throw new Error('请求失败')

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const dataPrefix = 'data:'
        if (line.startsWith(dataPrefix)) {
          const jsonStr = line.substring(dataPrefix.length).trim()
          if (!jsonStr) continue
          try {
            const data: SSEChunk = JSON.parse(jsonStr)
            if (data.type === 'content' && data.content) {
              chatStore.appendStreamChunk(data.content)
              scrollToBottom()
            } else if (data.type === 'reasoning' && data.content) {
              chatStore.appendReasoningChunk(data.content)
            } else if (data.type === 'done') {
              chatStore.finishStreaming(data.messageId ?? 0, data.tokenCount ?? 0)
              if (chatStore.activeSessionId) {
                syncSessionInfo(chatStore.activeSessionId)
              }
              scrollToBottom()
            } else if (data.type === 'error') {
              chatStore.stopStreaming()
              ElMessage.error(data.message || '重新生成失败')
            }
          } catch {
            // 忽略解析错误
          }
        }
      }
    }
  } catch (error: any) {
    if (error?.name === 'AbortError') return
    console.error('重新生成失败:', error)
    chatStore.stopStreaming()
    ElMessage.error('重新生成失败，请重试')
  } finally {
    currentAbortController = null
  }
}

/** 顶部菜单操作 */
async function handleCommand(command: string) {
  if (command === 'clear' && chatStore.activeSessionId) {
    try {
      await ElMessageBox.confirm('确定要清空该会话的所有消息吗？此操作不可恢复。', '清空对话', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await messageApi.deleteMessages(chatStore.activeSessionId)
      chatStore.setMessages([], null, false)
      sessionStore.updateSessionItem(chatStore.activeSessionId, { messageCount: 0 })
      ElMessage.success('对话已清空')
    } catch {
      // 用户取消
    }
  }
}

/** 监听活跃会话切换，加载消息 */
watch(() => chatStore.activeSessionId, (newId) => {
  if (newId) {
    chatStore.setMessages([], null, false)
    loadMessages(newId)
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fafbfc;
}

.chat-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;

  .session-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    max-width: 80%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 40px;
  display: flex;
  flex-direction: column;
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reasoning-box {
  margin: 4px 0;
  padding: 10px 14px;
  background: #f4f4f5;
  border-radius: 8px;
  border-left: 3px solid #c0c4cc;
  max-height: 160px;
  overflow-y: auto;

  .reasoning-title {
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
  }

  .reasoning-content {
    font-size: 13px;
    color: #909399;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.stream-loading {
  padding: 12px 0;
}

.load-more {
  text-align: center;
  padding: 12px 0;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
  user-select: none;

  &:hover {
    text-decoration: underline;
  }
}
</style>
