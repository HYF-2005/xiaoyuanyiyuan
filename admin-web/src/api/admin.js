import http from './http'

// 后台认证接口；与小程序 /api/auth 完全分开。
export async function loginAdmin(payload) {
  const { data } = await http.post('/admin/auth/login', payload)
  return data
}

export async function fetchAdminMe() {
  const { data } = await http.get('/admin/auth/me')
  return data
}

export async function logoutAdmin() {
  const { data } = await http.post('/admin/auth/logout')
  return data
}
