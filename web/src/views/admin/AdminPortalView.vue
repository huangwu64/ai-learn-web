<template>
  <div class="admin-portal">
    <!-- 未登录 → 管理员登录 -->
    <div v-if="!isAdminLoggedIn" class="admin-login">
      <div class="admin-card">
        <div class="admin-logo">⚙️</div>
        <h1 class="admin-title">管理员登录</h1>
        <p class="admin-subtitle">AI 模型与接口配置管理</p>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入管理员账号"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keydown.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            block
            :loading="loggingIn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <div class="admin-footer">
          <el-link type="info" @click="$router.push('/')">返回用户端</el-link>
        </div>
      </div>
    </div>

    <!-- 已登录 → AI 配置 -->
    <div v-else class="admin-config">
      <div class="config-header">
        <h2 class="config-title">AI 模型与接口配置</h2>
        <div class="config-header-actions">
          <el-button text type="info" :icon="Back" @click="$router.push('/')">返回用户端</el-button>
          <el-button text type="danger" :icon="SwitchButton" @click="handleLogout">退出管理</el-button>
        </div>
      </div>

      <el-form
        ref="configFormRef"
        :model="configForm"
        label-width="150px"
        label-position="left"
        class="config-form"
      >
        <el-card class="config-card" shadow="never">
          <template #header><span>接口接入</span></template>
          <el-form-item label="API 基础地址">
            <el-input v-model="configForm.apiBaseUrl" placeholder="如 https://api.deepseek.com" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input
              v-model="configForm.apiKey"
              type="password"
              show-password
              :placeholder="keyPlaceholder"
            />
            <div class="field-tip">留空表示不修改；已配置：{{ config.hasApiKey ? config.apiKeyMasked : '无' }}</div>
          </el-form-item>
          <el-form-item label="模型编码">
            <el-select
              v-model="configForm.modelCode"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入模型编码"
              style="width: 100%"
            >
              <el-option label="deepseek-chat（V4 通用对话）" value="deepseek-chat" />
              <el-option label="deepseek-reasoner（V4 推理）" value="deepseek-reasoner" />
              <el-option label="deepseek-v4-pro（自定义）" value="deepseek-v4-pro" />
              <el-option label="deepseek-v4-flash（自定义）" value="deepseek-v4-flash" />
            </el-select>
            <div class="field-tip">可自由输入你 API 支持的任何模型编码</div>
          </el-form-item>
        </el-card>

        <el-card class="config-card" shadow="never">
          <template #header><span>模型参数</span></template>
          <el-form-item label="最大输出 Token">
            <el-input-number v-model="configForm.maxTokens" :min="1" :max="32768" :step="512" controls-position="right" />
          </el-form-item>
          <el-form-item label="温度 Temperature">
            <el-input-number v-model="configForm.temperature" :min="0" :max="2" :step="0.1" controls-position="right" />
            <div class="field-tip">0~2，越高越随机</div>
          </el-form-item>
          <el-form-item label="核采样 Top P">
            <el-input-number v-model="configForm.topP" :min="0" :max="1" :step="0.05" controls-position="right" />
          </el-form-item>
          <el-form-item label="话题新鲜度惩罚">
            <el-input-number v-model="configForm.presencePenalty" :min="-2" :max="2" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="频率惩罚">
            <el-input-number v-model="configForm.frequencyPenalty" :min="-2" :max="2" :step="0.1" controls-position="right" />
          </el-form-item>
        </el-card>

        <el-card class="config-card" shadow="never">
          <template #header><span>初始提示词（System Prompt）</span></template>
          <el-input
            v-model="configForm.systemPrompt"
            type="textarea"
            :rows="5"
            placeholder="设置 AI 的初始提示词，训练场景能力可在此定义"
          />
        </el-card>

        <div class="config-actions">
          <el-button type="primary" size="large" :icon="Check" :loading="saving" @click="handleSave">
            保存配置
          </el-button>
          <el-button size="large" :icon="Connection" :loading="testing" @click="handleTest">
            测试连接
          </el-button>
        </div>

        <div v-if="config.updatedAt" class="config-updated">
          最近更新：{{ config.updatedAt }}
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, SwitchButton, Back, Check, Connection } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { isAdminLoggedIn, setAdminToken, clearAdminToken } from '@/utils/adminAuth'
import { adminLogin, adminLogout, getAiConfig, updateAiConfig, testAiConfig } from '@/api/admin'
import type { AiConfig } from '@/types/admin'

const router = useRouter()

