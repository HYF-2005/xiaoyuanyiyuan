<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">LOCATION INTELLIGENCE</p>
        <h1 class="adm-title">地理信息</h1>
        <p class="adm-sub">维护发布页的驿站、取餐档口和送达建筑，并按真实订单查看地点热度。</p>
      </div>
      <span class="adm-count">{{ tab === 'addresses' ? `地址字典 · ${overview.total} 项` : '只读统计 · 不含精确经纬度' }}</span>
    </div>

    <div class="loc-tabs">
      <button type="button" :class="{ active: tab === 'addresses' }" @click="tab = 'addresses'">地址配置</button>
      <button type="button" :class="{ active: tab === 'analytics' }" @click="tab = 'analytics'">使用分析</button>
    </div>

    <template v-if="tab === 'addresses'">
      <div v-if="addressError" class="adm-error">
        <el-icon><WarningFilled /></el-icon><span>{{ addressError }}</span>
        <button class="adm-btn adm-btn-sm" type="button" @click="loadAddresses">重试</button>
      </div>

      <div class="geo-cards">
        <article><span>地址总数</span><strong>{{ overview.total }}</strong><small>驿站 / 食堂 / 建筑统一维护</small></article>
        <article><span>取件驿站</span><strong>{{ countOf('station') }}</strong><small>取件发布页可选</small></article>
        <article><span>取餐档口</span><strong>{{ countOf('vendor') }}</strong><small>{{ countOf('canteen') }} 个食堂 · {{ countOf('floor') }} 个楼层</small></article>
        <article><span>送达建筑</span><strong>{{ countOf('building') }}</strong><small>{{ countOf('building_category') }} 个建筑分类</small></article>
      </div>

      <section class="adm-panel">
        <div class="adm-panel-head">
          <div>
            <p class="adm-panel-eyebrow">LOCATION DICTIONARY</p>
            <h2 class="adm-panel-title">地址字典</h2>
          </div>
          <div class="loc-head-actions">
            <span class="adm-panel-meta">{{ enabledCount }} 项启用 / {{ overview.total }} 项</span>
            <button class="adm-btn adm-btn-primary adm-btn-sm" type="button" @click="openCreate">
              <el-icon><Plus /></el-icon>新增地址
            </button>
          </div>
        </div>

        <div class="adm-filter loc-filter">
          <label class="adm-search">
            <el-icon><Search /></el-icon>
            <input v-model="keyword" placeholder="搜索地址名称或上级" />
          </label>
          <select v-model="kindFilter" class="adm-select">
            <option value="all">全部类型</option>
            <option v-for="item in KIND_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="statusFilter" class="adm-select">
            <option value="all">全部状态</option>
            <option value="enabled">已启用</option>
            <option value="disabled">已停用</option>
          </select>
          <button class="adm-btn adm-btn-sm" type="button" @click="resetFilters">重置</button>
        </div>

        <div v-if="addressLoading" class="geo-loading">正在读取地址字典…</div>
        <template v-else>
          <div v-if="filteredItems.length" class="adm-table-wrap">
            <table class="adm-table loc-table">
              <thead>
                <tr>
                  <th>地址名称</th>
                  <th>类型</th>
                  <th>上级</th>
                  <th class="loc-num">排序</th>
                  <th class="loc-num">子项</th>
                  <th>状态</th>
                  <th class="adm-align-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedItems" :key="item.id" :class="{ 'loc-disabled': !item.enabled }">
                  <td>
                    <div class="loc-name">
                      <span class="loc-dot" :class="`loc-dot-${item.kind}`"></span>
                      <strong>{{ item.name }}</strong>
                    </div>
                  </td>
                  <td><span class="adm-tag" :class="kindMeta(item.kind).className">{{ item.kindLabel }}</span></td>
                  <td class="loc-parent">{{ item.parentPath || '—' }}</td>
                  <td class="loc-num">{{ item.sortOrder }}</td>
                  <td class="loc-num">{{ item.childCount || '—' }}</td>
                  <td>
                    <span class="adm-tag" :class="item.enabled ? 'adm-tag-green' : 'adm-tag-gray'">
                      {{ item.enabled ? '已启用' : '已停用' }}
                    </span>
                  </td>
                  <td class="adm-align-right">
                    <button class="adm-btn adm-btn-sm" type="button" @click="openEdit(item)">编辑</button>
                    <button class="adm-btn adm-btn-sm adm-btn-danger" type="button" @click="removeLocation(item)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="adm-empty">
            <el-icon><Location /></el-icon>
            <strong>没有匹配的地址</strong>
            <span>可以调整筛选条件，或新增一个地址</span>
          </div>

          <div v-if="filteredItems.length" class="adm-pagination">
            <span class="adm-pagination-meta">共 {{ filteredItems.length }} 项 · 第 {{ page }} / {{ totalPages }} 页</span>
            <div class="adm-page-buttons">
              <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">‹</button>
              <button v-for="item in visiblePages" :key="item" type="button" :class="{ active: item === page }" @click="changePage(item)">{{ item }}</button>
              <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">›</button>
            </div>
          </div>
        </template>
      </section>

      <p class="adm-foot">地址保存后，小程序发布页重新进入时会读取最新启用项；停用或改名不会修改历史订单里已保存的文字。取件驿站用于取件发布，食堂 / 楼层 / 档口用于取餐发布，建筑分类 / 送达建筑用于送达地址。</p>
    </template>

    <template v-else>
      <div v-if="analyticsError" class="adm-error">
        <el-icon><WarningFilled /></el-icon><span>{{ analyticsError }}</span>
        <button class="adm-btn adm-btn-sm" type="button" @click="loadAnalytics">重试</button>
      </div>
      <div v-if="analyticsLoading" class="geo-loading">正在聚合订单地点…</div>
      <template v-else>
        <div class="geo-cards">
          <article><span>取件/取餐点</span><strong>{{ analytics.distinctPickupPoints }}</strong><small>有订单的不同地点</small></article>
          <article><span>订单地点记录</span><strong>{{ totalRecords }}</strong><small>取件点 + 送达建筑</small></article>
          <article><span>最热取件点</span><strong class="ellipsis">{{ topPickup?.name || '—' }}</strong><small>{{ topPickup ? `${topPickup.orderCount} 笔订单` : '暂无数据' }}</small></article>
          <article><span>最热送达建筑</span><strong class="ellipsis">{{ topBuilding?.name || '—' }}</strong><small>{{ topBuilding ? `${topBuilding.orderCount} 笔订单` : '暂无数据' }}</small></article>
        </div>

        <div class="geo-grid">
          <section class="adm-panel">
            <div class="adm-panel-head">
              <div><p class="adm-panel-eyebrow">PICKUP POINTS</p><h2 class="adm-panel-title">取件 / 取餐点分布</h2></div>
              <span class="adm-panel-meta">{{ analytics.pickupPoints?.length || 0 }} 个地点</span>
            </div>
            <div v-if="analytics.pickupPoints?.length" class="geo-list">
              <div v-for="(item, index) in analytics.pickupPoints" :key="`${item.name}-${item.kind}`" class="geo-row">
                <span class="geo-rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="geo-info">
                  <strong>{{ item.name }}</strong>
                  <span class="adm-tag" :class="item.kind === '取餐' ? 'adm-tag-amber' : 'adm-tag-blue'">{{ item.kind }}</span>
                </div>
                <div class="geo-bar"><i :style="{ width: `${percent(item.orderCount, maxPickup)}%` }"></i></div>
                <div class="geo-stat"><strong>{{ item.orderCount }}</strong><span>笔</span></div>
                <div class="geo-money">¥{{ money(item.totalAmount) }}</div>
              </div>
            </div>
            <div v-else class="adm-empty"><el-icon><Location /></el-icon><strong>暂无地点数据</strong><span>发布订单后会按取件驿站或餐厅聚合</span></div>
          </section>

          <section class="adm-panel">
            <div class="adm-panel-head">
              <div><p class="adm-panel-eyebrow">DELIVERY BUILDINGS</p><h2 class="adm-panel-title">送达建筑分布</h2></div>
              <span class="adm-panel-meta">{{ analytics.deliveryBuildings?.length || 0 }} 栋</span>
            </div>
            <div v-if="analytics.deliveryBuildings?.length" class="geo-list">
              <div v-for="(item, index) in analytics.deliveryBuildings" :key="item.name" class="geo-row">
                <span class="geo-rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="geo-info"><strong>{{ item.name }}</strong><span class="adm-tag adm-tag-green">送达</span></div>
                <div class="geo-bar"><i :style="{ width: `${percent(item.orderCount, maxBuilding)}%` }"></i></div>
                <div class="geo-stat"><strong>{{ item.orderCount }}</strong><span>笔</span></div>
                <div class="geo-money">¥{{ money(item.totalAmount) }}</div>
              </div>
            </div>
            <div v-else class="adm-empty"><el-icon><Location /></el-icon><strong>暂无送达数据</strong><span>订单送达建筑会在这里汇总</span></div>
          </section>
        </div>
        <p class="adm-foot">订单只保存地点名称，没有经纬度，因此这里做地点聚合而不是地图点位；使用分析只读，不会反向修改地址字典。</p>
      </template>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑地址' : '新增地址'" width="540px" append-to-body>
      <div class="loc-form">
        <label>
          <span>地址类型</span>
          <el-select v-model="form.kind" class="loc-control" @change="onKindChange">
            <el-option v-for="item in KIND_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </label>
        <label v-if="needsParent">
          <span>上级地址</span>
          <el-select v-model="form.parentId" class="loc-control" filterable placeholder="请选择上级地址">
            <el-option v-for="item in parentOptions" :key="item.id" :value="item.id" :label="parentLabel(item)" />
          </el-select>
        </label>
        <label>
          <span>地址名称</span>
          <input v-model="form.name" class="adm-input" maxlength="80" placeholder="例如 北苑菜鸟驿站 / 1楼 / 一号学生宿舍" />
        </label>
        <div class="loc-form-row">
          <label>
            <span>排序</span>
            <input v-model.number="form.sortOrder" class="adm-input" type="number" min="0" max="9999" />
          </label>
          <label>
            <span>状态</span>
            <span class="loc-switch"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></span>
          </label>
        </div>
        <p class="loc-hint">
          {{ kindHint }}<br />
          修改类型或上级会同步调整层级；如果存在不兼容的子项，系统会阻止保存。
        </p>
        <p v-if="formError" class="loc-error">{{ formError }}</p>
      </div>
      <template #footer>
        <button class="adm-btn" type="button" @click="dialogVisible = false">取消</button>
        <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="saveLocation">{{ saving ? '保存中…' : '保存' }}</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus, Search, WarningFilled } from '@element-plus/icons-vue'
