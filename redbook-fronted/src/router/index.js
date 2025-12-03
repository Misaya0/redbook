import { createRouter, createWebHistory } from 'vue-router'
import Explore from '@/views/Explore.vue'
import Follow from '@/views/Follow.vue'
import Login from '@/views/Login.vue'
import Profile from '@/views/Profile.vue'
import Publish from '@/views/Publish.vue'

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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router