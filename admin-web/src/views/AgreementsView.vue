<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">LEGAL & POLICY</p>
        <h1 class="adm-title">协议管理</h1>
        <p class="adm-sub">维护平台服务协议、隐私政策等文本；版本号用于标记更新批次。</p>
      </div>
      <button class="adm-btn adm-btn-primary" type="button" @click="open()">新增协议</button>
    </div>

    <section class="adm-panel">
      <div class="adm-panel-head">
        <div>
          <p class="adm-panel-eyebrow">AGREEMENTS</p>
          <h2 class="adm-panel-title">协议文本</h2>
        </div>
        <span class="adm-panel-meta">{{ agreements.length }} 份</span>
      </div>

      <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span><button class="adm-btn adm-btn-sm" type="button" @click="load">重试</button></div>
      <div v-if="loading" class="ag-loading">正在读取协议…</div>
      <template v-else>
        <div v-if="agreements.length" class="adm-table-wrap">
          <table class="adm-table">
            <thead>
              <tr>
                <th>协议</th>
                <th>编码</th>
                <th>版本</th>
                <th>状态</th>
                <th>最近更新</th>
                <th class="adm-align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in agreements" :key="item.id">
                <td>
                  <div class="ag-title">
                    <strong>{{ item.title }}</strong>
                    <span>{{ preview(item.content) }}</span>
                  </div>
                </td>
                <td class="adm-muted">{{ item.code }}</td>
                <td class="adm-muted">{{ item.version }}</td>
                <td><span class="adm-tag" :class="item.isPublished ? 'adm-tag-green' : 'adm-tag-gray'">{{ item.isPublished ? '已发布' : '草稿' }}</span></td>
                <td class="adm-muted">{{ formatDateTime(item.updatedAt) }}</td>
                <td class="adm-align-right">
                  <button class="adm-btn adm-btn-sm" type="button" @click="open(item)">编辑</button>
                  <button class="adm-btn adm-btn-sm adm-btn-danger" type="button" @click="remove(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="adm-empty"><el-icon><Document /></el-icon><strong>暂无协议</strong><span>新增服务协议或隐私政策文本</span></div>
      </template>
    </section>

    <p class="adm-foot">协议内容目前保存在后台，供运营维护；小程序仍使用包内 PDF 协议，接入后台文本后才会自动同步。</p>

    <el-dialog v-model="dialog" :title="form.id ? '编辑协议' : '新增协议'" width="720px" append-to-body :close-on-click-modal="false">
      <el-form label-position="top">
        <div class="adm-form-grid">
          <el-form-item label="协议编码" required>
            <el-input v-model="form.code" maxlength="64" placeholder="service / privacy" />
          </el-form-item>
          <el-form-item label="版本号" required>
            <el-input v-model="form.version" maxlength="40" placeholder="2026.10" />
          </el-form-item>
        </div>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="120" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="13" maxlength="100000" show-word-limit />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-switch v-model="form.isPublished" active-text="已发布" inactive-text="草稿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="dialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, WarningFilled } from '@element-plus/icons-vue'
import {
  createAdminAgreement,
  deleteAdminAgreement,
  fetchAdminAgreements,
  updateAdminAgreement
} from '../api/agreements'

const agreements = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dialog = ref(false)
const form = reactive({ id: null, code: '', title: '', version: '1.0', content: '', isPublished: true })

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function preview(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text.length > 100 ? `${text.slice(0, 100)}…` : text
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    agreements.value = await fetchAdminAgreements()
  } catch (err) {
    error.value = err.message || '协议加载失败'
  } finally {
    loading.value = false
  }
}

function open(item) {
  Object.assign(form, item
    ? { id: item.id, code: item.code, title: item.title, version: item.version, content: item.content, isPublished: item.isPublished }
    : { id: null, code: '', title: '', version: '1.0', content: '', isPublished: true })
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    const { id, ...payload } = form
    if (id) await updateAdminAgreement(id, payload)
    else await createAdminAgreement(payload)
    ElMessage.success(id ? '协议已更新' : '协议已创建')
    dialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.message || '协议保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`确认删除协议「${item.title}」？`, '删除协议', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAdminAgreement(item.id)
    ElMessage.success('协议已删除')
    await load()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.ag-loading {
  padding: 42px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.ag-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 520px;
}

.ag-title strong {
  color: #263d55;
  font-size: 12px;
}

.ag-title span {
  overflow: hidden;
  color: #9aa9ba;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
