import axios from 'axios'
import { API_CONFIG, getFullUrl } from './config'
import { useModal } from '../utils/modal'

// 获取modal实例
const { showAlert } = useModal()

const rejectWithAlert = (message) => {
  const error = new Error(message)
  error.isHandled = true
  return Promise.reject(error)
}

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
  async (response) => {
    // 统一处理响应数据
    const { data } = response

    // 假设后端返回格式：{ code: 200, message: 'success', data: ... }
    if (data.code === 200) {
      return data.data
    } else {
      // 统一错误处理
      await showAlert(data.message || '请求失败', '错误')
      return rejectWithAlert(data.message || '请求失败')
    }
  },
  async (error) => {
    // 统一错误处理
    if (error.response) {
      // 服务器响应错误
      const { status, data } = error.response

      switch (status) {
        case 400:
          await showAlert(data.message || '请求参数错误', '错误')
          return rejectWithAlert(data.message || '请求参数错误')
        case 401:
          // 清除token，跳转到登录页
          localStorage.removeItem('token')
          await showAlert('登录已过期，请重新登录', '提示')
          window.location.href = '/login'
          return rejectWithAlert('登录已过期，请重新登录')
        case 403:
          await showAlert('没有权限访问', '错误')
          return rejectWithAlert('没有权限访问')
        case 404:
          await showAlert('请求的资源不存在', '错误')
          return rejectWithAlert('请求的资源不存在')
        case 500:
          await showAlert('服务器内部错误', '错误')
          return rejectWithAlert('服务器内部错误')
        default:
          await showAlert(data.message || '服务器错误', '错误')
          return rejectWithAlert(data.message || '服务器错误')
      }
    } else if (error.request) {
      // 请求发送失败
      await showAlert('网络连接失败，请检查网络', '错误')
      return rejectWithAlert('网络连接失败，请检查网络')
    } else {
      // 其他错误
      await showAlert(error.message || '请求失败', '错误')
      return rejectWithAlert(error.message || '请求失败')
    }
  }
)

export default request