import http from './http'

// 系统只读信息和操作审计。
export async function fetchSystemInfo() {
  const { data } = await http.get('/admin/system/info')
  return data
}

export async function fetchAuditLogs(params) {
  const { data } = await http.get('/admin/audit-logs', { params })
  return data
}

export async function fetchGeographyOverview() {
  const { data } = await http.get('/admin/geography/overview')
  return data
}
