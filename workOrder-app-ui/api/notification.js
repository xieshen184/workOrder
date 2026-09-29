import request from '@/utils/request'

const DEFAULT_PAGE_SIZE = 10

function requireNotificationId(notificationId) {
  if (notificationId === undefined || notificationId === null || notificationId === '') {
    return Promise.reject(new Error('消息编号不能为空'))
  }
  return encodeURIComponent(notificationId)
}

// 消息列表直接消费若依分页响应，页面负责兼容 AjaxResult.data 包裹的部署差异。
export function getNotifications(params = {}) {
  return request({
    url: '/workorder/notifications',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || DEFAULT_PAGE_SIZE,
      category: params.category || '',
      readStatus: params.readStatus || ''
    }
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/workorder/notifications/unread-count',
    method: 'get'
  })
}

export function markNotificationRead(notificationId) {
  const encodedId = requireNotificationId(notificationId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId
  return request({
    url: `/workorder/notifications/${encodedId}/read`,
    method: 'put'
  })
}

export function markAllNotificationsRead(category) {
  const data = {}
  if (category) data.category = category
  return request({
    url: '/workorder/notifications/read-all',
    method: 'put',
    data
  })
}

export default {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead
}
