// 注销账户：确认协议和密码；后端停用账号并保留历史记录
const {
  request,
  clearSession
}
= require('../../utils/api')

Page({
  data: {
    agreed: false,
    password: '',
    showPassword: false,
    submitting: false
  },

  toggleAgreement() {
    this.setData({
      agreed: !this.data.agreed
    })
  },

  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    })
  },
  togglePassword() {
    this.setData({
      showPassword: !this.data.showPassword
    })
  },
  // submitCancel：检查协议和密码，再二次确认并请求停用账号；清除会话，历史订单不会被物理删除。
  submitCancel() {
    if (!this.data.agreed || this.data.submitting) return
    if (!this.data.password) {
      wx.showToast({
        title: '请输入当前密码',
        icon: 'none'
      }); return
    }
    wx.showModal({
      title: '确认注销账号',
      content: '注销后无法再次登录，历史订单保留用于审计。确定继续吗？',
      success: async result => {
        if (!result.confirm) return
        this.setData({
          submitting: true
        })
        try {
          await request('/me/deactivate', 'POST', {
            password: this.data.password
          })
          clearSession()
          wx.showModal({
            title: '注销成功',
            content: '账号已停用。',
            showCancel: false,
            success: () => wx.reLaunch({
              url: '/pages/denglu/denglu'
            })
          })
        } catch (error) {
          wx.showToast({
            title: error.message || '注销失败',
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
  }
})
