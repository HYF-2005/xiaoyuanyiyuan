<template>
  <div class="dashboard-page">
    <div class="page-head">
      <div>
        <p class="page-eyebrow">OVERVIEW / LIVE DATA</p>
        <h1>数据看板</h1>
        <p class="page-sub">{{ todayText }} · 数据来自 MySQL 实时统计</p>
      </div>
      <div class="head-actions">
        <span class="updated-at">更新于 {{ updatedAt }}</span>
        <button class="refresh-button" type="button" :disabled="loading" @click="load(true)">
          <el-icon :class="{ spinning: loading }"><Refresh /></el-icon>
          刷新
        </button>
      </div>
    </div>

    <div v-if="error" class="error-banner">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <button type="button" @click="load(false)">重试</button>
    </div>

    <div v-if="loading && !data" class="loading-layout">
      <div class="skeleton-grid">
        <div v-for="index in 4" :key="index" class="skeleton-card">
          <div class="skeleton skeleton-line short"></div>
          <div class="skeleton skeleton-value"></div>
          <div class="skeleton skeleton-line"></div>
        </div>
      </div>
      <div class="skeleton-panels">
        <div class="skeleton skeleton-panel"></div>
        <div class="skeleton skeleton-panel"></div>
      </div>
    </div>

    <template v-else>
      <div class="stat-grid">
        <article v-for="card in cards" :key="card.label" class="stat-card" :class="card.tone">
          <div class="stat-top">
            <span class="stat-icon">
              <el-icon><component :is="card.icon" /></el-icon>
            </span>
            <span class="stat-label">{{ card.label }}</span>
          </div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-bottom">
            <span>{{ card.note }}</span>
            <span class="stat-extra">{{ card.extra }}</span>
          </div>
          <div v-if="card.progress !== undefined" class="stat-progress">
            <i :style="{ width: `${Math.min(card.progress, 100)}%` }"></i>
          </div>
        </article>
      </div>

      <div class="chart-grid">
        <section class="panel trend-panel">
          <div class="panel-head">
            <div>
              <p class="panel-eyebrow">TREND</p>
              <h2>订单趋势</h2>
            </div>
            <div class="range-switch">
              <button
                v-for="range in ranges"
                :key="range.value"
                type="button"
                :class="{ active: days === range.value }"
                @click="changeDays(range.value)"
              >
                {{ range.label }}
              </button>
            </div>
          </div>
          <div class="chart-note">单位：单 / 人 · 按创建日期统计</div>
          <TrendChart :points="trend" />
        </section>

        <section class="panel status-panel">
          <div class="panel-head">
            <div>
              <p class="panel-eyebrow">STATUS</p>
              <h2>订单状态分布</h2>
            </div>
            <span class="panel-count">{{ summary.totalOrders }} 单</span>
          </div>
          <StatusChart v-if="summary.totalOrders > 0" :items="status" />
          <div v-else class="chart-empty">
            <el-icon><Tickets /></el-icon>
            <span>暂无订单</span>
          </div>
        </section>
      </div>

      <div class="bottom-grid">
        <section class="panel orders-panel">
          <div class="panel-head">
            <div>
              <p class="panel-eyebrow">RECENT ORDERS</p>
              <h2>最近订单</h2>
            </div>
            <span class="panel-count">最近 6 条</span>
          </div>

          <div v-if="recentOrders.length" class="table-wrap">
            <table class="data-table recent-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>订单类型</th>
                  <th>订单标题</th>
                  <th>发布者</th>
                  <th>接单者</th>
                  <th class="align-right">报酬/价格</th>
                  <th class="align-right">平台手续费</th>
                  <th>订单状态</th>
                  <th class="align-right">创建时间</th>
                  <th class="align-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in recentOrders" :key="order.id">
                  <td><span class="order-no">{{ formatOrderNo(order) }}</span></td>
                  <td><span class="type-tag">{{ typeLabel(order.type) }}</span></td>
                  <td><span class="title-cell" :title="order.title">{{ order.title }}</span></td>
                  <td>
                    <div class="person-cell">
                      <strong>{{ order.publisherName || '未知用户' }}</strong>
                      <span>信用分：{{ order.publisherCredit ?? '—' }}</span>
                    </div>
                  </td>
                  <td>
                    <div v-if="order.runnerId" class="person-cell">
                      <strong>{{ order.runnerName }}</strong>
                      <span>信用分：{{ order.runnerCredit ?? '—' }}</span>
                    </div>
                    <span v-else class="unassigned">—</span>
                  </td>
                  <td class="align-right money">¥{{ formatMoney(order.reward) }}</td>
                  <td class="align-right time">¥{{ formatMoney(order.platformFee) }}</td>
                  <td><span class="status-tag" :class="statusClass(order.status)">{{ order.status }}</span></td>
                  <td class="align-right time">{{ formatDateTime(order.createdAt) }}</td>
                  <td class="align-right"><button class="detail-link" type="button" @click="openOrderDetail(order)">详情</button></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="empty-state">
            <el-icon><Tickets /></el-icon>
            <strong>暂无订单</strong>
            <span>小程序发布订单后会出现在这里</span>
          </div>
        </section>

        <section class="panel feedback-panel">
          <div class="panel-head">
            <div>
              <p class="panel-eyebrow">FEEDBACK</p>
              <h2>用户反馈</h2>
            </div>
            <span class="panel-count">{{ summary.feedbackCount }} 条</span>
          </div>

          <div v-if="recentFeedback.length" class="feedback-list">
            <article v-for="item in recentFeedback" :key="item.id" class="feedback-item">
              <div class="feedback-avatar">{{ (item.userName || '用').slice(0, 1) }}</div>
              <div class="feedback-body">
                <div class="feedback-meta">
                  <strong>{{ item.userName || '微信用户' }}</strong>
                  <span>{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <p :title="item.content">{{ item.content }}</p>
              </div>
            </article>
          </div>

          <div v-else class="empty-state compact">
            <el-icon><ChatDotRound /></el-icon>
            <strong>暂无用户反馈</strong>
            <span>小程序提交反馈后会出现在这里</span>
          </div>
        </section>
      </div>

      <p class="page-foot">
        <span class="foot-dot"></span>
        完成金额只统计「已完成」订单，是平台内部流水，不代表真实支付。
      </p>
    </template>

    <el-drawer v-model="detailDrawer" size="620px" :with-header="false" append-to-body destroy-on-close class="order-drawer">
      <div v-if="selectedOrder" class="detail-sheet">
        <header class="detail-head">
          <div>
            <p class="detail-eyebrow">ORDER DETAIL</p>
            <h2>{{ detailOrder.title }}</h2>
            <div class="detail-badges">
              <span class="type-tag">{{ typeLabel(detailOrder.type) }}</span>
              <span class="status-tag" :class="statusClass(detailOrder.status)">{{ detailOrder.status }}</span>
            </div>
          </div>
          <button class="detail-close" type="button" aria-label="关闭详情" @click="detailDrawer = false">
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div v-if="detailLoading" class="detail-loading">正在读取订单详情…</div>
        <div v-if="detailError" class="detail-error">{{ detailError }}</div>

        <section class="detail-section">
          <h3>基本信息</h3>
          <table class="detail-table">
            <tbody>
              <tr>
                <th>订单号</th>
                <td colspan="3" class="mono">{{ formatOrderNo(detailOrder) }}</td>
              </tr>
              <tr>
                <th>订单类型</th>
                <td><span class="type-tag">{{ typeLabel(detailOrder.type) }}</span></td>
                <th>订单状态</th>
                <td><span class="status-tag" :class="statusClass(detailOrder.status)">{{ detailOrder.status }}</span></td>
              </tr>
              <tr>
                <th>订单标题</th>
                <td colspan="3">{{ detailOrder.title }}</td>
              </tr>
              <tr>
                <th>订单描述</th>
                <td colspan="3">{{ orderDescription(detailOrder) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="detail-section">
          <h3>金额信息</h3>
          <table class="detail-table">
            <tbody>
              <tr>
                <th>报酬金额</th>
                <td class="amount-green">¥{{ formatMoney(detailOrder.reward) }}</td>
                <th>平台手续费</th>
                <td class="amount-amber">¥{{ formatMoney(detailOrder.platformFee) }}</td>
              </tr>
              <tr>
                <th>实际收入</th>
                <td colspan="3" class="amount-blue">¥{{ formatMoney(detailOrder.actualIncome) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="detail-section">
          <h3>位置信息</h3>
          <table class="detail-table">
            <tbody>
              <tr>
                <th>取件/取餐地点</th>
                <td colspan="3">{{ orderPlace(detailOrder) }}</td>
              </tr>
              <tr>
                <th>送达建筑</th>
                <td>{{ detailOrder.deliveryBuilding || '—' }}</td>
                <th>详细地址</th>
                <td>{{ detailOrder.deliveryAddress || '—' }}</td>
              </tr>
              <tr>
                <th>联系方式</th>
                <td colspan="3">{{ contactText(detailOrder) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="detail-section">
          <h3>用户信息</h3>
          <table class="detail-table">
            <tbody>
              <tr>
                <th>发布者</th>
                <td colspan="3">
                  <div class="user-detail">
                    <strong>{{ detailOrder.publisherName }}（ID: {{ detailOrder.publisherId }}）</strong>
                    <span>信用分：{{ detailOrder.publisherCredit ?? '—' }}</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th>接单者</th>
                <td colspan="3">
                  <div v-if="detailOrder.runnerId" class="user-detail">
                    <strong>{{ detailOrder.runnerName }}（ID: {{ detailOrder.runnerId }}）</strong>
                    <span>信用分：{{ detailOrder.runnerCredit ?? '—' }}</span>
                  </div>
                  <span v-else class="detail-empty">尚未接单</span>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="detail-section">
          <h3>时间信息</h3>
          <table class="detail-table">
            <tbody>
              <tr>
                <th>创建时间</th>
                <td>{{ formatFullDateTime(detailOrder.createdAt) }}</td>
                <th>截止时间</th>
                <td>{{ formatFullDateTime(detailOrder.deadlineAt) }}</td>
              </tr>
              <tr>
                <th>接单时间</th>
                <td>{{ formatFullDateTime(detailOrder.acceptedAt) }}</td>
                <th>完成时间</th>
                <td>{{ formatFullDateTime(detailOrder.completedAt) }}</td>
              </tr>
              <tr v-if="detailOrder.submittedAt || detailOrder.cancelledAt">
                <th>提交时间</th>
                <td>{{ formatFullDateTime(detailOrder.submittedAt) }}</td>
                <th>取消时间</th>
                <td>{{ formatFullDateTime(detailOrder.cancelledAt) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <p class="detail-foot">后台订单详情不展示用户取件码原文，避免隐私信息扩散。</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  ChatDotRound,
  CircleCheck,
  Close,
  Money,
  Refresh,
  Tickets,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import TrendChart from '../components/TrendChart.vue'
import StatusChart from '../components/StatusChart.vue'
import { fetchAdminDashboard } from '../api/dashboard'
import { fetchAdminOrderDetail } from '../api/orders'
import { errorMessage } from '../api/http'

const days = ref(14)
const data = ref(null)
const loading = ref(true)
const error = ref('')
const detailDrawer = ref(false)
const selectedOrder = ref(null)
const orderDetail = ref(null)
const detailLoading = ref(false)
const detailError = ref('')

const ranges = [
  { label: '7 天', value: 7 },
  { label: '14 天', value: 14 },
  { label: '30 天', value: 30 }
]

const emptySummary = {
  totalUsers: 0,
  activeUsers: 0,
  newUsersToday: 0,
  totalOrders: 0,
  newOrdersToday: 0,
  openOrders: 0,
  completedOrders: 0,
  feedbackCount: 0,
  completionRate: 0,
  completedAmount: 0
}

const summary = computed(() => data.value?.summary || emptySummary)
const trend = computed(() => data.value?.trend || [])
const status = computed(() => data.value?.status || [])
const recentOrders = computed(() => data.value?.recentOrders || [])
const recentFeedback = computed(() => data.value?.recentFeedback || [])
const detailOrder = computed(() => ({ ...(selectedOrder.value || {}), ...(orderDetail.value || {}) }))

const todayText = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const updatedAt = computed(() => {
  if (!data.value?.generatedAt) return '--:--:--'
  return formatClock(data.value.generatedAt)
})

const cards = computed(() => {
  const item = summary.value
  return [
    {
      label: '用户总数',
      value: formatNumber(item.totalUsers),
      note: `活跃用户 ${formatNumber(item.activeUsers)}`,
      extra: `今日 +${formatNumber(item.newUsersToday)}`,
      tone: 'blue',
      icon: User
    },
    {
      label: '订单总数',
      value: formatNumber(item.totalOrders),
      note: `进行中 ${formatNumber(item.openOrders)}`,
      extra: `今日 +${formatNumber(item.newOrdersToday)}`,
      tone: 'indigo',
      icon: Tickets
    },
    {
      label: '完成率',
      value: `${formatPercent(item.completionRate)}%`,
      note: `已完成 ${formatNumber(item.completedOrders)} 单`,
      extra: '按全部订单',
      tone: 'teal',
      icon: CircleCheck,
      progress: item.completionRate
    },
    {
      label: '完成金额',
      value: `¥${formatMoney(item.completedAmount)}`,
      note: '仅统计已完成订单',
      extra: '内部流水',
      tone: 'amber',
      icon: Money
    }
  ]
})

async function load(silent = false) {
  if (!silent) error.value = ''
  loading.value = true
  try {
    data.value = await fetchAdminDashboard(days.value)
    error.value = ''
  } catch (requestError) {
    error.value = errorMessage(requestError, '数据看板加载失败')
  } finally {
    loading.value = false
  }
}

async function changeDays(value) {
  if (value === days.value) return
  days.value = value
  await load(true)
}

async function openOrderDetail(order) {
  selectedOrder.value = order
  orderDetail.value = null
  detailError.value = ''
  detailLoading.value = true
  detailDrawer.value = true
  try {
    orderDetail.value = await fetchAdminOrderDetail(order.id)
  } catch (requestError) {
    detailError.value = errorMessage(requestError, '订单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function typeLabel(type) {
  return { parcel: '取件', meal: '取餐' }[type] || type || '其他'
}

function statusClass(value) {
  return {
    'status-pending': value === '待接单',
    'status-accepted': value === '已接单',
    'status-confirming': value === '待确认',
    'status-completed': value === '已完成',
    'status-cancelled': value === '已取消'
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN').format(Number(value || 0))
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

function formatPercent(value) {
  return Number(value || 0).toFixed(1)
}

function formatClock(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--:--'
  return [date.getHours(), date.getMinutes(), date.getSeconds()]
    .map((item) => String(item).padStart(2, '0'))
    .join(':')
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(5, 16).replace('T', ' ')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatFullDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 19)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function formatOrderNo(order) {
  if (!order) return '—'
  const date = new Date(order.createdAt)
  if (Number.isNaN(date.getTime())) return `UO${String(order.id || '')}`
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `UO${year}${month}${day}${String(order.id || 0).padStart(6, '0')}`
}

function orderPlace(order) {
  return order?.station || order?.restaurant || '—'
}

function orderDescription(order) {
  if (!order) return '—'
  const place = order.type === 'meal'
    ? `取餐餐厅：${order.restaurant || '—'}`
    : `取件驿站：${order.station || '—'}`
  const count = Number(order.packageCount || order.codeCount || 0)
  return count > 1 ? `${place} · 共 ${count} 件` : place
}

function contactText(order) {
  const parts = []
  if (order?.publisherPhone) parts.push(`发布者手机：${order.publisherPhone}`)
  if (order?.runnerPhone) parts.push(`接单者手机：${order.runnerPhone}`)
  return parts.length ? parts.join('　') : '未填写联系方式'
}

onMounted(() => load())
</script>

<style scoped>
.dashboard-page {
  animation: page-in 0.35s ease both;
}

@keyframes page-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.page-eyebrow {
  margin: 0 0 9px;
  color: #7f93ac;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
}

.page-head h1 {
  margin: 0;
  color: #172b45;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.4px;
}

.page-sub {
  margin: 8px 0 0;
  color: #8393a8;
  font-size: 12px;
  line-height: 1.7;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
}

.updated-at {
  color: #9aaabe;
  font-size: 10px;
}

.refresh-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  cursor: pointer;
  border: 1px solid #dce7f5;
  border-radius: 8px;
  background: #fff;
  color: #527399;
  font-size: 11px;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.refresh-button:hover:not(:disabled) {
  border-color: #bcd4f1;
  color: #2f7df6;
  box-shadow: 0 4px 12px rgba(47, 125, 246, 0.08);
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 16px;
  padding: 11px 14px;
  border: 1px solid #f2d7d7;
  border-radius: 10px;
  background: #fff8f8;
  color: #b45656;
  font-size: 11px;
}

.error-banner button {
  margin-left: auto;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #a64747;
  font-size: 11px;
  text-decoration: underline;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 18px 19px 15px;
  border: 1px solid #e7edf5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(28, 57, 92, 0.035);
}

.stat-card::after {
  position: absolute;
  top: -34px;
  right: -30px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  content: '';
  pointer-events: none;
}

.stat-card.blue::after {
  background: radial-gradient(circle, rgba(47, 125, 246, 0.10), transparent 68%);
}

.stat-card.indigo::after {
  background: radial-gradient(circle, rgba(91, 111, 240, 0.10), transparent 68%);
}

.stat-card.teal::after {
  background: radial-gradient(circle, rgba(26, 168, 141, 0.10), transparent 68%);
}

.stat-card.amber::after {
  background: radial-gradient(circle, rgba(194, 138, 41, 0.12), transparent 68%);
}

.stat-top {
  display: flex;
  align-items: center;
  gap: 9px;
}

.stat-icon {
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border-radius: 8px;
  font-size: 15px;
}

.stat-card.blue .stat-icon {
  background: #edf4ff;
  color: #2f7df6;
}

.stat-card.indigo .stat-icon {
  background: #f0f1ff;
  color: #5b6ff0;
}

.stat-card.teal .stat-icon {
  background: #e9faf6;
  color: #1aa88d;
}

.stat-card.amber .stat-icon {
  background: #fff7e9;
  color: #c28a29;
}

.stat-label {
  color: #7d8fa5;
  font-size: 11px;
  font-weight: 500;
}

.stat-value {
  margin-top: 15px;
  color: #1b304b;
  font-size: 29px;
  font-weight: 650;
  letter-spacing: -0.6px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  color: #95a4b6;
  font-size: 9.5px;
}

.stat-extra {
  color: #6b8fb5;
}

.stat-progress {
  height: 3px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 99px;
  background: #edf3f9;
}

.stat-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1aa88d, #2dd4bf);
  transition: width 0.6s ease;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(290px, 0.85fr);
  gap: 16px;
  margin-top: 16px;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.panel {
  min-width: 0;
  padding: 20px 21px;
  border: 1px solid #e7edf5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(28, 57, 92, 0.035);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 13px;
  border-bottom: 1px solid #eef3f9;
}

.panel-eyebrow {
  margin: 0 0 5px;
  color: #9aaabe;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 1.9px;
}

.panel-head h2 {
  margin: 0;
  color: #2c4059;
  font-size: 14px;
  font-weight: 600;
}

.panel-count {
  flex: 0 0 auto;
  padding-top: 2px;
  color: #9aaabe;
  font-size: 9.5px;
}

.range-switch {
  display: flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid #e7eef7;
  border-radius: 8px;
  background: #f8fafd;
}

.range-switch button {
  height: 24px;
  padding: 0 9px;
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #8a9bb0;
  font-size: 9.5px;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.range-switch button:hover {
  color: #3c6da8;
}

.range-switch button.active {
  background: #fff;
  color: #2c5f9c;
  box-shadow: 0 1px 4px rgba(31, 67, 110, 0.10);
}

.chart-note {
  margin-top: 10px;
  color: #a6b3c3;
  font-size: 9px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 286px;
  color: #a2b0c1;
  font-size: 11px;
}

.chart-empty .el-icon {
  font-size: 17px;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  margin-top: 5px;
  border-collapse: collapse;
}

.data-table th {
  padding: 11px 8px 9px;
  color: #a0adbd;
  font-size: 8.5px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-align: left;
  white-space: nowrap;
}

.data-table td {
  padding: 11px 8px;
  border-top: 1px solid #f0f4f9;
  color: #6f8198;
  font-size: 10px;
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: #fbfdff;
}

.recent-table th {
  padding: 12px 7px 10px;
  font-size: 9px;
}

.recent-table td {
  padding: 12px 7px;
  font-size: 10.5px;
}

.order-no {
  color: #4d6b8d;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 9.5px;
  white-space: nowrap;
}

.title-cell {
  display: block;
  overflow: hidden;
  max-width: 142px;
  color: #344b66;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.person-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 84px;
}

.person-cell strong {
  color: #3e5874;
  font-size: 10.5px;
  font-weight: 550;
  white-space: nowrap;
}

.person-cell span {
  color: #9ba9b9;
  font-size: 8.5px;
  white-space: nowrap;
}

.unassigned {
  color: #a6b2c0;
  font-size: 11px;
}

.detail-link {
  padding: 0;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #2f7df6;
  font-size: 10px;
  white-space: nowrap;
}

.detail-link:hover {
  color: #1b5fc4;
  text-decoration: underline;
}

.type-tag {
  display: inline-flex;
  padding: 3px 7px;
  border-radius: 4px;
  background: #f0f5fc;
  color: #5a7da3;
  font-size: 8.5px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 7px;
  border-radius: 4px;
  font-size: 8.5px;
  white-space: nowrap;
}

.status-tag::before {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  content: '';
}

.status-pending {
  background: #fff7e9;
  color: #b88224;
}

.status-accepted {
  background: #eef4ff;
  color: #3978d8;
}

.status-confirming {
  background: #eef7ff;
  color: #4b8dc6;
}

.status-completed {
  background: #e9f8f2;
  color: #1b9679;
}

.status-cancelled {
  background: #f4f6f9;
  color: #93a1b2;
}

.align-right {
  text-align: right !important;
}

.money {
  color: #3b5e84 !important;
  font-size: 10px !important;
  font-variant-numeric: tabular-nums;
}

.time {
  color: #9ba9b9 !important;
  font-size: 9px !important;
  white-space: nowrap;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-top: 4px;
}

.feedback-item {
  display: flex;
  gap: 9px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f4f9;
}

.feedback-item:last-child {
  border-bottom: 0;
}

.feedback-avatar {
  display: grid;
  width: 27px;
  height: 27px;
  flex: 0 0 27px;
  place-items: center;
  border-radius: 8px;
  background: #edf4ff;
  color: #4d7fbd;
  font-size: 10px;
  font-weight: 600;
}

.feedback-body {
  min-width: 0;
  flex: 1;
}

.feedback-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.feedback-meta strong {
  color: #405671;
  font-size: 10px;
  font-weight: 600;
}

.feedback-meta span {
  flex: 0 0 auto;
  color: #a2afbe;
  font-size: 8.5px;
}

.feedback-body p {
  display: -webkit-box;
  overflow: hidden;
  margin: 5px 0 0;
  color: #7b8da1;
  font-size: 10px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 230px;
  color: #a3b0bf;
  text-align: center;
}

.empty-state .el-icon {
  margin-bottom: 3px;
  color: #c0cfdf;
  font-size: 25px;
}

.empty-state strong {
  color: #7e90a5;
  font-size: 11px;
  font-weight: 500;
}

.empty-state span {
  font-size: 9.5px;
}

.empty-state.compact {
  min-height: 120px;
}

.page-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 18px 0 0;
  color: #a0adbd;
  font-size: 9.5px;
}

.foot-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #9bb6d6;
}

.loading-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-card {
  padding: 18px;
  border: 1px solid #e7edf5;
  border-radius: 14px;
  background: #fff;
}

.skeleton {
  border-radius: 6px;
  background: linear-gradient(90deg, #eef3f9 25%, #f8fbfe 37%, #eef3f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-line {
  width: 62%;
  height: 9px;
}

.skeleton-line.short {
  width: 38%;
}

.skeleton-value {
  width: 48%;
  height: 25px;
  margin: 17px 0 14px;
}

.skeleton-panels {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(290px, 0.85fr);
  gap: 16px;
}

.skeleton-panel {
  height: 360px;
  border-radius: 14px;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid,
  .skeleton-panels {
    grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.9fr);
  }

  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1050px) {
  .chart-grid,
  .skeleton-panels {
    grid-template-columns: 1fr;
  }

  .status-chart {
    height: 260px;
  }
}

@media (max-width: 680px) {
  .page-head {
    flex-direction: column;
    gap: 12px;
  }

  .head-actions {
    width: 100%;
    justify-content: space-between;
  }

  .stat-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 17px 15px;
  }
}

:global(.order-drawer) {
  max-width: 94vw;
}

.detail-sheet {
  box-sizing: border-box;
  padding: 22px 26px 28px;
  color: #334b66;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e8eef6;
}

.detail-eyebrow {
  margin: 0 0 6px;
  color: #9aaabe;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.detail-head h2 {
  margin: 0;
  color: #1b304b;
  font-size: 19px;
  font-weight: 650;
  letter-spacing: -0.2px;
}

.detail-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.detail-close {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  cursor: pointer;
  border: 0;
  border-radius: 7px;
  background: #f4f8fd;
  color: #6f8198;
  place-items: center;
  transition: background 0.2s ease, color 0.2s ease;
}

.detail-close:hover {
  background: #eaf2fc;
  color: #2f7df6;
}

.detail-loading,
.detail-error {
  margin-top: 14px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 10px;
}

.detail-loading {
  background: #f5f9ff;
  color: #6585a9;
}

.detail-error {
  background: #fff8f8;
  color: #b45656;
}

.detail-section {
  margin-top: 24px;
}

.detail-section h3 {
  margin: 0;
  color: #223a55;
  font-size: 14px;
  font-weight: 650;
}

.detail-table {
  width: 100%;
  margin-top: 10px;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid #e6edf5;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  border: 1px solid #e6edf5;
  font-size: 10.5px;
  line-height: 1.5;
  text-align: left;
  vertical-align: middle;
  word-break: break-word;
}

.detail-table th {
  width: 104px;
  background: #f5f9fd;
  color: #526a82;
  font-weight: 550;
}

.detail-table td {
  color: #334b66;
}

.detail-table .mono {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10px;
}

.amount-green {
  color: #1b9679 !important;
}

.amount-amber {
  color: #b88224 !important;
}

.amount-blue {
  color: #2f7df6 !important;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.user-detail strong {
  color: #334b66;
  font-size: 11px;
  font-weight: 550;
}

.user-detail span {
  color: #93a3b5;
  font-size: 9.5px;
}

.detail-empty {
  color: #a0adbd;
  font-size: 10.5px;
}

.detail-foot {
  margin: 24px 0 0;
  color: #a2b0c1;
  font-size: 9px;
  line-height: 1.6;
}

</style>
