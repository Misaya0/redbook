import request from './request'

/**
 * 上传文件
 * @param {File} file - 文件对象
 * @returns {Promise<string>} - 返回文件访问URL
 */
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/product/uploadImage', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