import { errorMessage } from '../api/http'
import { createAdminLocation, deleteAdminLocation, fetchAdminLocations, updateAdminLocation } from '../api/locations'
import { fetchGeographyOverview } from '../api/system'

const KIND_OPTIONS = [
  { value: 'station', label: '取件驿站' },
  { value: 'canteen', label: '食堂' },
  { value: 'floor', label: '食堂楼层' },
  { value: 'vendor', label: '取餐档口' },
  { value: 'building_category', label: '建筑分类' },
  { value: 'building', label: '送达建筑' }
]
const KIND_META = {
  station: { className: 'adm-tag-blue' },
  canteen: { className: 'adm-tag-amber' },
  floor: { className: 'adm-tag-gray' },
  vendor: { className: 'adm-tag-green' },
  building_category: { className: 'adm-tag-gray' },
  building: { className: 'adm-tag-blue' }
}
const PAGE_SIZE = 20

const tab = ref('addresses')
const overview = ref({ items: [], total: 0, counts: {} })
const addressLoading = ref(false)
const addressError = ref('')
const keyword = ref('')
const kindFilter = ref('all')
const statusFilter = ref('all')
const page = ref(1)
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({ kind: 'station', parentId: 0, name: '', sortOrder: 0, enabled: true })

const analytics = ref({ distinctPickupPoints: 0, pickupPoints: [], deliveryBuildings: [] })
const analyticsLoading = ref(false)
const analyticsError = ref('')

