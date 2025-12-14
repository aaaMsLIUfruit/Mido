<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  login,
  register,
  type AuthResponse,
  type RegisterRequest,
  type LoginRequest,
} from '../api/auth'

interface LoginForm extends LoginRequest {}

interface RegisterForm extends RegisterRequest {}

const router = useRouter()
const logo = '/logo.png'

const typingLines = ['你的AI工作空间', '创建登录属于你的Mido账号']
const displayLines = ref<string[]>(typingLines.map(() => ''))
const activeLine = ref(0)
const activeChar = ref(0)
let typingTimer: ReturnType<typeof setTimeout> | null = null

const startTyping = () => {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }

  displayLines.value = typingLines.map(() => '')
  activeLine.value = 0
  activeChar.value = 0

  const typeLine = (lineIndex: number) => {
    const text = typingLines[lineIndex] ?? ''
    let charIndex = 0

    const typeChar = () => {
      activeLine.value = lineIndex
      activeChar.value = charIndex

      const next = [...displayLines.value]
      next[lineIndex] = text.slice(0, charIndex)
      displayLines.value = next

      if (charIndex < text.length) {
        charIndex++
        typingTimer = setTimeout(typeChar, 120)
      } else if (lineIndex + 1 < typingLines.length) {
        typingTimer = setTimeout(() => typeLine(lineIndex + 1), 600)
      } else {
        activeLine.value = -1
        typingTimer = setTimeout(startTyping, 2000)
      }
    }

    typeChar()
  }

  typeLine(0)
}

const stopTyping = () => {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
}

onMounted(startTyping)
onBeforeUnmount(stopTyping)

const activeTab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive<LoginForm>({
  identifier: '',
  password: '',
})

const registerForm = reactive<RegisterForm>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const loginRules: FormRules<LoginForm> = {
  identifier: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: FormRules<RegisterForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    {
      required: true,
      message: '请确认密码',
      trigger: 'blur',
    },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// Base64 URL 解码并正确处理 UTF-8 编码的中文字符
const base64UrlDecode = (str: string): string => {
  // 将 base64url 转换为 base64
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  
  // 添加必要的 padding
  while (base64.length % 4) {
    base64 += '='
  }
  
  try {
    // 使用 atob 解码 base64
    const binaryString = atob(base64)
    
    // 将 Latin-1 字符串转换为 UTF-8
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i)
    }
    
    // 使用 TextDecoder 将 UTF-8 bytes 解码为字符串
    return new TextDecoder('utf-8').decode(bytes)
  } catch (error) {
    console.error('Base64 解码失败:', error)
    throw error
  }
}

const saveAuthPayload = (response: AuthResponse) => {
  const token = response.token ?? response.data?.token
  if (!token) {
    throw new Error(response.message || '操作失败，请稍后重试')
  }
  localStorage.setItem('token', token)
  try {
    const parts = token.split('.')
    if (parts.length >= 2 && parts[1]) {
      const decoded = base64UrlDecode(parts[1])
      const payload = JSON.parse(decoded)
      if (payload?.sub) {
        localStorage.setItem('username', payload.sub)
      }
      if (payload?.email) {
        localStorage.setItem('email', payload.email)
      }
    }
  } catch (error) {
    console.error('解析 token 失败:', error)
    // ignore decode errors
  }
}

const redirectHome = () => {
  router.replace('/')
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
    loginLoading.value = true
    const response = await login({ ...loginForm })
    saveAuthPayload(response)
    ElMessage.success('登录成功')
    redirectHome()
  } catch (error) {
    if (error instanceof Error && error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  try {
    await registerFormRef.value.validate()
    registerLoading.value = true
    const response = await register({ ...registerForm })
    saveAuthPayload(response)
    ElMessage.success('注册成功')
    redirectHome()
  } catch (error) {
    if (error instanceof Error && error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <div class="login-layout">
    <div class="hero-panel">
      <div class="brand">
        <img :src="logo" alt="Mido Logo" />
        <span>Mido</span>
      </div>
      <div class="typing-box">
        <p class="typing-text">
          <span class="typing-line">
            {{ displayLines[0] ?? '' }}
            <span
              v-if="
                activeLine === 0 &&
                activeChar <= (typingLines[0]?.length ?? 0)
              "
              class="cursor"
              >|</span
            >
          </span>
        </p>
        <p class="typing-text">
          <span class="typing-line">
            {{ displayLines[1] ?? '' }}
            <span
              v-if="
                activeLine === 1 &&
                activeChar <= (typingLines[1]?.length ?? 0)
              "
              class="cursor"
              >|</span
            >
          </span>
        </p>
      </div>
    </div>
    <div class="form-panel">
      <div class="form-card">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              size="large"
              @keyup.enter="handleLogin"
            >
              <el-form-item label="用户名或邮箱" prop="identifier">
                <el-input
                  v-model.trim="loginForm.identifier"
                  autocomplete="username"
                  placeholder="请输入用户名或邮箱"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model.trim="loginForm.password"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入密码"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="loginLoading"
                  @click="handleLogin"
                >
                  立即登录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              size="large"
              @keyup.enter="handleRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model.trim="registerForm.username"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input
                  v-model.trim="registerForm.email"
                  type="email"
                  autocomplete="email"
                  placeholder="请输入邮箱"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model.trim="registerForm.password"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请输入密码"
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model.trim="registerForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请再次输入密码"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="registerLoading"
                  @click="handleRegister"
                >
                  立即注册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-layout {
  min-height: 100vh;
  width: 100vw;
  max-width: 100%;
  display: grid;
  grid-template-columns: minmax(480px, 1.5fr) minmax(420px, 1fr);
  background: linear-gradient(135deg, #dff3ff 0%, #f5f9ff 100%);
}

.hero-panel {
  position: relative;
  padding: 48px;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, #dceeff 0%, #f2faff 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
  font-weight: 600;
  position: absolute;
  top: 32px;
  left: 48px;
}

.brand img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.typing-box {
  flex: 1;
  width: 100%;
  max-width: 640px;
  padding: 40px 40px 48px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 24px;
  margin: 0 auto;
  text-align: left;
}

.typing-text {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.08em;
  margin: 0;
}

.typing-line {
  min-height: 1.3em;
  display: inline-block;
  white-space: nowrap;
}

.cursor {
  display: inline-block;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

.form-panel {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 48px 32px;
  background: linear-gradient(135deg, #e1f3ff 0%, #f5f9ff 100%);
}

.form-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 18px;
  padding: 40px 32px;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.08);
}

.submit-btn {
  width: 100%;
}

@media (max-width: 768px) {
  .login-layout {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    padding: 32px 24px 12px;
    text-align: center;
    align-items: center;
  }

  .brand {
    position: static;
    margin-bottom: 24px;
  }

  .typing-text {
    font-size: 28px;
  }

  .form-panel {
    padding: 24px;
  }

  .form-card {
    padding: 28px 20px;
  }
}
</style>


