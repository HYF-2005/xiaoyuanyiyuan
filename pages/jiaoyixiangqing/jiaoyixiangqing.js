// 账单详情：展示选中账单的金额、时间和订单编号
const {
  request
}
= require('../../utils/api')
const {
  transactionView
}
= require('../../utils/api-format')
Page({
  // transaction：当前选中账单，没有查到时为 null
  data: {
    transaction: null
  },
  // onLoad：读取所有本人账单，格式化后按 options.id 查出所选记录。
  async onLoad(options) {
    try {
      const transaction = (await request('/me/transactions')).map(transactionView).find(item => item.id === String(options.id || ''))
      if (!transaction) {
        wx.showToast({
          title: '账单不存在',
          icon: 'none'
        }); return
      }
      this.setData({
        transaction
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取账单失败',
        icon: 'none'
      })
    }
  },
  // copyOrderNumber：把当前账单的订单编号复制到系统剪贴板。
  copyOrderNumber() {
    if (!this.data.transaction) return
    wx.setClipboardData({
      data: this.data.transaction.orderNumber,
      success: () => wx.showToast({
        title: '复制成功',
        icon: 'success'
      }),
      fail: () => wx.showToast({
        title: '复制失败，请重试',
        icon: 'none'
      })
    })
  }
})
