<template>
  <div class="orders-page">
    <div class="page-head">
      <div>
        <p class="page-eyebrow">ORDER OPERATIONS</p>
        <h1>订单管理</h1>
        <p class="page-sub">查看全校订单、处理异常状态并核对完成凭证。</p>
      </div>
      <span class="total-badge">共 {{ total }} 笔订单</span>
    </div>

    <div class="filter-bar">
      <div class="filter-main">
        <label class="search-field">
          <el-icon><Search /></el-icon>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索订单号、标题或用户姓名"
            maxlength="64"
            @keyup.enter="applyFilters"
          />
          <button
            v-if="keyword"
            class="clear-search"
            type="button"
            aria-label="清空搜索"
            @click="clearKeyword"
          >
            <el-icon><Close /></el-icon>
          </button>
        </label>

        <span class="filter-label">类型</span>
        <div class="type-tabs">
          <button
            v-for="item in typeOptions"
            :key="item.value"
            type="button"
            :class="{ active: type === item.value }"
            @click="changeType(item.value)"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="filter-actions">
          <button class="search-button" type="button" :disabled="loading" @click="applyFilters">搜索</button>
          <button class="reset-button" type="button" @click="resetFilters">重置</button>
        </div>
      </div>

      <div class="status-row">
        <span class="filter-label">状态</span>
        <div class="status-tabs">
          <button
            v-for="item in statusOptions"
            :key="item.value"
            type="button"
            :class="{ active: status === item.value }"
            @click="changeStatus(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="panel-eyebrow">ALL ORDERS</p>
          <h2>订单列表</h2>
        </div>
        <span class="panel-meta">第 {{ page }} / {{ totalPages }} 页</span>
      </div>

      <div v-if="error" class="error-banner">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ error }}</span>
        <button type="button" @click="load">重试</button>
      </div>

      <div v-if="loading" class="table-skeleton">
        <div v-for="index in 5" :key="index" class="skeleton-row">
          <div class="skeleton skeleton-order"></div>
          <div class="skeleton skeleton-mid"></div>
          <div class="skeleton skeleton-short"></div>
          <div class="skeleton skeleton-short"></div>
          <div class="skeleton skeleton-mid"></div>
        </div>
      </div>

      <template v-else>
        <div v-if="rows.length" class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>订单</th>
                <th>类型</th>
                <th>发布者</th>
                <th>接单者</th>
                <th class="align-right">金额</th>
                <th>状态</th>
                <th>创建时间</th>
                <th class="align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in rows" :key="order.id">
                <td>
                  <div class="order-cell">
                    <strong>#{{ order.id }}</strong>
                    <span :title="order.title">{{ order.title }}</span>
                  </div>
                </td>
                <td>
                  <span class="type-tag">{{ typeLabel(order.type) }}</span>
                </td>
                <td>
                  <div class="person-cell">
                    <span class="person-avatar publisher-tone">{{ initial(order.publisherName) }}</span>
                    <strong>{{ order.publisherName || '—' }}</strong>
                  </div>
                </td>
                <td>
                  <div class="person-cell">
                    <template v-if="order.runnerName">
                      <span class="person-avatar runner-tone">{{ initial(order.runnerName) }}</span>
                      <strong>{{ order.runnerName }}</strong>
                    </template>
                    <span v-else class="muted">待接单</span>
                  </div>
                </td>
                <td class="align-right money">¥{{ formatMoney(order.reward) }}</td>
                <td>
                  <span class="status-tag" :class="statusClass(order.status)">{{ order.status }}</span>
                </td>
                <td class="time">{{ formatDateTime(order.createdAt) }}</td>
                <td class="align-right">
                  <button class="detail-button" type="button" @click="openDetail(order)">详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <el-icon><Tickets /></el-icon>
          <strong>没有找到订单</strong>
          <span>{{ keyword ? '换个关键词试试' : '当前筛选条件下暂无订单' }}</span>
        </div>

        <div v-if="total > 0" class="pagination">
          <span class="pagination-meta">共 {{ total }} 笔订单 · 第 {{ page }} / {{ totalPages }} 页</span>
          <div class="page-buttons">
            <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <button
              v-for="item in visiblePages"
              :key="item"
              type="button"
              :class="{ active: item === page }"
              @click="changePage(item)"
            >
              {{ item }}
            </button>
            <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
      </template>
    </section>

    <p class="page-foot">
      <span class="foot-dot"></span>
      管理员取消订单会把操作者记录为「管理员」，不会触发接单者取消扣分；强制完成只适用于已上传凭证的待确认订单。
    </p>

    <el-drawer
      v-model="drawerVisible"
      class="order-drawer"
      size="540px"
      :with-header="false"
      append-to-body
      :destroy-on-close="true"
      @closed="onDrawerClosed"
    >
      <div v-if="detailLoading" class="drawer-loading">
        <div class="skeleton skeleton-drawer-head"></div>
        <div class="skeleton skeleton-drawer-block"></div>
        <div class="skeleton skeleton-drawer-block"></div>
      </div>

      <div v-else-if="detail" class="drawer-content">
        <div class="drawer-head">
          <div>
            <p class="drawer-eyebrow">ORDER DETAIL</p>
            <h2>#{{ detail.id }} · {{ detail.title }}</h2>
            <div class="drawer-sub">
              <span class="status-tag" :class="statusClass(detail.status)">{{ detail.status }}</span>
              <span>{{ typeLabel(detail.type) }}</span>
              <span>创建于 {{ formatDateTime(detail.createdAt) }}</span>
            </div>
          </div>
          <button class="drawer-close" type="button" aria-label="关闭" @click="drawerVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="order-hero">
          <div class="hero-amount">
            <span>订单金额</span>
            <strong>¥{{ formatMoney(detail.reward) }}</strong>
          </div>
          <div>
            <span>截止时间</span>
            <strong>{{ formatDateTime(detail.deadlineAt) }}</strong>
          </div>
          <div>
            <span>完成时间</span>
            <strong>{{ formatDateTime(detail.completedAt) }}</strong>
          </div>
        </div>

        <section class="detail-section">
          <div class="section-head">
            <div>
              <p class="section-eyebrow">PARTICIPANTS</p>
              <h3>参与用户</h3>
            </div>
          </div>
          <div class="participant-grid">
            <div class="participant-card">
              <span class="participant-role">发布者</span>
              <strong>{{ detail.publisherName || '—' }}</strong>
              <span>{{ detail.publisherPhone || '手机号未绑定' }}</span>
            </div>
            <div class="participant-card runner">
              <span class="participant-role">接单者</span>
              <template v-if="detail.runnerName">
                <strong>{{ detail.runnerName }}</strong>
                <span>{{ detail.runnerPhone || '手机号未绑定' }}</span>
              </template>
              <template v-else>
                <strong class="muted">等待接单</strong>
                <span>尚未有用户接单</span>
              </template>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-head">
            <div>
              <p class="section-eyebrow">ROUTE</p>
              <h3>取送信息</h3>
            </div>
          </div>
          <div class="info-list">
            <div>
              <span>取件 / 取餐地点</span>
              <strong>{{ detail.type === 'parcel' ? detail.station || '—' : detail.restaurant || '—' }}</strong>
            </div>
            <div>
              <span>送达地址</span>
              <strong>{{ detail.deliveryBuilding }} · {{ detail.deliveryAddress }}</strong>
            </div>
            <div>
              <span>物品信息</span>
              <strong>
                {{ detail.type === 'parcel' ? `${detail.packageCount} 个包裹` : `${detail.codeCount} 份餐品` }}
              </strong>
            </div>
          </div>
          <p class="privacy-note">
            <el-icon><Lock /></el-icon>
            取件码 / 取餐码原文已隐藏，仅订单参与者可见。
          </p>
        </section>

        <section class="detail-section">
          <div class="section-head">
            <div>
              <p class="section-eyebrow">TIMELINE</p>
              <h3>订单进度</h3>
            </div>
            <span class="section-note">{{ detail.status }}</span>
          </div>
          <div class="timeline">
            <div v-for="(step, index) in timeline" :key="step.label" class="timeline-item" :class="{ active: step.active, cancelled: step.cancelled }">
              <div class="timeline-rail">
                <span class="timeline-dot"></span>
                <i v-if="index < timeline.length - 1"></i>
              </div>
              <div class="timeline-copy">
                <strong>{{ step.label }}</strong>
                <span>{{ step.time ? formatDateTime(step.time) : '尚未发生' }}</span>
                <em v-if="step.extra">{{ step.extra }}</em>
              </div>
            </div>
          </div>
        </section>

        <section class="detail-section proof-section">
          <div class="section-head proof-head">
            <div>
              <p class="section-eyebrow">PROOF</p>
              <h3>完成凭证</h3>
            </div>
            <button v-if="detail.hasCompletionPhoto" class="proof-button" type="button" @click="openProof">
              查看凭证
            </button>
            <span v-else class="section-note">未上传</span>
          </div>
          <p class="proof-note">
            <template v-if="detail.hasCompletionPhoto">接单者已上传完成照片，管理员可查看用于异常核对。</template>
            <template v-else>订单尚未上传完成照片。</template>
          </p>
        </section>

        <div class="drawer-actions">
          <button class="secondary-button" type="button" @click="drawerVisible = false">关闭</button>
          <button
            v-if="detail.cancellable"
            class="danger-button"
            type="button"
            :disabled="actionLoading !== null"
            @click="cancelOrder"
          >
            {{ actionLoading === 'cancel' ? '取消中…' : '取消订单' }}
          </button>
          <button
            v-if="detail.canForceComplete"
            class="warning-button"
            type="button"
            :disabled="actionLoading !== null"
            @click="forceComplete"
          >
            {{ actionLoading === 'complete' ? '处理中…' : '强制完成' }}
          </button>
          <span v-if="!detail.cancellable && !detail.canForceComplete" class="terminal-note">订单已结束，无可用操作</span>
        </div>
      </div>
    </el-drawer>

    <el-dialog
      v-model="proofVisible"
      class="proof-dialog"
      title="完成凭证"
      width="560px"
      append-to-body
      @closed="releaseProof"
    >
      <div v-loading="proofLoading" class="proof-viewer">
        <img v-if="proofUrl" :src="proofUrl" alt="订单完成凭证" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Close,
  Lock,
  Search,
  Tickets,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  cancelAdminOrder,
  fetchAdminOrderDetail,
  fetchAdminOrderProof,
  fetchAdminOrders,
  forceCompleteAdminOrder
} from '../api/orders'
import { errorMessage } from '../api/http'

