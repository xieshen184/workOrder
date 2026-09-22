import request from '@/utils/request'
import {
  uploadAttachment as uploadOrderAttachment,
  deleteAttachment as deleteOrderAttachment
} from '@/api/order/submit'

const DEFAULT_PAGE_SIZE = 10
let idempotencySequence = 0

// A key is created once for a user-visible submit and reused by that submit's retry.
// The backend therefore receives the same replay key instead of a second command.
export function createIdempotencyKey(action = 'ACTION') {
  const safeAction = String(action).replace(/[^a-zA-Z0-9_-]/g, '-').toLowerCase()
  const timestamp = Date.now().toString(36)
  idempotencySequence = (idempotencySequence + 1) % 1679616
  return `m1b02-${safeAction}-${timestamp}-${idempotencySequence.toString(36)}`.slice(0, 64)
}

function orderUrl(orderId, action) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return `/workorder/orders/${encodeURIComponent(orderId)}/${action}`
}

function postAction(orderId, action, data, idempotencyKey) {
  const url = orderUrl(orderId, action)
  if (url && typeof url.then === 'function') return url

  return request({
    url,
    method: 'post',
    data,
    // Callers retain this key when a submit fails so a retry is replay-safe.
    header: {
      'Idempotency-Key': idempotencyKey || createIdempotencyKey(action)
    }
  })
}

export function getEngineerOrders(params = {}) {
  return request({
    url: '/workorder/engineer/orders',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || DEFAULT_PAGE_SIZE,
      status: params.status || ''
    }
  })
}

// Kept as a small compatibility alias for pages that already use getOrders.
// It still always calls the real engineer list endpoint.
export function getOrders(params = {}) {
  return getEngineerOrders(typeof params === 'string' ? { status: params } : params)
}

export function getOrderDetail(orderId) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return request({
    url: `/workorder/orders/${encodeURIComponent(orderId)}`,
    method: 'get'
  })
}

export function acceptOrder(orderId, data = {}) {
  return postAction(orderId, 'accept', {
    version: data.version
  }, data.idempotencyKey)
}

export function arriveOrder(orderId, data = {}) {
  return postAction(orderId, 'arrive', {
    content: data.content,
    attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
    version: data.version
  }, data.idempotencyKey)
}

export function assessmentOrder(orderId, data = {}) {
  return postAction(orderId, 'assessment', {
    content: data.content,
    requiresParts: Boolean(data.requiresParts),
    partsDescription: data.partsDescription || '',
    assessedHours: data.assessedHours === '' || data.assessedHours === undefined
      ? null
      : data.assessedHours,
    assessedUrgency: data.assessedUrgency === '' || data.assessedUrgency === undefined
      ? null
      : data.assessedUrgency,
    assessedScope: data.assessedScope === '' || data.assessedScope === undefined
      ? null
      : data.assessedScope,
    requiresExtension: Boolean(data.requiresExtension),
    attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
    version: data.version
  }, data.idempotencyKey)
}

export function progressOrder(orderId, data = {}) {
  return postAction(orderId, 'progress', {
    content: data.content,
    attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
    version: data.version
  }, data.idempotencyKey)
}

export function finishOrder(orderId, data = {}) {
  return postAction(orderId, 'finish', {
    content: data.content,
    attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
    version: data.version
  }, data.idempotencyKey)
}

// The upload endpoint returns the server-issued attachment id. Pages must use that
// id in the action body; this method never creates or substitutes an attachment id.
export function uploadAttachment(filePath, name = 'file', bizStage = 'SUBMIT') {
  return uploadOrderAttachment(filePath, name, bizStage)
}

export function deleteAttachment(attachmentId) {
  if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
    return Promise.reject(new Error('附件编号不能为空'))
  }
  return deleteOrderAttachment(attachmentId)
}

const orderApi = {
  getEngineerOrders,
  getOrders,
  getOrderDetail,
  acceptOrder,
  arriveOrder,
  assessmentOrder,
  progressOrder,
  finishOrder,
  uploadAttachment,
  deleteAttachment,
  createIdempotencyKey
}

export default orderApi
