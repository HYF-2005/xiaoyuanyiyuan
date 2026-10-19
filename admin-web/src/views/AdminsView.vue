<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">ACCESS CONTROL</p>
        <h1 class="adm-title">管理员管理</h1>
        <p class="adm-sub">后台账号与小程序用户完全分离；仅超级管理员可以创建、停用、重置密码或配置菜单权限。</p>
      </div>
      <button v-if="isSuper" class="adm-btn adm-btn-primary" type="button" @click="openCreate">新增管理员</button>
    </div>

    <section class="adm-panel">
      <div class="adm-panel-head">
        <div>
          <p class="adm-panel-eyebrow">ADMIN ACCOUNTS</p>
          <h2 class="adm-panel-title">管理员账号</h2>
        </div>
        <span class="adm-panel-meta">{{ admins.length }} 个账号</span>
      </div>

      <div v-if="!isSuper" class="adm-error"><el-icon><Lock /></el-icon><span>当前账号不是超级管理员，无法查看管理员列表。</span></div>
      <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span><button class="adm-btn adm-btn-sm" type="button" @click="load">重试</button></div>
      <div v-if="loading" class="admin-loading">正在读取管理员…</div>
      <template v-else>
        <div v-if="admins.length" class="adm-table-wrap">
          <table class="adm-table">
            <thead>
              <tr>
                <th>管理员</th>
                <th>联系方式</th>
                <th>角色</th>
                <th>状态</th>
                <th>最近登录</th>
                <th>创建时间</th>
                <th class="adm-align-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in admins" :key="item.id">
                <td>
                  <div class="admin-person">
                    <span class="admin-avatar">{{ (item.displayName || item.username).slice(0, 1) }}</span>
                    <div>
                      <strong>{{ item.displayName }}<em v-if="item.id === auth.admin?.id">当前</em></strong>
                      <small>{{ item.username }}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="admin-contact">
                    <span>{{ item.phone || '未填手机号' }}</span>
                    <small>{{ item.email || '未填邮箱' }}</small>
                    <em v-if="item.remark" :title="item.remark">{{ item.remark }}</em>
                  </div>
                </td>
                <td>
                  <span class="adm-tag" :class="roleClass(item.role)">{{ roleLabel(item.role) }}</span>
                  <small class="permission-count">{{ item.role === 'super_admin' ? '全部权限' : `${item.permissions?.length || 0} 项权限` }}</small>
                </td>
                <td><span class="adm-tag" :class="item.active ? 'adm-tag-green' : 'adm-tag-gray'">{{ item.active ? '启用' : '已停用' }}</span></td>
                <td class="adm-muted">{{ formatDateTime(item.lastLoginAt) }}</td>
                <td class="adm-muted">{{ formatDateTime(item.createdAt) }}</td>
                <td class="adm-align-right">
                  <button class="adm-btn adm-btn-sm" type="button" :disabled="!isSuper" @click="openEdit(item)">编辑</button>
                  <button class="adm-btn adm-btn-sm" type="button" :disabled="!isSuper || item.role === 'super_admin'" :title="item.role === 'super_admin' ? '超级管理员默认拥有全部权限' : '编辑菜单权限'" @click="openPermissions(item)">权限</button>
                  <button class="adm-btn adm-btn-sm" type="button" :disabled="!isSuper" @click="openPassword(item)">重置密码</button>
                  <button class="adm-btn adm-btn-sm" :class="{ 'adm-btn-danger': item.active }" type="button" :disabled="!isSuper || item.id === auth.admin?.id" @click="toggleStatus(item)">{{ item.active ? '停用' : '启用' }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="adm-empty"><el-icon><Avatar /></el-icon><strong>暂无管理员</strong><span>超级管理员可以创建新的后台账号</span></div>
      </template>
    </section>

    <p class="adm-foot">停用管理员会立即撤销其全部后台会话；重置密码也会让该账号已有会话失效。超级管理员可以为其他管理员配置菜单权限，后端接口会同步按权限二次校验。</p>

    <el-dialog v-model="createDialog" title="添加管理员" width="560px" append-to-body :close-on-click-modal="false" class="admin-dialog">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" maxlength="64" placeholder="字母、数字、下划线、点或短横线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="72" placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="displayName">
          <el-input v-model="createForm.displayName" maxlength="64" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" maxlength="128" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" class="full-width" placeholder="请选择角色">
            <el-option v-for="item in roles" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="createForm.active">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="createDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="create">{{ saving ? '保存中…' : '确定' }}</button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑管理员" width="560px" append-to-body :close-on-click-modal="false" class="admin-dialog">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="用户名"><el-input :model-value="editForm.username" disabled /></el-form-item>
        <el-form-item label="真实姓名" prop="displayName"><el-input v-model="editForm.displayName" maxlength="64" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="editForm.phone" maxlength="20" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="editForm.email" maxlength="128" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" class="full-width" :disabled="editForm.id === auth.admin?.id">
            <el-option v-for="item in roles" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="editDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="saveEdit">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialog" title="权限管理" width="540px" append-to-body :close-on-click-modal="false" class="permission-dialog">
      <p class="permission-tip">正在配置 <strong>{{ permissionForm.displayName }}</strong> 的后台菜单权限；保存后对方刷新页面即可生效。</p>
      <el-checkbox-group v-model="permissionForm.permissions" class="permission-grid">
        <el-checkbox v-for="item in permissionOptions" :key="item.value" :value="item.value" class="permission-option">
          <span class="permission-label">
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </span>
        </el-checkbox>
      </el-checkbox-group>
      <p v-if="permissionForm.permissions.length === 0" class="permission-warning">请至少选择一个菜单权限</p>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="permissionDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="permissionSaving || permissionForm.permissions.length === 0" @click="savePermissions">{{ permissionSaving ? '保存中…' : '确定' }}</button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialog" title="重置管理员密码" width="480px" append-to-body :close-on-click-modal="false">
      <p class="password-tip">正在重置 <strong>{{ passwordForm.displayName }}</strong> 的后台密码。保存后该账号所有现有会话会立即失效。</p>
      <el-form label-position="top">
        <el-form-item label="新密码" required><el-input v-model="passwordForm.password" type="password" show-password maxlength="72" placeholder="至少 8 位" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="passwordDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="resetPassword">{{ saving ? '保存中…' : '重置密码' }}</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Avatar, Lock, WarningFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import {
  createAdmin,
  fetchAdmins,
  resetAdminPassword,
  updateAdminPermissions,
  updateAdminProfile,
  updateAdminStatus
} from '../api/admins'
import { PERMISSION_OPTIONS } from '../constants/permissions'

