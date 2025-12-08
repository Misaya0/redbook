import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import Explore from '@/views/Explore.vue'
import Follow from '@/views/Follow.vue'
import Login from '@/views/Login.vue'
import Profile from '@/views/Profile.vue'
import Publish from '@/views/Publish.vue'
import Search from '@/views/Search.vue'
import UserProfile from '@/views/UserProfile.vue'
import MessageList from '@/views/Message/MessageList.vue'
import ChatRoom from '@/views/Message/ChatRoom.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Explore
  },
  {
    path: '/explore',
    name: 'Explore',
    component: Explore
  },
  {
    path: '/message',
    name: 'Message',
    component: MessageList,
    meta: { requiresAuth: true }
  },
  {
    path: '/message/chat/:id',
    name: 'ChatRoom',
    component: ChatRoom,
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: UserProfile
  },
  {
    path: '/follow',
    name: 'Follow',
    component: Follow,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/publish',
    name: 'Publish',
    component: Publish,
    meta: { requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: Search
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 1. 处理 /user/:id 跳转到 /profile 的逻辑
  if (to.name === 'UserProfile' && to.params.id) {
    // 如果没有用户信息但有token，尝试获取用户信息
    if (!userStore.userInfo && userStore.token) {
      try {
        await userStore.getUserInfo()
      } catch (e) {
        console.error('路由守卫获取用户信息失败', e)
      }
    }
    
    const currentUserId = userStore.userInfo?.id
    // 如果目标ID等于当前用户ID，重定向到个人主页
    if (currentUserId && String(to.params.id) === String(currentUserId)) {
      next('/profile')
      return
    }
  }

  // 2. 处理需要登录的路由
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router