const loginFormRef = ref()
const configFormRef = ref()
const loggingIn = ref(false)
const saving = ref(false)
const testing = ref(false)

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/** AI 配置表单 */
const configForm = reactive({
  apiBaseUrl: '',
  apiKey: '',
  modelCode: '',
  maxTokens: 4096 as number | null,
  temperature: 0.7 as number | null,
  topP: 1.0 as number | null,
  presencePenalty: 0.0 as number | null,
  frequencyPenalty: 0.0 as number | null,
  systemPrompt: '',
})

/** 从后端返回的脱敏配置（展示用） */
const config = reactive<AiConfig>({
  providerCode: 'deepseek',
  apiBaseUrl: '',
  apiKeyMasked: '',
  hasApiKey: false,
  modelCode: '',
  maxTokens: null,
  temperature: null,
  topP: null,
  presencePenalty: null,
  frequencyPenalty: null,
  systemPrompt: '',
  updatedAt: '',
})

const keyPlaceholder = computed(() => {
  return config.hasApiKey ? `${config.apiKeyMasked}（留空表示不修改）` : '请输入 API Key'
})

/** 管理员登录 */
async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loggingIn.value = true
  try {
    const res = await adminLogin({ username: loginForm.username, password: loginForm.password })
    setAdminToken(res.data.data.token)
    ElMessage.success('登录成功')
    await loadConfig()
  } catch (err: any) {
    // 错误提示由拦截器处理
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loggingIn.value = false
  }
}

/** 退出管理 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出管理后台吗？', '退出管理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await adminLogout()
  } catch {
    // 忽略
  }
  clearAdminToken()
  ElMessage.success('已退出管理')
}

/** 加载 AI 配置 */
async function loadConfig() {
  try {
    const res = await getAiConfig()
    const data = res.data.data
    Object.assign(config, data)
    configForm.apiBaseUrl = data.apiBaseUrl || ''
    configForm.apiKey = ''
    configForm.modelCode = data.modelCode || ''
    configForm.maxTokens = data.maxTokens ?? 4096
    configForm.temperature = data.temperature ?? 0.7
    configForm.topP = data.topP ?? 1.0
    configForm.presencePenalty = data.presencePenalty ?? 0.0
    configForm.frequencyPenalty = data.frequencyPenalty ?? 0.0
    configForm.systemPrompt = data.systemPrompt || ''
  } catch {
    // 已由拦截器提示
  }
}

/** 保存配置 */
async function handleSave() {
  saving.value = true
  try {
    await updateAiConfig({
      apiBaseUrl: configForm.apiBaseUrl.trim() || undefined,
      apiKey: configForm.apiKey.trim() || undefined,
      modelCode: configForm.modelCode.trim() || undefined,
      maxTokens: configForm.maxTokens,
      temperature: configForm.temperature,
      topP: configForm.topP,
      presencePenalty: configForm.presencePenalty,
      frequencyPenalty: configForm.frequencyPenalty,
      systemPrompt: configForm.systemPrompt,
    })
    ElMessage.success('AI 配置已保存并生效')
    configForm.apiKey = ''
    await loadConfig()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 测试连接 */
async function handleTest() {
  testing.value = true
  try {
    await testAiConfig({
      apiBaseUrl: configForm.apiBaseUrl.trim() || undefined,
      apiKey: configForm.apiKey.trim() || undefined,
      modelCode: configForm.modelCode.trim() || undefined,
    })
    ElMessage.success('连接成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '连接失败')
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  if (isAdminLoggedIn.value) {
    loadConfig()
  }
})
</script>

<style scoped lang="scss">
.admin-portal {
  min-height: 100vh;
  background: linear-gradient(135deg, #2b3a67 0%, #1d2440 100%);
}

/* 登录视图 */
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.admin-logo {
  text-align: center;
  font-size: 40px;
}

.admin-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 12px 0 4px;
}

.admin-subtitle {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin: 0 0 24px;
}

.admin-footer {
  text-align: center;
  margin-top: 16px;
}

/* 配置视图 */
.admin-config {
  max-width: 720px;
  margin: 0 auto;
  padding: 28px 20px 60px;
}

.config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .config-title {
    font-size: 20px;
    font-weight: 600;
    color: #fff;
    margin: 0;
  }

  .config-header-actions {
    display: flex;
    gap: 4px;
  }
}

.config-card {
  margin-bottom: 16px;

  .field-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }
}

.config-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.config-updated {
  margin-top: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
}
</style>
