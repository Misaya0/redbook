import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from './user'
import { parseDate } from '@/utils/date'

export const useChatStore = defineStore('chat', () => {
  const socket = ref(null)
  const isConnected = ref(false)
  const messageMap = ref({}) // { talkerId: [messages] }
  const currentTalkerId = ref(null) // 当前正在聊天的对象ID
  const lastReceivedMessage = ref(null) // 最新收到的一条消息（全局）
  const currentUserId = ref(null)

  // 当前聊天窗口的消息
  const messages = computed(() => {
    if (!currentTalkerId.value) return []
    return messageMap.value[currentTalkerId.value] || []
  })

  // 连接 WebSocket
  const connect = () => {
    const userStore = useUserStore()
    if (!userStore.token) return
    currentUserId.value = userStore.userInfo?.id

    if (isConnected.value) return

    try {
      // 建立连接
      socket.value = new WebSocket('ws://localhost:8888/socket')

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
          
          // 更新全局最新消息
          lastReceivedMessage.value = msg

          // 确定消息归属的 talkerId
          let talkerId = null
          // 确保ID都是字符串比较
          if (String(msg.senderId) === String(currentUserId.value)) {
             talkerId = msg.receiverId
          } else {
             talkerId = msg.senderId
          }
          
          if (talkerId) {
             if (!messageMap.value[talkerId]) {
                messageMap.value[talkerId] = []
             }
             // 去重
             const exists = messageMap.value[talkerId].some(m => m.id === msg.id)
             if (!exists) {
                messageMap.value[talkerId].push(msg)
             }
          }

        } catch (e) {
          console.error('Parse message failed:', e)
        }
      }

      socket.value.onclose = () => {
        console.log('WebSocket Closed')
        isConnected.value = false
        socket.value = null
        // 尝试重连
        setTimeout(() => {
          console.log('Attempting to reconnect...')
          connect()
        }, 3000)
      }

      socket.value.onerror = (error) => {
        console.error('WebSocket Error:', error)
      }

    } catch (e) {
      console.error('WebSocket connection failed:', e)
      // 尝试重连
      setTimeout(() => {
        console.log('Attempting to reconnect...')
        connect()
      }, 3000)
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
    let talkerId = null
    // 如果是我发的，归属给 receiver
    if (String(msg.senderId) === String(currentUserId.value)) {
        talkerId = msg.receiverId
    } else {
        talkerId = msg.senderId
    }
    
    // 如果没有指定 talkerId，默认用当前 talker
    if (!talkerId && currentTalkerId.value) {
        talkerId = currentTalkerId.value
    }

    if (talkerId) {
        if (!messageMap.value[talkerId]) {
            messageMap.value[talkerId] = []
        }
        // 简单去重
        const exists = messageMap.value[talkerId].some(m => m.id === msg.id)
        if (!exists) {
          messageMap.value[talkerId].push(msg)
        }
    }
  }

  // 设置历史消息（带去重合并）
  const setMessages = (msgs) => {
    if (!currentTalkerId.value) return
    const talkerId = currentTalkerId.value
    
    if (!messageMap.value[talkerId]) {
        messageMap.value[talkerId] = []
    }
    
    const currentMsgs = messageMap.value[talkerId]
    
    if (currentMsgs.length === 0) {
      messageMap.value[talkerId] = msgs
    } else {
      // 合并去重
      const existingIds = new Set(currentMsgs.map(m => m.id))
      const newMsgs = msgs.filter(m => !existingIds.has(m.id))
      
      messageMap.value[talkerId] = [...newMsgs, ...currentMsgs].sort((a, b) => {
        const dateA = parseDate(a.createTime)
        const dateB = parseDate(b.createTime)
        const timeA = dateA ? dateA.getTime() : 0
        const timeB = dateB ? dateB.getTime() : 0
        return (isNaN(timeA) ? 0 : timeA) - (isNaN(timeB) ? 0 : timeB)
      })
    }
  }

  // 清空消息（不再真正清空，只是保留缓存）
  const clearMessages = () => {
    // messageMap.value = {} // 如果需要彻底清空缓存可以取消注释
  }

  return {
    connect,
    disconnect,
    isConnected,
    messages,
    messageMap,
    currentTalkerId,
    lastReceivedMessage,
    setCurrentTalker,
    addMessage,
    setMessages,
    clearMessages
  }
})
