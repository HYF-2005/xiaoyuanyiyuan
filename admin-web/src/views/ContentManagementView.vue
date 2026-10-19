<template>
  <div class="adm-page">
    <div class="adm-page-head">
      <div>
        <p class="adm-eyebrow">CONTENT OPERATIONS</p>
        <h1 class="adm-title">业务配置</h1>
        <p class="adm-sub">维护小程序首页轮播与公告；公开接口只返回启用/已发布且在有效期内的内容。</p>
      </div>
      <span class="adm-count">{{ mode === 'banners' ? `${banners.length} 个轮播位` : `${announcements.length} 条公告` }}</span>
    </div>

    <div class="content-tabs">
      <button type="button" :class="{ active: mode === 'banners' }" @click="switchMode('banners')">轮播管理</button>
      <button type="button" :class="{ active: mode === 'announcements' }" @click="switchMode('announcements')">公告管理</button>
      <span class="content-note">修改会直接影响小程序首页和公告弹窗</span>
    </div>

    <template v-if="mode === 'banners'">
      <section class="adm-panel">
        <div class="adm-panel-head">
          <div>
            <p class="adm-panel-eyebrow">HOME BANNERS</p>
            <h2 class="adm-panel-title">首页轮播</h2>
          </div>
          <button class="adm-btn adm-btn-primary" type="button" @click="openBanner()">新增轮播</button>
        </div>

        <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span></div>
        <div v-if="loading" class="content-loading">正在读取轮播…</div>
        <template v-else>
          <div v-if="banners.length" class="adm-table-wrap">
            <table class="adm-table">
              <thead>
                <tr>
                  <th>预览</th>
                  <th>标题 / 编码</th>
                  <th>标签</th>
                  <th>跳转</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th class="adm-align-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="banner in banners" :key="banner.id">
                  <td>
                    <el-image class="banner-thumb" :src="banner.imageUrl" fit="cover">
                      <template #error><div class="thumb-fallback"><el-icon><Picture /></el-icon></div></template>
                    </el-image>
                  </td>
                  <td>
                    <div class="content-title-cell">
                      <strong>{{ banner.title }}</strong>
                      <span>{{ banner.code }}</span>
                    </div>
                  </td>
                  <td><span class="adm-tag">{{ banner.tag }}</span></td>
                  <td class="adm-muted">{{ linkLabel(banner.linkType) }}<span v-if="banner.linkValue"> · {{ banner.linkValue }}</span></td>
                  <td class="adm-muted">{{ banner.sortOrder }}</td>
                  <td>
                    <span class="adm-tag" :class="banner.enabled ? 'adm-tag-green' : 'adm-tag-gray'">{{ banner.enabled ? '展示中' : '已停用' }}</span>
                  </td>
                  <td class="adm-align-right">
                    <button class="adm-btn adm-btn-sm" type="button" @click="openBanner(banner)">编辑</button>
                    <button class="adm-btn adm-btn-sm adm-btn-danger" type="button" @click="removeBanner(banner)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="adm-empty"><el-icon><Picture /></el-icon><strong>暂无轮播</strong><span>新增后即可在小程序首页展示</span></div>
        </template>
      </section>
      <p class="adm-foot">图片支持小程序包内 /assets 路径或 HTTPS 地址；后台只保存地址，不上传图片文件。停用或超出有效期后，小程序公开接口会自动隐藏。</p>
    </template>

    <template v-else>
      <section class="adm-panel">
        <div class="adm-panel-head">
          <div>
            <p class="adm-panel-eyebrow">ANNOUNCEMENTS</p>
            <h2 class="adm-panel-title">公告列表</h2>
          </div>
          <button class="adm-btn adm-btn-primary" type="button" @click="openAnnouncement()">新增公告</button>
        </div>

        <div v-if="error" class="adm-error"><el-icon><WarningFilled /></el-icon><span>{{ error }}</span></div>
        <div v-if="loading" class="content-loading">正在读取公告…</div>
        <template v-else>
          <div v-if="announcements.length" class="adm-table-wrap">
            <table class="adm-table">
              <thead>
                <tr>
                  <th>公告</th>
                  <th>分类</th>
                  <th>级别</th>
                  <th>强提醒</th>
                  <th>状态</th>
                  <th>发布时间</th>
                  <th class="adm-align-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in announcements" :key="item.id">
                  <td>
                    <div class="content-title-cell">
                      <strong>{{ item.title }} <em v-if="item.pinned" class="pin-mark">置顶</em></strong>
                      <span>{{ item.summary || '未填写摘要' }}</span>
                    </div>
                  </td>
                  <td class="adm-muted">{{ item.category }}</td>
                  <td><span class="adm-tag" :class="levelClass(item.level)">{{ item.level }}</span></td>
                  <td class="adm-muted">{{ item.forceRead ? '5 秒强提醒' : '普通' }}</td>
                  <td><span class="adm-tag" :class="item.isPublished ? 'adm-tag-green' : 'adm-tag-gray'">{{ item.isPublished ? '已发布' : '草稿' }}</span></td>
                  <td class="adm-muted">{{ formatDateTime(item.publishedAt) }}</td>
                  <td class="adm-align-right">
                    <button class="adm-btn adm-btn-sm" type="button" @click="openAnnouncement(item)">编辑</button>
                    <button class="adm-btn adm-btn-sm adm-btn-danger" type="button" @click="removeAnnouncement(item)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="adm-empty"><el-icon><Document /></el-icon><strong>暂无公告</strong><span>新增并发布后，小程序会按发布时间展示</span></div>
        </template>
      </section>
      <p class="adm-foot">「强提醒」开启后，未读用户登录会弹出公告并执行 5 秒强制阅读；草稿不会出现在小程序公告列表。</p>
    </template>

    <el-dialog v-model="bannerDialog" :title="bannerForm.id ? '编辑轮播' : '新增轮播'" width="680px" append-to-body :close-on-click-modal="false">
      <el-form label-position="top" class="content-form">
        <div class="adm-form-grid">
          <el-form-item label="稳定编码" required>
            <el-input v-model="bannerForm.code" maxlength="64" placeholder="例如 home-2026-11-promo" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="bannerForm.tag" maxlength="20" placeholder="公告 / 活动 / 推广" />
          </el-form-item>
          <el-form-item class="full" label="图片地址" required>
            <el-input v-model="bannerForm.imageUrl" maxlength="500" placeholder="/assets/banners/xxx.jpg 或 https://..." />
          </el-form-item>
          <el-form-item class="full" label="标题" required>
            <el-input v-model="bannerForm.title" maxlength="120" />
          </el-form-item>
          <el-form-item class="full" label="副标题">
            <el-input v-model="bannerForm.subtitle" maxlength="255" />
          </el-form-item>
          <el-form-item label="跳转类型">
            <el-select v-model="bannerForm.linkType" class="full-width">
              <el-option label="不跳转" value="none" />
              <el-option label="公告详情" value="notice" />
              <el-option label="站内页面" value="page" />
              <el-option label="H5 网页" value="web" />
              <el-option label="其他小程序" value="miniProgram" />
            </el-select>
          </el-form-item>
          <el-form-item label="跳转目标">
            <el-input v-model="bannerForm.linkValue" maxlength="500" :disabled="bannerForm.linkType === 'none'" placeholder="公告 id / 页面路径 / URL / appId" />
          </el-form-item>
          <el-form-item label="排序（越小越靠前）">
            <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="9999" controls-position="right" class="full-width" />
          </el-form-item>
          <el-form-item label="是否展示">
            <el-switch v-model="bannerForm.enabled" active-text="展示" inactive-text="停用" />
          </el-form-item>
          <el-form-item label="生效时间（可选）">
            <el-date-picker v-model="bannerForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="立即生效" class="full-width" />
          </el-form-item>
          <el-form-item label="失效时间（可选）">
            <el-date-picker v-model="bannerForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="长期有效" class="full-width" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="bannerDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="saveBanner">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="announcementDialog" :title="announcementForm.id ? '编辑公告' : '新增公告'" width="720px" append-to-body :close-on-click-modal="false">
      <el-form label-position="top" class="content-form">
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" maxlength="120" />
        </el-form-item>
        <div class="adm-form-grid">
          <el-form-item label="分类">
            <el-select v-model="announcementForm.category" class="full-width">
              <el-option label="系统公告" value="系统公告" />
              <el-option label="活动" value="活动" />
            </el-select>
          </el-form-item>
          <el-form-item label="重要级别">
            <el-select v-model="announcementForm.level" class="full-width">
              <el-option label="普通" value="普通" />
              <el-option label="重要" value="重要" />
              <el-option label="紧急" value="紧急" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="摘要（留空自动截取正文）">
          <el-input v-model="announcementForm.summary" maxlength="255" />
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="8" maxlength="20000" show-word-limit />
        </el-form-item>
        <div class="adm-form-grid">
          <el-form-item label="展示选项">
            <div class="switch-stack">
              <el-switch v-model="announcementForm.pinned" active-text="置顶" />
              <el-switch v-model="announcementForm.forceRead" active-text="强提醒阅读" />
              <el-switch v-model="announcementForm.isPublished" active-text="发布" inactive-text="草稿" />
            </div>
          </el-form-item>
          <el-form-item label="发布时间">
            <el-date-picker v-model="announcementForm.publishedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="留空使用当前时间" class="full-width" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="adm-dialog-footer">
          <button class="adm-btn" type="button" @click="announcementDialog = false">取消</button>
          <button class="adm-btn adm-btn-primary" type="button" :disabled="saving" @click="saveAnnouncement">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Picture, WarningFilled } from '@element-plus/icons-vue'
