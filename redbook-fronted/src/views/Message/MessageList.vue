<template>
  <div class="message-list-container">
    <div class="header">
      <h2>消息</h2>
    </div>

    <!-- 消息类型入口 -->
    <div class="action-buttons">
      <div class="action-item" @click="handleAction('likeCollect')">
        <div class="icon-box like-bg">
          <span class="icon">❤️</span>
          <div v-if="unreadSummary.likeCollect > 0" class="badge">{{ unreadSummary.likeCollect > 99 ? '99+' : unreadSummary.likeCollect }}</div>
        </div>
        <span class="action-text">赞和收藏</span>
      </div>
      
      <div class="action-item" @click="handleAction('follow')">
        <div class="icon-box follow-bg">
          <span class="icon">👤</span>
          <div v-if="unreadSummary.follow > 0" class="badge">{{ unreadSummary.follow > 99 ? '99+' : unreadSummary.follow }}</div>
        </div>
        <span class="action-text">新增关注</span>
      </div>
      
      <div class="action-item" @click="handleAction('comment')">
        <div class="icon-box comment-bg">
          <span class="icon">💬</span>
          <div v-if="unreadSummary.comment > 0" class="badge">{{ unreadSummary.comment > 99 ? '99+' : unreadSummary.comment }}</div>
        </div>
        <span class="action-text">评论和@</span>
      </div>
    </div>
    
    <div class="conversation-list">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="conversations.length === 0" class="empty">暂无消息</div>
      
      <div 
        v-else 
        v-for="item in conversations" 
        :key="item.id" 
        class="conversation-item"
        :class="{ 'flashing': item.flashing }"
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
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getConversationList } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import { getImageUrl } from '@/utils/image'
import { formatTimeForList as formatTime } from '@/utils/date'

const router = useRouter()
const chatStore = useChatStore()
const conversations = ref([])
const loading = ref(true)

// 使用 store 中的未读数
const unreadSummary = computed(() => chatStore.unreadSummary)

const handleAction = (type) => {
  if (type === 'likeCollect') {
    router.push('/message/like-collect')
  } else if (type === 'follow') {
    router.push('/message/new-follow')
  } else if (type === 'comment') {
    router.push('/message/comment-at')
  } else {
    console.log('点击消息类型:', type)
    // TODO: 跳转到对应的消息列表页
  }
}

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

// 监听新消息
watch(() => chatStore.lastReceivedMessage, (newMsg) => {
  if (!newMsg) return
  
  // 查找对应的会话
  const index = conversations.value.findIndex(c => String(c.talkerId) === String(newMsg.senderId))
  
  if (index !== -1) {
    // 更新现有会话
    const conversation = conversations.value[index]
    conversation.lastMessageContent = newMsg.content
    conversation.lastMessageTime = newMsg.createTime
    conversation.unreadCount = (conversation.unreadCount || 0) + 1
    
    // 添加闪烁效果标记
    if (conversation.flashTimer) clearTimeout(conversation.flashTimer)
    conversation.flashing = true
    conversation.flashTimer = setTimeout(() => {
      conversation.flashing = false
      conversation.flashTimer = null
    }, 2000)

    // 移动到顶部
    conversations.value.splice(index, 1)
    conversations.value.unshift(conversation)
  } else {
    // 新会话，重新获取列表
    fetchConversations()
  }
})

const goToChat = (userId) => {
  router.push(`/message/chat/${userId}`)
}

onMounted(() => {
  fetchConversations()
  // 确保 WebSocket 连接 (store内会自动获取 unreadSummary)
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

.action-buttons {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
  border-bottom: 8px solid #f5f5f5;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.1s;
}

.action-item:active {
  transform: scale(0.95);
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 8px;
  position: relative;
}

.like-bg {
  background-color: #ffebef;
  color: #ff2442;
}

.follow-bg {
  background-color: #e8f4ff;
  color: #2486ff;
}

.comment-bg {
  background-color: #e6fffa;
  color: #00b386;
}

.icon {
  font-size: 24px;
}

.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background-color: #ff2442;
  color: white;
  font-size: 10px;
  min-width: 18px;
  height: 18px;
  line-height: 14px;
  text-align: center;
  padding: 0 4px;
  border-radius: 9px;
  border: 2px solid #fff;
  box-sizing: border-box;
}

.action-text {
  font-size: 13px;
  color: #333;
  font-weight: 500;
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

@keyframes flash {
  0% { background-color: #fff; }
  50% { background-color: #e6f7ff; }
  100% { background-color: #fff; }
}

.flashing {
  animation: flash 1s ease-in-out infinite;
}
</style>
