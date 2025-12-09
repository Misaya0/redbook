import request from './request'

// 获取会话列表
export const getConversationList = () => {
  return request({
    url: '/message/chat/conversation/list',
    method: 'get'
  })
}

// 获取聊天历史
export const getHistory = (talkerId) => {
  return request({
    url: '/message/chat/history',
    method: 'get',
    params: {
      talkerId
    }
  })
}

// 发送消息
export const sendMessage = (data) => {
  return request({
    url: '/message/chat/send',
    method: 'post',
    // 使用 form-data 或者 x-www-form-urlencoded，因为后端 controller 参数没有 @RequestBody，而是 @RequestParam
    // request.js 默认可能是 json，需要注意参数传递方式
    // 这里如果后端是 @RequestParam，axios 需要用 params 或者 FormData，或者 qs.stringify
    // 假设 request.js 配置了自动处理，或者我们可以直接传 params
    params: data
  })
}

// 标记会话已读
export const markAsRead = (talkerId) => {
  return request({
    url: '/message/chat/conversation/read',
    method: 'post',
    params: {
      talkerId
    }
  })
}
