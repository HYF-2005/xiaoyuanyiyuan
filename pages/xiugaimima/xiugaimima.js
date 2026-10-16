// 修改密码：旧密码校验、新密码强度提示与修改后重新登录
const {
  request,
  clearSession
}
= require('../../utils/api')

Page({
  data: {
    // oldPassword：输入的旧密码，由服务器匹配存储的 BCrypt 哈希
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    // showOldPassword：旧密码是否以明文展示
    showOldPassword: false,
    // showNewPassword：新密码是否以明文展示
    showNewPassword: false,
    // showConfirmPassword：确认密码是否以明文展示
    showConfirmPassword: false,
    strengthLevel: 0,
    // strengthText：密码强度说明文字
    strengthText: ''
  },

  onPasswordInput(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value

    if (field !== 'newPassword') {
      this.setData({
        [field]: value
      })
      return
    }

    let strengthLevel = 0
    let strengthText = ''

    if (value) {
      strengthLevel = 1
      strengthText = '弱'

      if (value.length >= 8 && value.length <= 10 &&
      /[A-Za-z]/.test(value) && /\d/.test(value)) {
        strengthLevel = 2
        strengthText = '中'

        if (/[^A-Za-z0-9]/.test(value)) {
          strengthLevel = 3
          strengthText = '强'
        }
      }
    }

    this.setData({
      newPassword: value,
      strengthLevel,
      strengthText
    })
  },
  togglePassword(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [field]: !this.data[field]
    })
  },
  // submitChange：校验旧/新/确认密码，POST /me/password；成功后清除旧令牌，要求重新登录。
  async submitChange() {
    const {
      oldPassword,
      newPassword,
      confirmPassword
    }
    = this.data
    let message = ''

    if (!oldPassword) {
      message = '请输入旧密码'
    } else if (!newPassword) {
      message = '请输入新密码'
    } else if (!confirmPassword) {
      message = '请再次输入新密码'
    } else if (
    newPassword.length < 8 ||
    newPassword.length > 10 ||
    !/[A-Za-z]/.test(newPassword) ||
    !/\d/.test(newPassword)
    ) {
      message = '新密码需为8-10位字母和数字组合'
    } else if (newPassword !== confirmPassword) {
      message = '两次输入的密码不一致'
    }

    if (message) {
      wx.showToast({
        title: message,
        icon: 'none'
      })
      return
    }

    try {
      await request('/me/password', 'POST', {
        oldPassword,
        newPassword
      })
      clearSession()
      wx.showModal({
        title: '密码已修改',
        content: '请使用新密码重新登录。',
        showCancel: false,
        confirmText: '返回登录',
        success: () => wx.reLaunch({
          url: '/pages/denglu/denglu'
        })
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  }
})
