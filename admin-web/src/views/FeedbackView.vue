<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">USER VOICE</p>
        <h1 class="adm-title">用户反馈</h1>
        <p class="adm-sub">查看小程序用户提交的问题与建议，记录处理结果和回复内容。</p>
      </div>
      <span class="adm-count">共 {{ total }} 条反馈</span>
    </div>

    <section class="adm-panel">
      <div class="adm-panel-head">
        <div>
          <p class="adm-panel-eyebrow">FEEDBACK INBOX</p>
          <h2 class="adm-panel-title">反馈收件箱</h2>
        </div>
        <span class="adm-panel-meta">第 {{ page }} / {{ totalPages }} 页</span>
      </div>

      <div class="adm-filter">
        <label class="adm-search">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" placeholder="搜索反馈内容或用户" maxlength="64" @keyup.enter="applyFilters" />
        </label>
        <select v-model="status" class="adm-select" @change="applyFilters">
          <option value="all">全部状态</option>
          <option value="待处理">待处理</option>
          <option value="已处理">已处理</option>
        </select>
        <button class="adm-btn adm-btn-primary" type="button" :disabled="loading" @click="applyFilters">搜索</button>
        <button class="adm-btn" type="button" @click="resetFilters">重置</button>
      </div>

      <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span><button class="adm-btn adm-btn-sm" type="button" @click="load">重试</button></div>
      <div v-if="loading" class="fb-loading">正在读取反馈…</div>
      <template v-else>
        <div v-if="rows.length" class="adm-table-wrap">
          <table class="adm-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>反馈内容</th>
                <th>状态</th>
                <th>提交时间</th>
                <th>处理人</th>
                <th class="adm-align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in rows" :key="item.id">
                <td>
                  <div class="fb-user">
                    <span class="fb-avatar">{{ (item.userName || item.account || '用').slice(0, 1) }}</span>
                    <div><strong>{{ item.userName || item.account || '用户' }}</strong><small>@{{ item.account }}</small></div>
                  </div>
                </td>
                <td><p class="fb-preview">{{ item.content }}</p></td>
                <td><span class="adm-tag" :class="item.status === '已处理' ? 'adm-tag-green' : 'adm-tag-amber'">{{ item.status }}</span></td>
                <td class="adm-muted">{{ formatDateTime(item.createdAt) }}</td>
                <td class="adm-muted">{{ item.handledByName || '—' }}</td>
                <td class="adm-align-right"><button class="adm-btn adm-btn-sm" type="button" @click="open(item)">查看 / 回复</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="adm-empty">
          <el-icon><ChatDotRound /></el-icon>
          <strong>暂无反馈</strong>
          <span>小程序「我的 - 问题反馈」提交后会出现在这里</span>
        </div>

        <div v-if="total > 0" class="adm-pagination">
          <span class="adm-pagination-meta">共 {{ total }} 条反馈 · 第 {{ page }} / {{ totalPages }} 页</span>
          <div class="adm-page-buttons">
            <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">‹</button>
            <button v-for="item in visiblePages" :key="item" type="button" :class="{ active: item === page }" @click="changePage(item)">{{ item }}</button>
            <button type="button" :disabled="page >= totalPages" @click="changePage(page + 1)">›</button>
          </div>
        </div>
      </template>
    </section>

    <p class="adm-foot">处理状态只用于后台跟踪，不会自动推送给用户；当前小程序端没有反馈回复展示入口。</p>

    <el-drawer v-model="drawer" size="520px" :with-header="false" append-to-body destroy-on-close>
      <div v-if="detail" class="fb-drawer">
        <div class="adm-drawer-head">
          <div>
            <p class="adm-eyebrow">FEEDBACK #{{ detail.id }}</p>
            <h2>{{ detail.userName || detail.account || '用户反馈' }}</h2>
            <div class="adm-drawer-sub">
              <span class="adm-tag" :class="detail.status === '已处理' ? 'adm-tag-green' : 'adm-tag-amber'">{{ detail.status }}</span>
              <span>{{ formatDateTime(detail.createdAt) }}</span>
            </div>
          </div>
          <button class="adm-drawer-close" type="button" aria-label="关闭" @click="drawer = false"><el-icon><Close /></el-icon></button>
        </div>

        <div class="fb-block">
          <span>用户反馈</span>
          <p>{{ detail.content }}</p>
        </div>
        <div v-if="detail.handledAt" class="fb-handled">
          最近处理：{{ detail.handledByName || '管理员' }} · {{ formatDateTime(detail.handledAt) }}
        </div>

        <div class="fb-reply">
          <label>后台回复 / 处理备注</label>
          <el-input v-model="reply" type="textarea" :rows="7" maxlength="2000" show-word-limit placeholder="记录处理结果；当前不会直接发送给用户" />
        </div>
      </div>
      <div class="adm-drawer-actions">
        <button class="adm-btn" type="button" @click="drawer = false">关闭</button>
        <button class="adm-btn" type="button" :disabled="saving" @click="save('待处理')">标记待处理</button>
        <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="save('已处理')">{{ saving ? '保存中…' : '保存并标记已处理' }}</button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Close, Search, WarningFilled } from '@element-plus/icons-vue'
