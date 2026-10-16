// 启动日志：读取本机的小程序启动记录；不是后端服务器日志
// logs.js
const util = require('../../utils/util.js')

Page({
  data: {
    // logs：本机缓存中的启动时间列表
    logs: []
  },
  // onLoad：读取 app.js 存在微信缓存 logs 的启动时间，格式化为列表。
  onLoad() {
    this.setData({
      logs: (wx.getStorageSync('logs') || []).map(log => {
        return {
          date: util.formatTime(new Date(log)),
          timeStamp: log
        }
      })
    })
  }
})
