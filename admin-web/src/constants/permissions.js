import {
  ChatDotRound,
  Document,
  Location,
  Money,
  Odometer,
  Setting,
  Tickets,
  Tools,
  User,
  Wallet
} from '@element-plus/icons-vue'

// 权限编码与后台侧边栏一一对应；顺序也作为权限弹窗的展示顺序。
export const PERMISSION_OPTIONS = [
  { value: 'dashboard', label: '数据看板', icon: Odometer },
  { value: 'users', label: '用户管理', icon: User },
  { value: 'orders', label: '订单管理', icon: Tickets },
  { value: 'finance', label: '财务管理', icon: Money },
  { value: 'wallet', label: '钱包管理', icon: Wallet },
  { value: 'content', label: '业务配置', icon: Setting },
  { value: 'locations', label: '地理信息', icon: Location },
  { value: 'system', label: '系统管理', icon: Tools },
  { value: 'agreements', label: '协议管理', icon: Document },
  { value: 'feedback', label: '用户反馈', icon: ChatDotRound }
]

export const ALL_PERMISSIONS = Object.freeze(PERMISSION_OPTIONS.map(item => item.value))

// 与后端 AdminPermissions 的默认值保持一致；旧会话没有 permissions 字段时走这里。
export const ROLE_PERMISSIONS = {
  super_admin: ALL_PERMISSIONS,
  operator: Object.freeze(['dashboard', 'users', 'orders', 'locations', 'feedback']),
  finance: Object.freeze(['dashboard', 'finance', 'wallet']),
  content: Object.freeze(['dashboard', 'content', 'agreements', 'feedback'])
}
