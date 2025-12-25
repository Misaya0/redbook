<template>
  <header style="background: #fff; border-bottom: 1px solid #e5e5e5; padding: 15px 20px; position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
    <div style="max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center;">
      <div style="display: flex; align-items: center; gap: 30px;">
        <h1 style="color: #ff2442; font-size: 24px; font-weight: bold; cursor: pointer;" @click="router.push('/')">小红书</h1>
        <nav style="display: flex; gap: 20px;">
          <template v-if="userStore.userInfo?.role === 1">
            <!-- 商家导航 -->
            <router-link to="/merchant/products" style="text-decoration: none; color: #666; font-weight: 500;">商品</router-link>
            <router-link to="/orders" style="text-decoration: none; color: #666; font-weight: 500;">订单</router-link>
            <router-link to="/message" style="text-decoration: none; color: #666; font-weight: 500; position: relative;">
              消息
              <span v-if="chatStore.totalUnreadCount > 0" style="position: absolute; top: -8px; right: -12px; background: #ff2442; color: white; border-radius: 10px; padding: 0 4px; font-size: 10px; min-width: 16px; height: 16px; line-height: 16px; text-align: center; transform: scale(0.8);">
                {{ chatStore.totalUnreadCount > 99 ? '99+' : chatStore.totalUnreadCount }}
              </span>
            </router-link>
          </template>
          <template v-else>
            <!-- 普通用户导航 -->
            <router-link to="/" style="text-decoration: none; color: #666; font-weight: 500;">探索</router-link>
            <router-link to="/follow" style="text-decoration: none; color: #666; font-weight: 500;">关注</router-link>
            <router-link to="/message" style="text-decoration: none; color: #666; font-weight: 500; position: relative;">
              消息
              <span v-if="chatStore.totalUnreadCount > 0" style="position: absolute; top: -8px; right: -12px; background: #ff2442; color: white; border-radius: 10px; padding: 0 4px; font-size: 10px; min-width: 16px; height: 16px; line-height: 16px; text-align: center; transform: scale(0.8);">
                {{ chatStore.totalUnreadCount > 99 ? '99+' : chatStore.totalUnreadCount }}
              </span>
            </router-link>
            <router-link to="/products" style="text-decoration: none; color: #666; font-weight: 500;">商城</router-link>
            <router-link to="/orders" style="text-decoration: none; color: #666; font-weight: 500;">订单</router-link>
          </template>
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
            @focus="handleFocus"
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

          <div class="search-history" v-if="(showHistory && historyList.length > 0) || (showHistory && !searchKeyword.trim())">
            <!-- 搜索历史 -->
            <div v-if="historyList.length > 0">
              <div class="history-header">
                <span class="history-title">搜索历史</span>
                <button class="history-clear" @mousedown.prevent="clearHistory">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </div>
              <div class="history-list">
                <button
                  v-for="(h, index) in historyList"
                  :key="h.id || index"
                  class="history-item"
                  @mousedown.prevent="selectHistory(h.history)"
                >
                  {{ h.history }}
                </button>
              </div>
            </div>

            <!-- 小红书热点 -->
            <div class="hot-search-container">
              <div class="hot-header">
                <div class="hot-title">
                  <span class="hot-icon">🔥</span>
                  小红书热点
                </div>
                <div class="hot-tabs">
                  <span 
                    :class="['hot-tab-item', { active: currentHotType === 1 }]"
                    @mousedown.prevent="switchHotType(1)"
                  >今日</span>
                  <span class="hot-tab-divider">|</span>
                  <span 
                    :class="['hot-tab-item', { active: currentHotType === 0 }]"
                    @mousedown.prevent="switchHotType(0)"
                  >全站</span>
                </div>
              </div>

              <div v-if="hotLoading" class="hot-loading-skeleton">
                <div v-for="i in 5" :key="i" class="skeleton-item"></div>
              </div>
              
              <div v-else class="hot-list">
                <div 
                  v-for="(item, index) in hotList" 
                  :key="index" 
                  class="hot-item"
                  :title="`搜索: ${item.key}`"
                  @mousedown.prevent="selectHotSearch(item.key)"
                >
                  <div class="hot-left">
                    <span :class="['hot-index', `index-${index + 1}`, { 'top-three': index < 3 }]">{{ index + 1 }}</span>
                    <span class="hot-keyword">{{ item.key }}</span>
                    <span v-if="index < 3" class="hot-tag tag-red">热</span>
                    <span v-else-if="index < 6" class="hot-tag tag-orange">梗</span>
                  </div>
                  <div class="hot-right">
                    <span class="hot-score-icon">🔥</span>
                    <span class="hot-score">{{ formatNumber(item.score || 0) }}</span>
                  </div>
                </div>
              </div>
              <div v-if="!hotLoading && hotList.length === 0" class="hot-empty">
                暂无热点数据
              </div>
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
               @click="handleProfileClick">
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useChatStore } from '@/store/chat'
import { useModal } from '@/utils/modal'
import { searchApi } from '@/api/search'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const { showConfirm, showAlert } = useModal()

// 建立 WebSocket 连接以接收实时通知
if (userStore.isLoggedIn) {
  chatStore.connect()
}

