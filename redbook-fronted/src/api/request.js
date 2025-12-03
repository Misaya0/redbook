import axios from 'axios'
import { API_CONFIG, getFullUrl } from './config'

// 创建axios实例
const request = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.timeout,
  headers: API_CONFIG.headers
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 后端网关需要 token 请求头
      config.headers.token = token
      // 同时保留 Authorization 头以兼容其他可能的需求
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 统一处理响应数据
    const { data } = response

    // 假设后端返回格式：{ code: 200, message: 'success', data: ... }
    if (data.code === 200) {
      return data.data
    } else {
      // 统一错误处理
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  (error) => {
    // 统一错误处理
    if (error.response) {
      // 服务器响应错误
      const { status, data } = error.response

      switch (status) {
        case 400:
          return Promise.reject(new Error(data.message || '请求参数错误'))
        case 401:
          // 清除token，跳转到登录页
          localStorage.removeItem('token')
          window.location.href = '/login'
          return Promise.reject(new Error('登录已过期，请重新登录'))
        case 403:
          return Promise.reject(new Error('没有权限访问'))
        case 404:
          return Promise.reject(new Error('请求的资源不存在'))
        case 500:
          return Promise.reject(new Error('服务器内部错误'))
        default:
          return Promise.reject(new Error(data.message || '服务器错误'))
      }
    } else if (error.request) {
      // 请求发送失败
      return Promise.reject(new Error('网络连接失败，请检查网络'))
    } else {
      // 其他错误
      return Promise.reject(new Error(error.message || '请求失败'))
    }
  }
)

export default request