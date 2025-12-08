<template>
  <div class="user-profile">
    <!-- 顶部导航 -->
    <div class="nav-header">
      <button class="back-btn" @click="router.back()">
        <span class="icon">←</span>
      </button>
      <span class="title">用户主页</span>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载用户信息...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="retry-btn" @click="fetchUserInfo">重试</button>
    </div>

    <div v-else class="profile-content">
      <!-- 用户信息展示模块 -->
      <div class="user-info-card">
        <div class="user-header">
          <div class="avatar-wrapper" @click="showAvatarModal = true">
            <img :src="getImageUrl(userInfo.image, defaultAvatar)" alt="头像" class="avatar"/>
          </div>
          <div class="info-right">
            <h2 class="nickname">{{ userInfo.nickname || '未设置昵称' }}</h2>
            <p class="red-id">小红书号：{{ userInfo.number || '未知' }}</p>
          </div>
        </div>

        <p class="bio" v-if="userInfo.intro">{{ userInfo.intro }}</p>

        <div class="stats-row">
          <div class="stat-item" @click="openUserList('attention')">
            <span class="count">{{ userInfo.attentionCount || 0 }}</span>
            <span class="label">关注</span>
          </div>
          <div class="stat-item" @click="openUserList('fans')">
            <span class="count">{{ userInfo.fansCount || 0 }}</span>
            <span class="label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="count">{{ userInfo.likeCount || 0 }}</span>
            <span class="label">获赞与收藏</span>
          </div>
        </div>

        <div class="action-row">
          <button
              class="follow-btn"
              :class="{ 'followed': isFollowed }"
              @click="handleFollow"
              :disabled="followLoading"
          >
            {{ isFollowed ? '已关注' : '关注' }}
          </button>
          <button class="chat-btn" @click="handleChat">私信</button>
        </div>
      </div>

      <!-- 标签页导航 -->
      <div class="tabs-nav">
        <div
            class="tab-item"
            :class="{ active: currentTab === 'notes' }"
            @click="switchTab('notes')"
        >
          笔记
        </div>
        <div
            class="tab-item"
            :class="{ active: currentTab === 'collection' }"
            @click="switchTab('collection')"
        >
          收藏
        </div>
        <div class="active-bar" :style="activeBarStyle"></div>
      </div>

      <!-- 内容展示区域 -->
      <div class="tab-content">
        <Transition name="fade" mode="out-in">
          <div :key="currentTab" class="grid-container">
            <!-- 骨架屏或加载中 -->
            <div v-if="listLoading && page === 1" class="loading-grid">
              <div class="spinner"></div>
            </div>

            <div v-else-if="currentList.length === 0" class="empty-state">
              <p>暂无内容</p>
            </div>

            <div v-else class="note-grid">
              <PostCard
                  v-for="note in currentList"
                  :key="note.id"
                  :post="note"
                  @click="handleNoteClick"
              />
            </div>

            <!-- 加载更多 -->
            <div v-if="hasMore" class="load-more">
              <button @click="loadMore" :disabled="listLoading">
                {{ listLoading ? '加载中...' : '加载更多' }}
              </button>
            </div>
            <div v-else-if="currentList.length > 0" class="no-more">
              没有更多了
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- 头像放大 Modal -->
    <div v-if="showAvatarModal" class="avatar-modal" @click="showAvatarModal = false">
      <img :src="getImageUrl(userInfo.image, defaultAvatar)" class="enlarged-avatar"/>
    </div>

    <!-- 笔记详情 Modal -->
    <NoteDetailModal
        v-model:visible="showNoteDetail"
        :note-id="currentNoteId"
    />

    <!-- 用户列表 Modal (关注/粉丝) -->
    <div v-if="showUserListModal" class="modal-overlay" @click="showUserListModal = false">
      <div class="modal-content user-list-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ userListTitle }}</h3>
          <button class="close-btn" @click="showUserListModal = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="userListLoading" class="loading-spinner-small"></div>
          <div v-else-if="userList.length === 0" class="empty-text">暂无数据</div>
          <div v-else class="user-list">
            <div v-for="user in userList" :key="user.userId" class="user-list-item" @click="handleUserClick(user)">
              <img :src="getImageUrl(user.image, defaultAvatar)" class="user-item-avatar"/>
              <span class="user-item-name">{{ user.nickname || '用户' + (user.userId || user.id) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getUserById, isAttention, toggleAttention, getAttentionList, getFansList} from '@/api/user'
import {getNoteListByUserId, getNoteListByCollectionUserId} from '@/api/note'
import PostCard from '@/components/PostCard.vue'
import NoteDetailModal from '@/components/NoteDetailModal.vue'
import { getImageUrl } from '@/utils/image'

import {useModal} from '@/utils/modal'

const route = useRoute()
const router = useRouter()
const { showConfirm } = useModal()
const userId = computed(() => route.params.id)
const defaultAvatar = 'https://via.placeholder.com/100'

// 状态
const loading = ref(true)
const error = ref(null)
const userInfo = ref({})
const isFollowed = ref(false)
const followLoading = ref(false)
const showAvatarModal = ref(false)

// 弹窗列表
const showUserListModal = ref(false)
const userListTitle = ref('')
const userList = ref([])
const userListLoading = ref(false)

// 标签页与列表
const currentTab = ref('notes') // 'notes' | 'collection'
const notesList = ref([])
const collectionList = ref([])
const listLoading = ref(false)
const page = ref(1)
const pageSize = 12
const hasMore = ref(true)

// 笔记详情
const showNoteDetail = ref(false)
const currentNoteId = ref(null)

// 样式计算
const activeBarStyle = computed(() => {
  return {
    transform: currentTab.value === 'notes' ? 'translateX(0)' : 'translateX(100%)'
  }
})

const currentList = computed(() => {
  return currentTab.value === 'notes' ? notesList.value : collectionList.value
})

// 初始化
const fetchUserInfo = async () => {
  if (!userId.value) return
  loading.value = true
  error.value = null
  try {
    const res = await getUserById(userId.value)
    if (res) {
      userInfo.value = res
      // 检查关注状态
      checkAttention()
      // 加载统计数据
      loadStats()
    } else {
      error.value = '用户不存在'
    }
  } catch (err) {
    console.error(err)
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    // 并行获取关注和粉丝列表以计算数量
    const [attentions, fans] = await Promise.all([
      getAttentionList(userId.value),
      getFansList(userId.value)
    ])

    userInfo.value = {
      ...userInfo.value,
      attentionCount: attentions ? attentions.length : 0,
      fansCount: fans ? fans.length : 0
    }
  } catch (err) {
    console.error('Failed to load stats:', err)
  }
}

const checkAttention = async () => {
  try {
    const res = await isAttention(userId.value)
    // 后端返回 1 表示已关注，2 表示未关注
    isFollowed.value = res === 1
  } catch (err) {
    console.error('Check attention failed', err)
  }
}

// 打开用户列表
const openUserList = async (type) => {
  showUserListModal.value = true
  userListLoading.value = true
  userList.value = []

  if (type === 'attention') {
    userListTitle.value = '关注列表'
    try {
      const res = await getAttentionList(userId.value)
      userList.value = res || []
    } catch (err) {
      console.error(err)
    }
  } else {
    userListTitle.value = '粉丝列表'
    try {
      const res = await getFansList(userId.value)
      userList.value = res || []
    } catch (err) {
      console.error(err)
    }
  }
  userListLoading.value = false
}

// 点击列表用户跳转
const handleUserClick = (user) => {
  const targetUserId = user.userId || user.id
  if (targetUserId) {
    showUserListModal.value = false
    // 如果是当前页面，不跳转
    if (String(targetUserId) === String(userId.value)) return
    router.push(`/user/${targetUserId}`)
  }
}

// 关注操作
const handleFollow = async () => {
  if (followLoading.value) return
  
  // 如果是已关注状态，需要确认是否取消关注
  if (isFollowed.value) {
    const confirmed = await showConfirm('确定要取消关注吗？', '取消关注')
    if (!confirmed) return
  }

  followLoading.value = true
  try {
    await toggleAttention(userId.value)
    isFollowed.value = !isFollowed.value
    // 更新粉丝数
    if (isFollowed.value) {
      userInfo.value.fansCount = (userInfo.value.fansCount || 0) + 1
    } else {
      userInfo.value.fansCount = Math.max((userInfo.value.fansCount || 0) - 1, 0)
    }
  } catch (err) {
    console.error('Toggle attention failed', err)
  } finally {
    followLoading.value = false
  }
}

const handleChat = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (String(userId.value) === String(userStore.userInfo?.id)) {
      return
  }
  router.push(`/message/chat/${userId.value}`)
}

