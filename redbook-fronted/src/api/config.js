// API配置
export const API_CONFIG = {
  // 使用Vite代理，基础URL添加/api前缀
  BASE_URL: '/api',

  // API端点
  endpoints: {
    // 登录相关
    sendCode: '/user/send/', // 发送验证码 GET /user/send/{phone}
    verifyLogin: '/user/verify', // 验证登录 GET /user/verify?phone=xxx&code=xxx
  },

  // 请求超时时间
  timeout: 10000,

  // 请求头配置
  headers: {
    'Content-Type': 'application/json',
  }
}

// 获取完整的API URL
export const getFullUrl = (endpoint) => {
  return `${API_CONFIG.BASE_URL}${endpoint}`
}