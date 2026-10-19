<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">SYSTEM CONSOLE</p>
        <h1 class="adm-title">系统管理</h1>
        <p class="adm-sub">{{ mode === 'logs' ? '追踪后台写操作、登录和退出；只读查询不会写入日志。' : '查看当前运行环境、参数和模块接入状态，不提供危险的服务器配置修改。' }}</p>
      </div>
      <span class="adm-count">{{ mode === 'logs' ? '仅超级管理员可见' : `后台版本 ${info.version || '—'}` }}</span>
    </div>

    <div class="sys-tabs">
      <button type="button" :class="{ active: mode === 'settings' }" @click="switchMode('settings')">系统参数</button>
      <button type="button" :class="{ active: mode === 'logs' }" @click="switchMode('logs')">操作日志</button>
    </div>

    <template v-if="mode === 'settings'">
      <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span><button class="adm-btn adm-btn-sm" type="button" @click="loadInfo">重试</button></div>
      <div v-if="infoLoading" class="sys-loading">正在读取系统信息…</div>
      <template v-else>
        <div class="sys-cards">
          <article><span>后台版本</span><strong>{{ info.version || '—' }}</strong><small>管理后台前端/API</small></article>
          <article><span>运行环境</span><strong>{{ info.profile || '—' }}</strong><small>Spring Boot profile</small></article>
          <article><span>数据库</span><strong>{{ info.database || '—' }}</strong><small>MySQL {{ info.databaseVersion || '' }}</small></article>
          <article><span>Java</span><strong>{{ info.javaVersion || '—' }}</strong><small>后端运行版本</small></article>
        </div>

        <section class="adm-panel">
          <div class="adm-panel-head">
            <div><p class="adm-panel-eyebrow">RUNTIME PARAMETERS</p><h2 class="adm-panel-title">运行参数</h2></div>
            <span class="adm-panel-meta">服务器时间 {{ formatDateTime(info.serverTime) }}</span>
          </div>
          <div class="adm-table-wrap">
            <table class="adm-table">
              <thead><tr><th>参数</th><th>当前值</th><th>说明</th></tr></thead>
              <tbody>
                <tr v-for="item in info.parameters || []" :key="item.name">
                  <td class="adm-strong">{{ item.name }}</td>
                  <td>{{ item.value }}</td>
                  <td class="adm-muted">{{ item.note }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="adm-panel feature-panel">
          <div class="adm-panel-head">
            <div><p class="adm-panel-eyebrow">MODULE STATUS</p><h2 class="adm-panel-title">模块接入状态</h2></div>
          </div>
          <div class="feature-grid">
            <div v-for="item in info.features || []" :key="item.name" class="feature-item">
              <div><strong>{{ item.name }}</strong><span>{{ item.note }}</span></div>
              <span class="adm-tag" :class="featureClass(item.status)">{{ item.status }}</span>
            </div>
          </div>
        </section>
      </template>
      <p class="adm-foot">系统参数为只读展示，避免在后台误改端口、上传目录等启动配置；需要调整时请修改服务器配置并重启服务。</p>
    </template>

    <template v-else>
      <section class="adm-panel">
        <div class="adm-panel-head">
          <div><p class="adm-panel-eyebrow">AUDIT TRAIL</p><h2 class="adm-panel-title">操作日志</h2></div>
          <span class="adm-panel-meta">第 {{ page }} / {{ totalPages }} 页</span>
        </div>
        <div class="adm-filter">
          <label class="adm-search"><el-icon><Search /></el-icon><input v-model="keyword" placeholder="搜索管理员、动作或目标" maxlength="64" @keyup.enter="applyFilters" /></label>
          <select v-model="action" class="adm-select" @change="applyFilters">
            <option value="all">全部动作</option>
            <option v-for="item in actionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="logsLoading" @click="applyFilters">搜索</button>
          <button class="adm-btn" type="button" @click="resetFilters">重置</button>
        </div>
        <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span><button class="adm-btn adm-btn-sm" type="button" @click="loadLogs">重试</button></div>
        <div v-if="logsLoading" class="sys-loading">正在读取日志…</div>
        <template v-else>
          <div v-if="logs.length" class="adm-table-wrap">
            <table class="adm-table">
              <thead><tr><th>时间</th><th>管理员</th><th>动作</th><th>目标</th><th>详情</th><th>IP</th></tr></thead>
              <tbody>
                <tr v-for="item in logs" :key="item.id">
                  <td class="adm-muted">{{ formatDateTime(item.createdAt) }}</td>
                  <td class="adm-strong">{{ item.adminName }}</td>
                  <td><span class="adm-tag" :class="auditClass(item.action)">{{ actionLabel(item.action) }}</span></td>
                  <td class="adm-muted">{{ item.targetType || '—' }}<span v-if="item.targetId"> #{{ item.targetId }}</span></td>
                  <td>{{ item.detail || '—' }}</td>
                  <td class="adm-muted">{{ item.ip || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="adm-empty"><el-icon><Tickets /></el-icon><strong>暂无操作日志</strong><span>登录或执行写操作后会在这里留下记录</span></div>
          <div v-if="total > 0" class="adm-pagination">
            <span class="adm-pagination-meta">共 {{ total }} 条日志 · 第 {{ page }} / {{ totalPages }} 页</span>
            <div class="adm-page-buttons">
              <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">‹</button>
              <button v-for="item in visiblePages" :key="item" type="button" :class="{ active: item === page }" @click="changePage(item)">{{ item }}</button>
              <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">›</button>
            </div>
          </div>
        </template>
      </section>
      <p class="adm-foot">日志只追加，不提供后台删除；当前记录管理端写操作与登录/退出，不记录每次页面浏览或只读查询。</p>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Tickets, WarningFilled } from '@element-plus/icons-vue'
import { fetchAuditLogs, fetchSystemInfo } from '../api/system'

const route = useRoute()
const router = useRouter()
const mode = computed(() => (route.name === 'system-logs' ? 'logs' : 'settings'))
const info = ref({})
const infoLoading = ref(false)
const logs = ref([])
const logsLoading = ref(false)
const error = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
const action = ref('all')
const keyword = ref('')

const actionOptions = [
  { value: 'LOGIN', label: '登录' },
  { value: 'LOGOUT', label: '退出' },
  { value: 'USER_ENABLE', label: '启用用户' },
  { value: 'USER_DISABLE', label: '停用用户' },
  { value: 'USER_IMPORT', label: '导入用户' },
  { value: 'ORDER_CANCEL', label: '取消订单' },
  { value: 'ORDER_FORCE_COMPLETE', label: '强制完成' },
  { value: 'ORDER_RISK_SCAN', label: '扫描风险' },
  { value: 'ORDER_RISK_REVIEW', label: '审核风险' },
  { value: 'BANNER_CREATE', label: '创建轮播' },
  { value: 'BANNER_UPDATE', label: '更新轮播' },
  { value: 'BANNER_DELETE', label: '删除轮播' },
  { value: 'ANNOUNCEMENT_CREATE', label: '创建公告' },
  { value: 'ANNOUNCEMENT_UPDATE', label: '更新公告' },
  { value: 'ANNOUNCEMENT_DELETE', label: '删除公告' },
  { value: 'FEEDBACK_HANDLE', label: '处理反馈' },
  { value: 'AGREEMENT_CREATE', label: '创建协议' },
  { value: 'AGREEMENT_UPDATE', label: '更新协议' },
  { value: 'AGREEMENT_DELETE', label: '删除协议' },
  { value: 'ADMIN_CREATE', label: '创建管理员' },
  { value: 'ADMIN_UPDATE', label: '更新管理员' },
  { value: 'ADMIN_ENABLE', label: '启用管理员' },
  { value: 'ADMIN_DISABLE', label: '停用管理员' },
  { value: 'ADMIN_RESET_PASSWORD', label: '重置密码' },
  { value: 'ADMIN_PERMISSIONS', label: '更新权限' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, Math.min(page.value - 2, totalPages.value - 4))
  for (let i = start; i <= Math.min(totalPages.value, start + 4); i += 1) pages.push(i)
  return pages
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}

function actionLabel(value) {
  return actionOptions.find(item => item.value === value)?.label || value
}

function auditClass(value) {
  if (value.startsWith('LOGIN') || value.startsWith('USER_ENABLE') || value.endsWith('_ENABLE')) return 'adm-tag-green'
  if (value.includes('DELETE') || value.includes('DISABLE') || value === 'ORDER_CANCEL') return 'adm-tag-red'
  if (value.includes('FORCE') || value.includes('RESET') || value === 'ORDER_RISK_REVIEW') return 'adm-tag-amber'
  return 'adm-tag-blue'
}

function featureClass(value) {
  if (value === '已接入') return 'adm-tag-green'
  if (value === '只读演示') return 'adm-tag-amber'
  return 'adm-tag-gray'
}

async function loadInfo() {
  infoLoading.value = true
  error.value = ''
  try {
    info.value = await fetchSystemInfo()
  } catch (err) {
    error.value = err.message || '系统信息加载失败'
  } finally {
    infoLoading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  error.value = ''
  try {
    const data = await fetchAuditLogs({ page: page.value, size, action: action.value, keyword: keyword.value.trim() })
    logs.value = data.items || []
    total.value = data.total || 0
  } catch (err) {
    error.value = err.message || '操作日志加载失败'
  } finally {
    logsLoading.value = false
  }
}

function switchMode(value) {
  router.replace(`/system/${value}`)
}

function applyFilters() {
  page.value = 1
  loadLogs()
}

function resetFilters() {
  action.value = 'all'
  keyword.value = ''
  page.value = 1
  loadLogs()
}

function changePage(value) {
  if (value < 1 || value > totalPages.value) return
  page.value = value
  loadLogs()
}

watch(mode, (value) => {
  error.value = ''
  if (value === 'settings') loadInfo()
  else loadLogs()
})

onMounted(() => {
  if (mode.value === 'settings') loadInfo()
  else loadLogs()
})
</script>

<style scoped>
.sys-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}

.sys-tabs button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #fff;
  color: #76889d;
  font-size: 11px;
  cursor: pointer;
}

.sys-tabs button.active {
  border-color: #d7e6fb;
  background: #edf5ff;
  color: #2f7df6;
  font-weight: 600;
}

.sys-loading {
  padding: 42px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.sys-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.sys-cards article {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid #e7eef7;
  border-radius: 14px;
  background: #fff;
}

.sys-cards span {
  color: #8b9caf;
  font-size: 10px;
}

.sys-cards strong {
  color: #1f3650;
  font-size: 19px;
}

.sys-cards small {
  color: #a1afbe;
  font-size: 9px;
}

.feature-panel {
  margin-top: 16px;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
  padding: 0 20px 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fa;
}

.feature-item div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.feature-item strong {
  color: #344b63;
  font-size: 11px;
}

.feature-item span:not(.adm-tag) {
  color: #9aa9ba;
  font-size: 9px;
}

@media (max-width: 900px) {
  .sys-cards,
  .feature-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .sys-cards,
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