import { fetchAdminFeedback, fetchAdminFeedbackDetail, handleAdminFeedback } from '../api/feedback'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 15
const status = ref('all')
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const drawer = ref(false)
const detail = ref(null)
const reply = ref('')
const saving = ref(false)

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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchAdminFeedback({ page: page.value, size, status: status.value, keyword: keyword.value.trim() })
    rows.value = data.items || []
    total.value = data.total || 0
  } catch (err) {
    error.value = err.message || '反馈加载失败'
  } finally {
    loading.value = false
  }
}

async function open(item) {
  try {
    detail.value = await fetchAdminFeedbackDetail(item.id)
    reply.value = detail.value.reply || ''
    drawer.value = true
  } catch (err) {
    ElMessage.error(err.message || '反馈详情加载失败')
  }
}

async function save(nextStatus) {
  if (!detail.value) return
  saving.value = true
  try {
    detail.value = await handleAdminFeedback(detail.value.id, { status: nextStatus, reply: reply.value })
    ElMessage.success(nextStatus === '已处理' ? '反馈已标记为已处理' : '反馈已标记为待处理')
    await load()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
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
  status.value = 'all'
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
.fb-loading {
  padding: 42px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.fb-user {
  display: flex;
  align-items: center;
  gap: 9px;
}

.fb-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #eef5ff;
  color: #4a8cd5;
  font-size: 11px;
  font-weight: 600;
}

.fb-user div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fb-user strong {
  color: #2c425a;
  font-size: 11px;
}

.fb-user small {
  color: #a1afbe;
  font-size: 9px;
}

.fb-preview {
  display: -webkit-box;
  max-width: 360px;
  margin: 0;
  overflow: hidden;
  color: #5a6d83;
  font-size: 11px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.fb-drawer {
  display: flex;
  flex-direction: column;
  height: calc(100% - 58px);
  overflow-y: auto;
  padding-right: 2px;
}

.fb-block {
  margin-top: 18px;
  padding: 15px;
  border: 1px solid #edf2f8;
  border-radius: 10px;
  background: #fbfdff;
}

.fb-block span,
.fb-reply label {
  color: #8fa0b3;
  font-size: 10px;
}

.fb-block p {
  margin: 8px 0 0;
  color: #344b63;
  font-size: 12px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.fb-handled {
  margin-top: 10px;
  padding: 8px 11px;
  border-radius: 8px;
  background: #f5f9ff;
  color: #7890a8;
  font-size: 10px;
}

.fb-reply {
  margin-top: 18px;
}

.fb-reply label {
  display: block;
  margin-bottom: 7px;
}
</style>
