<template>
  <header style="background: #fff; border-bottom: 1px solid #e5e5e5; padding: 15px 20px; position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
    <div style="max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center;">
      <div style="display: flex; align-items: center; gap: 30px;">
        <h1 style="color: #ff2442; font-size: 24px; font-weight: bold; cursor: pointer;" @click="router.push('/')">小红书</h1>
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
            style="width: 300px; padding: 8px 40px 8px 15px; border: 1px solid #e5e5e5; border-radius: 20px; outline: none; font-size: 14px;"
            v-model="searchKeyword"
            @keyup.enter="handleSearch"
            @input="handleInput"
            @focus="showSuggestions = true"
            @blur="handleBlur"
          />

          <!-- 搜索按钮图标 -->
          <div 
            @click="handleSearch"
            style="position: absolute; right: 10px; top: 50%; transform: translateY(-50%); cursor: pointer; color: #666; padding: 5px; display: flex; align-items: center;"
            @mouseenter="$event.target.style.color = '#ff2442'"
            @mouseleave="$event.target.style.color = '#666'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
          
          <!-- Search Suggestions -->
          <div class="search-suggestions" v-if="showSuggestions && suggestions.length > 0">
            <div 
              v-for="(item, index) in suggestions" 
              :key="index"
              class="suggestion-item"
              @mousedown="selectSuggestion(item)"
            >
              {{ item }}
            </div>
          </div>
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
            @click="handlePublish"
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
              :src="userAvatar"
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
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useModal } from '@/utils/modal'
import { searchApi } from '@/api/search'

const router = useRouter()
const userStore = useUserStore()
const { showConfirm, showAlert } = useModal()

const searchKeyword = ref('')
const suggestions = ref([])
const showSuggestions = ref(false)

// 获取用户头像
const userAvatar = computed(() => {
  return userStore.userInfo?.image || 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U'
})

const goToLogin = () => {
  router.push('/login')
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  showSuggestions.value = false
  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value }
  })
}

const debounce = (fn, delay) => {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

const fetchSuggestions = async () => {
  if (!searchKeyword.value.trim()) {
    suggestions.value = []
    return
  }
  try {
    const res = await searchApi.getSuggestions(searchKeyword.value)
    suggestions.value = res || []
  } catch (error) {
    console.error('Fetch suggestions failed:', error)
  }
}

const handleInput = debounce(() => {
  fetchSuggestions()
}, 300)

const selectSuggestion = (item) => {
  searchKeyword.value = item
  handleSearch()
}

const handleBlur = () => {
  // Delay hiding to allow click event to trigger
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

// 退出登录
const handleLogout = async () => {
  const confirmed = await showConfirm('确定要退出登录吗？', '退出登录')
  if (confirmed) {
    userStore.logout()
    router.push('/')
  }
}

// 点击发布按钮跳转到发布页面
const handlePublish = async () => {
  if (userStore.isLoggedIn) {
    router.push('/publish')
  } else {
    await showAlert('请先登录', '提示')
    router.push('/login')
  }
}
</script>

<style scoped>
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  margin-top: 5px;
  max-height: 300px;
  overflow-y: auto;
  z-index: 1001;
}

.suggestion-item {
  padding: 10px 15px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.suggestion-item:hover {
  background: #f5f5f5;
}

/* 路由链接激活状态 */
.router-link-active {
  color: #ff2442 !important;
  font-weight: 600 !important;
}
</style>
