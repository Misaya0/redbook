<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <header style="background: #fff; border-bottom: 1px solid #e5e5e5; padding: 15px 20px; position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
      <div style="max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; align-items: center; gap: 30px;">
          <h1 style="color: #ff2442; font-size: 24px; font-weight: bold;">小红书</h1>
          <nav style="display: flex; gap: 20px;">
            <router-link to="/" style="text-decoration: none; color: #666; font-weight: 500;">探索</router-link>
            <router-link to="/follow" style="text-decoration: none; color: #666; font-weight: 500;">关注</router-link>
            <router-link to="/message" style="text-decoration: none; color: #666; font-weight: 500;">消息</router-link>
          </nav>
        </div>

        <div style="display: flex; align-items: center; gap: 15px;">
          <div style="position: relative;">
            <input
              type="text"
              placeholder="搜索用户、笔记、商品"
              style="width: 300px; padding: 8px 15px; border: 1px solid #e5e5e5; border-radius: 20px; outline: none; font-size: 14px;"
            />
          </div>

          <!-- 未登录状态 -->
          <div v-if="!userStore.isLoggedIn" style="display: flex; gap: 10px;">
            <button
              @click="goToLogin"
              style="background: #ff2442; color: white; border: none; padding: 8px 20px; border-radius: 20px; cursor: pointer; font-size: 14px; font-weight: 500; transition: background 0.3s ease;"
              @mouseenter="$event.target.style.background = '#e01e3a'"
              @mouseleave="$event.target.style.background = '#ff2442'"
            >
              登录
            </button>
            <button
              @click="goToLogin"
              style="background: transparent; color: #ff2442; border: 1px solid #ff2442; padding: 8px 20px; border-radius: 20px; cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.3s ease;"
              @mouseenter="$event.target.style.background = '#ff2442'; $event.target.style.color = 'white'"
              @mouseleave="$event.target.style.background = 'transparent'; $event.target.style.color = '#ff2442'"
            >
              注册
            </button>
          </div>

          <!-- 已登录状态 -->
          <div v-else style="display: flex; align-items: center; gap: 15px;">
            <button
              @click="$router.push('/publish')"
              style="background: #ff2442; color: white; border: none; padding: 8px 16px; border-radius: 20px; cursor: pointer; font-size: 14px; font-weight: 500; transition: background 0.3s ease; display: flex; align-items: center; gap: 5px;"
              @mouseenter="$event.target.style.background = '#e01e3a'"
              @mouseleave="$event.target.style.background = '#ff2442'"
            >
              <span style="font-size: 16px;">+</span> 发布
            </button>

            <div style="display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 5px 10px; border-radius: 20px; transition: background 0.3s ease;"
                 @mouseenter="$event.target.style.background = '#f8f8f8'"
                 @mouseleave="$event.target.style.background = 'transparent'"
                 @click="$router.push('/profile')">
              <img
                :src="userStore.userInfo?.image || 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U'"
                alt="用户头像"
                style="width: 32px; height: 32px; border-radius: 50%;"
              />
              <span style="font-size: 14px; color: #333; font-weight: 500;">{{ userStore.userInfo?.nickname || '用户' }}</span>
            </div>

            <button
              @click="handleLogout"
              style="background: transparent; color: #999; border: 1px solid #e5e5e5; padding: 6px 12px; border-radius: 16px; cursor: pointer; font-size: 12px; transition: all 0.3s ease;"
              @mouseenter="$event.target.style.borderColor = '#ff2442'; $event.target.style.color = '#ff2442'"
              @mouseleave="$event.target.style.borderColor = '#e5e5e5'; $event.target.style.color = '#999'"
            >
              退出
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- 页面内容区域 -->
    <router-view />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 用户信息
const userInfo = ref(userStore.userInfo)

// 检查登录状态
const checkLoginStatus = () => {
  userInfo.value = userStore.userInfo
}

// 登录跳转函数
const goToLogin = () => {
  router.push('/login')
}

// 退出登录
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    userStore.logout()
    userInfo.value = null
    router.push('/')
  }
}

// 初始化
onMounted(() => {
  userStore.initUser()
  checkLoginStatus()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f8f8f8;
}

#app {
  min-height: 100vh;
}

/* 路由链接激活状态 */
.router-link-active {
  color: #ff2442 !important;
  font-weight: 600 !important;
}
</style>