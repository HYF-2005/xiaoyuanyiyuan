import http from './http'

// 管理员管理，仅超级管理员接口开放。
export async function fetchAdmins() {
  const { data } = await http.get('/admin/admins')
  return data
}

export async function createAdmin(payload) {
  const { data } = await http.post('/admin/admins', payload)
  return data
}

export async function updateAdminProfile(id, payload) {
  const { data } = await http.put(`/admin/admins/${id}`, payload)
  return data
}

export async function updateAdminStatus(id, active) {
  const { data } = await http.patch(`/admin/admins/${id}/status`, { active })
  return data
}

export async function updateAdminPermissions(id, payload) {
  const { data } = await http.put(`/admin/admins/${id}/permissions`, payload)
  return data
}

export async function resetAdminPassword(id, password) {
  const { data } = await http.patch(`/admin/admins/${id}/password`, { password })
  return data
}
