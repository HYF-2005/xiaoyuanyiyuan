import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import UserManagementView from '../views/UserManagementView.vue'
import OrderManagementView from '../views/OrderManagementView.vue'
import RiskOrdersView from '../views/RiskOrdersView.vue'
import FinanceView from '../views/FinanceView.vue'
import WalletView from '../views/WalletView.vue'
import ContentManagementView from '../views/ContentManagementView.vue'
import LocationsView from '../views/LocationsView.vue'
import SystemManagementView from '../views/SystemManagementView.vue'
import AgreementsView from '../views/AgreementsView.vue'
import FeedbackView from '../views/FeedbackView.vue'
import AdminsView from '../views/AdminsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true, title: '管理员登录' }
    },
    {
      path: '/',
      component: AdminLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '数据看板', activeMenu: 'dashboard', permission: 'dashboard' }
        },
        {
          path: 'users',
          name: 'users',
          component: UserManagementView,
          meta: { title: '用户管理', activeMenu: 'users-list', permission: 'users' }
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderManagementView,
          meta: { title: '订单管理', activeMenu: 'orders-list', permission: 'orders' }
        },
        {
          path: 'orders/risk',
          name: 'risk-orders',
          component: RiskOrdersView,
          meta: { title: '风险订单', activeMenu: 'orders-risk', permission: 'orders' }
        },
        {
          path: 'finance',
          name: 'finance',
          component: FinanceView,
          meta: { title: '财务管理', activeMenu: 'finance-ledger', permission: 'finance' }
        },
        {
          path: 'wallet/accounts',
          name: 'wallet-accounts',
          component: WalletView,
          meta: { title: '钱包管理', activeMenu: 'wallet-accounts', permission: 'wallet' }
        },
        {
          path: 'wallet/adjust',
          name: 'wallet-adjust',
          component: WalletView,
          meta: { title: '钱包管理', activeMenu: 'wallet-adjust', permission: 'wallet' }
        },
        {
          path: 'content/banners',
          name: 'content-banners',
          component: ContentManagementView,
          meta: { title: '业务配置', activeMenu: 'content-banners', permission: 'content' }
        },
        {
          path: 'content/announcements',
          name: 'content-announcements',
          component: ContentManagementView,
          meta: { title: '业务配置', activeMenu: 'content-announcements', permission: 'content' }
        },
        {
          path: 'locations',
          name: 'locations',
          component: LocationsView,
          meta: { title: '地理信息', activeMenu: 'locations', permission: 'locations' }
        },
        {
          path: 'system/settings',
          name: 'system-settings',
          component: SystemManagementView,
          meta: { title: '系统管理', activeMenu: 'system-settings', permission: 'system' }
        },
        {
          path: 'system/logs',
          name: 'system-logs',
          component: SystemManagementView,
          meta: { title: '系统管理', activeMenu: 'system-logs', permission: 'system' }
        },
        {
          path: 'agreements',
          name: 'agreements',
          component: AgreementsView,
          meta: { title: '协议管理', activeMenu: 'agreements', permission: 'agreements' }
        },
        {
          path: 'feedback',
          name: 'feedback',
          component: FeedbackView,
          meta: { title: '用户反馈', activeMenu: 'feedback', permission: 'feedback' }
        },
        {
          path: 'admins',
          name: 'admins',
          component: AdminsView,
          meta: { title: '管理员管理', activeMenu: 'admins', superOnly: true }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

const PERMISSION_LANDING = [
  ['dashboard', '/'],
  ['users', '/users'],
  ['orders', '/orders'],
  ['finance', '/finance'],
  ['wallet', '/wallet/accounts'],
  ['content', '/content/banners'],
  ['locations', '/locations'],
  ['system', '/system/settings'],
  ['agreements', '/agreements'],
  ['feedback', '/feedback']
]

function firstAllowedPath(auth) {
  const found = PERMISSION_LANDING.find(([code]) => auth.hasPermission(code))
  return found ? found[1] : '/login'
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    // 已登录用户访问登录页时，先确认会话仍有效，再回到后台首页。
    if (auth.isLoggedIn && (await auth.ensureAdmin())) {
      return { path: '/' }
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const ok = await auth.ensureAdmin()
  if (!ok) return { path: '/login', query: { redirect: to.fullPath } }

  if (to.meta.superOnly && auth.admin?.role !== 'super_admin') {
    return firstAllowedPath(auth)
  }
  if (to.meta.permission && !auth.hasPermission(to.meta.permission)) {
    return firstAllowedPath(auth)
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 校园易援管理后台` : '校园易援管理后台'
})

export default router
