// 注册：收集账号、姓名与密码，提交给后端创建用户
const {
  request,
  clearSession,
  openPolicyDocument
}
= require('../../utils/api')

Page({
  // showPassword：密码是否以明文展示
  data: {
    username: '',
    displayName: '',
    password: '',
    confirmPassword: '',
    showPassword: false,
    showConfirmPassword: false,
    agreed: false,
    submitting: false
  },
  // onInput：根据输入框 data-field 动态更新对应字段，一个函数处理多个输入框。
  onInput(e) {
    this.setData({
      [e.currentTarget.dataset.field]: e.detail.value
    })
  },
  togglePassword(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [field]: !this.data[field]
    })
  },
  toggleAgreement() {
    this.setData({
      agreed: !this.data.agreed
    })
  },
  // openPolicy：注册前也可以查看完整的用户协议或隐私政策 PDF。
  async openPolicy(e) {
    wx.showLoading({ title: '正在打开' })
    try {
      await openPolicyDocument(e.currentTarget.dataset.type)
    } catch (error) {
      wx.showToast({
        title: error.message || '协议打开失败',
        icon: 'none'
      })
    } finally {
      wx.hideLoading()
    }
  },
  // submit：校验注册账号、密码、姓名/协议等，POST 注册接口；本页不保存注册返回的令牌，完成后回到登录。
  async submit() {
    if (this.data.submitting) return
    const {
      username,
      displayName,
      password,
      confirmPassword,
      agreed
    }
    = this.data
    if (!/^[A-Za-z0-9_-]{4,64}$/.test(username.trim())) {
      wx.showToast({
        title: '账号需为4位以上字母或数字',
        icon: 'none'
      }); return
    }
    if (!displayName.trim()) {
      wx.showToast({
        title: '请输入姓名或昵称',
        icon: 'none'
      }); return
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(password)) {
      wx.showToast({
        title: '密码至少8位，含字母和数字',
        icon: 'none'
      }); return
    }
    if (password !== confirmPassword) {
      wx.showToast({
        title: '两次密码不一致',
        icon: 'none'
      }); return
    }
    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      }); return
    }
    this.setData({
      submitting: true
    })
    try {
      await request('/auth/register', 'POST', {
        username: username.trim(),
        password,
        displayName: displayName.trim()
      })
      // 注册成功后返回登录页，让用户显式登录。
      clearSession()
      wx.showModal({
        title: '注册成功',
        content: '请使用新账号登录。',
        showCancel: false,
        success: () => wx.navigateBack()
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '注册失败',
        icon: 'none'
      })
    }
    finally {
      this.setData({
        submitting: false
      })
    }
  }
})
