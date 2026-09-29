import request from '@/utils/request'

// 驾驶舱使用一次快照接口，避免前端把不同统计口径的多个请求拼在一起。
export function getDashboard(query) {
  return request({
    url: '/workorder/dashboard',
    method: 'get',
    params: query
  })
}

// 绩效页的指标卡、工程师明细与筛选条件由同一份快照返回。
export function getPerformance(query) {
  return request({
    url: '/workorder/performance',
    method: 'get',
    params: query
  })
}
