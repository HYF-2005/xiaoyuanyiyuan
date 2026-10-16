// 账户设置：安全页面入口、协议弹窗与退出登录
const {
  request,
  logout,
  openPolicyDocument
}
= require('../../utils/api')

Page({
  data: {
    // boundPhone：账户设置里显示的已绑定手机号
    boundPhone: '',
    showAbout: false
  },

  // onShow：GET /me，刷新绑定手机号显示；页面 data 并不是数据库本身。
  async onShow() {
    try {
      const account = await request('/me')
      this.setData({
        boundPhone: account.phone || ''
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取资料失败',
        icon: 'none'
      })
    }
  },
  // goToChangePassword：打开修改密码页。
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/xiugaimima/xiugaimima'
    })
  },
  // goToChangePhone：打开绑定手机号页。
  goToChangePhone() {
    wx.navigateTo({
      url: '/pages/xiugaishouji/xiugaishouji'
    })
  },
  // goToCancelAccount：打开注销账户页，尚未实际注销。
  goToCancelAccount() {
    wx.navigateTo({
      url: '/pages/zhuxiaozhanghao/zhuxiaozhanghao'
    })
  },
  // openAbout：显示关于信息弹窗。
  openAbout() {
    this.setData({
      showAbout: true
    })
  },

  // closeAbout：关闭关于信息弹窗。
  closeAbout() {
    this.setData({
      showAbout: false
    })
  },
  // openPolicy：下载并打开完整的用户协议或隐私政策 PDF。
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
  noop() {},
  // logout：询问后调用后端注销当前会话，清除本地令牌并回到登录页；不是注销账号。
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: async res => {
        if (res.confirm) {
          try {
            await logout()
          } catch (_) {}
          wx.reLaunch({
            url: '/pages/denglu/denglu'
          })
        }
      }
    })
  }
})
