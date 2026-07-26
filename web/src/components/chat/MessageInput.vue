<template>
  <div class="message-input">
    <div class="input-wrapper">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        resize="none"
        :disabled="disabled"
        @keydown.enter.exact.prevent="handleSend"
      />
      <div class="input-actions">
        <el-button
          v-if="disabled"
          type="warning"
          :icon="VideoPause"
          @click="$emit('stop')"
        >
          停止生成
        </el-button>
        <el-button
          v-else
          type="primary"
          :icon="Promotion"
          :disabled="!inputText.trim()"
          @click="handleSend"
          class="send-btn"
        >
          发送
        </el-button>
      </div>
    </div>
    <p class="input-tip" v-if="disabled">AI 正在回复中... 点击"停止生成"可中断</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Promotion, VideoPause } from '@element-plus/icons-vue'

const props = defineProps<{
  disabled: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
  stop: []
}>()

const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped lang="scss">
.message-input {
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;

  :deep(.el-textarea__inner) {
    border-radius: 8px;
  }

  .input-actions {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .send-btn {
      height: 40px;
    }
  }
}

.input-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: #e6a23c;
}
</style>
