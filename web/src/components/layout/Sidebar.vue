<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2 class="logo">提示词训练</h2>
      <el-button v-if="!editMode" text size="small" @click="enterEditMode">编辑</el-button>
      <el-button v-else text size="small" type="primary" @click="exitEditMode">完成</el-button>
    </div>

    <div class="new-chat-btn">
      <el-button
        type="primary"
        :icon="Plus"
        block
        @click="handleNewSession"
        :loading="creating"
      >
        新建对话
      </el-button>
    </div>

    <!-- 搜索框 -->
    <div class="search-box" v-if="!editMode">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索会话..."
        :prefix-icon="Search"
        size="small"
        clearable
        @input="handleSearch"
      />
    </div>

    <div class="session-list">
      <!-- 编辑模式 -->
      <template v-if="editMode">
        <div
          v-for="session in sessionStore.sessions"
          :key="session.id"
          class="session-item edit-item"
          @click="toggleSelect(session.id)"
        >
          <el-checkbox
            :model-value="selectedIds.includes(session.id)"
            class="check-box"
            @click.stop
            @change="toggleSelect(session.id)"
          />
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
          </div>
        </div>
      </template>

      <!-- 普通模式 -->
      <template v-else>
        <div
          v-for="session in filteredSessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === chatStore.activeSessionId }"
          @click="handleSelectSession(session.id)"
        >
          <div class="session-info">
            <div
              class="session-title"
              @dblclick.stop="startRename(session)"
            >
              <template v-if="renamingId === session.id">
                <el-input
                  v-model="renameTitle"
                  size="small"
                  @keydown.enter="confirmRename(session.id)"
                  @blur="confirmRename(session.id)"
                  @click.stop
                  ref="renameInputRef"
                />
              </template>
              <template v-else>
                <span v-html="highlightMatch(session.title)"></span>
              </template>
            </div>
            <div class="session-meta">{{ session.messageCount }} 条消息</div>
          </div>
          <el-popconfirm
            title="确定删除此会话？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm.stop="handleDeleteSession(session.id)"
          >
            <template #reference>
              <el-button
                text
                :icon="Delete"
                size="small"
                class="delete-btn"
                @click.stop
              />
            </template>
          </el-popconfirm>
        </div>
      </template>

      <EmptyState
        v-if="!sessionStore.loading && filteredSessions.length === 0"
        :description="searchKeyword ? '未找到匹配的会话' : '暂无会话，点击上方按钮创建'"
      />
    </div>

    <!-- 编辑模式底部：批量删除 -->
    <div class="batch-delete-bar" v-if="editMode && selectedIds.length > 0">
      <el-button
        type="danger"
        size="small"
        @click="handleBatchDelete"
      >
        删除选中（{{ selectedIds.length }}）
      </el-button>
    </div>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <div class="user-area" @click="$router.push('/profile')">
        <el-avatar :size="28" :icon="User" />
        <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username || '未登录' }}</span>
      </div>
      <el-button
        class="logout-btn"
        text
        type="danger"
        :icon="SwitchButton"
        @click="handleLogout"
      >
        退出登录
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Search, User, SwitchButton } from '@element-plus/icons-vue'
import { useSessionStore } from '@/stores/session'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { searchSessions, batchDeleteSessions, updateSession } from '@/api/session'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const sessionStore = useSessionStore()
const chatStore = useChatStore()
const userStore = useUserStore()

const creating = ref(false)
const searchKeyword = ref('')
const searchResults = ref<typeof sessionStore.sessions>([])
const editMode = ref(false)
const selectedIds = ref<string[]>([])
const renamingId = ref<string | null>(null)
const renameTitle = ref('')
const renameInputRef = ref()

/** 过滤后的会话列表（搜索模式使用搜索结果，否则显示全部） */
const filteredSessions = computed(() => {
  if (searchKeyword.value) {
    return searchResults.value
  }
  return sessionStore.sessions
})

