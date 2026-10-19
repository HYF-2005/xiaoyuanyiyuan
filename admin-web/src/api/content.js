import http from './http'

// 轮播管理。
export async function fetchAdminBanners() {
  const { data } = await http.get('/admin/content/banners')
  return data
}

export async function createAdminBanner(payload) {
  const { data } = await http.post('/admin/content/banners', payload)
  return data
}

export async function updateAdminBanner(id, payload) {
  const { data } = await http.put(`/admin/content/banners/${id}`, payload)
  return data
}

export async function deleteAdminBanner(id) {
  const { data } = await http.delete(`/admin/content/banners/${id}`)
  return data
}

// 公告管理。
export async function fetchAdminAnnouncements() {
  const { data } = await http.get('/admin/content/announcements')
  return data
}

export async function createAdminAnnouncement(payload) {
  const { data } = await http.post('/admin/content/announcements', payload)
  return data
}

export async function updateAdminAnnouncement(id, payload) {
  const { data } = await http.put(`/admin/content/announcements/${id}`, payload)
  return data
}

export async function deleteAdminAnnouncement(id) {
  const { data } = await http.delete(`/admin/content/announcements/${id}`)
  return data
}
