// 绑定手机号：获取演示验证码、重发倒计时和后端绑定
const {
  request
}
= require('../../utils/api')

Page({
  data: {
    phone: '',
    code: '',
    demoCode: '',
    codePhone: '',
    // codeExpiresAt：演示码的本地有效期限，最终仍需服务器检查
    codeExpiresAt: 0,
    // countdown：再次获取验证码还需等待的秒数
    countdown: 0
  },

  onPhoneInput(e) {
    this.setData({
      phone: e.detail.value
    })
  },
  onCodeInput(e) {
    this.setData({
      code: e.detail.value
    })
  },
  // onShow：重回页面时恢复按截止时间计算的验证码倒计时。
  onShow() {
    if (this.resendUntil) this.startCountdown()
  },

  // onHide：页面被遮住时停止倒计时；重新显示时从实际截止时间恢复。
  onHide() {
    this.clearCountdown()
  },

  // onUnload：页面销毁时清理倒计时，避免资源泄漏。
  onUnload() {
    this.clearCountdown()
  },

  // clearCountdown：清除验证码重发定时器，避免离开页面后继续运行。
  clearCountdown() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  // startCountdown：按重发截止时间计算剩余秒数，每秒更新显示；不是每次进入页面都重新倒数。
  startCountdown() {
    this.clearCountdown()

    const update = () => {
      const countdown = Math.max(
      0,
      Math.ceil((this.resendUntil - Date.now()) / 1000)
      )
      this.setData({
        countdown
      })
      if (countdown === 0) this.clearCountdown()
    }

    update()
    if (this.data.countdown > 0) {
      this.countdownTimer = setInterval(update, 1000)
    }
  },
  // sendCode：校验手机号，请求 bind 用途的演示验证码；记录有效期和 60 秒重发限制。
  async sendCode() {
    if (this.data.countdown > 0) return
    const phone = this.data.phone

    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    try {
      const result = await request('/auth/code', 'POST', {
        phone,
        purpose: 'bind'
      })
      this.setData({
        demoCode: result.demoCode || '',
        codePhone: phone,
        codeExpiresAt: Date.now() + 5 * 60 * 1000
      })
      this.resendUntil = Date.now() + 60 * 1000
      this.startCountdown()
      wx.showModal({
        title: '演示验证码',
        content: result.demoCode ? `本次验证码：${result.demoCode}。仅用于本地演示，不会发送短信。` : result.message,
        showCancel: false
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  // confirmBind：先做本地提示校验，再 POST /me/phone；是否能绑定最终由服务器判断。
  async confirmBind() {
    const {
      phone,
      code,
      demoCode,
      codePhone,
      codeExpiresAt
    }
    = this.data

    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    if (!code.trim()) {
      wx.showToast({
        title: '请输入验证码',
        icon: 'none'
      })
      return
    }

    if (!demoCode || phone !== codePhone) {
      wx.showToast({
        title: '请重新获取验证码',
        icon: 'none'
      })
      return
    }

    if (Date.now() > codeExpiresAt) {
      wx.showToast({
        title: '验证码已过期，请重新获取',
        icon: 'none'
      })
      return
    }

    if (code !== demoCode) {
      wx.showToast({
        title: '验证码错误',
        icon: 'none'
      })
      return
    }

    try {
      await request('/me/phone', 'POST', {
        phone,
        code: code.trim()
      })
      wx.showToast({
        title: '绑定成功',
        icon: 'success'
      })
      setTimeout(() => wx.navigateBack(), 1000)
    } catch (error) {
      wx.showToast({
        title: error.message || '绑定失败，请重试',
        icon: 'none'
      })
    }
  }
})
