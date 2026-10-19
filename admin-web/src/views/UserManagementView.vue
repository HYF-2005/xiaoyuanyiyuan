<template>
  <div class="users-page">
    <div class="page-head">
      <div>
        <p class="page-eyebrow">USER DIRECTORY</p>
        <h1>用户管理</h1>
        <p class="page-sub">管理小程序注册用户、账号状态与信用记录，支持 Excel 批量导入。</p>
      </div>
      <div class="head-actions">
        <span class="total-badge">共 {{ total }} 位用户</span>
        <button class="import-open-btn" type="button" @click="openImport">
          <el-icon><UploadFilled /></el-icon>
          批量导入
        </button>
      </div>
    </div>

    <div class="filter-bar">
      <label class="search-field">
        <el-icon><Search /></el-icon>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索账号、姓名或手机号"
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

      <div class="status-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          :class="{ active: status === tab.value }"
          @click="changeStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="filter-actions">
        <button class="search-button" type="button" :disabled="loading" @click="applyFilters">
          搜索
        </button>
        <button class="reset-button" type="button" @click="resetFilters">重置</button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head">
        <div>
          <p class="panel-eyebrow">MEMBERS</p>
          <h2>用户列表</h2>
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
          <div class="skeleton skeleton-avatar"></div>
          <div class="skeleton skeleton-name"></div>
          <div class="skeleton skeleton-mid"></div>
          <div class="skeleton skeleton-short"></div>
          <div class="skeleton skeleton-mid"></div>
        </div>
      </div>

      <template v-else>
        <div v-if="rows.length" class="table-wrap">
          <table class="user-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>手机号</th>
                <th>订单</th>
                <th>信用</th>
                <th>状态</th>
                <th>注册时间</th>
                <th class="align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in rows" :key="user.id">
                <td>
                  <div class="user-cell">
                    <div class="user-avatar" :class="avatarTone(user.id)">
                      {{ initial(user.displayName || user.username) }}
                    </div>
                    <div class="user-identity">
                      <strong>{{ user.displayName || '未命名用户' }}</strong>
                      <span>@{{ user.username }}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="phone" :class="{ muted: !user.phone }">
                    {{ maskPhone(user.phone) }}
                  </span>
                </td>
                <td>
                  <div class="order-count">
                    <strong>{{ user.publishedOrders + user.acceptedOrders }}</strong>
                    <span>
                      发 {{ user.publishedOrders }} · 接 {{ user.acceptedOrders }}
                      <b v-if="user.openOrders > 0">进行中 {{ user.openOrders }}</b>
                    </span>
                  </div>
                </td>
                <td>
                  <span class="credit-badge">{{ user.creditBase }}</span>
                  <span class="credit-note">基础分</span>
                </td>
                <td>
                  <span class="status-tag" :class="statusClass(user)">
                    {{ statusLabel(user) }}
                  </span>
                </td>
                <td class="time">{{ formatDate(user.createdAt) }}</td>
                <td class="align-right">
                  <div class="row-actions">
                    <button type="button" @click="openDetail(user)">详情</button>
                    <button
                      v-if="!user.closed"
                      type="button"
                      :class="{ danger: user.active }"
                      :disabled="actionLoading === user.id"
                      @click="confirmToggle(user)"
                    >
                      {{ actionLoading === user.id ? '处理中…' : user.active ? '停用' : '启用' }}
                    </button>
                    <span v-else class="closed-note">已注销</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="empty-state">
          <el-icon><User /></el-icon>
          <strong>没有找到用户</strong>
          <span>{{ keyword ? '换个关键词试试' : '当前筛选条件下暂无用户' }}</span>
        </div>

        <div v-if="total > 0" class="pagination">
          <span class="pagination-meta">共 {{ total }} 位用户 · 第 {{ page }} / {{ totalPages }} 页</span>
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
      停用账号会立即撤销该用户的小程序登录会话；历史订单和账单保留。
    </p>

    <el-dialog
      v-model="importDialog"
      class="import-dialog"
      title="批量导入用户"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="!importing"
      :show-close="!importing"
      @closed="resetImport"
    >
      <div class="import-type-box">
        <p>请选择要导入的用户类型：</p>
        <div class="import-type-options">
          <button class="import-type active" type="button">
            <el-icon><User /></el-icon>
            学生用户
          </button>
          <button class="import-type" type="button" disabled title="当前仅提供学生用户导入模板">
            <el-icon><UserFilled /></el-icon>
            教师用户
            <em>暂未开放</em>
          </button>
          <button class="import-type" type="button" disabled title="当前仅提供学生用户导入模板">
            <el-icon><Shop /></el-icon>
            商家用户
            <em>暂未开放</em>
          </button>
        </div>
      </div>

      <div class="import-steps" aria-label="导入步骤">
        <div
          v-for="(item, index) in importSteps"
          :key="item"
          class="import-step"
          :class="{ active: importStep === index + 1, done: importStep > index + 1 }"
        >
          <span class="step-index">
            <el-icon v-if="importStep > index + 1"><CircleCheckFilled /></el-icon>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <strong>{{ item }}</strong>
          <i v-if="index < importSteps.length - 1"></i>
        </div>
      </div>

      <div v-if="importStep === 1" class="import-pane">
        <div class="import-template-card">
          <div class="template-icon"><el-icon><Document /></el-icon></div>
          <div class="template-copy">
            <strong>学生批量导入模板.xlsx</strong>
            <span>用于批量导入学生用户信息</span>
            <small>字段：学号、姓名、手机号、身份证号码、学院、专业、年级、班级、宿舍、信用分</small>
          </div>
          <a
            class="template-download"
            :href="templateUrl"
            download="学生批量导入模板.xlsx"
          >
            <el-icon><Download /></el-icon>
            下载模板
          </a>
        </div>

        <div class="import-instructions">
          <h4>学生用户导入说明</h4>
          <ol>
            <li>请先下载学生用户 Excel 模板文件</li>
            <li>按照模板格式填写用户信息，不要修改表头</li>
            <li>必填字段：学号、姓名、身份证号码</li>
            <li class="warn">每次最多导入 1000 条数据</li>
            <li>信用分不填时使用系统默认分（61）</li>
            <li>默认密码为身份证号码后六位，登录后请及时修改</li>
            <li>身份证号码只用于生成初始密码，不会保存到系统</li>
            <li>文件大小不超过 10MB，仅支持 .xlsx 格式</li>
          </ol>
        </div>

        <div class="import-footer">
          <button class="import-btn secondary" type="button" @click="importDialog = false">取消</button>
          <button class="import-btn primary" type="button" @click="importStep = 2">下一步</button>
        </div>
      </div>

      <div v-else-if="importStep === 2" class="import-pane">
        <input
          ref="importInput"
          class="import-file-input"
          type="file"
          accept=".xlsx"
          @change="handleFileChange"
        />
        <div
          class="upload-zone"
          :class="{ filled: importFile }"
          role="button"
          tabindex="0"
          @click="chooseFile"
          @keydown.enter="chooseFile"
          @keydown.space.prevent="chooseFile"
          @dragover.prevent
          @drop.prevent="handleFileDrop"
        >
          <div class="upload-icon"><el-icon><UploadFilled /></el-icon></div>
          <template v-if="importFile">
            <strong>{{ importFile.name }}</strong>
            <span>{{ formatFileSize(importFile.size) }} · 点击可重新选择</span>
          </template>
          <template v-else>
            <strong>点击选择或拖拽 Excel 文件到这里</strong>
            <span>仅支持 .xlsx 格式，单个文件不超过 10MB</span>
          </template>
        </div>

        <div v-if="importFile" class="selected-file">
          <div>
            <el-icon><Document /></el-icon>
            <div>
              <strong>{{ importFile.name }}</strong>
              <span>{{ formatFileSize(importFile.size) }}</span>
            </div>
          </div>
          <button type="button" :disabled="importing" @click.stop="clearImportFile">移除</button>
        </div>

        <div class="import-footer">
          <button class="import-btn secondary" type="button" :disabled="importing" @click="importStep = 1">上一步</button>
          <button
            class="import-btn primary"
            type="button"
            :disabled="!importFile || importing"
            @click="startImport"
          >
            {{ importing ? '导入中…' : '开始导入' }}
          </button>
        </div>
      </div>

      <div v-else class="import-pane">
        <div class="import-result-head" :class="{ failed: !importResult?.success }">
          <el-icon v-if="importResult?.success"><CircleCheckFilled /></el-icon>
          <el-icon v-else><WarningFilled /></el-icon>
          <div>
            <strong>{{ importResult?.success ? '导入完成' : '没有成功导入的用户' }}</strong>
            <span>
              共处理 {{ importResult?.total || 0 }} 条数据，成功 {{ importResult?.success || 0 }} 条，失败
              {{ importResult?.failed || 0 }} 条
            </span>
          </div>
        </div>

        <div class="import-result-stats">
          <div><strong>{{ importResult?.total || 0 }}</strong><span>总行数</span></div>
          <div class="success"><strong>{{ importResult?.success || 0 }}</strong><span>导入成功</span></div>
          <div class="failed"><strong>{{ importResult?.failed || 0 }}</strong><span>导入失败</span></div>
        </div>

        <div v-if="importResult?.errors?.length" class="import-errors">
          <div class="import-errors-head">
            <strong>失败明细</strong>
            <span v-if="importResult.errors.length > 50">仅展示前 50 条</span>
          </div>
          <div v-for="item in importResult.errors.slice(0, 50)" :key="`${item.row}-${item.studentNo}`" class="import-error-row">
            <span class="error-row">第 {{ item.row }} 行</span>
            <b>{{ item.studentNo || '—' }}</b>
            <span>{{ item.message }}</span>
          </div>
          <p v-if="importResult.errors.length > 50">
            还有 {{ importResult.errors.length - 50 }} 条失败记录未展示，请修正后分批重新导入。
          </p>
        </div>

        <div class="import-footer">
          <button class="import-btn secondary" type="button" @click="importDialog = false">完成</button>
          <button class="import-btn primary" type="button" @click="restartImport">继续导入</button>
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="drawerVisible"
      class="user-drawer"
      size="500px"
      :with-header="false"
      append-to-body
      :destroy-on-close="true"
    >
      <div v-if="detailLoading" class="drawer-loading">
        <div class="skeleton skeleton-drawer-head"></div>
        <div class="skeleton skeleton-drawer-block"></div>
        <div class="skeleton skeleton-drawer-block"></div>
      </div>

      <div v-else-if="detail" class="drawer-content">
        <div class="drawer-head">
          <div>
            <p class="drawer-eyebrow">USER PROFILE</p>
            <h2>{{ detail.displayName || '未命名用户' }}</h2>
            <span class="drawer-username">@{{ detail.username }}</span>
          </div>
          <button class="drawer-close" type="button" aria-label="关闭" @click="drawerVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="profile-card">
          <div class="profile-avatar" :class="avatarTone(detail.id)">
            {{ initial(detail.displayName || detail.username) }}
          </div>
          <div class="profile-copy">
            <strong>{{ detail.displayName || '未命名用户' }}</strong>
            <span>用户 ID #{{ detail.id }}</span>
            <div class="profile-status">
              <span class="status-tag" :class="statusClass(detail)">{{ statusLabel(detail) }}</span>
              <span class="role-note">小程序用户</span>
            </div>
          </div>
        </div>

        <div class="profile-meta">
          <div>
            <el-icon><Phone /></el-icon>
            <span>手机号</span>
            <strong>{{ detail.phone || '未绑定' }}</strong>
          </div>
          <div>
            <el-icon><Calendar /></el-icon>
            <span>注册时间</span>
            <strong>{{ formatDateTime(detail.createdAt) }}</strong>
          </div>
        </div>

        <div
          v-if="detail.college || detail.major || detail.grade || detail.className || detail.dormitory"
          class="academic-card"
        >
          <div><span>学院</span><strong>{{ detail.college || '—' }}</strong></div>
          <div><span>专业</span><strong>{{ detail.major || '—' }}</strong></div>
          <div><span>年级</span><strong>{{ detail.grade || '—' }}</strong></div>
          <div><span>班级</span><strong>{{ detail.className || '—' }}</strong></div>
          <div><span>宿舍</span><strong>{{ detail.dormitory || '—' }}</strong></div>
        </div>

        <div class="detail-stats">
          <div>
            <strong>{{ detail.publishedOrders }}</strong>
            <span>发布订单</span>
          </div>
          <div>
            <strong>{{ detail.acceptedOrders }}</strong>
            <span>接单次数</span>
          </div>
          <div>
            <strong>{{ detail.openOrders }}</strong>
            <span>进行中</span>
          </div>
        </div>

        <section class="detail-section">
          <div class="section-head">
            <div>
              <p class="section-eyebrow">CREDIT</p>
              <h3>信用记录</h3>
            </div>
            <div class="credit-score">
              <strong>{{ detail.creditScore }}</strong>
              <span>当前信用分</span>
            </div>
          </div>

          <div v-if="detail.creditRecords?.length" class="credit-list">
            <div v-for="record in detail.creditRecords" :key="record.id" class="credit-item">
              <span class="credit-change" :class="record.change > 0 ? 'positive' : 'negative'">
                {{ record.change > 0 ? '+' : '' }}{{ record.change }}
              </span>
              <div>
                <strong>{{ record.reason }}</strong>
                <span>{{ formatDateTime(record.at) }}</span>
              </div>
            </div>
          </div>
          <div v-else class="small-empty">暂无信用变动，当前为基础分 {{ detail.creditBase }}。</div>
        </section>

        <section class="detail-section">
          <div class="section-head">
            <div>
              <p class="section-eyebrow">RECENT ORDERS</p>
              <h3>最近订单</h3>
            </div>
            <span class="section-note">最近 5 条</span>
          </div>

          <div v-if="detail.recentOrders?.length" class="order-list">
            <div v-for="order in detail.recentOrders" :key="order.id" class="order-item">
              <div class="order-main">
                <div class="order-title">
                  <span class="type-tag">{{ order.role === 'runner' ? '接单' : '发布' }}</span>
                  <strong>{{ order.title }}</strong>
                </div>
                <div class="order-sub">
                  <span>#{{ order.id }} · {{ typeLabel(order.type) }}</span>
                  <span>{{ formatDateTime(order.createdAt) }}</span>
                </div>
              </div>
              <div class="order-side">
                <span class="status-tag" :class="orderStatusClass(order.status)">{{ order.status }}</span>
                <strong>¥{{ formatMoney(order.reward) }}</strong>
              </div>
            </div>
          </div>
          <div v-else class="small-empty">暂无订单记录。</div>
        </section>

        <div class="drawer-actions">
          <button class="secondary-button" type="button" @click="drawerVisible = false">关闭</button>
          <button
            v-if="!detail.closed"
            class="primary-button"
            :class="{ danger: detail.active }"
            type="button"
            :disabled="actionLoading === detail.id"
            @click="confirmToggle(detail)"
          >
            {{ actionLoading === detail.id ? '处理中…' : detail.active ? '停用账号' : '启用账号' }}
          </button>
          <span v-else class="drawer-closed-note">该账号已自主注销，不可后台启用</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  CircleCheckFilled,
  Close,
  Document,
  Download,
  Phone,
  Search,
  Shop,
  UploadFilled,
  User,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  fetchAdminUserDetail,
  fetchAdminUsers,
  importAdminUsers,
  updateAdminUserStatus
} from '../api/users'
import { errorMessage } from '../api/http'

