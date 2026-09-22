import request from '@/utils/request'

// Work-order APIs intentionally stay in this module so the page never falls
// back to local data when the service is unavailable.
export function listOrders(query) {
  return request({
    url: '/workorder/admin/orders',
    method: 'get',
    params: query
  })
}

export function getOrder(orderId) {
  return request({
    url: '/workorder/orders/' + encodeURIComponent(orderId),
    method: 'get'
  })
}

export function getOrderEvaluation(orderId) {
  return request({
    url: '/workorder/orders/' + encodeURIComponent(orderId) + '/evaluation',
    method: 'get'
  })
}

export function getEngineers(query) {
  return request({
    url: '/workorder/admin/engineers',
    method: 'get',
    params: query
  })
}

// Category data is already exposed by the work-order service and keeps the
// categoryId filter usable without duplicating category labels in the UI.
export function listCategories() {
  return request({
    url: '/workorder/categories',
    method: 'get'
  })
}

export function createIdempotencyKey() {
  const cryptoApi = typeof window !== 'undefined' && window.crypto
  if (cryptoApi && typeof cryptoApi.randomUUID === 'function') {
    return cryptoApi.randomUUID()
  }
  return 'wo-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
}

function submitCommand(path, data, idempotencyKey) {
  return request({
    url: path,
    method: 'post',
    // The server-side key is the source of truth for retries. Disable the
    // generic one-second client guard so an intentional retry can reach it.
    headers: {
      'Idempotency-Key': idempotencyKey || createIdempotencyKey(),
      repeatSubmit: false
    },
    data: data
  })
}

export function assignOrder(orderId, data, idempotencyKey) {
  return submitCommand(
    '/workorder/orders/' + encodeURIComponent(orderId) + '/assign',
    data,
    idempotencyKey
  )
}

export function reassignOrder(orderId, data, idempotencyKey) {
  return submitCommand(
    '/workorder/orders/' + encodeURIComponent(orderId) + '/reassign',
    data,
    idempotencyKey
  )
}

export function returnOrder(orderId, data, idempotencyKey) {
  return submitCommand(
    '/workorder/orders/' + encodeURIComponent(orderId) + '/return',
    data,
    idempotencyKey
  )
}

// request.js adds the current Bearer token to this blob request, so the
// private attachment route is never opened as an unauthenticated URL.
export function downloadAttachment(attachmentId) {
  return request({
    url: '/workorder/attachments/' + encodeURIComponent(attachmentId),
    method: 'get',
    responseType: 'blob'
  })
}
