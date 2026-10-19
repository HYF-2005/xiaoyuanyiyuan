<template>
  <div class="adm-page risk-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">TRUST &amp; SAFETY</p>
        <h1 class="adm-title">风险订单</h1>
        <p class="adm-sub">用可解释的规则扫描刷单、互刷和异常完成行为；人工审核后再决定是否处罚。</p>
      </div>
      <div class="risk-head-actions">
        <span class="adm-count">待审核 {{ summary.pending || 0 }} 笔</span>
        <button class="adm-btn" type="button" :disabled="scanning" @click="scan">
          <el-icon><Refresh /></el-icon>{{ scanning ? '扫描中…' : '重新扫描' }}
        </button>
      </div>
    </div>

    <div class="risk-summary">
      <article class="risk-summary-card risk-high">
        <span>高风险</span>
        <strong>{{ summary.high || 0 }}</strong>
        <small>建议优先处理</small>
      </article>
      <article class="risk-summary-card risk-medium">
        <span>中风险</span>
        <strong>{{ summary.medium || 0 }}</strong>
        <small>需要结合证据判断</small>
      </article>
      <article class="risk-summary-card risk-pending">
        <span>待审核</span>
        <strong>{{ summary.pending || 0 }}</strong>
        <small>尚未给出人工结论</small>
      </article>
      <article class="risk-summary-card risk-confirmed">
        <span>已确认风险</span>
        <strong>{{ summary.confirmed || 0 }}</strong>
        <small>已留下审核结论</small>
      </article>
    </div>

    <section class="adm-panel">
      <div class="adm-panel-head">
        <div>
          <p class="adm-panel-eyebrow">RISK QUEUE</p>
          <h2 class="adm-panel-title">风险队列</h2>
        </div>
        <span class="adm-panel-meta">第 {{ page }} / {{ totalPages }} 页</span>
      </div>

      <div class="adm-filter">
        <label class="adm-search">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" placeholder="搜索订单号、标题或用户" maxlength="64" @keyup.enter="applyFilters" />
        </label>
        <select v-model="level" class="adm-select" @change="applyFilters">
          <option value="all">全部等级</option>
          <option value="high">高风险</option>
          <option value="medium">中风险</option>
          <option value="low">低风险</option>
        </select>
        <select v-model="reviewStatus" class="adm-select" @change="applyFilters">
          <option value="all">全部审核状态</option>
          <option value="待审核">待审核</option>
          <option value="已放行">已放行</option>
          <option value="已确认风险">已确认风险</option>
        </select>
        <button class="adm-btn adm-btn-primary" type="button" :disabled="loading" @click="applyFilters">搜索</button>
        <button class="adm-btn" type="button" @click="resetFilters">重置</button>
      </div>

      <div v-if="error" class="adm-error">
        <el-icon><WarningFilled /></el-icon><span>{{ error }}</span>
        <button class="adm-btn adm-btn-sm" type="button" @click="load">重试</button>
      </div>
      <div v-if="loading" class="risk-loading">正在读取风险订单…</div>

      <template v-else>
        <div v-if="rows.length" class="adm-table-wrap">
          <table class="adm-table risk-table">
            <thead>
              <tr>
                <th>风险</th>
                <th>订单</th>
                <th>交易双方</th>
                <th>状态</th>
                <th>报酬</th>
                <th>命中原因</th>
                <th>审核</th>
                <th>创建时间</th>
                <th class="adm-align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in rows" :key="item.orderId">
                <td>
                  <div class="risk-cell">
                    <span class="risk-badge" :class="`risk-${item.riskLevel}`">{{ levelLabel(item.riskLevel) }}</span>
                    <small>{{ item.riskScore }} 分</small>
                  </div>
                </td>
                <td>
                  <div class="risk-order">
                    <strong>#{{ item.orderId }}</strong>
                    <span :title="item.title">{{ item.title }}</span>
                  </div>
                </td>
                <td>
                  <div class="risk-pair">
                    <span>{{ item.publisherName || '—' }}</span>
                    <i>→</i>
                    <span>{{ item.runnerName || '未接单' }}</span>
                  </div>
                </td>
                <td><span class="adm-tag" :class="statusClass(item.orderStatus)">{{ item.orderStatus }}</span></td>
                <td class="risk-money">¥{{ money(item.reward) }}</td>
                <td>
                  <div class="risk-flags">
                    <span v-for="flag in (item.riskFlags || []).slice(0, 2)" :key="flag" class="risk-flag">{{ flag }}</span>
                    <small v-if="(item.riskFlags || []).length > 2">+{{ item.riskFlags.length - 2 }} 项</small>
                  </div>
                </td>
                <td><span class="adm-tag" :class="reviewClass(item.reviewStatus)">{{ item.reviewStatus }}</span></td>
                <td class="adm-muted risk-created">{{ formatDateTime(item.createdAt) }}</td>
                <td class="adm-align-right">
                  <button class="adm-btn adm-btn-sm" type="button" @click="open(item)">详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="adm-empty">
          <el-icon><CircleCheck /></el-icon>
          <strong>当前没有风险订单</strong>
          <span>规则已经扫过一遍，也可以点右上角「重新扫描」获取最新结果</span>
        </div>

        <div v-if="total > 0" class="adm-pagination">
          <span class="adm-pagination-meta">共 {{ total }} 笔风险订单 · 第 {{ page }} / {{ totalPages }} 页</span>
          <div class="adm-page-buttons">
            <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">‹</button>
            <button v-for="item in visiblePages" :key="item" type="button" :class="{ active: item === page }" @click="changePage(item)">{{ item }}</button>
            <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">›</button>
          </div>
        </div>
      </template>
    </section>

    <p class="adm-foot">
      风险分只用于后台排队和人工判断，不会自动扣信用、取消订单或封禁账号。已取消订单不再默认算作异常；取消只是订单状态，是否异常要看这里的风险信号。
    </p>

    <el-drawer v-model="drawer" size="580px" :with-header="false" append-to-body destroy-on-close>
      <div v-if="detail" class="risk-drawer">
        <div class="adm-drawer-head">
          <div>
            <p class="adm-eyebrow">RISK ORDER #{{ detail.orderId }}</p>
            <h2>{{ detail.title }}</h2>
            <div class="adm-drawer-sub">
              <span class="risk-badge" :class="`risk-${detail.riskLevel}`">{{ levelLabel(detail.riskLevel) }} · {{ detail.riskScore }} 分</span>
              <span class="adm-tag" :class="reviewClass(detail.reviewStatus)">{{ detail.reviewStatus }}</span>
            </div>
          </div>
          <button class="adm-drawer-close" type="button" aria-label="关闭" @click="drawer = false"><el-icon><Close /></el-icon></button>
        </div>

        <div class="risk-block">
          <span class="risk-block-title">命中原因</span>
          <ul class="risk-reason-list">
            <li v-for="flag in detail.riskFlags || []" :key="flag">{{ flag }}</li>
          </ul>
        </div>

        <div class="risk-facts">
          <div><span>订单状态</span><strong>{{ detail.orderStatus }}</strong></div>
          <div><span>类型</span><strong>{{ typeLabel(detail.type) }}</strong></div>
          <div><span>报酬</span><strong>¥{{ money(detail.reward) }}</strong></div>
          <div><span>创建时间</span><strong>{{ formatDateTime(detail.createdAt) }}</strong></div>
          <div><span>发布者</span><strong>{{ detail.publisherName || '—' }}</strong></div>
          <div><span>接单者</span><strong>{{ detail.runnerName || '未接单' }}</strong></div>
          <div><span>接单时间</span><strong>{{ formatDateTime(orderDetail?.acceptedAt) }}</strong></div>
          <div><span>提交凭证</span><strong>{{ formatDateTime(orderDetail?.submittedAt) }}</strong></div>
          <div><span>完成时间</span><strong>{{ formatDateTime(orderDetail?.completedAt) }}</strong></div>
          <div><span>取消时间</span><strong>{{ formatDateTime(orderDetail?.cancelledAt) }}</strong></div>
        </div>

        <div v-if="detail.reviewNote || detail.reviewedAt" class="risk-review-note">
          <span>最近审核</span>
          <p>{{ detail.reviewNote || '未填写备注' }}</p>
          <small>{{ detail.reviewerName || '管理员' }} · {{ formatDateTime(detail.reviewedAt) }}</small>
        </div>

        <p class="risk-drawer-hint">
          审核结论只记录风险队列，不会自动修改订单状态或用户信用。确认风险后，可到订单管理取消订单，或到用户管理停用账号。
        </p>
      </div>

      <div class="adm-drawer-actions">
        <button class="adm-btn" type="button" @click="drawer = false">关闭</button>
        <button class="adm-btn" type="button" :disabled="saving" @click="review('已放行')">标记放行</button>
        <button class="adm-btn adm-btn-danger" type="button" :disabled="saving" @click="review('已确认风险')">{{ saving ? '保存中…' : '确认风险' }}</button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Close, Refresh, Search, WarningFilled } from '@element-plus/icons-vue'