// 列表加载
const loadList = async (isLoadMore = false) => {
  if (listLoading.value) return
  listLoading.value = true

  try {
    const api = currentTab.value === 'notes' ? getNoteListByUserId : getNoteListByCollectionUserId
    const res = await api(userId.value, page.value, pageSize)
    const newData = Array.isArray(res) ? res : []

    // 适配 PostCard 数据结构
    const formattedData = newData.map(item => ({
      ...item,
      image: getImageUrl(item.image, 'https://via.placeholder.com/300x400/f0f0f0/999999?text=小红书'),
      likes: item.like || 0, // 映射 like 到 likes
      // 确保 PostCard 需要的字段存在
      author: {
        name: item.user?.nickname || userInfo.value.nickname || '用户',
        avatar: getImageUrl(item.user?.image || userInfo.value.image, defaultAvatar),
        id: item.user?.id || item.userId || userId.value
      }
    }))

    if (currentTab.value === 'notes') {
      notesList.value = isLoadMore ? [...notesList.value, ...formattedData] : formattedData
    } else {
      collectionList.value = isLoadMore ? [...collectionList.value, ...formattedData] : formattedData
    }

    hasMore.value = newData.length === pageSize
  } catch (err) {
    console.error('Load list failed', err)
  } finally {
    listLoading.value = false
  }
}

