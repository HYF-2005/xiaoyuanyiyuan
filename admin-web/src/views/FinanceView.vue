<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">FINANCE · READ ONLY</p>
        <h1 class="adm-title">财务管理</h1>
        <p class="adm-sub">按订单状态查看演示账本与对账口径；不包含真实支付、提现或退款操作。</p>
      </div>
      <span class="adm-count">只读账本 · 不产生资金操作</span>
    </div>

    <div class="fin-cards">
      <article class="fin-card">
        <span>发布订单总额</span>
        <strong>¥{{ money(summary.publishedAmount) }}</strong>
        <small>{{ summary.totalOrders }} 笔订单</small>
      </article>
      <article class="fin-card accent">
        <span>已完成金额</span>
        <strong>¥{{ money(summary.completedAmount) }}</strong>
        <small>{{ summary.completedOrders }} 笔已完成</small>
      </article>
      <article class="fin-card warn">
        <span>已取消退款</span>
        <strong>¥{{ money(summary.refundedAmount) }}</strong>
        <small>{{ summary.cancelledOrders }} 笔已取消</small>
      </article>
      <article class="fin-card">
        <span>订单总数</span>
        <strong>{{ summary.totalOrders }}</strong>
        <small>完成 {{ summary.completedOrders }} · 取消 {{ summary.cancelledOrders }}</small>
      </article>
    </div>

    <div class="fin-tabs">
      <button type="button" :class="{ active: view === 'ledger' }" @click="changeView('ledger')">收入流水</button>
      <button type="button" :class="{ active: view === 'summary' }" @click="changeView('summary')">对账汇总</button>
      <span class="fin-note">金额来自 orders.reward，不代表真实资金流转</span>
    </div>

    <template v-if="view === 'ledger'">
      <section class="adm-panel">
        <div class="adm-panel-head">
          <div>
            <p class="adm-panel-eyebrow">DEMO LEDGER</p>
            <h2 class="adm-panel-title">收入流水</h2>
          </div>
          <span class="adm-panel-meta">第 {{ page }} / {{ totalPages }} 页</span>
        </div>

        <div class="adm-filter">
          <label class="adm-search">
            <el-icon><Search /></el-icon>
            <input v-model="keyword" placeholder="搜索订单号或标题" maxlength="64" @keyup.enter="applyFilters" />
          </label>
          <select v-model="kind" class="adm-select" @change="applyFilters">
            <option value="all">全部类型</option>
            <option value="publish">发布支出</option>
            <option value="refund">取消退款</option>
            <option value="income">完成收入</option>
          </select>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="loading" @click="applyFilters">搜索</button>
          <button class="adm-btn" type="button" @click="resetFilters">重置</button>
        </div>

        <div v-if="error" class="adm-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ error }}</span>
          <button class="adm-btn adm-btn-sm" type="button" @click="loadLedger">重试</button>
        </div>

        <div v-if="loading" class="fin-loading">正在读取流水…</div>
        <template v-else>
          <div v-if="entries.length" class="adm-table-wrap">
            <table class="adm-table">
              <thead>
                <tr>
                  <th>流水号</th>
                  <th>关联订单</th>
                  <th>说明</th>
                  <th>类型</th>
                  <th class="adm-align-right">金额</th>
                  <th>发生时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="entry in entries" :key="entry.id">
                  <td class="adm-muted">{{ entry.id }}</td>
                  <td><span class="adm-strong">#{{ entry.orderId }}</span></td>
                  <td>{{ entry.title }}</td>
                  <td><span class="adm-tag" :class="kindClass(entry.kind)">{{ kindLabel(entry.kind) }}</span></td>
                  <td class="adm-align-right adm-money" :class="{ negative: Number(entry.amount) < 0 }">
                    {{ Number(entry.amount) < 0 ? '-' : '+' }}¥{{ money(Math.abs(Number(entry.amount))) }}
                  </td>
                  <td class="adm-muted">{{ formatDateTime(entry.occurredAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="adm-empty">
            <el-icon><Money /></el-icon>
            <strong>暂无流水</strong>
            <span>当前筛选条件下没有可展示的演示流水</span>
          </div>

          <div v-if="total > 0" class="adm-pagination">
            <span class="adm-pagination-meta">共 {{ total }} 条流水 · 第 {{ page }} / {{ totalPages }} 页</span>
            <div class="adm-page-buttons">
              <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">‹</button>
              <button v-for="item in visiblePages" :key="item" type="button" :class="{ active: item === page }" @click="changePage(item)">{{ item }}</button>
              <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">›</button>
            </div>
          </div>
        </template>
      </section>
      <p class="adm-foot">发布支出为负、完成收入和取消退款为正；这是内部演示账本，未接入支付渠道，不能用于真实对账。</p>
    </template>

    <template v-else>
      <section class="adm-panel summary-panel">
        <div class="adm-panel-head">
          <div>
            <p class="adm-panel-eyebrow">RECONCILIATION</p>
            <h2 class="adm-panel-title">订单状态对账汇总</h2>
          </div>
          <span class="adm-panel-meta">口径版本 2026.10</span>
        </div>
        <div class="summary-grid">
          <div class="summary-item">
            <span>订单总数</span>
            <strong>{{ summary.totalOrders }}</strong>
            <small>全部订单</small>
          </div>
          <div class="summary-item">
            <span>已完成</span>
            <strong>{{ summary.completedOrders }}</strong>
            <small>计入完成金额</small>
          </div>
          <div class="summary-item">
            <span>已取消</span>
            <strong>{{ summary.cancelledOrders }}</strong>
            <small>计入取消退款</small>
          </div>
          <div class="summary-item">
            <span>进行中</span>
            <strong>{{ inProgress }}</strong>
            <small>发布后尚未结束</small>
          </div>
        </div>
        <div class="summary-explain">
          <h3>对账口径</h3>
          <ul>
            <li>发布金额：所有订单报酬之和，仅用于统计发布规模。</li>
            <li>完成金额：状态为「已完成」的订单报酬之和。</li>
            <li>取消退款：状态为「已取消」的订单报酬之和，演示为退款流水。</li>
            <li>当前系统采用线下支付，后台不保存真实支付流水，也不支持提现、打款或人工改账。</li>
          </ul>
        </div>
      </section>
      <p class="adm-foot">如后续接入真实支付，需要新增支付渠道流水、分账/退款状态和幂等对账键，不能直接使用订单金额代替真实账单。</p>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Money, Search, WarningFilled } from '@element-plus/icons-vue'
import { fetchFinanceLedger, fetchFinanceSummary } from '../api/finance'

const route = useRoute()
const router = useRouter()
const view = computed(() => (route.query.view === 'summary' ? 'summary' : 'ledger'))
const summary = ref({ totalOrders: 0, completedOrders: 0, cancelledOrders: 0, publishedAmount: 0, completedAmount: 0, refundedAmount: 0, netAmount: 0 })
const entries = ref([])
const keyword = ref('')
const kind = ref('all')
const page = ref(1)
const size = 15
const total = ref(0)
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const inProgress = computed(() => Math.max(0, Number(summary.value.totalOrders || 0) - Number(summary.value.completedOrders || 0) - Number(summary.value.cancelledOrders || 0)))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, Math.min(page.value - 2, totalPages.value - 4))
  for (let i = start; i <= Math.min(totalPages.value, start + 4); i += 1) pages.push(i)
  return pages
})