onMounted(() => {
  sessionStore.loadSessions()
  userStore.restoreUser()
})

/** 搜索会话 */
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  try {
    const res = await searchSessions(searchKeyword.value.trim())
    searchResults.value = res.data.data
  } catch {
    // 降级为前端过滤
    const kw = searchKeyword.value.toLowerCase()
    searchResults.value = sessionStore.sessions.filter(s =>
      s.title.toLowerCase().includes(kw)
    )
  }
}

/** 高亮匹配文字 */
function highlightMatch(text: string): string {
  if (!searchKeyword.value) return text
  const regex = new RegExp(`(${searchKeyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

async function handleNewSession() {
  creating.value = true
  const sessionId = await sessionStore.createSession()
  creating.value = false
  if (sessionId) {
    chatStore.setActiveSession(sessionId)
  }
}

function handleSelectSession(id: string) {
  chatStore.setActiveSession(id)
}

async function handleDeleteSession(id: string) {
  if (id === chatStore.activeSessionId) {
    chatStore.setActiveSession(null)
  }
  await sessionStore.removeSession(id)
}

/** 双击重命名 */
function startRename(session: any) {
  if (editMode.value) return
  renamingId.value = session.id
  renameTitle.value = session.title
  nextTick(() => {
    const input = document.querySelector('.session-item .el-input__inner') as HTMLInputElement
    if (input) input.focus()
  })
}

/** 确认重命名 */
async function confirmRename(sessionId: string) {
  if (renameTitle.value.trim() && renameTitle.value !== sessionStore.sessions.find(s => s.id === sessionId)?.title) {
    sessionStore.updateSessionItem(sessionId, { title: renameTitle.value.trim() })
    try {
      await updateSession(sessionId, renameTitle.value.trim())
    } catch {
      // 静默失败
    }
  }
  renamingId.value = null
}

/** 进入编辑模式 */
function enterEditMode() {
  editMode.value = true
  selectedIds.value = []
}

/** 退出编辑模式 */
function exitEditMode() {
  editMode.value = false
  selectedIds.value = []
}

/** 切换选中 */
function toggleSelect(id: string) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

/** 批量删除 */
async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个会话吗？`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await batchDeleteSessions(selectedIds.value)
    const deletedSet = new Set(selectedIds.value)
    // 如果删除了当前活跃会话，清除它
    if (chatStore.activeSessionId && deletedSet.has(chatStore.activeSessionId)) {
      chatStore.setActiveSession(null)
    }
    await sessionStore.loadSessions()
    ElMessage.success(`成功删除 ${selectedIds.value.length} 个会话`)
    exitEditMode()
  } catch {
    // 取消
  }
}

/** 退出登录 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  chatStore.resetAll()
  await userStore.logoutAction()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.sidebar {
  width: 260px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .logo {
    font-size: 18px;
    font-weight: 700;
    color: #409eff;
    margin: 0;
  }
}

.new-chat-btn {
  padding: 12px 16px;
}

.search-box {
  padding: 0 16px 8px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;

  &:hover {
    background: #e6f0ff;
  }

  &.active {
    background: #d9ecff;
  }

  &.edit-item {
    cursor: default;
  }

  .session-info {
    flex: 1;
    min-width: 0;

    .session-title {
      font-size: 14px;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      :deep(mark) {
        background: #fff3cd;
        color: #856404;
        padding: 0 2px;
        border-radius: 2px;
      }
    }

    .session-meta {
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
    }
  }

  .delete-btn {
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .delete-btn {
    opacity: 1;
  }

  .check-box {
    margin-right: 8px;
  }
}

.batch-delete-bar {
  padding: 8px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fff;

  .user-area {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    flex: 1;
    min-width: 0;

    .user-name {
      font-size: 13px;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &:hover {
      opacity: 0.8;
    }
  }

  .logout-btn {
    flex-shrink: 0;
    margin-left: 8px;
  }
}
</style>