const enabledCount = computed(() => overview.value.items.filter(item => item.enabled).length)
const filteredItems = computed(() => {
  const query = keyword.value.trim()
  return overview.value.items.filter(item => {
    if (kindFilter.value !== 'all' && item.kind !== kindFilter.value) return false
    if (statusFilter.value === 'enabled' && !item.enabled) return false
    if (statusFilter.value === 'disabled' && item.enabled) return false
    if (!query) return true
    return `${item.name} ${item.parentPath || ''} ${item.kindLabel || ''}`.includes(query)
  })
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)))
const pagedItems = computed(() => filteredItems.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))
const visiblePages = computed(() => Array.from({ length: totalPages.value }, (_, index) => index + 1))
const needsParent = computed(() => ['floor', 'vendor', 'building'].includes(form.kind))
const parentKind = computed(() => form.kind === 'floor' ? 'canteen' : form.kind === 'vendor' ? 'floor' : form.kind === 'building' ? 'building_category' : '')
const parentOptions = computed(() => {
  if (!parentKind.value) return []
  return overview.value.items.filter(item => item.kind === parentKind.value)
})
const kindHint = computed(() => {
  const hints = {
    station: '取件发布页的驿站选项，可继续增加其他驿站。',
    canteen: '取餐发布的食堂一级选项，例如北苑食堂。',
    floor: '挂在食堂下的楼层，例如 1楼 / 2楼。',
    vendor: '挂在楼层下的具体档口，发布页会拼成“食堂-楼层-档口”。',
    building_category: '送达建筑的一级分类，例如宿舍楼 / 教学楼。',
    building: '具体的送达建筑，发布页会展示为送达地址。'
  }
  return hints[form.kind] || ''
})