function money(value) {
  return Number(value || 0).toFixed(2)
}

function kindLabel(value) {
  return { publish: '发布支出', refund: '取消退款', income: '完成收入' }[value] || value
}

function kindClass(value) {
  return { publish: 'adm-tag-gray', refund: 'adm-tag-amber', income: 'adm-tag-green' }[value] || ''
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

async function loadSummary() {
  try {
    summary.value = await fetchFinanceSummary()
  } catch (err) {
    ElMessage.error(err.message || '财务汇总加载失败')
  }
}

async function loadLedger() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchFinanceLedger({ page: page.value, size, kind: kind.value, keyword: keyword.value.trim() })
    entries.value = data.items || []
    total.value = data.total || 0
    summary.value = data.summary || summary.value
  } catch (err) {
    error.value = err.message || '流水加载失败'
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  loadLedger()
}

function resetFilters() {
  keyword.value = ''
  kind.value = 'all'
  page.value = 1
  loadLedger()
}

function changePage(value) {
  if (value < 1 || value > totalPages.value) return
  page.value = value
  loadLedger()
}

function changeView(value) {
  router.replace({ path: '/finance', query: value === 'summary' ? { view: 'summary' } : {} })
}

onMounted(() => {
  loadSummary()
  loadLedger()
})
</script>

<style scoped>
.fin-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.fin-card {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 17px 18px;
  border: 1px solid #e7eef7;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 6px 22px rgba(31, 63, 99, 0.035);
}

.fin-card span {
  color: #8b9caf;
  font-size: 10px;
}

.fin-card strong {
  color: #1d334d;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.fin-card small {
  color: #a4b2c1;
  font-size: 9px;
}

.fin-card.accent strong {
  color: #2f7df6;
}

.fin-card.warn strong {
  color: #c58e37;
}

.fin-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}

.fin-tabs button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #fff;
  color: #76889d;
  font-size: 11px;
  cursor: pointer;
}

.fin-tabs button.active {
  border-color: #d7e6fb;
  background: #edf5ff;
  color: #2f7df6;
  font-weight: 600;
}

.fin-note {
  margin-left: auto;
  color: #a1afbe;
  font-size: 9px;
}

.negative {
  color: #c77575 !important;
}

.fin-loading {
  padding: 40px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 8px 20px 20px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 15px;
  border: 1px solid #edf2f8;
  border-radius: 11px;
  background: #fbfdff;
}

.summary-item span {
  color: #8b9caf;
  font-size: 10px;
}

.summary-item strong {
  color: #20364f;
  font-size: 22px;
}

.summary-item small {
  color: #a4b2c1;
  font-size: 9px;
}

.summary-explain {
  padding: 0 20px 22px;
}

.summary-explain h3 {
  margin: 0 0 8px;
  color: #455d76;
  font-size: 12px;
}

.summary-explain ul {
  margin: 0;
  padding-left: 17px;
  color: #8596a9;
  font-size: 11px;
  line-height: 1.9;
}

@media (max-width: 1000px) {
  .fin-cards,
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .fin-cards,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .fin-note {
    display: none;
  }
}
</style>
