<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="sidebar-mark">援</div>
        <div class="sidebar-title">
          <strong>校园易援</strong>
          <span>管理后台</span>
        </div>
      </div>

      <div class="sidebar-label">运营中心</div>

      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        :unique-opened="true"
        @select="onMenuSelect"
      >
        <el-menu-item v-if="can('dashboard')" index="dashboard">
          <el-icon><Odometer /></el-icon>
          <span>数据看板</span>
        </el-menu-item>

        <el-sub-menu v-if="can('users')" index="users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="users-list">用户列表</el-menu-item>
          <el-menu-item index="users-status">账号状态</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="can('orders')" index="orders">
          <template #title>
            <el-icon><Tickets /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item index="orders-list">订单列表</el-menu-item>
          <el-menu-item index="orders-risk">风险订单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="can('finance')" index="finance">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item index="finance-ledger">收入流水</el-menu-item>
          <el-menu-item index="finance-summary">对账汇总</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="can('wallet')" index="wallet">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>钱包管理</span>
          </template>
          <el-menu-item index="wallet-accounts">钱包账户</el-menu-item>
          <el-menu-item index="wallet-adjust">调账记录</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="can('content')" index="content">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>业务配置</span>
          </template>
          <el-menu-item index="content-banners">轮播管理</el-menu-item>
          <el-menu-item index="content-announcements">公告管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="can('locations')" index="locations">
          <el-icon><Location /></el-icon>
          <span>地理信息</span>
        </el-menu-item>

        <el-sub-menu v-if="can('system')" index="system">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="system-settings">系统参数</el-menu-item>
          <el-menu-item v-if="isSuper" index="system-logs">操作日志</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="can('agreements')" index="agreements">
          <el-icon><Document /></el-icon>
          <span>协议管理</span>
        </el-menu-item>

        <el-menu-item v-if="can('feedback')" index="feedback">
          <el-icon><ChatDotRound /></el-icon>
          <span>用户反馈</span>
        </el-menu-item>

        <el-menu-item v-if="isSuper" index="admins">
          <el-icon><Avatar /></el-icon>
          <span>管理员管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot">
        <span class="version-dot"></span>
        v1.2.0 · 校园易援
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-left">
          <div class="breadcrumb">
            <span>运营中心</span>
            <i>/</i>
            <strong>{{ route.meta.title || '数据看板' }}</strong>
          </div>
        </div>

        <div class="topbar-right">
          <div class="admin-profile">
            <div class="admin-avatar">{{ adminInitial }}</div>
            <div class="admin-meta">
              <strong>{{ auth.admin?.displayName || '管理员' }}</strong>
              <span>{{ roleLabel }}</span>
            </div>
          </div>

          <el-dropdown trigger="click" @command="onCommand">
            <button class="more-button" type="button" aria-label="管理员菜单">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Avatar,
  ChatDotRound,
  Document,
  Location,
  Money,
  MoreFilled,
  Odometer,
  Setting,
  SwitchButton,
  Tickets,
  Tools,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.name === 'users') {
    return route.query.status === 'disabled' ? 'users-status' : 'users-list'
  }
  if (route.name === 'orders') {
    return 'orders-list'
  }
  if (route.name === 'risk-orders') {
    return 'orders-risk'
  }
  if (route.name === 'finance') {
    return route.query.view === 'summary' ? 'finance-summary' : 'finance-ledger'
  }
  return route.meta.activeMenu || 'dashboard'
})

const isSuper = computed(() => auth.admin?.role === 'super_admin')

function can(permission) {
  return auth.hasPermission(permission)
}

const adminInitial = computed(() => {
  const name = auth.admin?.displayName || auth.admin?.username || '管'
  return name.slice(0, 1).toUpperCase()
})

const roleLabel = computed(() => {
  const labels = {
    super_admin: '超级管理员',
    operator: '运营管理员',
    finance: '财务管理员',
    content: '内容管理员'
  }
  return labels[auth.admin?.role] || '管理员'
})

function onMenuSelect(index) {
  const routes = {
    dashboard: '/',
    'users-list': '/users',
    'users-status': '/users?status=disabled',
    'orders-list': '/orders',
    'orders-risk': '/orders/risk',
    'finance-ledger': '/finance',
    'finance-summary': '/finance?view=summary',
    'wallet-accounts': '/wallet/accounts',
    'wallet-adjust': '/wallet/adjust',
    'content-banners': '/content/banners',
    'content-announcements': '/content/announcements',
    locations: '/locations',
    'system-settings': '/system/settings',
    'system-logs': '/system/logs',
    agreements: '/agreements',
    feedback: '/feedback',
    admins: '/admins'
  }
  if (routes[index]) router.push(routes[index])
}