// 切换标签
const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  page.value = 1
  hasMore.value = true
  // 如果目标列表为空，则加载
  if ((tab === 'notes' && notesList.value.length === 0) ||
      (tab === 'collection' && collectionList.value.length === 0)) {
    loadList()
  }
}

// 加载更多
const loadMore = () => {
  page.value++
  loadList(true)
}

// 点击笔记
const handleNoteClick = (note) => {
  currentNoteId.value = note.id
  showNoteDetail.value = true
}

// 初始化和监听
const initData = () => {
  if (userId.value) {
    // 重置列表数据
    notesList.value = []
    collectionList.value = []
    page.value = 1
    hasMore.value = true
    currentTab.value = 'notes'

    fetchUserInfo()
    loadList()
  } else {
    error.value = '无效的用户ID'
    loading.value = false
  }
}

onMounted(() => {
  initData()
})

// 监听路由参数变化，重新加载数据
watch(() => route.params.id, (newId) => {
  if (newId) {
    initData()
  }
})
</script>

<style scoped>
.user-profile {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f8f8f8;
  min-height: 100vh;
}

.nav-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 5px 10px;
  margin-right: 10px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.user-info-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.user-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.avatar-wrapper {
  margin-right: 20px;
  cursor: zoom-in;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.info-right {
  flex: 1;
}

.nickname {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 8px 0;
}

.red-id {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.bio {
  font-size: 14px;
  color: #333;
  margin-bottom: 20px;
  line-height: 1.5;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.count {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.action-row {
  display: flex;
  gap: 12px;
}

.follow-btn, .chat-btn {
  flex: 1;
  padding: 10px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.follow-btn {
  background-color: #ff2442;
  color: white;
}

.follow-btn.followed {
  background-color: #eee;
  color: #666;
  border: 1px solid #ddd;
}

.chat-btn {
  background-color: white;
  color: #333;
  border: 1px solid #ddd;
}

.tabs-nav {
  display: flex;
  background: white;
  padding: 12px 0;
  border-radius: 12px 12px 0 0;
  position: relative;
  margin-bottom: 1px;
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding-bottom: 8px;
  transition: color 0.3s;
}

.tab-item.active {
  color: #333;
  font-weight: 600;
}

.active-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50%;
  height: 2px;
  background-color: #ff2442;
  transition: transform 0.3s ease;
}

.active-bar::after {
  content: '';
  display: block;
  width: 40px;
  height: 2px;
  background-color: #ff2442;
  margin: 0 auto;
}

.tab-content {
  background: white;
  padding: 16px;
  min-height: 300px;
  border-radius: 0 0 12px 12px;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.empty-state, .loading-grid, .error-container, .loading-container {
  text-align: center;
  padding: 40px;
  color: #999;
}

.load-more, .no-more {
  text-align: center;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
}

.load-more button {
  padding: 8px 20px;
  background: #f5f5f5;
  border: none;
  border-radius: 16px;
  color: #666;
  cursor: pointer;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 头像放大 */
.avatar-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: zoom-out;
}

.enlarged-avatar {
  max-width: 90%;
  max-height: 90%;
  border-radius: 50%;
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.2);
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #ff2442;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 10px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.user-list-modal {
  height: 500px;
}

.modal-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.user-list-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}

.user-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
}

.user-item-name {
  font-size: 14px;
  color: #333;
}

.loading-spinner-small {
  width: 24px;
  height: 24px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #ff2442;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 20px auto;
}
</style>