const route = useRoute()
const router = useRouter()

const size = 10
const page = ref(1)
const total = ref(0)
const rows = ref([])
const keyword = ref('')
const status = ref(normalizeStatus(route.query.status))
const loading = ref(true)
const error = ref('')
const actionLoading = ref(null)
const drawerVisible = ref(false)
const detail = ref(null)
const detailLoading = ref(false)
const importDialog = ref(false)
const importStep = ref(1)
const importFile = ref(null)
const importInput = ref(null)
const importing = ref(false)
const importResult = ref(null)
const importSteps = ['下载模板', '上传文件', '导入结果']
const templateUrl = `${import.meta.env.BASE_URL}templates/${encodeURIComponent('学生批量导入模板.xlsx')}`

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '正常', value: 'active' },
  { label: '已停用', value: 'disabled' }
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchAdminUsers({
      page: page.value,
      size,
      keyword: keyword.value.trim(),
      status: status.value
    })
    rows.value = result.items
    total.value = result.total
    if (!rows.value.length && total.value > 0 && page.value > 1) {
      page.value = totalPages.value
      await load()
    }
  } catch (requestError) {
    error.value = errorMessage(requestError, '用户列表加载失败')
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
  router.replace({ path: '/users', query: value === 'all' ? {} : { status: value } })
  load()
}

