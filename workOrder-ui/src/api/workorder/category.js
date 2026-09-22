import request from '@/utils/request'

export function listManagedCategories() {
  return request({
    url: '/workorder/categories/manage',
    method: 'get'
  })
}

export function getCategory(categoryId) {
  return request({
    url: '/workorder/categories/' + encodeURIComponent(categoryId),
    method: 'get'
  })
}

export function addCategory(data) {
  return request({
    url: '/workorder/categories',
    method: 'post',
    data
  })
}

export function updateCategory(categoryId, data) {
  return request({
    url: '/workorder/categories/' + encodeURIComponent(categoryId),
    method: 'put',
    data
  })
}
