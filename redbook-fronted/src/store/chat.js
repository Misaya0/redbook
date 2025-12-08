import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'

export const useChatStore = defineStore('chat', () => {
  const socket = ref(null)
  const isConnected = ref(false)
  const messages = ref([]) // 当前聊天窗口的消息
  const currentTalkerId = ref(null) // 当前正在聊天的对象ID

  // 连接 WebSocket
  const connect = () => {
    const userStore = useUserStore()
    if (!userStore.token || isConnected.value) return

    try {
      // 建立连接
      socket.value = new WebSocket('ws://localhost:8888')

      socket.value.onopen = () => {
        console.log('WebSocket Connected')
        isConnected.value = true
        // 连接成功后发送 token 进行认证
        socket.value.send(userStore.token)
      }

      socket.value.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          console.log('Received message:', msg)
          
          // 如果是当前正在聊天的对象发来的消息，追加到列表
          // msg.senderId 是发送者ID
          // 如果当前打开了与 msg.senderId 的聊天窗口
          if (currentTalkerId.value && String(msg.senderId) === String(currentTalkerId.value)) {
            messages.value.push(msg)
          } else {
            // TODO: 更新会话列表的未读数（如果做了全局会话状态管理）
            // 目前简单处理：只处理当前聊天窗口
          }
        } catch (e) {
          console.error('Parse message failed:', e)
        }
      }

      socket.value.onclose = () => {
        console.log('WebSocket Closed')
        isConnected.value = false
        socket.value = null
      }

      socket.value.onerror = (error) => {
        console.error('WebSocket Error:', error)
      }

    } catch (e) {
      console.error('WebSocket connection failed:', e)
    }
  }

  // 断开连接
  const disconnect = () => {
    if (socket.value) {
      socket.value.close()
      socket.value = null
      isConnected.value = false
    }
  }

  // 设置当前聊天对象（用于判断是否接收消息并显示）
  const setCurrentTalker = (id) => {
    currentTalkerId.value = id
  }

  // 追加发送的消息
  const addMessage = (msg) => {
    messages.value.push(msg)
  }

  // 设置历史消息
  const setMessages = (msgs) => {
    messages.value = msgs
  }

  return {
    connect,
    disconnect,
    isConnected,
    messages,
    currentTalkerId,
    setCurrentTalker,
    addMessage,
    setMessages
  }
})
