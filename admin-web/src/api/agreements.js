import http from './http'

// 协议管理。
export async function fetchAdminAgreements() {
  const { data } = await http.get('/admin/agreements')
  return data
}

export async function createAdminAgreement(payload) {
  const { data } = await http.post('/admin/agreements', payload)
  return data
}

export async function updateAdminAgreement(id, payload) {
  const { data } = await http.put(`/admin/agreements/${id}`, payload)
  return data
}

export async function deleteAdminAgreement(id) {
  const { data } = await http.delete(`/admin/agreements/${id}`)
  return data
}
