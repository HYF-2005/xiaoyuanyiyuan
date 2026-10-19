import http from './http'

// 用户反馈查询与处理。
export async function fetchAdminFeedback(params) {
  const { data } = await http.get('/admin/feedback', { params })
  return data
}

export async function fetchAdminFeedbackDetail(id) {
  const { data } = await http.get(`/admin/feedback/${id}`)
  return data
}

export async function handleAdminFeedback(id, payload) {
  const { data } = await http.put(`/admin/feedback/${id}`, payload)
  return data
}