import { fetchAdminOrderDetail } from '../api/orders'
import { fetchRiskOrders, reviewRiskOrder, scanRiskOrders } from '../api/orders'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const level = ref('all')
const reviewStatus = ref('all')
const keyword = ref('')
const loading = ref(false)
const scanning = ref(false)
const saving = ref(false)
const error = ref('')
const summary = ref({})
const drawer = ref(false)
const detail = ref(null)
const orderDetail = ref(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, Math.min(page.value - 2, totalPages.value - 4))
  for (let i = start; i <= Math.min(totalPages.value, start + 4); i += 1) pages.push(i)
  return pages
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function money(value) {
  return value == null ? '0.00' : Number(value).toFixed(2)
}

function levelLabel(value) {
  return { high: '高风险', medium: '中风险', low: '低风险' }[value] || '风险'
}

function reviewClass(value) {
  return value === '已确认风险' ? 'adm-tag-red' : value === '已放行' ? 'adm-tag-green' : 'adm-tag-amber'
}

function statusClass(value) {
  return value === '已完成' ? 'adm-tag-green' : value === '已取消' ? 'adm-tag-gray' : 'adm-tag-blue'
}

function typeLabel(value) {
  return value === 'meal' ? '取餐' : '取件'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchRiskOrders({
      page: page.value,
      size,
      level: level.value,
      reviewStatus: reviewStatus.value,
      keyword: keyword.value.trim()
    })
    rows.value = data.items || []
    total.value = data.total || 0
    summary.value = data.summary || {}
  } catch (err) {
    error.value = err.message || '风险订单加载失败'
  } finally {
    loading.value = false
  }
}

