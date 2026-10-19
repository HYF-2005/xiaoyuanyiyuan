<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-grid" aria-hidden="true"></div>
      <div class="brand-glow" aria-hidden="true"></div>

      <div class="brand-content">
        <div class="brand-top">
          <div class="brand-mark">援</div>
          <div class="brand-name">
            <strong>校园易援</strong>
            <span>CAMPUS HELP</span>
          </div>
        </div>

        <div class="brand-headline">
          <p class="eyebrow">CAMPUS HELP · ADMIN CONSOLE</p>
          <h1>让每一次<br /><span>校园互助</span><br />都被清晰看见。</h1>
          <p class="lead">校园互助跑腿平台 · 运营管理后台</p>
        </div>

        <div class="brand-metrics">
          <div class="metric">
            <strong>21</strong>
            <span>小程序页面</span>
          </div>
          <div class="metric">
            <strong>8</strong>
            <span>数据表</span>
          </div>
          <div class="metric">
            <strong>v1.2.0</strong>
            <span>当前版本</span>
          </div>
        </div>

        <p class="brand-foot">取件 · 取餐 · 互助 · 信用</p>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-wrap">
        <div class="login-card">
          <div class="card-head">
            <span class="card-badge">ADMIN CONSOLE</span>
            <h2>管理员登录</h2>
            <p>使用后台管理员账号，不是小程序用户账号。</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            @submit.prevent="submit"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                size="large"
                placeholder="管理员账号"
                :prefix-icon="User"
                autocomplete="username"
                @keyup.enter="submit"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                placeholder="登录密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="current-password"
                @keyup.enter="submit"
              />
            </el-form-item>

            <el-alert
              v-if="error"
              class="login-error"
              :title="error"
              type="error"
              :closable="false"
              show-icon
            />

            <el-button
              class="login-submit"
              type="primary"
              size="large"
              :loading="loading"
              @click="submit"
            >
              <span>进入管理后台</span>
              <el-icon v-if="!loading"><ArrowRight /></el-icon>
            </el-button>
          </el-form>

          <div class="login-foot">
            <span class="status-dot"></span>
            管理员会话 12 小时 · 仅限授权人员
          </div>
        </div>

        <p class="form-copyright">校园易援 · 运营管理平台</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { errorMessage } from '../api/http'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function submit() {
  if (loading.value) return
  error.value = ''

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (requestError) {
    error.value = errorMessage(requestError, '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(460px, 0.92fr);
  min-height: 100vh;
  background: #f3f6fb;
}

.brand-panel {
  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 12%, rgba(47, 125, 246, 0.38), transparent 32%),
    radial-gradient(circle at 82% 82%, rgba(45, 212, 191, 0.16), transparent 30%),
    linear-gradient(145deg, #06162b 0%, #071f3b 52%, #0a2849 100%);
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: linear-gradient(to bottom right, rgba(0, 0, 0, 0.95), transparent 78%);
  pointer-events: none;
}

.brand-glow {
  position: absolute;
  right: -170px;
  bottom: -230px;
  width: 620px;
  height: 620px;
  border: 1px solid rgba(88, 166, 255, 0.14);
  border-radius: 50%;
  box-shadow:
    0 0 0 70px rgba(47, 125, 246, 0.035),
    0 0 0 150px rgba(47, 125, 246, 0.018);
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 48px 64px 42px;
  color: #fff;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 14px;
  background: linear-gradient(145deg, rgba(86, 162, 255, 0.95), rgba(32, 104, 219, 0.92));
  box-shadow: 0 12px 28px rgba(26, 91, 196, 0.35);
  font-size: 23px;
  font-weight: 700;
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-name strong {
  font-size: 18px;
  letter-spacing: 2px;
}

.brand-name span {
  color: rgba(184, 207, 234, 0.72);
  font-size: 10px;
  letter-spacing: 2.6px;
}

.brand-headline {
  margin-top: auto;
  padding-top: 72px;
}

.eyebrow {
  margin: 0 0 20px;
  color: #6fa9ff;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3.2px;
}

.brand-headline h1 {
  margin: 0;
  color: #f4f8ff;
  font-size: clamp(44px, 4.3vw, 66px);
  font-weight: 650;
  letter-spacing: -1.5px;
  line-height: 1.18;
}

.brand-headline h1 span {
  background: linear-gradient(100deg, #7eb6ff 8%, #2dd4bf 94%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.lead {
  margin: 24px 0 0;
  color: rgba(194, 213, 238, 0.78);
  font-size: 15px;
  letter-spacing: 1.2px;
}

.brand-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  max-width: 520px;
  margin-top: auto;
  padding-top: 54px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding-left: 20px;
  border-left: 1px solid rgba(170, 199, 234, 0.16);
}

.metric:first-child {
  padding-left: 0;
  border-left: 0;
}

.metric strong {
  color: #f1f8ff;
  font-size: 27px;
  font-weight: 600;
  letter-spacing: -0.4px;
}

.metric span {
  color: rgba(171, 195, 224, 0.66);
  font-size: 11px;
  letter-spacing: 1.2px;
}

.brand-foot {
  margin: 42px 0 0;
  color: rgba(143, 173, 209, 0.5);
  font-size: 11px;
  letter-spacing: 3px;
}

.form-panel {
  position: relative;
  display: grid;
  min-height: 100vh;
  padding: 44px;
  place-items: center;
  background:
    radial-gradient(circle at 82% 12%, rgba(47, 125, 246, 0.10), transparent 34%),
    linear-gradient(160deg, #f5f8fd 0%, #edf3fa 100%);
}

.form-panel::before {
  position: absolute;
  top: 7%;
  right: 7%;
  width: 170px;
  height: 170px;
  border: 1px solid rgba(47, 125, 246, 0.11);
  border-radius: 50%;
  content: '';
  pointer-events: none;
}

.form-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 430px;
}

.login-card {
  padding: 40px 40px 30px;
  border: 1px solid rgba(218, 228, 241, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 28px 70px rgba(19, 52, 92, 0.12),
    0 4px 14px rgba(19, 52, 92, 0.04);
  backdrop-filter: blur(14px);
}

.card-head {
  margin-bottom: 28px;
}

.card-badge {
  display: inline-flex;
  padding: 5px 9px;
  border: 1px solid #dbe8fa;
  border-radius: 6px;
  background: #f2f7ff;
  color: #3978d8;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.card-head h2 {
  margin: 15px 0 8px;
  color: #10233f;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.card-head p {
  margin: 0;
  color: #7b8ca3;
  font-size: 13px;
  line-height: 1.65;
}

.login-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-card :deep(.el-input__wrapper) {
  min-height: 52px;
  padding: 0 15px;
  border-radius: 12px;
  background: #f7f9fd;
  box-shadow: 0 0 0 1px #e4ebf4 inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.login-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c9d9ed inset;
}

.login-card :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #2f7df6 inset, 0 0 0 4px rgba(47, 125, 246, 0.08);
}

.login-card :deep(.el-input__prefix) {
  color: #8ea1ba;
  font-size: 18px;
}

.login-card :deep(.el-input__inner) {
  color: #183251;
  font-size: 14px;
}

.login-card :deep(.el-input__inner::placeholder) {
  color: #a5b3c5;
}

.login-error {
  margin: 2px 0 18px;
  border-radius: 10px;
}

.login-submit {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #337ff0 0%, #2164d6 100%);
  box-shadow: 0 12px 22px rgba(33, 100, 214, 0.22);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-submit:hover,
.login-submit:focus {
  background: linear-gradient(135deg, #438bf5 0%, #2a70df 100%);
}

.login-submit .el-icon {
  margin-left: 8px;
  font-size: 15px;
}

.login-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 25px;
  color: #98a8bb;
  font-size: 11px;
  letter-spacing: 0.2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #25b79b;
  box-shadow: 0 0 0 3px rgba(37, 183, 155, 0.12);
}

.form-copyright {
  margin: 20px 0 0;
  color: #9aa9bb;
  font-size: 10px;
  letter-spacing: 1.4px;
  text-align: center;
}

@media (max-width: 1050px) {
  .brand-content {
    padding: 40px 44px 35px;
  }

  .brand-headline h1 {
    font-size: 48px;
  }

  .form-panel {
    padding: 30px;
  }
}

@media (max-width: 860px) {
  .login-page {
    display: block;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 24px;
  }

  .login-card {
    padding: 32px 26px 26px;
  }
}

@media (max-height: 760px) and (min-width: 861px) {
  .brand-headline {
    padding-top: 36px;
  }

  .brand-metrics {
    padding-top: 32px;
  }

  .brand-foot {
    margin-top: 28px;
  }
}
</style>