import {
  createAdminAnnouncement,
  createAdminBanner,
  deleteAdminAnnouncement,
  deleteAdminBanner,
  fetchAdminAnnouncements,
  fetchAdminBanners,
  updateAdminAnnouncement,
  updateAdminBanner
} from '../api/content'

const route = useRoute()
const router = useRouter()
const mode = computed(() => (route.name === 'content-announcements' ? 'announcements' : 'banners'))
const banners = ref([])
const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const bannerDialog = ref(false)
const announcementDialog = ref(false)

const emptyBanner = () => ({
  id: null, code: '', imageUrl: '', tag: '公告', title: '', subtitle: '',
  linkType: 'none', linkValue: '', sortOrder: 0, enabled: true, startAt: null, endAt: null
})
const emptyAnnouncement = () => ({
  id: null, title: '', summary: '', content: '', category: '系统公告', level: '普通',
  pinned: false, forceRead: true, isPublished: true, publishedAt: null
})

const bannerForm = reactive(emptyBanner())
const announcementForm = reactive(emptyAnnouncement())

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function linkLabel(value) {
  return { none: '不跳转', notice: '公告', page: '站内页', web: 'H5', miniProgram: '小程序' }[value] || value || '—'
}

function levelClass(value) {
  return { 普通: '', 重要: 'adm-tag-amber', 紧急: 'adm-tag-red' }[value] || ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'banners') banners.value = await fetchAdminBanners()
    else announcements.value = await fetchAdminAnnouncements()
  } catch (err) {
    error.value = err.message || '内容加载失败'
  } finally {
    loading.value = false
  }
}

