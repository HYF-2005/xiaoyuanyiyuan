// 实名认证注册：手机验证、设置密码、身份证信息、本人照片和确认提交。
// 页面只负责交互与校验；注册请求仍由后端 /auth/register 创建账号并校验验证码。
const {
  request,
  clearSession,
  openPolicyDocument
} = require('../../utils/api')

const PHONE_PATTERN = /^1[3-9]\d{9}$/
const CODE_PATTERN = /^\d{6}$/
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,10}$/
const ID_CARD_PATTERN = /^\d{17}[\dX]$/
const MAX_SELFIE_BYTES = 5 * 1024 * 1024

Page({
  data: {
    // stage 控制当前展示的子页面：phone/password/id/selfie/confirm
    stage: 'phone',
    step: 1,
    phone: '',
    code: '',
    codePhone: '',
    countdown: 0,
    agreed: false,

    newPassword: '',
    confirmPassword: '',
    showPassword: false,
    showConfirmPassword: false,

    realName: '',
    idCard: '',
    gender: '',
    birthDate: '',
    idPhoto: '',

    selfie: '',
    selfieSizeText: '',

    canNextPhone: false,
    canNextPassword: false,
    canNextId: false,
    canNextSelfie: false,
    canSubmit: false,
    submitting: false
  },

  onUnload() {
    this.clearCountdown()
  },

  // 通用输入处理：输入框通过 data-field 指定要更新的字段。
  onFieldInput(e) {
    const field = e.currentTarget.dataset.field
    if (!field) return
    this.setData({
      [field]: e.detail.value
    }, () => this.refreshReady())
  },

  // 手机号变化时，旧号码收到的验证码不再可用。
  onPhoneInput(e) {
    const phone = e.detail.value
    const patch = {
      phone
    }
    if (phone !== this.data.codePhone) {
      patch.code = ''
      patch.codePhone = ''
      patch.countdown = 0
      this.clearCountdown()
    }
    this.setData(patch, () => this.refreshReady())
  },

  // 身份证号输入后自动带出性别和出生日期，便于确认页展示。
  onIdInput(e) {
    const idCard = (e.detail.value || '').replace(/\s/g, '').toUpperCase()
    let gender = ''
    let birthDate = ''
    if (ID_CARD_PATTERN.test(idCard)) {
      const year = idCard.slice(6, 10)
      const month = idCard.slice(10, 12)
      const day = idCard.slice(12, 14)
      birthDate = `${year}-${month}-${day}`
      gender = Number(idCard.charAt(16)) % 2 === 1 ? '男' : '女'
    }
    this.setData({
      idCard,
      gender,
      birthDate
    }, () => this.refreshReady())
  },

  // 根据当前填写内容更新按钮可用状态，避免只依赖点按钮时提示。
  refreshReady() {
    const phone = (this.data.phone || '').trim()
    const phoneReady = PHONE_PATTERN.test(phone) && CODE_PATTERN.test(this.data.code)
    const passwordReady = PASSWORD_PATTERN.test(this.data.newPassword)
      && this.data.newPassword === this.data.confirmPassword
    const idReady = Boolean(this.data.idPhoto)
      && Boolean((this.data.realName || '').trim())
      && ID_CARD_PATTERN.test(this.data.idCard)
    const selfieReady = Boolean(this.data.selfie)
    this.setData({
      canNextPhone: phoneReady && this.data.agreed,
      canNextPassword: passwordReady,
      canNextId: idReady,
      canNextSelfie: selfieReady,
      canSubmit: phoneReady && this.data.agreed && passwordReady && idReady && selfieReady
    })
  },

  // 切换子页面时回到顶部，避免长页面保留上一页的滚动位置。
  goToStage(stage, step) {
    this.setData({
      stage,
      step
    }, () => wx.pageScrollTo({
      scrollTop: 0,
      duration: 0
    }))
  },

  toggleAgreement() {
    this.setData({
      agreed: !this.data.agreed
    }, () => this.refreshReady())
  },

  togglePassword(e) {
    const field = e.currentTarget.dataset.field
    if (!field) return
    this.setData({
      [field]: !this.data[field]
    })
  },

  // 发送 bind 用途验证码：号码不能已被其他账号绑定，正好用于新注册。
  async sendCode() {
    const phone = (this.data.phone || '').trim()
    if (!PHONE_PATTERN.test(phone)) {
      this.toast('请输入正确的手机号')
      return
    }
    if (this.data.countdown > 0) return
    try {
      const result = await request('/auth/code', 'POST', {
        phone,
        purpose: 'bind'
      })
      this.setData({
        code: '',
        codePhone: phone
      })
      this.startCountdown()
      wx.showModal({
        title: '验证码已发送',
        content: result.demoCode
          ? `本地演示验证码：${result.demoCode}`
          : '验证码已发送，请注意查收',
        showCancel: false
      })
    } catch (error) {
      this.toast(error.message || '验证码发送失败')
    }
  },

  startCountdown() {
    this.clearCountdown()
    this.setData({
      countdown: 60
    })
    this.countdownTimer = setInterval(() => {
      const next = this.data.countdown - 1
      if (next <= 0) {
        this.clearCountdown()
        this.setData({
          countdown: 0
        })
      } else {
        this.setData({
          countdown: next
        })
      }
    }, 1000)
  },

  clearCountdown() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  nextPhone() {
    if (!PHONE_PATTERN.test((this.data.phone || '').trim())) {
      this.toast('请输入正确的手机号')
      return
    }
    if (!CODE_PATTERN.test(this.data.code)) {
      this.toast('请输入6位验证码')
      return
    }
    if (!this.data.agreed) {
      this.toast('请先同意用户协议和隐私政策')
      return
    }
    this.goToStage('password', 1)
  },

  nextPassword() {
    if (!PASSWORD_PATTERN.test(this.data.newPassword)) {
      this.toast('密码需为8-10位，且包含数字和字母')
      return
    }
    if (this.data.newPassword !== this.data.confirmPassword) {
      this.toast('两次输入的密码不一致')
      return
    }
    this.goToStage('id', 2)
  },

  nextId() {
    if (!this.data.idPhoto) {
      this.toast('请先拍摄身份证人像面')
      return
    }
    if (!(this.data.realName || '').trim()) {
      this.toast('请输入身份证上的姓名')
      return
    }
    if (!ID_CARD_PATTERN.test(this.data.idCard)) {
      this.toast('请输入正确的18位身份证号')
      return
    }
    this.goToStage('selfie', 3)
  },

  nextSelfie() {
    if (!this.data.selfie) {
      this.toast('请先上传本人照片')
      return
    }
    this.goToStage('confirm', 3)
  },

  backToPhone() {
    this.goToStage('phone', 1)
  },

  backToPassword() {
    this.goToStage('password', 1)
  },

  backToId() {
    this.goToStage('id', 2)
  },

  backToSelfie() {
    this.goToStage('selfie', 3)
  },

  // 选择图片；优先使用 chooseMedia，旧基础库退回 chooseImage。
  pickImage() {
    return new Promise((resolve, reject) => {
      if (wx.chooseMedia) {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: ['camera', 'album'],
          sizeType: ['compressed'],
          success: res => resolve(res.tempFiles[0]),
          fail: reject
        })
        return
      }
      wx.chooseImage({
        count: 1,
        sourceType: ['camera', 'album'],
        sizeType: ['compressed'],
        success: res => resolve({
          tempFilePath: res.tempFilePaths[0],
          size: res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].size : 0
        }),
        fail: reject
      })
    })
  },

  async chooseIdPhoto() {
    try {
      const file = await this.pickImage()
      this.setData({
        idPhoto: file.tempFilePath
      }, () => this.refreshReady())
    } catch (error) {
      if (!/cancel/i.test(error.errMsg || '')) {
        this.toast(error.errMsg || '无法选择身份证照片')
      }
    }
  },

  async chooseSelfie() {
    try {
      const file = await this.pickImage()
      const size = Number(file.size || 0)
      if (size > MAX_SELFIE_BYTES) {
        this.toast('照片不能超过5MB，请重新拍摄')
        return
      }
      this.setData({
        selfie: file.tempFilePath,
        selfieSizeText: this.formatSize(size)
      }, () => this.refreshReady())
    } catch (error) {
      if (!/cancel/i.test(error.errMsg || '')) {
        this.toast(error.errMsg || '无法选择本人照片')
      }
    }
  },

  formatSize(size) {
    if (!size) return '已选择'
    if (size < 1024) return `${size}B`
    return `${(size / 1024).toFixed(2)}KB`
  },

  // 注册账号使用手机号作为登录账号；验证码在提交时由后端校验并消费。
  async submit() {
    if (this.data.submitting) return
    if (!this.data.canSubmit) {
      this.toast('请完善实名认证信息')
      return
    }
    const phone = (this.data.phone || '').trim()
    this.setData({
      submitting: true
    })
    try {
      await request('/auth/register', 'POST', {
        username: phone,
        password: this.data.newPassword,
        displayName: (this.data.realName || '').trim(),
        phone,
        code: this.data.code
      })
      clearSession()
      wx.showModal({
        title: '提交成功',
        content: `实名认证信息已提交，登录账号为 ${phone}，请使用手机号和密码登录。`,
        showCancel: false,
        success: () => wx.reLaunch({
          url: '/pages/denglu/denglu'
        })
      })
    } catch (error) {
      const message = error.message || '注册失败，请稍后重试'
      if (/验证码/.test(message)) {
        this.clearCountdown()
        this.setData({
          stage: 'phone',
          step: 1,
          countdown: 0,
          code: ''
        }, () => {
          this.refreshReady()
          wx.pageScrollTo({
            scrollTop: 0,
            duration: 0
          })
        })
        wx.showModal({
          title: '验证码已失效',
          content: '请重新获取验证码后继续注册。',
          showCancel: false
        })
      } else {
        this.toast(message)
      }
    } finally {
      this.setData({
        submitting: false
      })
    }
  },

  // 完整协议 PDF 仍由登录页同一套文档服务打开。
  async openPolicy(e) {
    wx.showLoading({
      title: '正在打开'
    })
    try {
      await openPolicyDocument(e.currentTarget.dataset.type)
    } catch (error) {
      this.toast(error.message || '协议打开失败')
    } finally {
      wx.hideLoading()
    }
  },

  toast(title) {
    wx.showToast({
      title,
      icon: 'none'
    })
  }
})
