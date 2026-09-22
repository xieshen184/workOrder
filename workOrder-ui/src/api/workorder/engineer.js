import request from '@/utils/request'

// 人员状态页面和派单抽屉共享同一个后端人员投影，避免两处解释不同的负载口径。
export function listEngineers(query) {
  return request({
    url: '/workorder/admin/engineers',
    method: 'get',
    params: query
  })
}
