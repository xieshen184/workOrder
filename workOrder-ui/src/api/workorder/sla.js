import request from '@/utils/request'

export function listSlaRules(query) {
  return request({
    url: '/workorder/sla-rules',
    method: 'get',
    params: query
  })
}

export function getSlaRule(ruleId) {
  return request({
    url: '/workorder/sla-rules/' + encodeURIComponent(ruleId),
    method: 'get'
  })
}

export function addSlaRule(data) {
  return request({
    url: '/workorder/sla-rules',
    method: 'post',
    data
  })
}

export function updateSlaRule(ruleId, data) {
  return request({
    url: '/workorder/sla-rules/' + encodeURIComponent(ruleId),
    method: 'put',
    data
  })
}
