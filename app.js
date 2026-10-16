// 小程序入口；保存本机启动日志。wx.login 目前只是模板调用，没有换取后端登录令牌。
// 阅读顺序：初始数据 → 生命周期 → 点击/输入事件 → 请求结果写回界面。
// app.js
// App 注册全局小程序实例，不是某个具体页面。
App({
  // onLaunch：小程序启动时记录 Date.now() 到本机 logs；wx.login 暂未与后端令牌体系连接。
  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    // 写入微信本机缓存；不同设备缓存不共享，不能把它讲成 MySQL 持久化。
    wx.setStorageSync('logs', logs)

    // 登录
    wx.login({
      success: res => {
        // 发送 res.code 到后台换取 openId, sessionKey, unionId
      }
    })
  },
  globalData: {
    // userInfo：模板预留全局用户信息；不是后端会话
    userInfo: null
  }
})
