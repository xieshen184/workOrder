import request from '@/utils/request'

export function listDelayRequests(query) {
  return request({
    url: '/workorder/admin/delay-requests',
    method: 'get',
    params: query
  })
}

export function approveDelayRequest(requestId, data) {
  return request({
    url: '/workorder/delay-requests/' + encodeURIComponent(requestId) + '/approve',
    method: 'post',
    data: data || {}
  })
}

export function rejectDelayRequest(requestId, data) {
  return request({
    url: '/workorder/delay-requests/' + encodeURIComponent(requestId) + '/reject',
    method: 'post',
    data: data || {}
  })
}
