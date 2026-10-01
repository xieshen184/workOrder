import request from '@/utils/request'

const STATUS_KEYS = [
  'WAIT_ASSIGN',
  'WAIT_ACCEPT',
  'ACCEPTED',
  'PROCESSING',
  'WAIT_CONFIRM',
  'COMPLETED',
  'CLOSED'
]

function hasPermission(permissions, permission) {
  return Array.isArray(permissions) && (permissions.includes('*:*:*') || permissions.includes(permission))
}

function resolveListUrl(permissions) {
  if (hasPermission(permissions, 'workorder:order:list')) return '/workorder/admin/orders'
  if (hasPermission(permissions, 'workorder:order:assigned:list')) return '/workorder/engineer/orders'
  if (hasPermission(permissions, 'workorder:order:query')) return '/workorder/orders/my'
  return ''
}

function count(url, status) {
  return request({
    url,
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: 1,
      status: status || ''
    }
  }).then(response => Number(response.total || 0))
}

// 首页只聚合后端能够准确提供的实时分页总数，不推算历史趋势，也不以本地默认值伪装请求成功。
export async function getDashboardSnapshot(permissions) {
  const url = resolveListUrl(permissions)
  if (!url) throw new Error('当前账号没有工单查询权限')

  const tasks = [{ key: 'TOTAL', promise: count(url, '') }]
    .concat(STATUS_KEYS.map(status => ({ key: status, promise: count(url, status) })))

  // “待评价”只能统计当前登录人作为报修人的已完成工单。
  // 管理员和工程师的首页主统计可能使用管理端或工程师端列表，不能复用其中的 COMPLETED 数量，
  // 否则卡片数量会与评价页固定使用的“我的工单”接口不一致。
  if (hasPermission(permissions, 'workorder:evaluation:add')) {
    tasks.push({
      key: 'PENDING_EVALUATION',
      promise: count('/workorder/orders/my', 'COMPLETED')
    })
  }
  const values = {}
  const failed = []

  await Promise.all(tasks.map(task => task.promise
    .then(total => { values[task.key] = total })
    .catch(() => {
      values[task.key] = 0
      failed.push(task.key)
    })))

  if (failed.length === tasks.length) throw new Error('工单统计加载失败')
  return { values, failed }
}

export default { getDashboardSnapshot }