const auth = useAuthStore()
const router = useRouter()
const admins = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const createDialog = ref(false)
const editDialog = ref(false)
const passwordDialog = ref(false)
const permissionDialog = ref(false)
const permissionSaving = ref(false)
const permissionOptions = PERMISSION_OPTIONS
const createFormRef = ref(null)
const editFormRef = ref(null)
const roles = [
  { value: 'super_admin', label: '超级管理员' },
  { value: 'operator', label: '运营管理员' },
  { value: 'finance', label: '财务管理员' },
  { value: 'content', label: '内容管理员' }
]
const createForm = reactive({
  username: '', displayName: '', phone: '', email: '', role: '', password: '', active: true, remark: ''
})
const editForm = reactive({
  id: null, username: '', displayName: '', phone: '', email: '', role: 'operator', remark: ''
})
const phoneValidator = (rule, value, callback) => {
  if (!value || /^1[3-9]\d{9}$/.test(value)) callback()
  else callback(new Error('请输入 11 位手机号'))
}
const emailValidator = (rule, value, callback) => {
  if (!value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback()
  else callback(new Error('请输入正确的邮箱'))
}
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.-]{3,64}$/, message: '只能包含字母、数字、下划线、点或短横线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度需为 8–72 位', trigger: 'blur' }
  ],
  displayName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
  email: [{ validator: emailValidator, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 字', trigger: 'blur' }]
}
const editRules = {
  displayName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
  email: [{ validator: emailValidator, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 字', trigger: 'blur' }]
}
const passwordForm = reactive({ id: null, displayName: '', password: '' })
const permissionForm = reactive({ id: null, displayName: '', username: '', permissions: [] })
const isSuper = computed(() => auth.admin?.role === 'super_admin')

function roleLabel(value) {
  return roles.find(item => item.value === value)?.label || value
}

function roleClass(value) {
  return { super_admin: 'adm-tag-blue', finance: 'adm-tag-amber', content: 'adm-tag-green' }[value] || ''
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  if (!isSuper.value) return
  loading.value = true
  error.value = ''
  try {
    admins.value = await fetchAdmins()
  } catch (err) {
    error.value = err.message || '管理员加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, {
    username: '', displayName: '', phone: '', email: '', role: '', password: '', active: true, remark: ''
  })
  createDialog.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

function openEdit(item) {
  Object.assign(editForm, {
    id: item.id, username: item.username, displayName: item.displayName,
    phone: item.phone || '', email: item.email || '', role: item.role, remark: item.remark || ''
  })
  editDialog.value = true
  nextTick(() => editFormRef.value?.clearValidate())
}

function openPassword(item) {
  Object.assign(passwordForm, { id: item.id, displayName: item.displayName, password: '' })
  passwordDialog.value = true
}

function openPermissions(item) {
  if (item.role === 'super_admin') {
    ElMessage.info('超级管理员默认拥有全部权限')
    return
  }
  Object.assign(permissionForm, {
    id: item.id,
    displayName: item.displayName,
    username: item.username,
    permissions: Array.isArray(item.permissions) ? [...item.permissions] : []
  })
  permissionDialog.value = true
}

async function savePermissions() {
  if (!permissionForm.permissions.length) {
    ElMessage.warning('请至少选择一个菜单权限')
    return
  }
  permissionSaving.value = true
  try {
    const saved = await updateAdminPermissions(permissionForm.id, {
      permissions: permissionForm.permissions
    })
    const index = admins.value.findIndex(item => item.id === saved.id)
    if (index >= 0) admins.value[index] = saved
    ElMessage.success('管理员权限已更新')
    permissionDialog.value = false
  } catch (err) {
    ElMessage.error(err.message || '权限保存失败')
  } finally {
    permissionSaving.value = false
  }
}

async function create() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createAdmin({
      username: createForm.username,
      displayName: createForm.displayName,
      phone: createForm.phone,
      email: createForm.email,
      role: createForm.role,
      password: createForm.password,
      active: createForm.active,
      remark: createForm.remark
    })
    ElMessage.success('管理员已创建')
    createDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.message || '创建失败')
  } finally {
    saving.value = false
  }
}

