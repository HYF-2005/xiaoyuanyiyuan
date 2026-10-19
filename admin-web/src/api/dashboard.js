import http from './http'

// 数据看板接口；days 只接受 7/14/30，后端会校验。
export async function fetchAdminDashboard(days = 14) {
  const { data } = await http.get('/admin/dashboard', { params: { days } })
  return data
}
