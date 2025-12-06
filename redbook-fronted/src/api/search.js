import request from './request'

export const searchApi = {
  // 综合搜索
  searchAll: (params) => {
    return request({
      url: '/search/search/all',
      method: 'get',
      params
    })
  },
  
  // 搜索建议
  getSuggestions: (keyword) => {
    return request({
      url: '/search/search/suggest',
      method: 'get',
      params: { keyword }
    })
  }
}
