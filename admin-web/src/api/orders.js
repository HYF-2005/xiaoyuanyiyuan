import http from './http'

// 后台订单管理接口。
export async function fetchAdminOrders(params) {
  const { data } = await http.get('/admin/orders', { params })
  return data
}

export async function fetchAdminOrderDetail(id) {
  const { data } = await http.get(`/admin/orders/${id}`)
  return data
}

export async function cancelAdminOrder(id) {
  const { data } = await http.patch(`/admin/orders/${id}/cancel`)
  return data
}

export async function forceCompleteAdminOrder(id) {
  const { data } = await http.patch(`/admin/orders/${id}/complete`)
  return data
}

export async function fetchAdminOrderProof(id) {
  const response = await http.get(`/admin/orders/${id}/proof`, { responseType: 'blob' })
  return response.data
}

// 风险订单：自动扫描队列、手动重扫和人工审核。
export async function fetchRiskOrders(params) {
  const { data } = await http.get('/admin/risk-orders', { params })
  return data
}

export async function scanRiskOrders() {
  const { data } = await http.post('/admin/risk-orders/scan')
  return data
}

export async function reviewRiskOrder(id, payload) {
  const { data } = await http.patch(`/admin/risk-orders/${id}/review`, payload)
  return data
}