const searchKeyword = ref('')
const suggestions = ref([])
const showSuggestions = ref(false)
const historyList = ref([])
const showHistory = ref(false)

// 热搜相关
const hotList = ref([])
const currentHotType = ref(1) // 默认展示今日热搜
const hotLoading = ref(false)

const formatNumber = (num) => {
  if (num >= 10000000) {
    return (num / 10000000).toFixed(1) + '千万'
  } else if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const fetchHotList = async () => {
  hotLoading.value = true
  try {
    const res = await searchApi.getHotList(currentHotType.value)
    hotList.value = Array.isArray(res) ? res.slice(0, 10) : [] // 只展示前10个
  } catch (e) {
    hotList.value = []
  } finally {
    hotLoading.value = false
  }
}

const switchHotType = async (type) => {
  if (currentHotType.value === type) return
  currentHotType.value = type
  await fetchHotList()
}

// 获取用户头像
const userAvatar = computed(() => {
  return getImageUrl(userStore.userInfo?.image, 'https://via.placeholder.com/32x32/ff2442/ffffff?text=U')
})

const handleProfileClick = () => {
  if (userStore.userInfo?.role === 1) {
    router.push('/merchant/profile')
  } else {
    router.push('/profile')
  }
}

const goToLogin = () => {
  router.push('/login')
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  showSuggestions.value = false
  showHistory.value = false
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

const debouncedFetchSuggestions = debounce(() => {
  fetchSuggestions()
}, 300)

const handleInput = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    suggestions.value = []
    showSuggestions.value = false
    showHistory.value = true
    return
  }

  showHistory.value = false
  showSuggestions.value = true
  debouncedFetchSuggestions()
}

const selectSuggestion = (item) => {
  searchKeyword.value = item
  handleSearch()
}

const selectHistory = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

const selectHotSearch = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

const clearHistory = async () => {
  try {
    await searchApi.deleteHistory()
    historyList.value = []
    showHistory.value = false
  } catch (e) {
    showHistory.value = false
  }
}

const fetchHistory = async () => {
  if (!userStore.isLoggedIn) {
    historyList.value = []
    return
  }
  try {
    const list = await searchApi.getHistoryList()
    historyList.value = Array.isArray(list) ? list : []
  } catch (e) {
    historyList.value = []
  }
}

const handleFocus = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    showHistory.value = true
    showSuggestions.value = false
    return
  }
  showHistory.value = false
  showSuggestions.value = true
  fetchSuggestions()
}

const handleBlur = () => {
  // Delay hiding to allow click event to trigger
  setTimeout(() => {
    showSuggestions.value = false
    showHistory.value = false
  }, 200)
}

onMounted(() => {
  fetchHistory()
  fetchHotList()
})

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

.search-history {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  margin-top: 8px;
  z-index: 1001;
  overflow: hidden;
  width: 350px; /* 稍微加宽一点展示热搜 */
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.history-title {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.history-clear {
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.history-clear:hover {
  background: #f5f5f5;
  color: #ff2442;
}

.history-list {
  padding: 0 16px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-item {
  border: none;
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  background: #eeeeee;
  color: #333;
}

/* 热搜榜单样式 */
.hot-search-container {
  border-top: 1px solid #f0f0f0;
  padding: 16px 0;
}

.hot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 12px;
}

.hot-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.hot-icon {
  font-size: 16px;
  color: #ff2442;
}

.hot-tabs {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.hot-tab-item {
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.2s;
}

.hot-tab-item.active {
  color: #ff2442;
  font-weight: 600;
}

.hot-tab-divider {
  margin: 0 4px;
  color: #eee;
  font-size: 10px;
}

.hot-list {
  display: flex;
  flex-direction: column;
}

.hot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.hot-item:hover {
  background: #f9f9f9;
}

.hot-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.hot-index {
  font-size: 14px;
  font-weight: 700;
  color: #999;
  width: 20px;
  text-align: center;
  font-style: italic;
}

.hot-index.top-three {
  color: #ff2442;
}

/* 渐变排名颜色 */
.index-1 { color: #ff2442; }
.index-2 { color: #ff6b22; }
.index-3 { color: #ffb112; }

.hot-keyword {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-tag {
  font-size: 10px;
  padding: 1px 3px;
  border-radius: 3px;
  color: #fff;
  font-weight: 600;
  line-height: 1;
  transform: scale(0.9);
}

.tag-red {
  background: #ff2442;
}

.tag-orange {
  background: #ffa502;
}

.hot-right {
  margin-left: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}

.hot-score-icon {
  font-size: 10px;
  filter: grayscale(1);
  opacity: 0.6;
}

.hot-score {
  font-size: 12px;
  color: #999;
}

.hot-loading-skeleton {
  padding: 0 16px;
}

.skeleton-item {
  height: 36px;
  background: #f5f5f5;
  margin-bottom: 8px;
  border-radius: 4px;
  animation: pulse 1.5s infinite ease-in-out;
}

@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

.hot-empty {
  text-align: center;
  padding: 20px 0;
  font-size: 13px;
  color: #999;
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
