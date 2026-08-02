<template>
  <div class="user-manage-panel">
    <div class="panel-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
    </div>

    <el-table :data="users" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="头像" width="70" align="center">
        <template #default="{ row }">
          <el-avatar :size="32" :src="row.avatarUrl || undefined">{{ row.nickname?.[0] || '?' }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button text type="warning" size="small" @click="openResetPwd(row)">重置密码</el-button>
          <el-popconfirm
            title="确定删除该用户？（其会话和消息将一并删除）"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button text type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增用户 -->
    <el-dialog v-model="createVisible" title="新增用户" width="420px">
      <el-form :model="createForm" label-width="70px" label-position="left">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="登录账号" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" placeholder="可选，默认同用户名" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="420px">
      <el-form :model="editForm" label-width="70px" label-position="left">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="64" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatarUrl" maxlength="255" placeholder="/uploads/avatars/xxx.png 或完整URL" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="380px">
      <el-form label-width="70px" label-position="left">
        <el-form-item label="用户">
          <span>{{ resetPwdTarget?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" show-password placeholder="设置新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="warning" :loading="resetting" @click="handleResetPwd">重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  adminListUsers,
  adminCreateUser,
  adminDeleteUser,
  adminUpdateUser,
  adminResetPassword,
} from '@/api/admin'
import type { AdminUser } from '@/types/admin'

const users = ref<AdminUser[]>([])
const loading = ref(false)
const creating = ref(false)
const saving = ref(false)
const resetting = ref(false)

const createVisible = ref(false)
const editVisible = ref(false)
const pwdVisible = ref(false)

const createForm = reactive({ username: '', password: '', nickname: '' })
const editForm = reactive({ nickname: '', avatarUrl: '', status: 1 })
const editTarget = ref<AdminUser | null>(null)
const resetPwdTarget = ref<AdminUser | null>(null)
const newPassword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminListUsers()
    users.value = res.data.data
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.nickname = ''
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.username.trim() || !createForm.password.trim()) {
    ElMessage.warning('用户名和密码不能为空')
    return
  }
  creating.value = true
  try {
    await adminCreateUser({
      username: createForm.username.trim(),
      password: createForm.password,
      nickname: createForm.nickname.trim() || undefined,
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    await loadUsers()
  } catch (err: any) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function openEdit(row: AdminUser) {
  editTarget.value = row
  editForm.nickname = row.nickname || ''
  editForm.avatarUrl = row.avatarUrl || ''
  editForm.status = row.status
  editVisible.value = true
}

async function handleEdit() {
  if (!editTarget.value) return
  saving.value = true
  try {
    await adminUpdateUser(editTarget.value.id, {
      nickname: editForm.nickname.trim() || undefined,
      avatarUrl: editForm.avatarUrl.trim() || undefined,
      status: editForm.status,
    })
    ElMessage.success('已更新')
    editVisible.value = false
    await loadUsers()
  } catch (err: any) {
    ElMessage.error(err?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

function openResetPwd(row: AdminUser) {
  resetPwdTarget.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!resetPwdTarget.value || !newPassword.value.trim()) return
  resetting.value = true
  try {
    await adminResetPassword(resetPwdTarget.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } catch (err: any) {
    ElMessage.error(err?.message || '重置失败')
  } finally {
    resetting.value = false
  }
}

async function handleDelete(row: AdminUser) {
  try {
    await adminDeleteUser(row.id)
    ElMessage.success('已删除')
    await loadUsers()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

onMounted(loadUsers)
</script>

<style scoped lang="scss">
.user-manage-panel {
  .panel-toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;
  }
}
</style>
