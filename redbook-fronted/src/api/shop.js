import request from './request'

/**
 * 创建店铺
 * @param {Object} shop - 店铺信息
 * @returns {Promise}
 */
export const createShop = (shop) => {
  return request.post('/shop/create', shop)
}

/**
 * 获取我的店铺信息
 * @returns {Promise}
 */
export const getMyShop = () => {
  return request.get('/shop/myShop')
}

/**
 * 根据用户ID获取店铺信息
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getShopByUserId = (userId) => {
  return request.get(`/shop/getShopByUserId/${userId}`)
}

/**
 * 根据店铺ID获取店铺信息
 * @param {number} shopId - 店铺ID
 * @returns {Promise}
 */
export const getShopById = (shopId) => {
  return request.get(`/shop/getShopById/${shopId}`)
}

/**
 * 获取店铺经营数据 (模拟)
 * @returns {Promise}
 */
export const getShopDashboard = () => {
  // 后端暂无此接口，模拟返回
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        totalProducts: 128,
        todayViews: 1024,
        totalCollects: 56,
        todayOrders: 12,
        monthOrders: 350,
        totalOrders: 1205,
        conversionRate: '1.2%'
      })
    }, 500)
  })
}
