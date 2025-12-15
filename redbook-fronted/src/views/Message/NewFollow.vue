<template>
  <div class="like-collect-container">
    <div class="header">
      <div class="back-icon" @click="router.back()">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
      </div>
      <h2>新增关注</h2>
    </div>

    <div class="notification-list" @scroll="handleScroll">
      <div v-if="loading && notifications.length === 0" class="loading">加载中...</div>
      <div v-else-if="notifications.length === 0" class="empty">暂无关注</div>
      
      <div 
        v-else 
        v-for="item in notifications" 
        :key="item.id" 
        class="notification-item"
        @click="goToProfile(item)"
      >
        <div class="avatar-wrapper">
          <img 
            v-if="item.actor && item.actor.image" 
            :src="getImageUrl(item.actor.image)" 
            class="avatar-img"
            alt="avatar" 
          />
          <div v-else class="avatar-placeholder">
            {{ item.actor && item.actor.nickname ? item.actor.nickname.charAt(0).toUpperCase() : 'U' }}
          </div>
        </div>
        
        <div class="content-wrapper">
          <div class="top-row">
            <span class="username">{{ item.actor ? item.actor.nickname : '未知用户' }}</span>
            <span class="time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <div class="action-text">关注了你</div>
        </div>
        
        <div class="follow-btn-wrapper">
             <button 
               class="follow-btn" 
               :class="{ 'is-followed': item.isFollowed }"
               @click.stop="handleAction(item)"
             >
               {{ item.isFollowed ? '发私信' : '回粉' }}
             </button>
        </div>
      </div>
      
      <div v-if="loading && notifications.length > 0" class="loading-more">加载更多...</div>
      <div v-if="!hasMore && notifications.length > 0" class="no-more">没有更多了</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markGroupRead } from '@/api/notification'
import { toggleAttention } from '@/api/user'
import { getImageUrl } from '@/utils/image'
import { formatTimeForList as formatTime } from '@/utils/date'

const router = useRouter()
const notifications = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const fetchNotifications = async (isLoadMore = false) => {
  if (loading.value) return
  loading.value = true
  
  try {
    const res = await getNotifications('follow', page.value)
    // Interceptor returns data directly
    const list = res.records || []
    if (isLoadMore) {
      notifications.value.push(...list)
    } else {
      notifications.value = list
    }
    
    if (list.length < 20) {
      hasMore.value = false
    } else {
      page.value++
    }
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    loading.value = false
  }
}

const handleScroll = (e) => {
  const { scrollTop, clientHeight, scrollHeight } = e.target
  if (scrollHeight - scrollTop - clientHeight < 50 && hasMore.value) {
    fetchNotifications(true)
  }
}

const goToProfile = (item) => {
  if (item.actor && item.actor.id) {
      router.push(`/user/${item.actor.id}`)
  }
}

const handleAction = async (item) => {
  if (item.isFollowed) {
    // Navigate to chat
    if (item.actor && item.actor.id) {
        router.push(`/message/chat/${item.actor.id}`)
    }
  } else {
    // Follow back
    if (!item.actor || !item.actor.id) return
    try {
        await toggleAttention(item.actor.id)
        item.isFollowed = true
    } catch (error) {
        console.error('关注失败', error)
    }
  }
}

onMounted(() => {
  fetchNotifications()
  // 标记已读
  markGroupRead('follow')
})
</script>

<style scoped>
.like-collect-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 10;
}
.header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}
.back-icon {
  cursor: pointer;
  margin-right: 15px;
  display: flex;
  align-items: center;
}
.notification-list {
  padding-bottom: 20px;
}
.loading, .empty {
  padding: 30px;
  text-align: center;
  color: #999;
}
.notification-item {
  display: flex;
  padding: 15px 20px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  align-items: center;
}
.notification-item:hover {
  background-color: #fafafa;
}
.avatar-wrapper {
  width: 40px;
  height: 40px;
  margin-right: 12px;
  flex-shrink: 0;
}
.avatar-img, .avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-placeholder {
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}
.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.top-row {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.username {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-right: 8px;
}
.time {
  font-size: 12px;
  color: #999;
}
.action-text {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.follow-btn-wrapper {
    margin-left: 12px;
    flex-shrink: 0;
}
.follow-btn {
    padding: 6px 16px;
    background-color: #ff2442;
    color: #fff;
    border: none;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
}
.follow-btn.is-followed {
    background-color: #fff;
    color: #666;
    border: 1px solid #ddd;
}
.loading-more, .no-more {
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 12px;
}
</style>