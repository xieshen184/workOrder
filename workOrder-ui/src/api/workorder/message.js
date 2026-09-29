import request from '@/utils/request'

// 消息模板列表支持后端分页，查询条件由页面原样传递。
export function listNotificationTemplates(query) {
  return request({
    url: '/workorder/notification-templates',
    method: 'get',
    params: query
  })
}

export function getNotificationTemplate(templateId) {
  return request({
    url: '/workorder/notification-templates/' + encodeURIComponent(templateId),
    method: 'get'
  })
}

// 模板没有新增接口，页面只提交已有模板的可编辑字段。
export function updateNotificationTemplate(templateId, data) {
  return request({
    url: '/workorder/notification-templates/' + encodeURIComponent(templateId),
    method: 'put',
    data
  })
}

export function previewNotificationTemplate(data) {
  return request({
    url: '/workorder/notification-templates/preview',
    method: 'post',
    data
  })
}

export function listNotificationTasks(query) {
  return request({
    url: '/workorder/notification-tasks',
    method: 'get',
    params: query
  })
}

export function retryNotificationTask(taskId) {
  return request({
    url: '/workorder/notification-tasks/' + encodeURIComponent(taskId) + '/retry',
    method: 'put'
  })
}