const topPickup = computed(() => analytics.value.pickupPoints?.[0] || null)
const topBuilding = computed(() => analytics.value.deliveryBuildings?.[0] || null)
const maxPickup = computed(() => Math.max(1, ...(analytics.value.pickupPoints || []).map(item => Number(item.orderCount) || 0)))
const maxBuilding = computed(() => Math.max(1, ...(analytics.value.deliveryBuildings || []).map(item => Number(item.orderCount) || 0)))
const totalRecords = computed(() => (analytics.value.pickupPoints?.length || 0) + (analytics.value.deliveryBuildings?.length || 0))

function kindMeta(kind) {
  return KIND_META[kind] || { className: 'adm-tag-gray' }
}

function countOf(kind) {
  return overview.value.counts?.[kind] || 0
}

function parentLabel(item) {
  if (item.kind === 'floor') return `${item.parentPath || '食堂'} / ${item.name}`
  return item.name
}

function percent(value, max) {
  return Math.max(8, Math.round((Number(value) / max) * 100))
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function changePage(value) {
  page.value = Math.min(Math.max(1, value), totalPages.value)
}

function resetFilters() {
  keyword.value = ''
  kindFilter.value = 'all'
  statusFilter.value = 'all'
  page.value = 1
}

watch([keyword, kindFilter, statusFilter], () => { page.value = 1 })
watch(totalPages, value => { if (page.value > value) page.value = value })

async function loadAddresses() {
  addressLoading.value = true
  addressError.value = ''
  try {
    overview.value = await fetchAdminLocations()
    if (page.value > totalPages.value) page.value = totalPages.value
  } catch (err) {
    addressError.value = errorMessage(err, '地址字典加载失败')
    ElMessage.error(addressError.value)
  } finally {
    addressLoading.value = false
  }
}

async function loadAnalytics() {
  analyticsLoading.value = true
  analyticsError.value = ''
  try {
    analytics.value = await fetchGeographyOverview()
  } catch (err) {
    analyticsError.value = errorMessage(err, '地理统计加载失败')
    ElMessage.error(analyticsError.value)
  } finally {
    analyticsLoading.value = false
  }
}

function openCreate() {
  editingId.value = null
  formError.value = ''
  Object.assign(form, { kind: 'station', parentId: 0, name: '', sortOrder: 0, enabled: true })
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  formError.value = ''
  Object.assign(form, { kind: item.kind, parentId: item.parentId || 0, name: item.name, sortOrder: item.sortOrder, enabled: item.enabled })
  dialogVisible.value = true
}

function onKindChange() {
  form.parentId = needsParent.value ? null : 0
  form.sortOrder = 0
  formError.value = ''
}

async function saveLocation() {
  formError.value = ''
  if (!form.name.trim()) {
    formError.value = '请填写地址名称'
    return
  }
  if (needsParent.value && !form.parentId) {
    formError.value = '请选择上级地址'
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAdminLocation(editingId.value, {
        kind: form.kind,
        parentId: needsParent.value ? form.parentId : 0,
        name: form.name,
        sortOrder: form.sortOrder,
        enabled: form.enabled
      })
      ElMessage.success('地址已更新')
    } else {
      await createAdminLocation({
        kind: form.kind,
        parentId: needsParent.value ? form.parentId : 0,
        name: form.name,
        sortOrder: form.sortOrder,
        enabled: form.enabled
      })
      ElMessage.success('地址已新增')
      page.value = 1
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (err) {
    formError.value = errorMessage(err, '地址保存失败')
  } finally {
    saving.value = false
  }
}

async function removeLocation(item) {
  if (item.childCount > 0) {
    ElMessage.warning('该地址下还有子项，请先删除或调整子项')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除「${item.name}」？历史订单中的地点文字不会被修改。`, '删除地址', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAdminLocation(item.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(errorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  loadAddresses()
  loadAnalytics()
})
</script>

<style scoped>
.loc-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.loc-tabs button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #dfe9f5;
  border-radius: 7px;
  background: #fff;
  color: #708198;
  font-size: 12px;
  cursor: pointer;
}

.loc-tabs button.active {
  border-color: #2f7df6;
  background: #eef5ff;
  color: #256fdc;
  font-weight: 600;
}

.loc-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loc-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5fa;
}

.loc-filter .adm-search {
  min-width: 240px;
  flex: 1 1 240px;
}

.loc-table td {
  padding-top: 12px;
  padding-bottom: 12px;
}

.loc-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 150px;
}

.loc-name strong {
  color: #2e465e;
  font-size: 11px;
  font-weight: 600;
}

.loc-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9bb4ce;
  flex: 0 0 auto;
}

.loc-dot-station { background: #6fa7e8; }
.loc-dot-canteen { background: #d9b05c; }
.loc-dot-floor { background: #9aaabc; }
.loc-dot-vendor { background: #5cba91; }
.loc-dot-building_category { background: #b4a4d9; }
.loc-dot-building { background: #5e9ede; }

.loc-disabled .loc-name strong,
.loc-disabled .loc-parent {
  color: #a6b4c2;
}

.loc-parent {
  max-width: 240px;
  overflow: hidden;
  color: #8395a8;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loc-num {
  width: 62px;
  color: #8497aa;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.geo-loading {
  padding: 46px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.geo-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.geo-cards article {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid #e7eef7;
  border-radius: 14px;
  background: #fff;
}

.geo-cards span {
  color: #8b9caf;
  font-size: 10px;
}

.geo-cards strong {
  overflow: hidden;
  color: #1f3650;
  font-size: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.geo-cards strong.ellipsis {
  font-size: 15px;
}

.geo-cards small {
  color: #a1afbe;
  font-size: 9px;
}

.geo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.geo-list {
  padding: 0 20px 10px;
}

.geo-row {
  display: grid;
  grid-template-columns: 22px minmax(120px, 1.4fr) minmax(50px, 1fr) 42px 58px;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f6fa;
}

.geo-row:last-child {
  border-bottom: 0;
}

.geo-rank {
  color: #b3c2d1;
  font-size: 9px;
  font-variant-numeric: tabular-nums;
}

.geo-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.geo-info strong {
  overflow: hidden;
  color: #31485f;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.geo-info .adm-tag {
  align-self: flex-start;
}

.geo-bar {
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #f0f5fa;
}

.geo-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #79b1f2, #2f7df6);
}

.geo-stat {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.geo-stat strong {
  color: #20364f;
  font-size: 13px;
}

.geo-stat span {
  color: #9cabbc;
  font-size: 9px;
}

.geo-money {
  color: #75879b;
  font-size: 10px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.loc-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.loc-form > label,
.loc-form-row > label {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #6b7f94;
  font-size: 11px;
}

.loc-form .loc-control,
.loc-form .adm-input {
  width: 100%;
  box-sizing: border-box;
}

.loc-form .el-select__wrapper {
  min-height: 34px;
  border-radius: 8px;
  background: #fbfdff;
  box-shadow: 0 0 0 1px #e2eaf4 inset;
}

.loc-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.loc-switch {
  display: flex;
  align-items: center;
  height: 34px;
}

.loc-hint {
  margin: 0;
  padding: 9px 11px;
  border-radius: 8px;
  background: #f6f9fd;
  color: #8da0b4;
  font-size: 10px;
  line-height: 1.6;
}

.loc-error {
  margin: 0;
  color: #d56b6b;
  font-size: 11px;
}

@media (max-width: 1100px) {
  .geo-grid {
    grid-template-columns: 1fr;
  }

  .geo-cards {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 650px) {
  .geo-cards {
    grid-template-columns: 1fr;
  }

  .geo-row {
    grid-template-columns: 20px minmax(100px, 1fr) 40px 54px;
  }

  .geo-bar {
    display: none;
  }

  .loc-head-actions {
    align-items: flex-end;
    flex-direction: column;
    gap: 8px;
  }

  .loc-form-row {
    grid-template-columns: 1fr;
  }
}
</style>
