<template>
  <div class="profile-view">
    <div class="profile-container">
      <h2 class="page-title">个人中心</h2>

      <!-- 基本信息 -->
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-button text type="primary" size="small" @click="showEditDialog = true">编辑</el-button>
          </div>
        </template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userStore.user?.username }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userStore.user?.nickname || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">头像</span>
            <span class="info-value">
              <el-avatar :size="48" :src="userStore.user?.avatarUrl || undefined" :icon="User" />
            </span>
          </div>
          <div class="info-item" v-if="userStore.user?.createdAt">
            <span class="info-label">注册时间</span>
            <span class="info-value">{{ formatDate(userStore.user?.createdAt) }}</span>
          </div>
          <div class="info-item" v-if="userStore.user?.lastLoginAt">
            <span class="info-label">最后登录</span>
            <span class="info-value">{{ formatDate(userStore.user?.lastLoginAt) }}</span>
          </div>
        </div>
      </el-card>

      <!-- 修改密码 -->
      <el-card class="profile-card">
        <template #header>
          <span>修改密码</span>
        </template>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          label-position="left"
          style="max-width: 460px;"
          @submit.prevent="handleChangePassword"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password
              placeholder="6-20位，至少包含字母和数字" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">
              修改密码
            </el-button>
            <el-button @click="resetPasswordForm">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 账号操作 -->
      <el-card class="profile-card account-card">
        <template #header>
          <span>账号操作</span>
        </template>
        <div class="account-actions">
          <el-button type="danger" plain :icon="SwitchButton" @click="handleLogout">
            退出登录
          </el-button>
          <span class="logout-tip">退出后需重新登录才能继续使用</span>
        </div>
      </el-card>
    </div>

    <!-- 编辑昵称/头像对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑个人信息" width="420px">
      <el-form :model="editForm" label-position="top" @submit.prevent="handleUpdateProfile">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="输入新昵称（最长64位）" maxlength="64" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatarUrl" placeholder="输入头像图片URL（最长255位）" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="updatingProfile" @click="handleUpdateProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import * as authApi from '@/api/auth'

const userStore = useUserStore()
const router = useRouter()

const showEditDialog = ref(false)
const updatingProfile = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()

const editForm = reactive({
  nickname: '',
  avatarUrl: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/,
      message: '密码需为6-20位且包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 加载最新用户信息
onMounted(async () => {
  try {
    const res = await authApi.getCurrentUser()
    userStore.updateUserInfo(res.data.data)
  } catch {
    // 如果获取失败，使用缓存中的信息
  }
})

// 打开编辑对话框时初始化表单
function initEditForm() {
  editForm.nickname = userStore.user?.nickname || ''
  editForm.avatarUrl = userStore.user?.avatarUrl || ''
}

// 当对话框打开时初始化数据
watch(showEditDialog, (val) => {
  if (val) initEditForm()
})

async function handleUpdateProfile() {
  if (!editForm.nickname.trim() && !editForm.avatarUrl.trim()) {
    ElMessage.warning('至少需要提供昵称或头像URL')
    return
  }
  updatingProfile.value = true
  try {
    const res = await authApi.updateUser({
      nickname: editForm.nickname.trim() || undefined,
      avatarUrl: editForm.avatarUrl.trim() || undefined,
    })
    userStore.updateUserInfo(res.data.data)
    ElMessage.success('更新成功')
    showEditDialog.value = false
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '更新失败')
  } finally {
    updatingProfile.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 清除 Token 并跳转登录页
    userStore.clearState()
    resetPasswordForm()
    router.push('/login')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
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
  try {
    await authApi.logout()
  } catch {
    // 接口失败也照常清理本地状态
  }
  userStore.clearState()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.profile-view {
  height: 100%;
  overflow-y: auto;
  background: #fafbfc;
}

.profile-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 28px 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
}

.profile-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.account-card {
  .account-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .logout-tip {
      font-size: 12px;
      color: #909399;
    }
  }
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;

  .info-label {
    width: 80px;
    font-size: 14px;
    color: #909399;
    flex-shrink: 0;
  }

  .info-value {
    font-size: 14px;
    color: #303133;
  }
}
</style>
