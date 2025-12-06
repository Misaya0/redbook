<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <Header />

    <!-- 页面内容区域 -->
    <router-view />

    <!-- 全局弹窗组件 -->
    <GlobalModal />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import GlobalModal from '@/components/GlobalModal.vue'
import Header from '@/components/Header.vue'
import { useModal } from '@/utils/modal'

const router = useRouter()
const userStore = useUserStore()
const { showConfirm } = useModal()

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
const handleLogout = async () => {
  const confirmed = await showConfirm('确定要退出登录吗？', '退出登录')
  if (confirmed) {
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