<template>
  <div class="chat-room-container">
    <div class="header">
      <button class="back-btn" @click="router.back()">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
      </button>
      <h3>{{ talkerName || '用户 ' + talkerId }}</h3>
      <div class="placeholder"></div>
    </div>

    <div class="message-container" ref="messageContainer">
      <div v-if="loading" class="loading">加载历史消息...</div>
      <div v-if="error" class="error-msg">{{ error }}</div>
      
      <div v-for="(msg, index) in messages" :key="index" 
           class="message-item" 
           :class="{ 'self': isSelf(msg) }">
        <img 
          v-if="isSelf(msg)"
          :src="getImageUrl(userStore.userInfo?.image, defaultAvatar)"
          class="avatar-img"
        />
        <img 
          v-else
          :src="getImageUrl(talkerAvatar, defaultAvatar)"
          class="avatar-img"
        />
        <div class="message-content">
          <div class="bubble">
            {{ msg.content }}
          </div>
          <div class="time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <input 
        type="text" 
        v-model="inputContent" 
        placeholder="发送消息..." 
        @keyup.enter="handleSend"
      />
      <button :disabled="!inputContent.trim()" @click="handleSend">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getHistory, sendMessage, markAsRead } from '@/api/chat'
import { getUserById } from '@/api/user'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'
import { getImageUrl } from '@/utils/image'
import { formatTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const talkerId = route.params.id
const talkerName = ref('')
const talkerAvatar = ref('')
const inputContent = ref('')
const messageContainer = ref(null)
const loading = ref(true)
const error = ref(null)
const defaultAvatar = 'https://via.placeholder.com/40'
let pollingTimer = null

// 使用 store 中的 messages
const messages = computed(() => chatStore.messages)

// 判断消息是否是自己发送的
const isSelf = (msg) => {
  return String(msg.senderId) === String(userStore.userInfo?.id)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动到底部
watch(messages, (newMsgs) => {
  scrollToBottom()
  // 如果收到新消息，且最后一条消息不是自己发的，标记为已读
  if (newMsgs.length > 0) {
    const lastMsg = newMsgs[newMsgs.length - 1]
    if (!isSelf(lastMsg)) {
      handleMarkAsRead()
    }
  }
}, { deep: true })

const handleMarkAsRead = async (retryCount = 0) => {
  try {
    await markAsRead(talkerId)
  } catch (err) {
    console.error('标记已读失败', err)
    if (retryCount < 3) {
      setTimeout(() => {
        handleMarkAsRead(retryCount + 1)
      }, 1000 * (retryCount + 1))
    }
  }
}

const fetchTalkerInfo = async () => {
  try {
    const res = await getUserById(talkerId)
    if (res) {
      talkerName.value = res.nickname
      talkerAvatar.value = res.image
    }
  } catch (err) {
    console.error('获取用户信息失败', err)
    error.value = '无法获取用户信息'
  }
}

const fetchHistory = async () => {
  try {
    const res = await getHistory(talkerId)
    chatStore.setMessages(res || [])
    scrollToBottom()
  } catch (err) {
    console.error('获取历史消息失败', err)
    error.value = '获取消息失败'
  } finally {
    loading.value = false
  }
}

const handleSend = async () => {
  const content = inputContent.value.trim()
  if (!content) return

  try {
    const msg = await sendMessage({
      receiverId: talkerId,
      content: content,
      type: 0 // 文本
    })
    
    // 发送成功后，手动添加到 store（虽然 websocket 可能也会推，但为了响应速度，本地先加）
    // 注意：如果 websocket 也会推自己的消息，这里需要去重。
    // 通常 websocket 只推给别人，自己发的消息靠接口响应确认。
    // 后端逻辑是：sendMessage -> save -> push to receiver.
    // 所以自己发的消息不会通过 websocket 收到。
    chatStore.addMessage(msg)
    
    inputContent.value = ''
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败', error)
  }
}

onMounted(() => {
  // 设置当前聊天对象
  chatStore.setCurrentTalker(talkerId)
  
  // 确保连接
  if (!chatStore.isConnected) {
    chatStore.connect()
  }
  
  fetchTalkerInfo()
  fetchHistory()
  // 进入页面时，标记已读
  handleMarkAsRead()

  // 启动轮询（作为 WebSocket 不可用时的降级方案）
  pollingTimer = setInterval(() => {
    if (!chatStore.isConnected) {
      console.log('WebSocket disconnected, polling history...')
      fetchHistory()
    }
  }, 5000)
})

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer)
  chatStore.setCurrentTalker(null)
  // chatStore.clearMessages() // 保留缓存，不清除消息
})
</script>

<style scoped>
.chat-room-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
  max-width: 800px;
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 15px;
  background: white;
  border-bottom: 1px solid #e5e5e5;
  height: 50px;
  flex-shrink: 0;
}

.back-btn {
  border: none;
  background: none;
  cursor: pointer;
  padding: 5px;
}

.placeholder {
  width: 34px;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.loading, .error-msg {
  text-align: center;
  color: #999;
  padding: 20px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;
}

.message-item.self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: white;
  color: #333;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-all;
}

.self .bubble {
  background: #ff2442;
  color: white;
  border-radius: 12px 0 12px 12px;
}

.message-item:not(.self) .bubble {
  border-radius: 0 12px 12px 12px;
}

.time {
  font-size: 12px;
  color: #999;
  padding: 0 4px;
}

.self .time {
  text-align: right;
}

.input-area {
  padding: 10px 15px;
  background: white;
  border-top: 1px solid #e5e5e5;
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.input-area input {
  flex: 1;
  padding: 10px 15px;
  border-radius: 20px;
  border: 1px solid #ddd;
  background: #f8f8f8;
  outline: none;
  font-size: 14px;
}

.input-area input:focus {
  background: white;
  border-color: #ff2442;
}

.input-area button {
  padding: 0 20px;
  border-radius: 20px;
  background: #ff2442;
  color: white;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.input-area button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
