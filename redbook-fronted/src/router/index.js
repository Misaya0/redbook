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
import LikeCollect from '@/views/Message/LikeCollect.vue'
import NewFollow from '@/views/Message/NewFollow.vue'
import CommentAt from '@/views/Message/CommentAt.vue'
import ProductList from '@/views/Product/ProductList.vue'
import ProductDetail from '@/views/Product/ProductDetail.vue'
import ProductManage from '@/views/Merchant/ProductManage.vue'
import MerchantProfile from '@/views/Merchant/MerchantProfile.vue'
import ProductPublish from '@/views/Merchant/ProductPublish.vue'
import OrderList from '@/views/Order/OrderList.vue'
import OrderListMerchant from '@/views/Order/OrderListMerchant.vue'
import NoteDetail from '@/views/NoteDetail.vue'
import ShopDetail from '@/views/shop/ShopDetail.vue'

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
    path: '/products',
    name: 'ProductList',
    component: ProductList,
    meta: { requiresAuth: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: ProductDetail
  },
  {
    path: '/merchant/products',
    name: 'ProductManage',
    component: ProductManage,
    meta: { requiresAuth: true }
  },
  {
    path: '/merchant/profile',
    name: 'MerchantProfile',
    component: MerchantProfile,
    meta: { requiresAuth: true }
  },
  {
    path: '/merchant/publish',
    name: 'ProductPublish',
    component: ProductPublish,
    meta: { requiresAuth: true }
  },
  {
    path: '/merchant/orders',
    name: 'OrderListMerchant',
    component: OrderListMerchant,
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: OrderList,
    meta: { requiresAuth: true }
  },
  {
    path: '/message',
    name: 'Message',
    component: MessageList,
    meta: { requiresAuth: true }
  },
  {
    path: '/message/like-collect',
    name: 'LikeCollect',
    component: LikeCollect,
    meta: { requiresAuth: true }
  },
  {
    path: '/message/new-follow',
    name: 'NewFollow',
    component: NewFollow,
    meta: { requiresAuth: true }
  },
  {
    path: '/message/comment-at',
    name: 'CommentAt',
    component: CommentAt,
    meta: { requiresAuth: true }
  },
  {
    path: '/note/:id',
    name: 'NoteDetail',
    component: NoteDetail
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
    path: '/shop/:id',
    name: 'ShopDetail',
    component: ShopDetail
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

  // 如果已登录但 userInfo 为空，优先拉取一次用户信息，便于后续基于角色做路由分流
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch (e) {
      console.error('路由守卫获取用户信息失败', e)
    }
  }
  
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

  // 1.5 商家/用户订单页面分流：商家访问 /orders 自动跳转到 /merchant/orders
  // 同时禁止普通用户访问商家订单页
  const isMerchant = userStore.userInfo?.role === 1
  if (to.path === '/orders' && isMerchant) {
    next('/merchant/orders')
    return
  }
  if (to.path === '/merchant/orders' && !isMerchant) {
    next('/orders')
    return
  }

  // 2. 处理需要登录的路由
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
