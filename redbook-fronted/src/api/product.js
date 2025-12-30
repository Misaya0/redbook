import request from './request.js'

/**
 * 获取商品列表
 * @param {number} pageNum - 页码
 * @param {number} pageSize - 每页数量
 * @returns {Promise}
 */
export const getProductList = (pageNum = 1, pageSize = 10) => {
  return request.get('/product/getProductList', {
    params: {
      pageNum,
      pageSize
    }
  })
}

/**
 * 获取商品详情
 * @param {number} productId - 商品ID
 * @returns {Promise}
 */
export const getProductDetail = (productId) => {
  return request.get(`/product/getProduct/${productId}`)
}

export const getProductSpecs = (productId, config = {}) => {
  return request.get(`/product/getProductSpecs/${productId}`, config)
}

export const getCategorySpecs = (categoryId, config = {}) => {
  return request.get(`/product/getCategorySpecs/${categoryId}`, config)
}

/**
 * 按店铺获取商品
 * @param {number} shopId - 店铺ID
 * @returns {Promise}
 */
export const getProductsByShop = (shopId) => {
  return request.get(`/product/getProductsByShop/${shopId}`)
}

/**
 * 获取所有商品分类
 * @returns {Promise}
 */
export const getCategoryList = (parentId, level) => {
  return request.get('/product/getCategoryList', {
    params: {
      parentId,
      level
    }
  })
}

/**
 * 搜索商品 (ES)
 * @param {Object} searchDto - 搜索条件
 * @returns {Promise}
 */
export const searchProduct = (searchDto) => {
  return request.post('/search/product', searchDto)
}

/**
 * 搜索商品 (MySQL)
 * @param {Object} searchDto - 搜索条件
 * @returns {Promise}
 */
export const searchProductMySql = (searchDto) => {
  return request.post('/product/search', searchDto)
}

/**
 * 搜索商品（用于发布笔记时关联商品）
 * @param {Object} data - 搜索条件，例如 { name: "关键词" }
 * @returns {Promise}
 */
export const searchProducts = (data = {}) => {
  const payload = { ...data }
  if (payload.name && !payload.keyword) {
    payload.keyword = payload.name
  }
  delete payload.name
  return request.post('/product/search', payload)
}

/**
 * 发布商品
 * @param {Object} productDto - 商品信息
 * @returns {Promise}
 */
export const postProduct = (productDto) => {
  return request.post('/product/postProduct', productDto)
}

/**
 * 更新商品
 * @param {Object} product - 商品信息
 * @returns {Promise}
 */
export const updateProduct = (product) => {
  return request.put('/product/updateProduct', product)
}

/**
 * 删除商品
 * @param {number} productId - 商品ID
 * @returns {Promise}
 */
export const deleteProduct = (productId) => {
  return request.delete(`/product/delete/${productId}`)
}