const route = useRoute()
const router = useRouter()

const size = 10
const page = ref(1)
const total = ref(0)
const rows = ref([])
const keyword = ref('')
const status = ref(normalizeOrderStatus(route.query.status))
const type = ref(normalizeType(route.query.type))
const loading = ref(true)
const error = ref('')
const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)
const actionLoading = ref(null)
const proofVisible = ref(false)
const proofLoading = ref(false)
const proofUrl = ref('')

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待接单', value: '待接单' },
  { label: '已接单', value: '已接单' },
  { label: '待确认', value: '待确认' },
  { label: '已完成', value: '已完成' },
  { label: '已取消', value: '已取消' }
]

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '取件', value: 'parcel' },
  { label: '取餐', value: 'meal' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const visiblePages = computed(() => {
  const count = totalPages.value
  if (count <= 5) return Array.from({ length: count }, (_, index) => index + 1)
  let start = Math.max(1, page.value - 2)
  const end = Math.min(count, start + 4)
  start = Math.max(1, end - 4)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

const timeline = computed(() => {
  if (!detail.value) return []
  const item = detail.value
  const steps = [
    { label: '订单创建', time: item.createdAt, active: true },
    { label: '接单', time: item.acceptedAt, active: Boolean(item.acceptedAt) },
    { label: '提交凭证', time: item.submittedAt, active: Boolean(item.submittedAt) },
    { label: '确认完成', time: item.completedAt, active: Boolean(item.completedAt) }
  ]
  if (item.cancelledAt) {
    steps.push({
      label: '订单取消',
      time: item.cancelledAt,
      active: true,
      cancelled: true,
      extra: item.cancelledByName ? `操作者：${item.cancelledByName}` : ''
    })
  }
  return steps
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchAdminOrders({
      page: page.value,
      size,
      keyword: keyword.value.trim(),
      status: status.value,
      type: type.value
    })
    rows.value = result.items
    total.value = result.total
    if (!rows.value.length && total.value > 0 && page.value > 1) {
      page.value = totalPages.value
      await load()
    }
  } catch (requestError) {
    error.value = errorMessage(requestError, '订单列表加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function clearKeyword() {
  keyword.value = ''
  applyFilters()
}

function changeStatus(value) {
  if (status.value === value) return
  status.value = value
  page.value = 1
  syncQuery()
  load()
}

function changeType(value) {
  if (type.value === value) return
  type.value = value
  page.value = 1
  syncQuery()
  load()
}

function syncQuery() {
  const query = {}
  if (status.value !== 'all') query.status = status.value
  if (type.value !== 'all') query.type = type.value
  router.replace({ path: '/orders', query })
}

function resetFilters() {
  keyword.value = ''
  status.value = 'all'
  type.value = 'all'
  page.value = 1
  syncQuery()
  load()
}

function changePage(value) {
  if (value < 1 || value > totalPages.value || value === page.value) return
  page.value = value
  load()
}

async function openDetail(order) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminOrderDetail(order.id)
  } catch (requestError) {
    drawerVisible.value = false
    ElMessage.error(errorMessage(requestError, '订单详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function cancelOrder() {
  if (!detail.value?.cancellable) return
  try {
    await ElMessageBox.confirm(
      `确认取消订单 #${detail.value.id}？取消后订单不可恢复，历史记录会保留。`,
      '取消订单',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  actionLoading.value = 'cancel'
  try {
    detail.value = await cancelAdminOrder(detail.value.id)
    ElMessage.success('订单已取消')
    await load()
  } catch (requestError) {
    ElMessage.error(errorMessage(requestError, '取消订单失败'))
  } finally {
    actionLoading.value = null
  }
}

async function forceComplete() {
  if (!detail.value?.canForceComplete) return
  try {
    await ElMessageBox.confirm(
      `确认强制完成订单 #${detail.value.id}？\n\n这会绕过发布者确认，直接标记完成，并计入接单者收入与信用分。仅用于异常处理。`,
      '强制完成订单',
      {
        confirmButtonText: '强制完成',
        cancelButtonText: '返回',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  actionLoading.value = 'complete'
  try {
    detail.value = await forceCompleteAdminOrder(detail.value.id)
    ElMessage.success('订单已强制完成')
    await load()
  } catch (requestError) {
    ElMessage.error(errorMessage(requestError, '强制完成失败'))
  } finally {
    actionLoading.value = null
  }
}

async function openProof() {
  if (!detail.value?.hasCompletionPhoto) return
  releaseProof()
  proofVisible.value = true
  proofLoading.value = true
  try {
    const blob = await fetchAdminOrderProof(detail.value.id)
    proofUrl.value = URL.createObjectURL(blob)
  } catch (requestError) {
    proofVisible.value = false
    ElMessage.error(errorMessage(requestError, '完成凭证读取失败'))
  } finally {
    proofLoading.value = false
  }
}

function releaseProof() {
  if (proofUrl.value) {
    URL.revokeObjectURL(proofUrl.value)
    proofUrl.value = ''
  }
}

function onDrawerClosed() {
  releaseProof()
}

watch(
  () => route.query.status,
  (value) => {
    const next = normalizeOrderStatus(value)
    if (next === status.value) return
    status.value = next
    page.value = 1
    load()
  }
)

watch(
  () => route.query.type,
  (value) => {
    const next = normalizeType(value)
    if (next === type.value) return
    type.value = next
    page.value = 1
    load()
  }
)

onMounted(load)
onBeforeUnmount(releaseProof)

function normalizeOrderStatus(value) {
  return ['待接单', '已接单', '待确认', '已完成', '已取消'].includes(value) ? value : 'all'
}

function normalizeType(value) {
  return value === 'parcel' || value === 'meal' ? value : 'all'
}

function initial(value) {
  return String(value || '—').slice(0, 1).toUpperCase()
}

function typeLabel(value) {
  return { parcel: '取件', meal: '取餐' }[value] || value || '其他'
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

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
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
</script>

<style scoped>
.orders-page {
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
  margin-bottom: 20px;
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

.total-badge {
  flex: 0 0 auto;
  margin-top: 5px;
  padding: 7px 11px;
  border: 1px solid #dce8f8;
  border-radius: 8px;
  background: #f5f9ff;
  color: #4c78b4;
  font-size: 10px;
  font-weight: 600;
}

.filter-bar {
  margin-bottom: 16px;
  padding: 13px 14px;
  border: 1px solid #e7edf5;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(28, 57, 92, 0.025);
}

.filter-main,
.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.status-row {
  gap: 10px;
  margin-top: 11px;
  padding-top: 11px;
  border-top: 1px solid #f0f4f9;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 300px;
  height: 36px;
  padding: 0 11px;
  border: 1px solid #e3ebf5;
  border-radius: 9px;
  background: #f8fafd;
  color: #9aaabe;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.search-field:focus-within {
  border-color: #b9d3f2;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(47, 125, 246, 0.07);
}

.search-field .el-icon {
  flex: 0 0 auto;
  font-size: 14px;
}

.search-field input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  color: #344b66;
  font-size: 11px;
}

.search-field input::placeholder {
  color: #a8b5c5;
}

.clear-search {
  display: grid;
  width: 18px;
  height: 18px;
  cursor: pointer;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: #e7eef7;
  color: #8a9db3;
}

.clear-search .el-icon {
  font-size: 10px;
}

.filter-label {
  flex: 0 0 auto;
  color: #8497ac;
  font-size: 10px;
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.2px;
}

.type-tabs,
.status-tabs {
  display: flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid #e7eef7;
  border-radius: 9px;
  background: #f8fafd;
}

.type-tabs button,
.status-tabs button {
  height: 28px;
  padding: 0 11px;
  cursor: pointer;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #8a9bb0;
  font-size: 9.5px;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.type-tabs button:hover,
.status-tabs button:hover {
  color: #3c6da8;
}

.type-tabs button.active,
.status-tabs button.active {
  background: #fff;
  color: #2c5f9c;
  box-shadow: 0 1px 4px rgba(31, 67, 110, 0.10);
}

.filter-actions {
  display: flex;
  gap: 7px;
  margin-left: auto;
}

.search-button,
.reset-button {
  height: 34px;
  padding: 0 13px;
  cursor: pointer;
  border-radius: 8px;
  font-size: 10.5px;
  transition: opacity 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.search-button {
  border: 1px solid #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.search-button:hover:not(:disabled) {
  background: #286fe0;
}

.search-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.reset-button {
  border: 1px solid #e1e9f3;
  background: #fff;
  color: #71839a;
}

.reset-button:hover {
  border-color: #c6d8ee;
  color: #3d6a9f;
}

.panel {
  padding: 20px 21px 16px;
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

.panel-meta {
  color: #9aaabe;
  font-size: 9.5px;
}

.table-wrap {
  overflow-x: auto;
}

.order-table {
  min-width: 930px;
  width: 100%;
  border-collapse: collapse;
}

.order-table th {
  padding: 12px 9px 9px;
  color: #a0adbd;
  font-size: 8.5px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-align: left;
  white-space: nowrap;
}

.order-table td {
  padding: 13px 9px;
  border-top: 1px solid #f0f4f9;
  color: #6f8198;
  font-size: 10px;
  vertical-align: middle;
}

.order-table tbody tr {
  transition: background 0.15s ease;
}

.order-table tbody tr:hover {
  background: #fbfdff;
}

.order-cell {
  display: flex;
  min-width: 168px;
  flex-direction: column;
  gap: 3px;
}

.order-cell strong {
  color: #4d6b8d;
  font-size: 9.5px;
  font-weight: 600;
}

.order-cell span {
  overflow: hidden;
  max-width: 225px;
  color: #344b66;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-tag {
  display: inline-flex;
  padding: 3px 7px;
  border-radius: 4px;
  background: #f0f5fc;
  color: #5a7da3;
  font-size: 8.5px;
}

.person-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 84px;
}

.person-avatar {
  display: grid;
  width: 21px;
  height: 21px;
  flex: 0 0 21px;
  place-items: center;
  border-radius: 6px;
  font-size: 8px;
  font-weight: 600;
}

.publisher-tone {
  background: #edf4ff;
  color: #4d7fbd;
}

.runner-tone {
  background: #eafaf6;
  color: #1a967b;
}

.person-cell strong {
  overflow: hidden;
  max-width: 90px;
  color: #526b87;
  font-size: 9.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted {
  color: #a9b6c5 !important;
}

.money {
  color: #3b5e84 !important;
  font-size: 10px !important;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.time {
  color: #98a7b8 !important;
  font-size: 9px !important;
  white-space: nowrap;
}

.align-right {
  text-align: right !important;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  border-radius: 5px;
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

.detail-button {
  height: 26px;
  padding: 0 9px;
  cursor: pointer;
  border: 1px solid #e3ebf5;
  border-radius: 6px;
  background: #fff;
  color: #5f7d9e;
  font-size: 9px;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.detail-button:hover {
  border-color: #bcd4f1;
  background: #f8fbff;
  color: #2f7df6;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 280px;
  color: #a3b0bf;
  text-align: center;
}

.empty-state .el-icon {
  margin-bottom: 3px;
  color: #c0cfdf;
  font-size: 26px;
}

.empty-state strong {
  color: #7e90a5;
  font-size: 11px;
  font-weight: 500;
}

.empty-state span {
  font-size: 9.5px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 15px;
  padding-top: 14px;
  border-top: 1px solid #eef3f9;
}

.pagination-meta {
  color: #9aaabe;
  font-size: 9.5px;
}

.page-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-buttons button {
  display: grid;
  min-width: 27px;
  height: 27px;
  cursor: pointer;
  place-items: center;
  border: 1px solid #e6edf6;
  border-radius: 6px;
  background: #fff;
  color: #7a8fa7;
  font-size: 9.5px;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.page-buttons button:hover:not(:disabled) {
  border-color: #bcd4f1;
  color: #2f7df6;
}

.page-buttons button.active {
  border-color: #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.page-buttons button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 14px 0;
  padding: 10px 12px;
  border: 1px solid #f2d7d7;
  border-radius: 9px;
  background: #fff8f8;
  color: #b45656;
  font-size: 10px;
}

.error-banner button {
  margin-left: auto;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #a64747;
  font-size: 10px;
  text-decoration: underline;
}

.page-foot {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 18px 0 0;
  color: #a0adbd;
  font-size: 9.5px;
  line-height: 1.7;
}

.foot-dot {
  width: 5px;
  height: 5px;
  flex: 0 0 5px;
  margin-top: 5px;
  border-radius: 50%;
  background: #9bb6d6;
}

.table-skeleton {
  padding-top: 12px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 9px;
  border-bottom: 1px solid #f4f7fb;
}

.skeleton {
  border-radius: 5px;
  background: linear-gradient(90deg, #eef3f9 25%, #f8fbfe 37%, #eef3f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-order {
  width: 190px;
  height: 24px;
}

.skeleton-mid {
  width: 88px;
  height: 9px;
}

.skeleton-short {
  width: 56px;
  height: 9px;
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

:global(.order-drawer .el-drawer__body) {
  padding: 0;
  background: #f7f9fd;
}

:global(.proof-dialog .el-dialog__body) {
  padding: 0 20px 20px;
}

.drawer-loading {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px;
}

.skeleton-drawer-head {
  height: 84px;
}

.skeleton-drawer-block {
  height: 160px;
}

.drawer-content {
  min-height: 100%;
  padding: 28px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.drawer-eyebrow {
  margin: 0 0 7px;
  color: #8a9db5;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 2px;
}

.drawer-head h2 {
  margin: 0;
  color: #1e3550;
  font-size: 21px;
  font-weight: 650;
  letter-spacing: -0.3px;
}

.drawer-sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: #9aaabe;
  font-size: 9px;
}

.drawer-close {
  display: grid;
  width: 30px;
  height: 30px;
  cursor: pointer;
  place-items: center;
  border: 1px solid #e2eaf4;
  border-radius: 8px;
  background: #fff;
  color: #8fa0b4;
}

.drawer-close:hover {
  color: #2f7df6;
}

.order-hero {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #e3ecf6;
  border-radius: 13px;
  background: #e3ecf6;
}

.order-hero > div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px;
  background: #fff;
}

.order-hero span {
  color: #9aaabe;
  font-size: 8.5px;
}

.order-hero strong {
  color: #455e79;
  font-size: 10px;
  font-weight: 500;
}

.order-hero .hero-amount strong {
  color: #b27c2b;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.detail-section {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #e3ecf6;
  border-radius: 13px;
  background: #fff;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 11px;
  border-bottom: 1px solid #eef3f9;
}

.section-eyebrow {
  margin: 0 0 5px;
  color: #9aaabe;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 1.7px;
}

.section-head h3 {
  margin: 0;
  color: #2d4259;
  font-size: 12px;
  font-weight: 600;
}

.section-note {
  color: #a3b0c0;
  font-size: 8.5px;
}

.participant-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 11px;
}

.participant-card {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px;
  border-radius: 9px;
  background: #f8fafd;
}

.participant-card.runner {
  background: #f6fcfa;
}

.participant-role {
  color: #9aaabe;
  font-size: 8px;
}

.participant-card strong {
  color: #40566f;
  font-size: 11px;
  font-weight: 600;
}

.participant-card span:last-child {
  color: #9aaabe;
  font-size: 8.5px;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-list > div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 9px 0;
  border-bottom: 1px solid #f2f6fa;
}

.info-list > div:last-child {
  border-bottom: 0;
}

.info-list span {
  flex: 0 0 auto;
  color: #9aaabe;
  font-size: 9px;
}

.info-list strong {
  color: #4c637d;
  font-size: 9.5px;
  font-weight: 500;
  text-align: right;
}

.privacy-note {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 10px 0 0;
  padding-top: 9px;
  border-top: 1px solid #f2f6fa;
  color: #a1afbf;
  font-size: 8.5px;
}

.privacy-note .el-icon {
  font-size: 10px;
}

.timeline {
  padding-top: 3px;
}

.timeline-item {
  display: flex;
  gap: 10px;
  min-height: 43px;
}

.timeline-rail {
  display: flex;
  align-items: center;
  flex-direction: column;
  width: 10px;
  flex: 0 0 10px;
}

.timeline-dot {
  width: 7px;
  height: 7px;
  margin-top: 13px;
  flex: 0 0 7px;
  border: 1.5px solid #c3d1e0;
  border-radius: 50%;
  background: #fff;
}

.timeline-rail i {
  width: 1px;
  flex: 1;
  background: #e7eef6;
}

.timeline-item.active .timeline-dot {
  border-color: #2f7df6;
  background: #2f7df6;
  box-shadow: 0 0 0 3px rgba(47, 125, 246, 0.09);
}

.timeline-item.cancelled .timeline-dot {
  border-color: #b3a1a1;
  background: #b3a1a1;
  box-shadow: 0 0 0 3px rgba(179, 161, 161, 0.1);
}

.timeline-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 0;
}

.timeline-copy strong {
  color: #6b8098;
  font-size: 9.5px;
  font-weight: 500;
}

.timeline-item.active .timeline-copy strong {
  color: #40566f;
}

.timeline-copy span {
  color: #a3b0c0;
  font-size: 8.5px;
}

.timeline-copy em {
  color: #b08b8b;
  font-size: 8.5px;
  font-style: normal;
}

.proof-head {
  align-items: center;
}

.proof-button {
  height: 27px;
  padding: 0 10px;
  cursor: pointer;
  border: 1px solid #cfe0f5;
  border-radius: 6px;
  background: #f7fbff;
  color: #4a7bb7;
  font-size: 9px;
}

.proof-button:hover {
  border-color: #a9c8ec;
  color: #2f6db3;
}

.proof-note {
  margin: 10px 0 0;
  color: #a1afbf;
  font-size: 8.5px;
  line-height: 1.65;
}

.drawer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  position: sticky;
  bottom: -1px;
  z-index: 2;
  margin-top: 18px;
  padding: 14px 0 4px;
  border-top: 1px solid #e6edf6;
  background: #f7f9fd;
}

.drawer-actions button {
  height: 34px;
  padding: 0 16px;
  cursor: pointer;
  border-radius: 8px;
  font-size: 10px;
}

.drawer-actions button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.secondary-button {
  border: 1px solid #dfe8f3;
  background: #fff;
  color: #71859c;
}

.danger-button {
  border: 1px solid #d47777;
  background: #fff;
  color: #b25555;
}

.danger-button:hover:not(:disabled) {
  background: #fff7f7;
}

.warning-button {
  border: 1px solid #c28a29;
  background: #c28a29;
  color: #fff;
}

.warning-button:hover:not(:disabled) {
  background: #b47d20;
}

.terminal-note {
  color: #a3b0c0;
  font-size: 9px;
}

.proof-viewer {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 9px;
  background: #f4f7fb;
}

.proof-viewer img {
  display: block;
  max-width: 100%;
  max-height: 65vh;
  object-fit: contain;
}

@media (max-width: 1180px) {
  .search-field {
    width: 100%;
  }

  .filter-actions {
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
    gap: 10px;
  }

  .total-badge {
    margin-top: 0;
  }

  .filter-main,
  .status-row {
    align-items: stretch;
  }

  .type-tabs,
  .status-tabs {
    width: 100%;
    overflow-x: auto;
  }

  .type-tabs button,
  .status-tabs button {
    flex: 0 0 auto;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions button {
    flex: 1;
  }

  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-hero,
  .participant-grid {
    grid-template-columns: 1fr;
  }

  .drawer-content {
    padding: 20px;
  }
}
</style>
