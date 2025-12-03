import request from './request'

/**
 * 发送短信验证码
 * @param {string} phone - 手机号码
 * @returns {Promise}
 */
export const sendVerifyCode = (phone) => {
  return request.get(`/user/send/${phone}`)
}

/**
 * 验证登录
 * @param {string} phone - 手机号码
 * @param {string} code - 验证码
 * @returns {Promise}
 */
export const verifyLogin = (phone, code) => {
  console.log('API - 验证登录请求:', { phone, code })
  return request.get('/user/verify', {
    params: {
      phone,
      code
    }
  }).then(response => {
    console.log('API - 验证登录响应:', response)
    return response
  }).catch(error => {
    console.error('API - 验证登录错误:', error)
    throw error
  })
}

/**
 * 获取用户信息
 * @returns {Promise}
 */
export const getUserInfo = () => {
  return request.get('/user/getInfo')
}

/**
 * 退出登录
 * @returns {Promise}
 */
export const logout = () => {
  return request.post('/user/logout')
}