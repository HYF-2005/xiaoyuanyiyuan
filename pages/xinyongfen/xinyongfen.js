// 信用分：展示后端依据订单记录计算的分数与变动原因
const {
  request
}
= require('../../utils/api')
const {
  creditRecord
}
= require('../../utils/api-format')

Page({
  // score：信用分展示值，随后被服务器结果覆盖
  // records：信用分变化记录
  data: {
    score: 61,
    records: []
  },
  // onShow：GET /me/credit，展示后端计算的分数和记录；初始 61 只是加载前占位。
  async onShow() {
    try {
      const summary = await request('/me/credit')
      this.setData({
        score: summary.score,
        records: (summary.records || []).map(creditRecord)
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取信用分失败',
        icon: 'none'
      })
    }
  }
})
