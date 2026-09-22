import request from '@/utils/request'
import upload from '@/utils/upload'

export function submitRepair(data = {}) {
  // 登录人资料由 /getInfo 提供；创建接口只接收工单业务字段，幂等键通过请求头单独传递。
  return request({
    url: '/workorder/orders',
    method: 'post',
    data: {
      categoryId: data.categoryId,
      location: data.location,
      urgencyLevel: data.urgencyLevel,
      impactScope: data.impactScope,
      description: data.description,
      possibleCause: data.possibleCause || '',
      attachmentIds: Array.isArray(data.attachmentIds) ? data.attachmentIds : [],
      sourceType: 1
    },
    header: {
      'Idempotency-Key': data.idempotencyKey
    }
  })
}

export function uploadAttachment(filePath, name = 'file', bizStage = 'SUBMIT') {
  return upload({
    url: '/workorder/attachments',
    filePath,
    name,
    // SUBMIT remains the default for the existing create-order uploader.
    formData: { bizStage }
  })
}

export function deleteAttachment(attachmentId) {
  // 服务端仅允许删除当前用户尚未绑定到工单的附件，页面须等请求成功后再移除本地项。
  return request({
    url: `/workorder/attachments/${encodeURIComponent(attachmentId)}`,
    method: 'delete'
  })
}

export function getCategories() {
  return request({
    url: '/workorder/categories',
    method: 'get'
  })
}

export function getDeptList() {
  return request({
    url: '/system/dept/list',
    method: 'get'
  })
}
