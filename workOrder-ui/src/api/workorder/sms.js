import request from '@/utils/request'

export function getSmsAccount() {
  return request({
    url: '/workorder/sms-account',
    method: 'get'
  })
}

export function updateSmsAccountSettings(data) {
  return request({
    url: '/workorder/sms-account/settings',
    method: 'put',
    data
  })
}

export function refreshSmsAccount() {
  return request({
    url: '/workorder/sms-account/refresh',
    method: 'put'
  })
}
