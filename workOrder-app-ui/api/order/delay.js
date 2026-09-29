import request from '@/utils/request'
import upload from '@/utils/upload'

// 延期佐证使用独立阶段，便于详情页区分报修、过程、完工和延期附件。
const DELAY_ATTACHMENT_STAGE = 'DELAY'
let idempotencySequence = 0

function orderUrl(orderId, suffix) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return `/workorder/orders/${encodeURIComponent(orderId)}${suffix}`
}

// 同一次可见提交始终复用该键，网络失败重试时由服务端返回首次申请结果。
export function createDelayIdempotencyKey(orderId = 'order') {
  const safeOrderId = String(orderId).replace(/[^a-zA-Z0-9_-]/g, '-')
  idempotencySequence = (idempotencySequence + 1) % 1679616
  return `delay-${safeOrderId}-${Date.now().toString(36)}-${idempotencySequence.toString(36)}`.slice(0, 64)
}

export function getLatestDelayRequest(orderId) {
  const url = orderUrl(orderId, '/delay-requests/latest')
  if (url && typeof url.then === 'function') return url

  return request({
    url,
    method: 'get'
  })
}

export function submitDelayRequest(orderId, data = {}) {
  const url = orderUrl(orderId, '/delay-requests')
  if (url && typeof url.then === 'function') return url

  const idempotencyKey = data.idempotencyKey || createDelayIdempotencyKey(orderId)
  return request({
    url,
    method: 'post',
    data: {
      requestedDeadline: data.requestedDeadline,
      reason: data.reason,
      attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
      version: data.version
    },
    // 后端使用该键和请求摘要防止重复创建，也拒绝同键提交不同内容。
    header: {
      'Idempotency-Key': idempotencyKey
    }
  })
}

export function uploadAttachment(filePath, name = 'file') {
  return upload({
    url: '/workorder/attachments',
    filePath,
    name,
    formData: { bizStage: DELAY_ATTACHMENT_STAGE }
  })
}

export function deleteAttachment(attachmentId) {
  if (attachmentId === undefined || attachmentId === null || attachmentId === '') {
    return Promise.reject(new Error('附件编号不能为空'))
  }
  return request({
    url: `/workorder/attachments/${encodeURIComponent(attachmentId)}`,
    method: 'delete'
  })
}

const delayApi = {
  createDelayIdempotencyKey,
  getLatestDelayRequest,
  submitDelayRequest,
  uploadAttachment,
  deleteAttachment
}

export default delayApi