function resetFilters() {
  keyword.value = ''
  status.value = 'all'
  page.value = 1
  router.replace({ path: '/users' })
  load()
}

function changePage(value) {
  if (value < 1 || value > totalPages.value || value === page.value) return
  page.value = value
  load()
}

async function openDetail(user) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminUserDetail(user.id)
  } catch (requestError) {
    drawerVisible.value = false
    ElMessage.error(errorMessage(requestError, '用户详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function confirmToggle(user) {
  if (!user || user.closed) return
  const next = !user.active
  const action = next ? '启用' : '停用'
  let content = next
    ? `确认启用「${user.displayName || user.username}」？启用后该账号可以重新登录小程序。`
    : `确认停用「${user.displayName || user.username}」？停用后该用户当前登录会立即失效。`
  if (!next && user.openOrders > 0) {
    content += ` 该用户还有 ${user.openOrders} 个进行中订单，历史订单会保留。`
  }

  try {
    await ElMessageBox.confirm(content, `${action}用户`, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  actionLoading.value = user.id
  try {
    await updateAdminUserStatus(user.id, next)
    ElMessage.success(`已${action}「${user.displayName || user.username}」`)
    await load()
    if (drawerVisible.value && detail.value?.id === user.id) {
      detail.value = await fetchAdminUserDetail(user.id)
    }
  } catch (requestError) {
    ElMessage.error(errorMessage(requestError, `${action}失败`))
  } finally {
    actionLoading.value = null
  }
}

function openImport() {
  resetImport()
  importDialog.value = true
}

function resetImport() {
  importStep.value = 1
  importFile.value = null
  importResult.value = null
  importing.value = false
  if (importInput.value) importInput.value.value = ''
}

function chooseFile() {
  if (importing.value) return
  importInput.value?.click()
}

function handleFileChange(event) {
  acceptImportFile(event.target.files?.[0])
  event.target.value = ''
}

function handleFileDrop(event) {
  if (importing.value) return
  acceptImportFile(event.dataTransfer?.files?.[0])
}

function acceptImportFile(file) {
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.warning('仅支持 .xlsx 格式，请下载模板填写后上传')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('Excel 文件不能超过 10MB')
    return
  }
  importFile.value = file
  importResult.value = null
}

function clearImportFile() {
  if (importing.value) return
  importFile.value = null
  if (importInput.value) importInput.value.value = ''
}

async function startImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的 Excel 文件')
    return
  }
  importing.value = true
  try {
    const result = await importAdminUsers(importFile.value)
    importResult.value = result
    importStep.value = 3
    if (result.success > 0) await load()
    if (result.failed > 0) {
      ElMessage.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    } else {
      ElMessage.success(`成功导入 ${result.success} 条用户`)
    }
  } catch (requestError) {
    ElMessage.error(errorMessage(requestError, '用户导入失败'))
  } finally {
    importing.value = false
  }
}

function restartImport() {
  importFile.value = null
  importResult.value = null
  importStep.value = 2
}

function formatFileSize(bytes) {
  const size = Number(bytes || 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

watch(
  () => route.query.status,
  (value) => {
    const next = normalizeStatus(value)
    if (next === status.value) return
    status.value = next
    page.value = 1
    load()
  }
)

onMounted(load)

function normalizeStatus(value) {
  return value === 'active' || value === 'disabled' ? value : 'all'
}

function initial(value) {
  return String(value || '用').slice(0, 1).toUpperCase()
}

function avatarTone(id) {
  return `tone-${Number(id || 0) % 5}`
}

function maskPhone(phone) {
  if (!phone) return '未绑定'
  return String(phone).replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

function statusLabel(user) {
  if (user.closed) return '已注销'
  return user.active ? '正常' : '已停用'
}

function statusClass(user) {
  if (user.closed) return 'status-closed'
  return user.active ? 'status-active' : 'status-disabled'
}

function orderStatusClass(value) {
  return {
    'status-pending': value === '待接单',
    'status-accepted': value === '已接单',
    'status-confirming': value === '待确认',
    'status-completed': value === '已完成',
    'status-cancelled': value === '已取消'
  }
}

function typeLabel(type) {
  return { parcel: '取件', meal: '取餐' }[type] || type || '其他'
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
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
.users-page {
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
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 13px 14px;
  border: 1px solid #e7edf5;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(28, 57, 92, 0.025);
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

.status-tabs {
  display: flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid #e7eef7;
  border-radius: 9px;
  background: #f8fafd;
}

.status-tabs button {
  height: 28px;
  padding: 0 12px;
  cursor: pointer;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #8a9bb0;
  font-size: 10px;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.status-tabs button:hover {
  color: #3c6da8;
}

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

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th {
  padding: 12px 9px 9px;
  color: #a0adbd;
  font-size: 8.5px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-align: left;
  white-space: nowrap;
}

.user-table td {
  padding: 12px 9px;
  border-top: 1px solid #f0f4f9;
  color: #6f8198;
  font-size: 10px;
  vertical-align: middle;
}

.user-table tbody tr {
  transition: background 0.15s ease;
}

.user-table tbody tr:hover {
  background: #fbfdff;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 160px;
}

.user-avatar {
  display: grid;
  width: 31px;
  height: 31px;
  flex: 0 0 31px;
  place-items: center;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 650;
}

.user-avatar.tone-0 {
  background: #edf4ff;
  color: #3978d8;
}

.user-avatar.tone-1 {
  background: #eafaf6;
  color: #1a967b;
}

.user-avatar.tone-2 {
  background: #f2efff;
  color: #7460c9;
}

.user-avatar.tone-3 {
  background: #fff6e8;
  color: #b8842a;
}

.user-avatar.tone-4 {
  background: #fff0f0;
  color: #bd6464;
}

.user-identity {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.user-identity strong {
  overflow: hidden;
  color: #304862;
  font-size: 10.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-identity span {
  color: #9aaabe;
  font-size: 9px;
}

.phone {
  color: #526b87;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.phone.muted {
  color: #a9b6c5;
}

.order-count {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 86px;
}

.order-count strong {
  color: #405a78;
  font-size: 11px;
  font-weight: 600;
}

.order-count span {
  color: #9aaabe;
  font-size: 8.5px;
  white-space: nowrap;
}

.order-count b {
  display: inline-block;
  margin-left: 3px;
  padding: 2px 4px;
  border-radius: 3px;
  background: #fff5e7;
  color: #b47d22;
  font-size: 8px;
  font-weight: 500;
}

.credit-badge {
  display: inline-flex;
  min-width: 25px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background: #f2f7ff;
  color: #4b79b4;
  font-size: 10px;
  font-weight: 600;
}

.credit-note {
  margin-left: 5px;
  color: #a5b2c1;
  font-size: 8px;
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

.status-active {
  background: #e9f8f2;
  color: #1b9679;
}

.status-disabled {
  background: #f4f6f9;
  color: #8c9cac;
}

.status-closed {
  background: #f7f3f3;
  color: #a88b8b;
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

.time {
  color: #98a7b8 !important;
  font-size: 9px !important;
  white-space: nowrap;
}

.align-right {
  text-align: right !important;
}

.row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  white-space: nowrap;
}

.row-actions button {
  height: 25px;
  padding: 0 8px;
  cursor: pointer;
  border: 1px solid #e3ebf5;
  border-radius: 6px;
  background: #fff;
  color: #5f7d9e;
  font-size: 9px;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.row-actions button:hover:not(:disabled) {
  border-color: #bcd4f1;
  background: #f8fbff;
  color: #2f7df6;
}

.row-actions button.danger {
  color: #b46f6f;
}

.row-actions button.danger:hover:not(:disabled) {
  border-color: #eccaca;
  background: #fffafa;
  color: #b65353;
}

.row-actions button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.closed-note {
  color: #b3a3a3;
  font-size: 8.5px;
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

.table-skeleton {
  padding-top: 12px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 13px 9px;
  border-bottom: 1px solid #f4f7fb;
}

.skeleton {
  border-radius: 5px;
  background: linear-gradient(90deg, #eef3f9 25%, #f8fbfe 37%, #eef3f9 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-avatar {
  width: 31px;
  height: 31px;
  flex: 0 0 31px;
}

.skeleton-name {
  width: 120px;
  height: 10px;
}

.skeleton-mid {
  width: 84px;
  height: 9px;
}

.skeleton-short {
  width: 54px;
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

:global(.user-drawer .el-drawer__body) {
  padding: 0;
  background: #f7f9fd;
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
  margin-bottom: 20px;
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
  font-size: 23px;
  font-weight: 650;
  letter-spacing: -0.3px;
}

.drawer-username {
  display: block;
  margin-top: 4px;
  color: #9aaabe;
  font-size: 10px;
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

.profile-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 16px;
  border: 1px solid #e3ecf6;
  border-radius: 13px;
  background: #fff;
}

.profile-avatar {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  border-radius: 13px;
  background: #edf4ff;
  color: #3978d8;
  font-size: 18px;
  font-weight: 650;
}

.profile-avatar.tone-1 {
  background: #eafaf6;
  color: #1a967b;
}

.profile-avatar.tone-2 {
  background: #f2efff;
  color: #7460c9;
}

.profile-avatar.tone-3 {
  background: #fff6e8;
  color: #b8842a;
}

.profile-avatar.tone-4 {
  background: #fff0f0;
  color: #bd6464;
}

.profile-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.profile-copy strong {
  color: #2b425d;
  font-size: 13px;
  font-weight: 600;
}

.profile-copy > span {
  color: #9aaabe;
  font-size: 9.5px;
}

.profile-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}

.role-note {
  color: #a7b4c3;
  font-size: 8.5px;
}

.profile-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #e3ecf6;
  border-radius: 12px;
  background: #e3ecf6;
}

.profile-meta > div {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  padding: 12px 13px;
  background: #fff;
}

.profile-meta .el-icon {
  color: #8ba3bd;
  font-size: 13px;
}

.profile-meta span {
  color: #9aaabe;
  font-size: 9px;
}

.profile-meta strong {
  width: 100%;
  margin-left: 20px;
  color: #455e79;
  font-size: 10px;
  font-weight: 500;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.detail-stats > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e3ecf6;
  border-radius: 10px;
  background: #fff;
}

.detail-stats strong {
  color: #2d4a68;
  font-size: 19px;
  font-weight: 650;
  line-height: 1;
}

.detail-stats span {
  color: #9aaabe;
  font-size: 8.5px;
}

.detail-section {
  margin-top: 18px;
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

.credit-score {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.credit-score strong {
  color: #1b9679;
  font-size: 24px;
  font-weight: 650;
  line-height: 1;
}

.credit-score span {
  color: #9aaabe;
  font-size: 8px;
}

.credit-list {
  display: flex;
  flex-direction: column;
}

.credit-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5fa;
}

.credit-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.credit-change {
  display: grid;
  min-width: 30px;
  height: 20px;
  place-items: center;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 600;
}

.credit-change.positive {
  background: #e9f8f2;
  color: #1b9679;
}

.credit-change.negative {
  background: #fff1f1;
  color: #bc6767;
}

.credit-item > div,
.order-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.credit-item strong {
  color: #50657c;
  font-size: 9.5px;
  font-weight: 500;
}

.credit-item span {
  color: #a3b0c0;
  font-size: 8.5px;
}

.order-list {
  display: flex;
  flex-direction: column;
}

.order-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid #f1f5fa;
}

.order-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.order-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.order-title strong {
  overflow: hidden;
  color: #40566f;
  font-size: 9.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-tag {
  flex: 0 0 auto;
  padding: 2px 5px;
  border-radius: 3px;
  background: #f0f5fc;
  color: #5a7da3;
  font-size: 7.5px;
}

.order-sub {
  display: flex;
  gap: 8px;
  color: #a0adbd;
  font-size: 8px;
}

.order-side {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 4px;
  flex: 0 0 auto;
}

.order-side strong {
  color: #4d6682;
  font-size: 9px;
  font-weight: 600;
}

.small-empty {
  padding: 18px 0 4px;
  color: #a3b0c0;
  font-size: 9.5px;
  text-align: center;
}

.drawer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #e6edf6;
}

.drawer-actions button {
  height: 34px;
  padding: 0 16px;
  cursor: pointer;
  border-radius: 8px;
  font-size: 10px;
}

.secondary-button {
  border: 1px solid #dfe8f3;
  background: #fff;
  color: #71859c;
}

.primary-button {
  border: 1px solid #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.primary-button.danger {
  border-color: #d47777;
  background: #d47777;
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.drawer-closed-note {
  color: #b0a0a0;
  font-size: 9px;
}


.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 5px;
}

.import-open-btn {
  display: inline-flex;
  height: 31px;
  align-items: center;
  gap: 6px;
  padding: 0 13px;
  cursor: pointer;
  border: 1px solid #2f7df6;
  border-radius: 8px;
  background: #2f7df6;
  color: #fff;
  font-size: 10px;
  font-weight: 500;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.import-open-btn:hover {
  background: #286fe0;
  box-shadow: 0 4px 12px rgba(47, 125, 246, 0.18);
}

.import-open-btn .el-icon {
  font-size: 13px;
}

.academic-card {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #e3ecf6;
  border-radius: 12px;
  background: #e3ecf6;
}

.academic-card > div {
  display: flex;
  min-height: 52px;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: #fff;
}

.academic-card span {
  color: #9aaabe;
  font-size: 8.5px;
}

.academic-card strong {
  overflow: hidden;
  color: #455e79;
  font-size: 10px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.import-dialog) {
  border-radius: 14px;
}

:global(.import-dialog .el-dialog__header) {
  padding: 22px 26px 16px;
  border-bottom: 1px solid #eef3f9;
}

:global(.import-dialog .el-dialog__title) {
  color: #1e3550;
  font-size: 17px;
  font-weight: 650;
}

:global(.import-dialog .el-dialog__body) {
  padding: 20px 26px 24px;
}

.import-type-box {
  padding: 15px 17px;
  border: 1px solid #e8f0fa;
  border-radius: 12px;
  background: #f6f9fe;
}

.import-type-box > p {
  margin: 0 0 11px;
  color: #3d5671;
  font-size: 12px;
  font-weight: 600;
}

.import-type-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.import-type {
  display: inline-flex;
  height: 38px;
  align-items: center;
  gap: 7px;
  padding: 0 18px;
  cursor: pointer;
  border: 1px solid #d4e1f1;
  border-radius: 5px;
  background: #fff;
  color: #4c6683;
  font-size: 12px;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.import-type .el-icon {
  font-size: 15px;
}

.import-type.active {
  border-color: #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.import-type:disabled {
  cursor: not-allowed;
  border-color: #e1e8f2;
  background: #fbfcfe;
  color: #b2bfcd;
}

.import-type em {
  padding: 2px 4px;
  border-radius: 3px;
  background: #f1f4f8;
  color: #a4b2c2;
  font-size: 8px;
  font-style: normal;
}

.import-steps {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  margin: 26px 14px 24px;
}

.import-step {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.import-step .step-index {
  display: grid;
  width: 26px;
  height: 26px;
  z-index: 1;
  place-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
  background: #fff;
  color: #9aaabe;
  font-size: 11px;
}

.import-step .step-index .el-icon {
  font-size: 15px;
}

.import-step strong {
  color: #9aaabe;
  font-size: 11px;
  font-weight: 500;
}

.import-step.active .step-index {
  border-color: #2f7df6;
  color: #2f7df6;
}

.import-step.active strong {
  color: #2f4f74;
  font-weight: 600;
}

.import-step.done .step-index {
  border-color: #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.import-step.done strong {
  color: #4c6c90;
}

.import-step i {
  position: absolute;
  top: 13px;
  left: calc(50% + 18px);
  width: calc(100% - 36px);
  height: 1px;
  background: #e0e7f0;
}

.import-step.done i {
  background: #c9dcf5;
}

.import-pane {
  min-height: 300px;
}

.import-template-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e5edf7;
  border-radius: 10px;
  background: #f8fafd;
}

.template-icon {
  display: grid;
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  place-items: center;
  border-radius: 10px;
  background: #eaf3ff;
  color: #2f7df6;
  font-size: 28px;
}

.template-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.template-copy strong {
  color: #2c4059;
  font-size: 13px;
  font-weight: 650;
}

.template-copy span {
  color: #8a9cb0;
  font-size: 10px;
}

.template-copy small {
  overflow: hidden;
  color: #a0adbd;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-download {
  display: inline-flex;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  border-radius: 7px;
  background: #2f7df6;
  color: #fff;
  font-size: 10px;
  text-decoration: none;
  transition: background 0.2s ease;
}

.template-download:hover {
  background: #286fe0;
}

.import-instructions {
  margin-top: 14px;
  padding: 15px 18px;
  border: 1px solid #eef2f8;
  border-radius: 10px;
  background: #fbfcfe;
}

.import-instructions h4 {
  margin: 0 0 8px;
  color: #536a83;
  font-size: 11px;
  font-weight: 600;
}

.import-instructions ol {
  display: flex;
  margin: 0;
  padding: 0;
  flex-direction: column;
  gap: 4px;
  list-style: none;
  counter-reset: import-note;
}

.import-instructions li {
  position: relative;
  padding-left: 18px;
  color: #8a9bb0;
  font-size: 10px;
  line-height: 1.65;
  counter-increment: import-note;
}

.import-instructions li::before {
  position: absolute;
  left: 0;
  color: #a5b4c5;
  content: counter(import-note) '.';
}

.import-instructions li.warn {
  color: #c78a2f;
  font-weight: 600;
}

.import-file-input {
  display: none;
}

.upload-zone {
  display: flex;
  min-height: 170px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 7px;
  padding: 24px;
  cursor: pointer;
  border: 1px dashed #bfd4ec;
  border-radius: 12px;
  background: #f8fbff;
  text-align: center;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.upload-zone:hover,
.upload-zone:focus-visible {
  outline: none;
  border-color: #2f7df6;
  background: #f3f8ff;
}

.upload-zone.filled {
  min-height: 130px;
  border-style: solid;
}

.upload-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 50%;
  background: #eaf3ff;
  color: #2f7df6;
  font-size: 22px;
}

.upload-zone strong {
  color: #405a78;
  font-size: 12px;
  font-weight: 600;
}

.upload-zone span {
  color: #9aaabe;
  font-size: 9.5px;
}

.selected-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #e4edf8;
  border-radius: 8px;
  background: #fff;
}

.selected-file > div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.selected-file .el-icon {
  flex: 0 0 auto;
  color: #2f7df6;
  font-size: 17px;
}

.selected-file > div > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.selected-file strong {
  overflow: hidden;
  color: #4e6680;
  font-size: 10px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-file span {
  color: #9aaabe;
  font-size: 8.5px;
}

.selected-file button {
  flex: 0 0 auto;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #b46f6f;
  font-size: 9px;
}

.selected-file button:disabled {
  cursor: wait;
  opacity: 0.5;
}

.import-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #eef3f9;
}

.import-btn {
  height: 36px;
  min-width: 98px;
  padding: 0 20px;
  cursor: pointer;
  border-radius: 7px;
  font-size: 11px;
  transition: background 0.2s ease, opacity 0.2s ease;
}

.import-btn.secondary {
  border: 1px solid #dfe8f3;
  background: #fff;
  color: #71859c;
}

.import-btn.primary {
  border: 1px solid #2f7df6;
  background: #2f7df6;
  color: #fff;
}

.import-btn.primary:hover:not(:disabled) {
  background: #286fe0;
}

.import-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.import-result-head {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 18px;
  border: 1px solid #d8efe7;
  border-radius: 11px;
  background: #f3fbf8;
}

.import-result-head > .el-icon {
  flex: 0 0 auto;
  color: #1b9679;
  font-size: 30px;
}

.import-result-head.failed {
  border-color: #f1dfc9;
  background: #fffaf3;
}

.import-result-head.failed > .el-icon {
  color: #c78a2f;
}

.import-result-head > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.import-result-head strong {
  color: #2d4a68;
  font-size: 14px;
  font-weight: 650;
}

.import-result-head span {
  color: #8a9bb0;
  font-size: 10px;
}

.import-result-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.import-result-stats > div {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 12px 14px;
  border: 1px solid #e8eef6;
  border-radius: 9px;
  background: #fff;
}

.import-result-stats strong {
  color: #3a5573;
  font-size: 22px;
  font-weight: 650;
  line-height: 1;
}

.import-result-stats span {
  color: #9aaabe;
  font-size: 9px;
}

.import-result-stats .success strong {
  color: #1b9679;
}

.import-result-stats .failed strong {
  color: #bf6f5c;
}

.import-errors {
  max-height: 230px;
  margin-top: 14px;
  overflow-y: auto;
  padding: 12px 14px;
  border: 1px solid #eef2f7;
  border-radius: 9px;
  background: #fbfcfe;
}

.import-errors-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.import-errors-head strong {
  color: #5b7087;
  font-size: 10px;
  font-weight: 600;
}

.import-errors-head span {
  color: #a3b0c0;
  font-size: 8.5px;
}

.import-error-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 6px 0;
  border-top: 1px solid #f0f4f9;
  color: #8a9bb0;
  font-size: 9.5px;
  line-height: 1.6;
}

.import-error-row b {
  flex: 0 0 auto;
  color: #5b7087;
  font-weight: 500;
}

.error-row {
  flex: 0 0 45px;
  color: #b46f6f;
  font-size: 9px;
}

.import-errors > p {
  margin: 8px 0 0;
  color: #a3b0c0;
  font-size: 9px;
}

@media (max-width: 1100px) {
  .filter-bar {
    align-items: stretch;
  }

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

  .head-actions {
    margin-top: 0;
  }

  .total-badge {
    margin-top: 0;
  }

  .import-type-options {
    flex-direction: column;
  }

  .import-type {
    justify-content: center;
  }

  .import-template-card {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .template-copy {
    flex: 1 1 220px;
  }

  .template-download {
    margin-left: 70px;
  }

  .academic-card {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    gap: 9px;
  }

  .status-tabs {
    width: 100%;
  }

  .status-tabs button {
    flex: 1;
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

  .profile-meta {
    grid-template-columns: 1fr;
  }

  .drawer-content {
    padding: 20px;
  }
}
</style>
