import axios from 'axios'

// 后台统一请求入口；开发环境由 Vite 把 /api 代理到 8080。
const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('campus_admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // 登录接口的 401 要留在登录页展示“账号或密码错误”，不能当成会话过期处理。
    if (error.response?.status === 401 && !error.config?.url?.includes('/admin/auth/login')) {
      sessionStorage.removeItem('campus_admin_token')
      sessionStorage.removeItem('campus_admin_profile')
    }
    return Promise.reject(error)
  }
)

export function errorMessage(error, fallback = '请求失败，请稍后重试') {
  return error.response?.data?.message || error.message || fallback
}

export default http
