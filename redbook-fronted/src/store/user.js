import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { sendVerifyCode as sendVerifyCodeAPI, verifyLogin as verifyLoginAPI, getUserInfo as getUserInfoAPI, logout as logoutAPI } from '@/api/login'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  const login = async (phone, code) => {
    try {
      console.log('用户状态管理 - 开始登录:', { phone, code })

      // 调用后端API进行登录验证
      const response = await verifyLoginAPI(phone, code)
      console.log('用户状态管理 - 登录API响应:', response)

      // 后端返回的response经过拦截器处理后，直接就是token字符串
      if (response) {
        // 保存token
        token.value = response
        localStorage.setItem('token', response)

        // 获取完整用户信息
        await getUserInfo()

        console.log('用户状态管理 - 登录成功，用户信息:', userInfo.value)
        return { success: true, message: '登录成功' }
      } else {
        console.log('用户状态管理 - 登录失败：未获取到token')
        return { success: false, message: '登录失败：未获取到token' }
      }
    } catch (error) {
      console.error('用户状态管理 - 登录错误:', error)
      return { success: false, message: error.message || '登录失败', isHandled: error.isHandled }
    }
  }

  // 登出
  const logout = () => {
    userInfo.value = null
    token.value = ''
    localStorage.removeItem('token')
  }

  // 获取用户信息
  const getUserInfo = async () => {
    if (!token.value) return null

    try {
      // 调用后端API获取用户信息
      if (!userInfo.value) {
        const response = await getUserInfoAPI()
        userInfo.value = response
      }
      return userInfo.value
    } catch (error) {
      // 如果获取失败，清除登录状态
      logout()
      throw error
    }
  }

  // 发送验证码
  const sendVerifyCode = async (phone) => {
    try {
      // 调用后端API发送验证码
      const response = await sendVerifyCodeAPI(phone)
      return { success: true, message: '验证码已发送' }
    } catch (error) {
      return { success: false, message: error.message || '发送失败', isHandled: error.isHandled }
    }
  }

  // 初始化用户状态
  const initUser = async () => {
    if (token.value) {
      try {
        await getUserInfo()
      } catch (error) {
        console.error('初始化用户信息失败:', error)
      }
    }
  }

  return {
    // 状态
    userInfo,
    token,
    isLoggedIn,

    // 方法
    login,
    logout,
    getUserInfo,
    sendVerifyCode,
    initUser
  }
})