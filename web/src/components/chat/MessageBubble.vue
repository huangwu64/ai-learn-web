<template>
  <div class="message-bubble-wrapper" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <div class="message-bubble" :class="role">
      <div class="avatar">
        <el-avatar :size="36" :icon="role === 'user' ? UserFilled : ChatDotSquare" />
      </div>
      <div class="bubble-content">
        <div class="role-label">{{ role === 'user' ? '你' : 'AI' }}</div>
        <div class="message-text" v-html="renderedContent"></div>
        <div class="message-bottom">
          <div class="message-meta" v-if="role === 'assistant' && tokenCount">
            消耗 {{ tokenCount }} token
          </div>
          <!-- 时间标签（hover 时显示） -->
          <div class="message-time" v-if="createdAt">
            {{ formatTime(createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <!-- AI 消息操作按钮（hover 时显示） -->
    <div class="message-actions" v-if="role === 'assistant' && hovered">
      <el-tooltip content="复制" placement="top">
        <el-button text :icon="copied ? Check : CopyDocument" size="small" @click="handleCopy" />
      </el-tooltip>
      <el-tooltip content="重新生成" placement="top">
        <el-button text :icon="Refresh" size="small" @click="$emit('regenerate', messageId!)" />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, ChatDotSquare, CopyDocument, Check, Refresh } from '@element-plus/icons-vue'

const props = defineProps<{
  role: 'user' | 'assistant'
  content: string
  tokenCount?: number
  messageId?: number
  createdAt?: string
}>()

defineEmits<{
  regenerate: [messageId: number]
}>()

const hovered = ref(false)
const copied = ref(false)

/** 渲染 Markdown 文本（简单处理换行和代码块） */
const renderedContent = computed(() => {
  return props.content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
})

/** 格式化时间：同一天显示 HH:mm，不同天显示 MM-DD HH:mm */
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  if (date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate()) {
    return `${hours}:${minutes}`
  }
  return `${month}-${day} ${hours}:${minutes}`
}

/** 复制消息内容 */
async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.content)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}
</script>

<style scoped lang="scss">
.message-bubble-wrapper {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;

  .message-actions {
    display: flex;
    gap: 2px;
    padding-top: 28px;
    opacity: 0.7;
    transition: opacity 0.2s;

    &:hover { opacity: 1; }
  }
}

.message-bubble {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  max-width: 85%;
  flex: 1;

  &.user {
    flex-direction: row-reverse;
    align-self: flex-end;
    margin-left: auto;

    .bubble-content {
      align-items: flex-end;
    }

    .role-label {
      text-align: right;
    }
  }

  .bubble-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .role-label {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
  }

  .message-text {
    background: #fff;
    border-radius: 12px;
    padding: 12px 16px;
    font-size: 14px;
    line-height: 1.7;
    color: #303133;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child { margin: 0; }
    }

    :deep(pre) {
      background: #f5f7fa;
      border-radius: 6px;
      padding: 12px;
      overflow-x: auto;
      font-size: 13px;
    }

    :deep(code) {
      background: #f0f2f5;
      border-radius: 3px;
      padding: 2px 6px;
      font-size: 13px;
    }
  }

  &.user .message-text {
    background: #409eff;
    color: #fff;

    :deep(code) {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
    }

    :deep(pre) {
      background: rgba(0, 0, 0, 0.1);
    }
  }

  .message-bottom {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .message-meta {
    font-size: 12px;
    color: #c0c4cc;
  }

  .message-time {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
