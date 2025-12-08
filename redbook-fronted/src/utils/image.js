export const BASE_API = import.meta.env.VITE_APP_BASE_API || '/api'

/**
 * 获取完整的图片访问地址
 * @param {string} img 图片路径
 * @param {string} defaultImg 默认图片
 * @returns {string} 完整图片地址
 */
export const getImageUrl = (img, defaultImg = '') => {
  if (!img) return defaultImg
  if (img.startsWith('http') || img.startsWith('data:')) return img
  
  // 如果已经包含 /api 前缀（兼容旧数据），直接返回
  if (img.startsWith('/api/')) return img
  
  // 确保路径以 / 开头
  const path = img.startsWith('/') ? img : `/${img}`
  
  // 拼接 BASE_API
  return `${BASE_API}${path}`
}
