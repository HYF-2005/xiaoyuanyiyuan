// 忘记密码：身份验证界面与设置新密码；最终校验由后端完成
const {
  request
}
= require('../../utils/api')

Page({
  data: {
    // studentId：界面叫学号，后端请求参数实际叫 username
    studentId: '',
    phone: '',
    code: '',
    demoCode: '',
    codePhone: '',
    codeStudentId: '',
    // step：忘记密码流程阶段：1 验证 / 2 设置密码
    step: 1,
    // newPassword：准备提交的新密码
    newPassword: '',
    // confirmPassword：重复输入的密码，只做一致性校验
    confirmPassword: '',
    // hideNewPassword：新密码是否隐藏
    hideNewPassword: true,
    // hideConfirmPassword：确认密码是否隐藏
    hideConfirmPassword: true,
    // passwordStrength：页面的密码强度标签
    passwordStrength: 0,
    strengthLabel: '',
    strengthColor: ''
  },

  // onStudentIdInput：保存界面上的学号/账号；重置密码请求会把它作为 username 发送。
  onStudentIdInput(e) {
    this.setData({
      studentId: e.detail.value
    })
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
  async getDemoCode() {
    const {
      studentId,
      phone
    }
    = this.data

    if (!studentId.trim()) {
      wx.showToast({
        title: '请先输入学号',
        icon: 'none'
      })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请先输入正确的手机号',
        icon: 'none'
      })
      return
    }

    try {
      const result = await request('/auth/code', 'POST', {
        phone,
        purpose: 'reset'
      })
      this.setData({
        demoCode: result.demoCode || '',
        codePhone: phone,
        codeStudentId: studentId
      })
      wx.showModal({
        title: '演示验证码',
        content: result.demoCode ? `本次验证码：${result.demoCode}` : result.message,
        showCancel: false
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  // nextStep：目前用返回的演示码做界面初步校验后切到 step=2；真正身份与验证码校验在后端重置接口。
  nextStep() {
    const {
      studentId,
      phone,
      code
    }
    = this.data

    if (!studentId.trim()) {
      wx.showToast({
        title: '请输入学号',
        icon: 'none'
      })
      return
    }
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
    if (
    code !== this.data.demoCode ||
    phone !== this.data.codePhone ||
    studentId !== this.data.codeStudentId
    ) {
      wx.showToast({
        title: '验证码错误，请重新获取',
        icon: 'none'
      })
      return
    }
    this.setData({
      step: 2
    })
  },
  // onNewPasswordInput：保存新密码并按字母、数字、特殊字符计算强度提示；当前此页要求 8–10 位。
  onNewPasswordInput(e) {
    const password = e.detail.value
    let strength = 0

    if (password) {
      strength = 1

      const valid =
      password.length >= 8 &&
      password.length <= 10 &&
      /[A-Za-z]/.test(password) &&
      /\d/.test(password)

      if (valid) {
        strength = /[^A-Za-z0-9]/.test(password) ? 3 : 2
      }
    }

    this.setData({
      newPassword: password,
      passwordStrength: strength,
      strengthLabel: ['',
      '弱',
      '中',
      '强'][strength],
      strengthColor: ['',
      '#ef476f',
      '#f5a623',
      '#0bd565'][strength]
    })
  },

  // onConfirmPasswordInput：保存再次输入的新密码；提交时必须与第一次一致。
  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value
    })
  },

  // confirmReset：检查新密码一致性，POST /auth/reset-password；服务器最终验证账号、绑定手机号和单次验证码。
  async confirmReset() {
    const {
      newPassword,
      confirmPassword
    }
    = this.data

    if (
    newPassword.length < 8 ||
    newPassword.length > 10 ||
    !/[A-Za-z]/.test(newPassword) ||
    !/\d/.test(newPassword)
    ) {
      wx.showToast({
        title: '密码需8–10位且含字母和数字',
        icon: 'none'
      })
      return
    }

    if (newPassword !== confirmPassword) {
      wx.showToast({
        title: '两次输入的密码不一致',
        icon: 'none'
      })
      return
    }

    try {
      await request('/auth/reset-password', 'POST', {
        username: this.data.studentId.trim(),
        phone: this.data.phone,
        code: this.data.code.trim(),
        newPassword
      })
      wx.showModal({
        title: '密码已重置',
        content: '请使用新密码登录。',
        showCancel: false,
        success: () => wx.navigateBack({
          delta: 1
        })
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },
  // toggleNewPassword：切换新密码输入框的隐藏状态。
  toggleNewPassword() {
    this.setData({
      hideNewPassword: !this.data.hideNewPassword
    })
  },

  // toggleConfirmPassword：切换确认密码输入框的隐藏状态。
  toggleConfirmPassword() {
    this.setData({
      hideConfirmPassword: !this.data.hideConfirmPassword
    })
  }
})
