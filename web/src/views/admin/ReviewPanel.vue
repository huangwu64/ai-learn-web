<template>
  <div class="review-panel">
    <el-tabs v-model="activeStatus" @tab-change="loadReviews">
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已拒绝" name="2" />
    </el-tabs>

    <div class="panel-toolbar">
      <el-button :icon="Refresh" :loading="loading" @click="loadReviews">刷新</el-button>
    </div>

    <el-table :data="reviews" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">{{ row.username }}{{ row.nickname ? '（' + row.nickname + '）' : '' }}</template>
      </el-table-column>
      <el-table-column label="变更字段" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="fieldTagType(row.fieldName)">{{ fieldLabel(row.fieldName) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="原值" min-width="120">
        <template #default="{ row }">
          <span class="old-value">{{ row.oldValue || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="新值" min-width="140">
        <template #default="{ row }">
          <div v-if="row.fieldName === 'avatar'" class="avatar-new">
            <el-avatar :size="32" :src="row.newValue" />
            <span class="new-value">{{ row.newValue }}</span>
          </div>
          <span v-else class="new-value">{{ row.newValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="170" />
      <el-table-column label="备注" min-width="100">
        <template #default="{ row }">{{ row.reviewRemark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button text type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button text type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else class="done-text">{{ row.status === 1 ? '已通过' : '已拒绝' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && reviews.length === 0" description="暂无审核记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminListReviews, adminApproveReview, adminRejectReview } from '@/api/admin'
import type { ProfileChangeRequest } from '@/types/admin'

const activeStatus = ref('0')
const reviews = ref<ProfileChangeRequest[]>([])
const loading = ref(false)

const FIELD_LABELS: Record<string, string> = {
  avatar: '头像',
  nickname: '昵称',
  username: '用户名',
}

function fieldLabel(f: string) {
  return FIELD_LABELS[f] || f
}

function fieldTagType(f: string) {
  if (f === 'avatar') return 'success'
  if (f === 'username') return 'warning'
  return 'info'
}

async function loadReviews() {
  loading.value = true
  try {
    const res = await adminListReviews(Number(activeStatus.value))
    reviews.value = res.data.data
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: ProfileChangeRequest) {
  try {
    await ElMessageBox.confirm(
      `确定通过「${row.username}」的${fieldLabel(row.fieldName)}变更吗？通过后将同步到用户端。`,
      '通过审核',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await adminApproveReview(row.id)
    ElMessage.success('已通过并同步')
    await loadReviews()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

async function handleReject(row: ProfileChangeRequest) {
  let remark = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝审核', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '拒绝原因',
      type: 'warning',
    })
    remark = value || ''
  } catch {
    return
  }
  try {
    await adminRejectReview(row.id, remark)
    ElMessage.success('已拒绝')
    await loadReviews()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

onMounted(loadReviews)
</script>

<style scoped lang="scss">
.review-panel {
  .panel-toolbar {
    margin-bottom: 14px;
  }

  .old-value {
    color: #c0c4cc;
  }

  .new-value {
    color: #409eff;
  }

  .avatar-new {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .done-text {
    color: #909399;
    font-size: 13px;
  }
}
</style>