async function scan() {
  scanning.value = true
  try {
    const data = await scanRiskOrders()
    ElMessage.success(`已扫描 ${data.scanned} 笔订单，发现 ${data.flagged} 笔风险`)
    page.value = 1
    await load()
  } catch (err) {
    ElMessage.error(err.message || '风险扫描失败')
  } finally {
    scanning.value = false
  }
}

async function open(item) {
  detail.value = item
  orderDetail.value = null
  drawer.value = true
  try {
    orderDetail.value = await fetchAdminOrderDetail(item.orderId)
  } catch {
    // 风险列表本身已带关键信息，订单详情失败不影响查看风险原因。
  }
}

async function review(status) {
  if (!detail.value) return
  const label = status === '已放行' ? '放行' : '确认风险'
  let note = ''
  try {
    const result = await ElMessageBox.prompt(
      `确定将订单 #${detail.value.orderId} 标记为「${status}」吗？可填写审核备注。`,
      label,
      {
        confirmButtonText: label,
        cancelButtonText: '取消',
        inputPlaceholder: '备注（可选，≤500 字）',
        inputValue: detail.value.reviewNote || '',
        inputValidator: value => !value || value.length <= 500 || '备注不能超过 500 字'
      }
    )
    note = result.value || ''
  } catch {
    return
  }
  saving.value = true
  try {
    const updated = await reviewRiskOrder(detail.value.orderId, { status, note })
    detail.value = updated
    ElMessage.success(`订单 #${updated.orderId} 已标记为${status}`)
    await load()
  } catch (err) {
    ElMessage.error(err.message || '审核保存失败')
  } finally {
    saving.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

function resetFilters() {
  keyword.value = ''
  level.value = 'all'
  reviewStatus.value = 'all'
  page.value = 1
  load()
}

function changePage(value) {
  if (value < 1 || value > totalPages.value) return
  page.value = value
  load()
}

onMounted(load)
</script>

<style scoped>
.risk-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.risk-summary-card {
  position: relative;
  overflow: hidden;
  padding: 15px 16px;
  border: 1px solid #e7eef7;
  border-radius: 11px;
  background: #fff;
}

.risk-summary-card::after {
  position: absolute;
  top: -24px;
  right: -14px;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: rgba(47, 125, 246, 0.05);
  content: '';
}

.risk-summary-card span,
.risk-summary-card small {
  display: block;
  color: #93a5b9;
  font-size: 10px;
}

.risk-summary-card strong {
  display: block;
  margin: 5px 0 2px;
  color: #2d435b;
  font-size: 22px;
  line-height: 1.2;
}

.risk-summary-card.risk-high strong { color: #d45f5f; }
.risk-summary-card.risk-medium strong { color: #c58a35; }
.risk-summary-card.risk-pending strong { color: #4a86d8; }
.risk-summary-card.risk-confirmed strong { color: #6c7d91; }

.risk-loading {
  padding: 44px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.risk-table {
  min-width: 1080px;
}

.risk-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.risk-cell small {
  color: #9aabba;
  font-size: 9px;
  white-space: nowrap;
}

.risk-created {
  white-space: nowrap;
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  height: 21px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 600;
  white-space: nowrap;
}

.risk-badge.risk-high {
  background: #fdf0f0;
  color: #c95858;
}

.risk-badge.risk-medium {
  background: #fff8eb;
  color: #ad7a2e;
}

.risk-badge.risk-low {
  background: #eff6ff;
  color: #4a86d8;
}

.risk-order {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 210px;
}

.risk-order strong {
  color: #2e455e;
  font-size: 10px;
}

.risk-order span {
  overflow: hidden;
  color: #7c8fa3;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-pair {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #526a82;
  font-size: 10px;
  white-space: nowrap;
}

.risk-pair i {
  color: #a3b3c3;
  font-style: normal;
}

.risk-money {
  color: #2f7df6;
  font-size: 11px;
  font-weight: 600;
}

.risk-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 270px;
}

.risk-flag {
  padding: 3px 6px;
  border-radius: 5px;
  background: #f6f9fd;
  color: #71859a;
  font-size: 8.5px;
  line-height: 1.35;
}

.risk-flags small {
  color: #9aaabc;
  font-size: 8.5px;
}

.risk-drawer {
  display: flex;
  flex-direction: column;
  height: calc(100% - 58px);
  overflow-y: auto;
  padding-right: 2px;
}

.risk-block {
  margin-top: 17px;
  padding: 14px;
  border: 1px solid #edf2f8;
  border-radius: 10px;
  background: #fbfdff;
}

.risk-block-title {
  color: #8fa0b3;
  font-size: 10px;
}

.risk-reason-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 9px 0 0;
  padding: 0;
  list-style: none;
}

.risk-reason-list li {
  position: relative;
  padding-left: 12px;
  color: #4d6278;
  font-size: 11px;
  line-height: 1.5;
}

.risk-reason-list li::before {
  position: absolute;
  top: 6px;
  left: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #dba36a;
  content: '';
}

.risk-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  margin-top: 14px;
}

.risk-facts div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 11px;
  border-radius: 8px;
  background: #f7fafd;
}

.risk-facts span {
  color: #93a5b9;
  font-size: 9px;
}

.risk-facts strong {
  color: #40556c;
  font-size: 10.5px;
  font-weight: 500;
  word-break: break-all;
}

.risk-review-note {
  margin-top: 13px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f9ff;
}

.risk-review-note span {
  color: #8fa0b3;
  font-size: 9px;
}

.risk-review-note p {
  margin: 5px 0 3px;
  color: #4d6278;
  font-size: 10.5px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.risk-review-note small {
  color: #9aaabc;
  font-size: 9px;
}

.risk-drawer-hint {
  margin: 16px 0 0;
  color: #9aaabc;
  font-size: 9.5px;
  line-height: 1.65;
}

@media (max-width: 1000px) {
  .risk-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .risk-head-actions {
    align-items: flex-end;
    flex-direction: column;
  }
}
</style>
