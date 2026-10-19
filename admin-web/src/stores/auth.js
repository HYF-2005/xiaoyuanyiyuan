import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchAdminMe, loginAdmin, logoutAdmin } from '../api/admin'
import { ALL_PERMISSIONS, ROLE_PERMISSIONS } from '../constants/permissions'

const TOKEN_KEY = 'campus_admin_token'
const PROFILE_KEY = 'campus_admin_profile'

function readProfile() {
  try {
    return JSON.parse(sessionStorage.getItem(PROFILE_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const admin = ref(readProfile())
  // 内存中的校验标记：刷新页面后必须重新请求 /me，不能只相信 sessionStorage。
  const validated = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const permissions = computed(() => {
    if (admin.value?.role === 'super_admin') return [...ALL_PERMISSIONS]
    if (Array.isArray(admin.value?.permissions)) return admin.value.permissions
    return ROLE_PERMISSIONS[admin.value?.role] || []
  })

  function hasPermission(code) {
    return permissions.value.includes(code)
  }

  function persist() {
    if (token.value) sessionStorage.setItem(TOKEN_KEY, token.value)
    else sessionStorage.removeItem(TOKEN_KEY)
    if (admin.value) sessionStorage.setItem(PROFILE_KEY, JSON.stringify(admin.value))
    else sessionStorage.removeItem(PROFILE_KEY)
  }

  async function login(credentials) {
    const result = await loginAdmin(credentials)
    token.value = result.token
    admin.value = result.admin
    validated.value = true
    persist()
    return result
  }

  async function ensureAdmin() {
    if (!token.value) return false
    if (validated.value && admin.value) return true
    try {
      admin.value = await fetchAdminMe()
      validated.value = true
      persist()
      return true
    } catch {
      clear()
      return false
    }
  }

  async function logout() {
    try {
      await logoutAdmin()
    } catch {
      // 会话已过期时也要清理本地状态，不能让退出按钮卡住。
    } finally {
      clear()
    }
  }

  function clear() {
    token.value = ''
    admin.value = null
    validated.value = false
    persist()
  }

  return { token, admin, isLoggedIn, permissions, hasPermission, login, ensureAdmin, logout, clear }
})
