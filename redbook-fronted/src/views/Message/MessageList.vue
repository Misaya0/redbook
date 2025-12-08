<template>
  <div class="message-list-container">
    <div class="header">
      <h2>消息</h2>
    </div>
    
    <div class="conversation-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="conversations.length === 0" class="empty">暂无消息</div>
      
      <div 
        v-else 
        v-for="item in conversations" 
        :key="item.id" 
        class="conversation-item"
        @click="goToChat(item.talkerId)"
      >
        <div class="avatar-wrapper">
          <img 
            v-if="item.targetAvatar" 
            :src="getImageUrl(item.targetAvatar)" 
            class="avatar-img"
            alt="avatar" 
          />
          <div v-else class="avatar-placeholder">
            {{ item.targetName ? item.targetName.charAt(0).toUpperCase() : 'U' }}
          </div>
        </div>
        
        <div class="content-wrapper">
          <div class="top-row">
            <span class="username">{{ item.targetName }}</span>
            <span class="time">{{ formatTime(item.lastMessageTime) }}</span>
          </div>
          <div class="bottom-row">
            <span class="last-message">{{ item.lastMessageContent }}</span>
            <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConversationList } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import { getImageUrl } from '@/utils/image'

const router = useRouter()
const chatStore = useChatStore()
const conversations = ref([])
const loading = ref(true)

const fetchConversations = async () => {
  try {
    const res = await getConversationList()
    conversations.value = res || []
  } catch (error) {
    console.error('获取会话列表失败', error)
  } finally {
    loading.value = false
  }
}

const goToChat = (userId) => {
  router.push(`/message/chat/${userId}`)
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  
  if (date.toDateString() === now.toDateString()) {
    // 今天，显示 HH:mm
    return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0')
  } else {
    // 非今天，显示 MM-dd
    return (date.getMonth() + 1).toString().padStart(2, '0') + '-' + date.getDate().toString().padStart(2, '0')
  }
}

onMounted(() => {
  fetchConversations()
  // 确保 WebSocket 连接
  chatStore.connect()
})
</script>

<style scoped>
.message-list-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
}

.header {
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.conversation-item {
  display: flex;
  padding: 15px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f8f8f8;
}

.conversation-item:hover {
  background: #f9f9f9;
}

.avatar-wrapper {
  margin-right: 15px;
  flex-shrink: 0;
}

.avatar-img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #ff2442;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 20px;
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.top-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.username {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-message {
  color: #666;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80%;
}

.unread-badge {
  background: #ff2442;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
