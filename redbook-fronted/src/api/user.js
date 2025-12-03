import request from './request'

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
export const getUserInfo = () => {
  return request.get('/user/getInfo')
}

/**
 * 根据ID获取用户信息
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getUserById = (userId) => {
  return request.get(`/user/getUserById/${userId}`)
}

/**
 * 更新用户头像
 * @param {File} file - 头像文件
 * @returns {Promise}
 */
export const updateUserImage = (file) => {
  const formData = new FormData()
  formData.append('image', file)
  return request.post('/user/updateImage', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 编辑用户信息
 * @param {Object} userInfo - 用户信息对象
 * @returns {Promise}
 */
export const editUserInfo = (userInfo) => {
  return request.put('/user/editInfo', userInfo)
}

/**
 * 查询是否关注某用户
 * @param {number} otherId - 目标用户ID
 * @returns {Promise}
 */
export const isAttention = (otherId) => {
  return request.get(`/user/isAttention/${otherId}`)
}

/**
 * 关注/取消关注用户
 * @param {number} otherId - 目标用户ID
 * @returns {Promise}
 */
export const toggleAttention = (otherId) => {
  return request.get(`/user/attention/${otherId}`)
}

/**
 * 获取用户的关注列表
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getAttentionList = (userId) => {
  return request.get(`/user/getAttention/${userId}`)
}

/**
 * 获取用户的粉丝列表
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getFansList = (userId) => {
  return request.get(`/user/getFans/${userId}`)
}