function switchMode(value) {
  router.replace(`/content/${value}`)
}

function openBanner(item) {
  Object.assign(bannerForm, item ? { ...item } : emptyBanner())
  bannerDialog.value = true
}

function openAnnouncement(item) {
  Object.assign(announcementForm, item ? { ...item } : emptyAnnouncement())
  announcementDialog.value = true
}

async function saveBanner() {
  saving.value = true
  try {
    const { id, ...payload } = bannerForm
    if (id) await updateAdminBanner(id, payload)
    else await createAdminBanner(payload)
    ElMessage.success(id ? '轮播已更新' : '轮播已创建')
    bannerDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.message || '轮播保存失败')
  } finally {
    saving.value = false
  }
}

async function saveAnnouncement() {
  saving.value = true
  try {
    const { id, ...payload } = announcementForm
    if (id) await updateAdminAnnouncement(id, payload)
    else await createAdminAnnouncement(payload)
    ElMessage.success(id ? '公告已更新' : '公告已创建')
    announcementDialog.value = false
    await load()
  } catch (err) {
    ElMessage.error(err.message || '公告保存失败')
  } finally {
    saving.value = false
  }
}

async function removeBanner(item) {
  try {
    await ElMessageBox.confirm(`确认删除轮播「${item.title}」？删除后小程序首页将不再展示。`, '删除轮播', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAdminBanner(item.id)
    ElMessage.success('轮播已删除')
    await load()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.message || '删除失败')
  }
}

async function removeAnnouncement(item) {
  try {
    await ElMessageBox.confirm(`确认删除公告「${item.title}」？公告已读记录也会一并清理。`, '删除公告', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAdminAnnouncement(item.id)
    ElMessage.success('公告已删除')
    await load()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') ElMessage.error(err.message || '删除失败')
  }
}

watch(mode, load)
onMounted(load)
</script>

<style scoped>
.content-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}

.content-tabs button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #fff;
  color: #76889d;
  font-size: 11px;
  cursor: pointer;
}

.content-tabs button.active {
  border-color: #d7e6fb;
  background: #edf5ff;
  color: #2f7df6;
  font-weight: 600;
}

.content-note {
  margin-left: auto;
  color: #9dacbd;
  font-size: 9px;
}

.content-loading {
  padding: 42px;
  color: #9aaabc;
  font-size: 11px;
  text-align: center;
}

.banner-thumb {
  width: 88px;
  height: 50px;
  border-radius: 7px;
  background: #f2f6fb;
}

.thumb-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: #b8c7d7;
}

.content-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 190px;
}

.content-title-cell strong {
  color: #263d55;
  font-size: 12px;
  font-weight: 600;
}

.content-title-cell span {
  max-width: 330px;
  overflow: hidden;
  color: #9aa9ba;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pin-mark {
  display: inline-block;
  margin-left: 5px;
  padding: 1px 4px;
  border-radius: 4px;
  background: #edf5ff;
  color: #4f8fd7;
  font-size: 8px;
  font-style: normal;
  font-weight: 500;
  vertical-align: 1px;
}

.full-width {
  width: 100%;
}

.content-form :deep(.el-form-item__label) {
  color: #6d8096;
  font-size: 11px;
}

.switch-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 32px;
  align-items: center;
}
</style>
