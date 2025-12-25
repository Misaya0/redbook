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
  }
}
