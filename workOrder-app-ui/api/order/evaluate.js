import request from '@/utils/request'
import { createOrderIdempotencyKey } from '@/api/order/core'

const DEFAULT_PAGE_SIZE = 10

function requireOrderId(orderId) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return encodeURIComponent(orderId)
}

function postCommand(orderId, action, data = {}) {
  const encodedId = requireOrderId(orderId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId

  return request({
    url: `/workorder/orders/${encodedId}/${action}`,
    method: 'post',
    data: data.payload,
    header: {
      'Idempotency-Key': data.idempotencyKey || createOrderIdempotencyKey(action)
    }
  })
}

function getEvaluationOrders(status, params = {}) {
  return request({
    url: '/workorder/orders/my',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || DEFAULT_PAGE_SIZE,
      status
    }
  })
}

export function getPendingEvaluationOrders(params = {}) {
  return getEvaluationOrders('COMPLETED', params)
}

export function getEvaluatedOrders(params = {}) {
  return getEvaluationOrders('CLOSED', params)
}

export function getOrderForEvaluation(orderId) {
  const encodedId = requireOrderId(orderId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId
  return request({
    url: `/workorder/orders/${encodedId}`,
    method: 'get'
  })
}

export function confirmOrder(orderId, data = {}) {
  return postCommand(orderId, 'confirm', {
    idempotencyKey: data.idempotencyKey,
    payload: { version: data.version }
  })
}

export function returnOrder(orderId, data = {}) {
  return postCommand(orderId, 'return', {
    idempotencyKey: data.idempotencyKey,
    payload: {
      reason: String(data.reason || '').trim(),
      version: data.version
    }
  })
}

export function submitEvaluation(orderId, data = {}) {
  return postCommand(orderId, 'evaluation', {
    idempotencyKey: data.idempotencyKey,
    payload: {
      overallScore: Number(data.overallScore === undefined ? data.score : data.overallScore),
      evaluationContent: String(data.evaluationContent === undefined ? (data.content || '') : data.evaluationContent).trim(),
      responseScore: data.responseScore === undefined ? null : data.responseScore,
      qualityScore: data.qualityScore === undefined ? null : data.qualityScore,
      attitudeScore: data.attitudeScore === undefined ? null : data.attitudeScore,
      version: data.version
    }
  })
}

export function getEvaluation(orderId) {
  const encodedId = requireOrderId(orderId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId
  return request({
    url: `/workorder/orders/${encodedId}/evaluation`,
    method: 'get'
  })
}

const evaluationApi = {
  getPendingEvaluationOrders,
  getEvaluatedOrders,
  getOrderForEvaluation,
  confirmOrder,
  returnOrder,
  submitEvaluation,
  getEvaluation
}

export default evaluationApi
