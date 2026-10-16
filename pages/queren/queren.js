// 取件发布确认：展示上一页的草稿，勾选协议后提交后端
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
    draft: null,
    agreementChecked: true,
    published: false,
    publishing: false
  },

  // onLoad：通过 getOpenerEventChannel 接收 publishDraft 草稿，并统计标题；刷新单独打开本页可能没有草稿。
  onLoad() {
    const eventChannel = this.getOpenerEventChannel()
    eventChannel.on('publishDraft', draft => {
      const title = ['小件',
      '中件',
      '大件']
      .map(size => {
        const count = draft.packages.filter(item => item.size === size).length
        return count > 0 ? `${size}${count}个` : ''
      })
      .filter(Boolean)
      .join('、')

      this.setData({
        draft,
        title
      })
    })
  },

  toggleAgreement() {
    this.setData({
      agreementChecked: !this.data.agreementChecked
    })
  },

  // onConfirmPublish：检查草稿与协议，POST /orders 创建取件订单；锁住发布按钮，成功后延时返回我的订单。
  async onConfirmPublish() {
    if (this.data.published || this.data.publishing) return

    if (!this.data.agreementChecked) {
      wx.showToast({
        title: '请先同意协议',
        icon: 'none'
      })
      return
    }

    if (!this.data.draft) {
      wx.showToast({
        title: '订单数据缺失',
        icon: 'none'
      })
      return
    }

    this.setData({
      publishing: true
    })
    try {
      await request('/orders', 'POST', {
        type: 'parcel',
        ...this.data.draft,
        reward: Number(this.data.draft.reward)
      })
      this.setData({
        published: true,
        publishing: false
      })
      wx.showToast({
        title: '发布成功',
        icon: 'success',
        duration: 1500
      })
      this.returnTimer = setTimeout(() => returnToOrders('parcel'), 1500)
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
  // onUnload：清理发布后的延迟返回任务，避免已经离开页面还发生跳转。
  onUnload() {
    if (this.returnTimer) clearTimeout(this.returnTimer)
  }
})
