<template>
  <div class="login-container">
    <div class="login-header">
      <h1 class="logo">小红书</h1>
      <p class="subtitle">欢迎回来，登录后发现更多精彩内容</p>
    </div>

    <div class="login-form-container">
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label class="form-label">手机号</label>
          <div class="phone-input-group">
            <select v-model="phonePrefix" class="phone-prefix">
              <option value="+86">+86 中国大陆</option>
              <option value="+852">+852 中国香港</option>
              <option value="+886">+886 中国台湾</option>
              <option value="+853">+853 中国澳门</option>
            </select>
            <input
              type="tel"
              v-model="phoneNumber"
              placeholder="请输入手机号"
              class="phone-input"
              maxlength="11"
              @input="validatePhone"
            />
          </div>
          <p v-if="phoneError" class="error-message">{{ phoneError }}</p>
        </div>

        <div class="form-group">
          <label class="form-label">验证码</label>
          <div class="verify-group">
            <input
              type="text"
              v-model="verifyCode"
              placeholder="请输入验证码"
              class="verify-input"
              maxlength="6"
            />
            <button
              type="button"
              @click="sendVerifyCode"
              :disabled="!canSendCode || sendingCode"
              class="send-code-btn"
              :class="{ disabled: !canSendCode || sendingCode }"
            >
              {{ sendingCode ? '发送中...' : countDown > 0 ? `${countDown}s` : '获取验证码' }}
            </button>
          </div>
          <p v-if="codeError" class="error-message">{{ codeError }}</p>
        </div>

        <button
          type="submit"
          :disabled="!canLogin || loggingIn"
          class="login-btn"
          :class="{ disabled: !canLogin || loggingIn }"
        >
          {{ loggingIn ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="login-tips">
        <p>未注册的手机号验证通过后将自动注册</p>
        <p>登录即表示同意 <a href="#" class="link">用户协议</a> 和 <a href="#" class="link">隐私政策</a></p>
      </div>

      <div class="other-login-methods">
        <p class="divider"><span>其他登录方式</span></p>
        <div class="social-login">
          <button @click="wechatLogin" class="social-btn wechat">
            <span class="icon">微</span> 微信登录
          </button>
          <button @click="qqLogin" class="social-btn qq">
            <span class="icon">Q</span> QQ登录
          </button>
        </div>
      </div>
    </div>

    <div class="back-to-home">
      <button @click="goBack" class="back-btn">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useModal } from '@/utils/modal'

// 手机号相关
const phonePrefix = ref('+86')
const phoneNumber = ref('')
const phoneError = ref('')

// 使用路由和用户状态
const router = useRouter()
const userStore = useUserStore()
const { showAlert } = useModal()

// 验证码相关
const verifyCode = ref('')
const codeError = ref('')
const sendingCode = ref(false)
const countDown = ref(0)

// 登录状态
const loggingIn = ref(false)

// 验证手机号
const validatePhone = () => {
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneNumber.value) {
    phoneError.value = '请输入手机号'
    return false
  } else if (!phoneRegex.test(phoneNumber.value)) {
    phoneError.value = '请输入正确的手机号'
    return false
  } else {
    phoneError.value = ''
    return true
  }
}

// 是否可以发送验证码
const canSendCode = computed(() => {
  return phoneNumber.value.length === 11 && !phoneError.value && countDown.value === 0
})

// 是否可以登录
const canLogin = computed(() => {
  return phoneNumber.value.length === 11 && verifyCode.value.length === 6 && !phoneError.value
})

// 发送验证码
const sendVerifyCode = async () => {
  if (!validatePhone()) return

  sendingCode.value = true

  try {
    const result = await userStore.sendVerifyCode(phonePrefix.value + phoneNumber.value)

    if (result.success) {
      await showAlert(result.message, '成功')
      startCountDown()
    } else {
      if (!result.isHandled) {
        await showAlert(result.message, '错误')
      }
    }
  } catch (error) {
    if (!error.isHandled) {
      await showAlert('发送失败，请重试', '错误')
    }
  } finally {
    sendingCode.value = false
  }
}

