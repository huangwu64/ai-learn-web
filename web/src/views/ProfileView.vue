<template>
  <div class="profile-view">
    <div class="profile-container">
      <h2 class="page-title">个人中心</h2>

      <!-- 基本信息 -->
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userStore.user?.username }}</span>
            <el-button text type="primary" size="small" @click="openProfileDialog">修改</el-button>
          </div>
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userStore.user?.nickname || '-' }}</span>
            <el-button text type="primary" size="small" @click="openProfileDialog">修改</el-button>
          </div>
          <div class="info-item">
            <span class="info-label">头像</span>
            <div class="avatar-row">
              <el-avatar :size="48" :src="displayAvatar" :icon="User" />
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                accept="image/png,image/jpeg,image/gif,image/webp"
                :on-change="handleAvatarFile"
              >
                <el-button :icon="Upload" size="small">上传新头像</el-button>
              </el-upload>
              <el-tag v-if="pendingAvatar" type="warning" size="small">头像待审核</el-tag>
            </div>
            <div class="field-tip">头像上传后需管理员审核，审核通过后才更新</div>
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

      <!-- 审核状态 -->
      <el-card class="profile-card">
        <template #header>
          <span>资料变更审核状态</span>
        </template>
        <el-table :data="myRequests" v-loading="loadingRequests" size="small" border>
          <el-table-column label="变更项" width="90">
            <template #default="{ row }">{{ fieldLabel(row.fieldName) }}</template>
          </el-table-column>
          <el-table-column label="内容" min-width="160">
            <template #default="{ row }">
              <span v-if="row.fieldName === 'avatar'" class="req-avatar">
                <el-avatar :size="24" :src="row.newValue" />
                {{ row.newValue }}
              </span>
              <span v-else>{{ row.newValue }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="170" />
        </el-table>
        <el-empty v-if="!loadingRequests && myRequests.length === 0" description="暂无变更申请" :image-size="60" />
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

    <!-- 修改资料对话框（走审核） -->
    <el-dialog v-model="showProfileDialog" title="修改资料（需管理员审核）" width="440px">
      <el-form label-position="top">
        <el-form-item label="新用户名（ID）">
          <el-input v-model="profileForm.username" placeholder="留空表示不修改" maxlength="64" />
        </el-form-item>
        <el-form-item label="新昵称">
          <el-input v-model="profileForm.nickname" placeholder="留空表示不修改" maxlength="64" />
        </el-form-item>
      </el-form>
      <div class="field-tip">提交后需管理员审核通过才会生效。</div>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitProfile">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import * as authApi from '@/api/auth'
import type { ProfileChangeRequest } from '@/types/admin'

const userStore = useUserStore()
const router = useRouter()

const loadingRequests = ref(false)
const myRequests = ref<ProfileChangeRequest[]>([])
const pendingAvatar = ref(false)
const showProfileDialog = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()

const profileForm = reactive({ username: '', nickname: '' })

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const displayAvatar = computed(() => {
  if (pendingAvatar.value && previewAvatarUrl.value) return previewAvatarUrl.value
  return userStore.user?.avatarUrl || undefined
})
const previewAvatarUrl = ref('')

const FIELD_LABELS: Record<string, string> = { avatar: '头像', nickname: '昵称', username: '用户名' }
function fieldLabel(f: string) { return FIELD_LABELS[f] || f }

function statusLabel(s: number) { return s === 0 ? '待审核' : s === 1 ? '已通过' : '已拒绝' }
function statusTagType(s: number) { return s === 0 ? 'warning' : s === 1 ? 'success' : 'info' }

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/, message: '密码需为6-20位且包含字母和数字', trigger: 'blur' },
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

async function loadMyRequests() {
  loadingRequests.value = true
  try {
    const res = await authApi.listMyProfileChanges()
    myRequests.value = res.data.data || []
    pendingAvatar.value = myRequests.value.some(r => r.fieldName === 'avatar' && r.status === 0)
  } catch {
    // 拦截器已提示
  } finally {
    loadingRequests.value = false
  }
}

/** 选择头像文件后直接上传（进入审核） */
async function handleAvatarFile(file: any) {
  const raw = file.raw as File
  if (!raw) return
  if (!['image/png', 'image/jpeg', 'image/gif', 'image/webp'].includes(raw.type)) {
    ElMessage.warning('仅支持 png/jpg/gif/webp 图片')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  previewAvatarUrl.value = URL.createObjectURL(raw)
  uploading.value = true
  try {
    const res = await authApi.uploadAvatar(raw)
    pendingAvatar.value = true
    ElMessage.success('头像已上传，等待管理员审核')
    await loadMyRequests()
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function openProfileDialog() {
  profileForm.username = ''
  profileForm.nickname = ''
  showProfileDialog.value = true
}

async function handleSubmitProfile() {
  if (!profileForm.username.trim() && !profileForm.nickname.trim()) {
    ElMessage.warning('请至少填写一项')
    return
  }
  submitting.value = true
  try {
    if (profileForm.username.trim()) {
      await authApi.submitProfileChange({ fieldName: 'username', newValue: profileForm.username.trim() })
    }
    if (profileForm.nickname.trim()) {
      await authApi.submitProfileChange({ fieldName: 'nickname', newValue: profileForm.nickname.trim() })
    }
    ElMessage.success('已提交审核，请等待管理员审核')
    showProfileDialog.value = false
    await loadMyRequests()
  } catch (err: any) {
    ElMessage.error(err?.message || '提交失败')
  } finally {
    submitting.value = false
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

onMounted(async () => {
  try {
    const res = await authApi.getCurrentUser()
    userStore.updateUserInfo(res.data.data)
  } catch {
    // 使用缓存
  }
  loadMyRequests()
})
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

  .field-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
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

  .avatar-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.req-avatar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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
</style>
