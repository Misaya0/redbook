import request from './request'

export const searchApi = {
  // 综合搜索
  searchAll: (params) => {
    return request({
      url: '/search/all',
      method: 'get',
      params
    })
  },
  
  // 搜索建议
  getSuggestions: (keyword) => {
    return request({
      url: '/search/suggest',
      method: 'get',
      params: { keyword }
    })
  },

  // 获取搜索历史
  getHistoryList: () => {
    return request({
      url: '/search/getHistoryList',
      method: 'get'
    })
  },

  // 清空搜索历史
  deleteHistory: () => {
    return request({
      url: '/search/deleteHistory',
      method: 'delete'
    })
  },

  // 获取热搜榜单
  getHotList: async (type) => {
    try {
      return await request({
        url: '/search/hot',
        method: 'get',
        params: { type },
        skipErrorHandler: true
      })
    } catch (e) {
      return request({
        url: '/search/getHotList',
        method: 'get',
        params: { type }
      })
    }
  }
}