// 倒计时
const startCountDown = () => {
  countDown.value = 60
  const timer = setInterval(() => {
    countDown.value--
    if (countDown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 处理登录
const handleLogin = async () => {
  console.log('登录按钮被点击')
  console.log('手机号:', phoneNumber.value)
  console.log('验证码:', verifyCode.value)
  console.log('是否可以登录:', canLogin.value)

  if (!validatePhone()) {
    console.log('手机号验证失败')
    return
  }

  if (verifyCode.value.length !== 6) {
    codeError.value = '请输入6位验证码'
    console.log('验证码长度不正确')
    return
  }

  codeError.value = ''
  loggingIn.value = true

  try {
    console.log('开始调用登录API')
    const result = await userStore.login(phonePrefix.value + phoneNumber.value, verifyCode.value)
    console.log('登录结果:', result)

    if (result.success) {
      await showAlert(result.message, '欢迎')
      resetForm()
      // 返回首页
      router.push('/')
    } else {
      if (!result.isHandled) {
        await showAlert(result.message, '登录失败')
      }
    }
  } catch (error) {
    console.error('登录错误:', error)
    if (!error.isHandled) {
      await showAlert('登录失败，请检查验证码', '错误')
    }
  } finally {
    loggingIn.value = false
  }
}

// 重置表单
const resetForm = () => {
  phoneNumber.value = ''
  verifyCode.value = ''
  phoneError.value = ''
  codeError.value = ''
}

// 返回首页
const goBack = () => {
  // 这里应该是路由返回
  window.history.back()
}

// 其他登录方式
const wechatLogin = async () => {
  await showAlert('微信登录功能开发中...', '提示')
}

const qqLogin = async () => {
  await showAlert('QQ登录功能开发中...', '提示')
}

// 验证输入
const validateInput = () => {
  if (phoneNumber.value.length === 11) {
    validatePhone()
  }

  if (verifyCode.value.length > 6) {
    verifyCode.value = verifyCode.value.slice(0, 6)
  }
}

// 监听输入变化
watch([phoneNumber, verifyCode], validateInput)
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  color: #ff2442;
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 16px;
}

.login-form-container {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.phone-input-group {
  display: flex;
  gap: 10px;
}

.phone-prefix {
  width: 100px;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: white;
}

.phone-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s ease;
}

.phone-input:focus {
  border-color: #ff2442;
}

.verify-group {
  display: flex;
  gap: 10px;
}

.verify-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s ease;
}

.verify-input:focus {
  border-color: #ff2442;
}

.send-code-btn {
  width: 120px;
  padding: 12px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.send-code-btn:hover:not(.disabled) {
  background: #e01e3a;
}

.send-code-btn.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-btn {
  padding: 14px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s ease;
  margin-top: 10px;
}

.login-btn:hover:not(.disabled) {
  background: #e01e3a;
}

.login-btn.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #ff2442;
  font-size: 12px;
  margin-top: 4px;
}

.login-tips {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
}

.other-login-methods {
  margin-top: 30px;
}

.divider {
  text-align: center;
  position: relative;
  margin-bottom: 20px;
}

.divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: #eee;
}

.divider span {
  background: white;
  padding: 0 10px;
  color: #999;
  font-size: 12px;
  position: relative;
  z-index: 1;
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.social-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  background: white;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.social-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: white;
  font-weight: bold;
}

.wechat .icon {
  background: #07c160;
}

.qq .icon {
  background: #12b7f5;
}

.back-to-home {
  margin-top: 30px;
}

.back-btn {
  background: none;
  border: none;
  color: #999;
  font-size: 14px;
  cursor: pointer;
}

.back-btn:hover {
  color: #666;
}
</style>
