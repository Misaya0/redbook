import request from './request'

export function getUnreadSummary() {
  return request({
    url: '/message/unread/summary',
    method: 'get'
  })
}

export function getNotifications(group, page = 1, size = 20) {
  return request({
    url: '/message/notifications',
    method: 'get',
    params: { group, page, size }
  })
}

export function markGroupRead(group) {
  return request({
    url: '/message/unread/markRead',
    method: 'post',
    params: { group }
  })
}