async function saveEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateAdminProfile(editForm.id, {
      displayName: editForm.displayName,
      phone: editForm.phone,
      email: editForm.email,
      role: editForm.role,
      remark: editForm.remark
    })
    ElMessage.success('管理员信息已更新')
    editDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function resetPassword() {
  saving.value = true
  try {
    await resetAdminPassword(passwordForm.id, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialog.value = false
    if (passwordForm.id === auth.admin?.id) {
      ElMessage.warning('当前账号密码已变更，请重新登录')
      await auth.logout()
      await router.replace('/login')
    } else {
      await load()
    }
  } catch (err) {
    ElMessage.error(err.message || '重置失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item) {
  const active = !item.active
  try {
    await ElMessageBox.confirm(
      active ? `确认启用管理员「${item.displayName}」？` : `确认停用管理员「${item.displayName}」？停用后其所有后台会话会立即失效。`,
      active ? '启用管理员' : '停用管理员',
      { confirmButtonText: active ? '启用' : '停用', cancelButtonText: '取消', type: 'warning' }
    )
    await updateAdminStatus(item.id, active)
    ElMessage.success(active ? '管理员已启用' : '管理员已停用')
    await load()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.admin-loading {
  padding: 42px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.admin-person {
  display: flex;
  align-items: center;
  gap: 9px;
}

.admin-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #eef5ff;
  color: #3f83cf;
  font-size: 12px;
  font-weight: 600;
}

.admin-person div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-person strong {
  color: #2b425a;
  font-size: 11px;
}

.admin-person em {
  margin-left: 5px;
  padding: 1px 4px;
  border-radius: 4px;
  background: #eaf8f2;
  color: #2f9b70;
  font-size: 8px;
  font-style: normal;
  font-weight: 500;
}

.admin-person small {
  color: #a1afbe;
  font-size: 9px;
}

.full-width {
  width: 100%;
}

.admin-contact {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 190px;
}

.admin-contact span {
  color: #526a82;
  font-size: 10px;
}

.admin-contact small {
  color: #9aaabc;
  font-size: 9px;
}

.admin-contact em {
  overflow: hidden;
  max-width: 190px;
  color: #a2865d;
  font-size: 8.5px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-dialog :deep(.el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.permission-count {
  display: block;
  margin-top: 3px;
  color: #a1afbe;
  font-size: 8px;
  white-space: nowrap;
}

.permission-tip {
  margin: 0 0 12px;
  color: #7f92a7;
  font-size: 11px;
  line-height: 1.7;
}

.permission-tip strong {
  color: #3d5c7e;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 14px;
}

.permission-option {
  height: 36px;
  margin-right: 0;
}

.permission-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #526a82;
  font-size: 11px;
}

.permission-label .el-icon {
  color: #7890a9;
  font-size: 14px;
}

.permission-warning {
  margin: 10px 0 0;
  color: #d46b6b;
  font-size: 10px;
}

.permission-dialog :deep(.el-dialog__body) {
  padding-bottom: 8px;
}

.password-tip {
  margin: 0 0 16px;
  color: #7f92a7;
  font-size: 11px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .adm-table {
    min-width: 940px;
  }
}
</style>
