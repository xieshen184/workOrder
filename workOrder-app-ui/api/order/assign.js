import request from '@/utils/request'

const DEFAULT_PAGE_SIZE = 10
let idempotencySequence = 0

const TAB_STATUS = {
  applying: 'WAIT_ASSIGN',
  dispatched: 'WAIT_ACCEPT'
}

function requireOrderId(orderId) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return Promise.reject(new Error('工单编号不能为空'))
  }
  return encodeURIComponent(orderId)
}

// 页面保存本次提交的 key；网络失败重试时复用，选择人员或版本变化后重新生成。
export function createDispatchIdempotencyKey(action = 'assign') {
  const safeAction = String(action).replace(/[^a-zA-Z0-9_.:-]/g, '-').toLowerCase()
  idempotencySequence = (idempotencySequence + 1) % 1679616
  return `m1b04-${safeAction}-${Date.now().toString(36)}-${idempotencySequence.toString(36)}`.slice(0, 64)
}

function dateRange(date) {
  if (!date) return undefined
  return {
    beginTime: `${date} 00:00:00`,
    endTime: `${date} 23:59:59`
  }
}

function mapOrder(order = {}) {
  return {
    ...order,
    id: order.id,
    orderNo: order.orderNo || String(order.id || ''),
    applicant: order.applicantName || '—',
    department: order.applicantDeptName || '—',
    phone: order.applicantPhone || '—',
    category: order.categoryName || '未分类',
    urgency: ({ 1: '一般', 2: '紧急', 3: '特急' })[order.urgencyLevel] || '未填写',
    scope: ({ 1: '个人事件', 2: '科室事件', 3: '多科室事件', 4: '全院事件' })[order.impactScope] || '未填写',
    explanation: order.possibleCause || '',
    createTime: order.submittedAt || order.createTime || '',
    dispatchTime: order.assignedAt || '',
    engineerId: order.currentAssigneeId,
    engineerName: order.currentAssigneeName || '',
    dispatchLog: (Array.isArray(order.timeline) ? order.timeline : []).map(item => ({
      time: item.actionTime || '',
      content: item.actionContent || item.actionType || '',
      operator: item.operatorName || ''
    })),
    attachments: Array.isArray(order.attachments) ? order.attachments : [],
    allowedActions: Array.isArray(order.allowedActions) ? order.allowedActions : []
  }
}

export function getOrders(tab, date, paging = {}) {
  const status = TAB_STATUS[tab]
  if (!status) return Promise.reject(new Error('不支持的派单列表状态'))

  return request({
    url: '/workorder/admin/orders',
    method: 'get',
    params: {
      pageNum: paging.pageNum || 1,
      pageSize: paging.pageSize || DEFAULT_PAGE_SIZE,
      status,
      params: dateRange(date)
    }
  }).then(response => ({
    rows: (Array.isArray(response.rows) ? response.rows : []).map(mapOrder),
    total: Number(response.total || 0)
  }))
}

export function getEngineers() {
  return request({
    url: '/workorder/admin/engineers',
    method: 'get'
  }).then(response => (Array.isArray(response.data) ? response.data : [])
    // 未上报心跳不等于休班，因此这里只排除明确休班的人员，方便本地联调账号正常派单。
    .filter(engineer => engineer.dutyStatus !== 'OFF_DUTY')
    .map(engineer => ({
      ...engineer,
      department: engineer.dept || '未分配科室',
      specialty: `状态：${engineer.status || '未知'} · 当前负载：${Number(engineer.load || 0)}`
    })))
}

export function getOrderDetail(orderId) {
  const encodedId = requireOrderId(orderId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId
  return request({
    url: `/workorder/orders/${encodedId}`,
    method: 'get'
  }).then(response => mapOrder(response.data || {}))
}

function submitAssignment(orderId, action, data = {}) {
  const encodedId = requireOrderId(orderId)
  if (encodedId && typeof encodedId.then === 'function') return encodedId
  return request({
    url: `/workorder/orders/${encodedId}/${action}`,
    method: 'post',
    data: {
      engineerId: data.engineerId,
      reason: String(data.reason || '').trim(),
      version: data.version
    },
    header: {
      'Idempotency-Key': data.idempotencyKey || createDispatchIdempotencyKey(action)
    }
  })
}

export function assignOrder(orderId, data) {
  return submitAssignment(orderId, 'assign', data)
}

export function reassignOrder(orderId, data) {
  return submitAssignment(orderId, 'reassign', data)
}

const dispatchApi = {
  getOrders,
  getEngineers,
  getOrderDetail,
  assignOrder,
  reassignOrder,
  createDispatchIdempotencyKey
}

export default dispatchApi
