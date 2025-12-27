<template>
  <div class="like-collect-container">
    <div class="header">
      <div class="back-icon" @click="router.back()">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
      </div>
      <h2>评论和@</h2>
    </div>

    <div class="notification-list" @scroll="handleScroll">
      <div v-if="loading && notifications.length === 0" class="loading">加载中...</div>
      <div v-else-if="notifications.length === 0" class="empty">暂无消息</div>
      
      <div 
        v-else 
        v-for="item in notifications" 
        :key="item.id" 
        class="notification-item"
        @click="goToDetail(item)"
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
          <div class="action-text">
              <span v-if="item.type === 'REPLY'">回复了你：</span>
              <span v-else>评论了你的笔记：</span>
              {{ item.contentSnapshot }}
          </div>
        </div>
        
        <div class="note-cover" v-if="item.noteCover">
            <img :src="getImageUrl(item.noteCover)" alt="note" />
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
    const res = await getNotifications('comment', page.value)
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

const goToDetail = (item) => {
  let noteId = null
  if (item.targetType === 'NOTE') {
      noteId = item.targetId
  }
  // 如果是评论，需要更多信息才能跳转
  if (noteId) {
      router.push(`/note/${noteId}`)
  }
}

onMounted(() => {
  fetchNotifications()
  // 标记已读
  markGroupRead('comment')
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
.note-cover {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  margin-left: 12px;
  flex-shrink: 0;
}
.note-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.loading-more, .no-more {
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 12px;
}
</style>
