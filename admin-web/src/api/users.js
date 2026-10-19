import http from './http'

// 后台用户管理接口。
export async function fetchAdminUsers(params) {
  const { data } = await http.get('/admin/users', { params })
  return data
}

export async function fetchAdminUserDetail(id) {
  const { data } = await http.get(`/admin/users/${id}`)
  return data
}

export async function updateAdminUserStatus(id, active) {
  const { data } = await http.patch(`/admin/users/${id}/status`, { active })
  return data
}

export async function importAdminUsers(file) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post('/admin/users/import', form)
  return data
}
