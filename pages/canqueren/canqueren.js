// 取餐发布确认：展示餐品草稿并调用后端创建订单
const {
  returnToOrders
}
= require('../../utils/publish-navigation')
const {
  request
}
= require('../../utils/api')

Page({
  data: {
    // draft：上一页通过 eventChannel 传来的未发布草稿
    draft: null,
    // agreementChecked：是否勾选需求协议
    agreementChecked: true,
    // published：本次草稿是否已经发布成功
    published: false,
    // publishing：发布请求是否进行中，防止重复点击
    publishing: false
  },

  // onLoad：监听 mealDraft 事件接收上一页草稿；草稿不是数据库中已经发布的订单。
  onLoad() {
    this.getOpenerEventChannel().on('mealDraft', draft => {
      this.setData({
        draft
      })
    })
  },

  // toggleAgreement：切换是否勾选协议；未勾选时提交函数会提示并终止。
  toggleAgreement() {
    this.setData({
      agreementChecked: !this.data.agreementChecked
    })
  },

  // publish：检查草稿与协议，防止重复点击，POST /orders 创建取餐订单；成功后返回我的订单。
  async publish() {
    if (this.data.published || this.data.publishing) return
    if (!this.data.draft) {
      wx.showToast({
        title: '订单数据缺失',
        icon: 'none'
      })
      return
    }
    if (!this.data.agreementChecked) {
      wx.showToast({
        title: '请先同意协议',
        icon: 'none'
      })
      return
    }

    this.setData({
      publishing: true
    })
    try {
      const draft = this.data.draft
      await request('/orders', 'POST', {
        ...draft,
        deadlineDate: draft.deadlineDate || null,
        deadlineTime: draft.deadlineTime || null,
        reward: Number(draft.reward)
      })
      this.setData({
        published: true,
        publishing: false
      })
      wx.showToast({
        title: '发布成功',
        icon: 'success'
      })
      this.returnTimer = setTimeout(() => returnToOrders('meal'), 900)
    } catch (error) {
      this.setData({
        publishing: false
      })
      wx.showToast({
        title: error.message || '发布失败，请重试',
        icon: 'none'
      })
    }
  },
  // onUnload：清理延迟返回计时器，防止卸载后触发二次导航。
  onUnload() {
    if (this.returnTimer) clearTimeout(this.returnTimer)
  }
})
