import http from './http'

// 校园地址字典：后台维护驿站、食堂档口和送达建筑。
export async function fetchAdminLocations() {
  const { data } = await http.get('/admin/locations')
  return data
}

export async function createAdminLocation(payload) {
  const { data } = await http.post('/admin/locations', payload)
  return data
}

export async function updateAdminLocation(id, payload) {
  const { data } = await http.put(`/admin/locations/${id}`, payload)
  return data
}

export async function deleteAdminLocation(id) {
  const { data } = await http.delete(`/admin/locations/${id}`)
  return data
}