async function onCommand(command) {
  if (command !== 'logout') return
  try {
    await ElMessageBox.confirm('退出后需要重新登录后台，确认退出吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}
</script>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #f3f6fb;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex: 0 0 252px;
  flex-direction: column;
  width: 252px;
  height: 100vh;
  overflow-y: auto;
  background:
    radial-gradient(circle at 0% 0%, rgba(47, 125, 246, 0.18), transparent 35%),
    #071a32;
  scrollbar-width: thin;
  scrollbar-color: rgba(138, 166, 199, 0.25) transparent;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
}

.sidebar-mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 11px;
  background: linear-gradient(145deg, #4c98ff, #2164d6);
  box-shadow: 0 8px 20px rgba(21, 83, 182, 0.3);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.sidebar-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.sidebar-title strong {
  color: #f3f8ff;
  font-size: 15px;
  letter-spacing: 1px;
}

.sidebar-title span {
  color: #6f8bab;
  font-size: 9px;
  letter-spacing: 2.4px;
}

.sidebar-label {
  padding: 8px 22px 10px;
  color: #536f90;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 2.6px;
}

.sidebar-menu {
  flex: 1;
  border-right: 0;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #a8bbd3;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.055);
  --el-menu-base-level-padding: 20px;
  --el-menu-level-padding: 20px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  margin: 2px 13px;
  padding-right: 14px;
  border-radius: 9px;
  color: #a8bbd3;
  font-size: 12.5px;
  line-height: 44px;
  letter-spacing: 0.3px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: #f0f6ff;
  background: rgba(255, 255, 255, 0.055);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  position: relative;
  background: linear-gradient(90deg, rgba(47, 125, 246, 0.30), rgba(47, 125, 246, 0.08));
  color: #fff;
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  position: absolute;
  top: 11px;
  bottom: 11px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: #65a5ff;
  content: '';
}

.sidebar-menu :deep(.el-icon) {
  width: 18px;
  margin-right: 11px;
  color: #8da7c4;
  font-size: 17px;
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #8dbdff;
}

.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  right: 15px;
  color: #6f8cad;
  font-size: 12px;
  transition: color 0.18s ease, transform 0.22s ease;
}

.sidebar-menu :deep(.el-sub-menu__title:hover .el-sub-menu__icon-arrow) {
  color: #d6e6fa;
}

.sidebar-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title .el-sub-menu__icon-arrow),
.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-sub-menu__icon-arrow) {
  color: #9cc4ff;
}

.sidebar-menu :deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  margin: 2px 13px 6px;
  padding: 4px 0;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  min-width: 0;
  height: 38px;
  margin: 1px 8px;
  padding-left: 48px !important;
  color: #829bb8;
  font-size: 11.5px;
  line-height: 38px;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item:hover) {
  color: #eaf3ff;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item.is-active) {
  background: rgba(47, 125, 246, 0.16);
}

.sidebar-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 18px 22px 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(144, 174, 209, 0.12);
  color: #526f90;
  font-size: 10px;
  letter-spacing: 0.6px;
}

.version-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #2dd4bf;
  box-shadow: 0 0 0 3px rgba(45, 212, 191, 0.09);
}

.admin-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 32px;
  border-bottom: 1px solid #e7edf5;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #97a6b8;
  font-size: 12px;
}

.breadcrumb i {
  color: #c4cedb;
  font-style: normal;
}

.breadcrumb strong {
  color: #2b405b;
  font-size: 13px;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.admin-profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(145deg, #e0edff, #c9ddfb);
  color: #326ec8;
  font-size: 13px;
  font-weight: 700;
}

.admin-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-meta strong {
  color: #283c55;
  font-size: 12px;
  font-weight: 600;
}

.admin-meta span {
  color: #93a3b7;
  font-size: 9px;
  letter-spacing: 0.4px;
}

.more-button {
  display: grid;
  width: 34px;
  height: 34px;
  cursor: pointer;
  place-items: center;
  border: 1px solid #e4ebf4;
  border-radius: 9px;
  background: #fff;
  color: #7890aa;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.more-button:hover {
  border-color: #cddcf0;
  background: #f8fbff;
  color: #2f7df6;
}

.admin-content {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 30px 32px 42px;
}

@media (max-width: 1050px) {
  .admin-sidebar {
    flex-basis: 224px;
    width: 224px;
  }

  .admin-topbar {
    padding: 0 24px;
  }

  .admin-content {
    padding: 24px;
  }
}
</style>
