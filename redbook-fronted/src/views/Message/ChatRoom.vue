<template>
  <div class="chat-room-container">
    <div class="header">
      <button class="back-btn" @click="router.back()">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
      </button>
      <h3>用户 {{ talkerId }}</h3>
      <div class="placeholder"></div>
    </div>

    <div class="message-container" ref="messageContainer">
      <div v-if="loading" class="loading">加载历史消息...</div>
      
      <div v-for="(msg, index) in messages" :key="index" 
           class="message-item" 
           :class="{ 'self': isSelf(msg) }">
        <div class="avatar">
           {{ isSelf(msg) ? '我' : 'Ta' }}
        </div>
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
import { getHistory, sendMessage } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const talkerId = route.params.id
const inputContent = ref('')
const messageContainer = ref(null)
const loading = ref(true)

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
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

const fetchHistory = async () => {
  try {
    const res = await getHistory(talkerId)
    chatStore.setMessages(res || [])
    scrollToBottom()
  } catch (error) {
    console.error('获取历史消息失败', error)
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

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0')
}

onMounted(() => {
  // 设置当前聊天对象
  chatStore.setCurrentTalker(talkerId)
  
  // 确保连接
  if (!chatStore.isConnected) {
    chatStore.connect()
  }
  
  fetchHistory()
})

onUnmounted(() => {
  chatStore.setCurrentTalker(null)
  chatStore.setMessages([]) // 离开时清空当前聊天记录，或者保留看需求
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

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: white;
  flex-shrink: 0;
}

.self .avatar {
  background: #ff2442;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.self .message-content {
  align-items: flex-end;
}

.bubble {
  padding: 10px 14px;
  background: white;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.4;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.self .bubble {
  background: #ff2442;
  color: white;
  border-radius: 12px 12px 0 12px;
}

.message-item:not(.self) .bubble {
  border-radius: 12px 12px 12px 0;
}

.time {
  font-size: 10px;
  color: #999;
  margin: 0 2px;
}

.input-area {
  padding: 10px 15px;
  background: white;
  border-top: 1px solid #e5e5e5;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
}

.input-area input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.input-area input:focus {
  border-color: #ff2442;
}

.input-area button {
  padding: 8px 20px;
  background: #ff2442;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
}

.input-area button:disabled {
  background: #fcc;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  color: #999;
  margin-top: 20px;
}
</style>
