import request from '@/utils/request'

let idempotencySequence = 0

// 调用方须保存本次操作的 key；失败重试复用，内容或版本变化后再生成新 key。
export function createOrderIdempotencyKey(action = 'ACTION') {
  const safeAction = String(action).replace(/[^a-zA-Z0-9_.:-]/g, '-').toLowerCase()
  idempotencySequence = (idempotencySequence + 1) % 1679616
  return `m1b03-${safeAction}-${Date.now().toString(36)}-${idempotencySequence.toString(36)}`.slice(0, 64)
}

export function getMyOrders(params = {}) {
  // /my 使用若依标准 pageNum/pageSize 分页，返回 rows/total；调用方据此维护加载更多边界。
  return request({
    url: '/workorder/orders/my',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
      status: params.status || ''
    }
  })
}

export function getOrderDetail(orderId) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  // 详情接口返回 AjaxResult.data，页面在这里保持原始响应，统一由详情页做字段映射。
  return request({
    url: `/workorder/orders/${encodeURIComponent(orderId)}`,
    method: 'get'
  })
}

export function cancelOrder(orderId, data = {}) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return request({
    url: `/workorder/orders/${encodeURIComponent(orderId)}/cancel`,
    method: 'post',
    data: { version: data.version },
    header: {
      'Idempotency-Key': data.idempotencyKey || createOrderIdempotencyKey('cancel')
    }
  })
}
