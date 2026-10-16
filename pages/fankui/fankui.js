// 问题反馈：提交文字意见并防止重复提交
const {
  request
}
= require('../../utils/api')

Page({
  data: {
    // content：用户输入的反馈正文
    content: '',
    // submitting：提交请求锁，防止多次创建/反馈/注销
    submitting: false
  },

  // onContentInput：保存反馈输入内容。
  onContentInput(e) {
    this.setData({
      content: e.detail.value
    })
  },

  // cancelFeedback：返回上一页，不提交反馈。
  cancelFeedback() {
    wx.navigateBack()
  },

  // submitFeedback：检查反馈字数并锁住按钮，POST /feedback 写数据库；成功后返回。
  async submitFeedback() {
    if (this.data.submitting) return
    const content = this.data.content.trim()

    if (content.length < 10) {
      wx.showToast({
        title: '请至少输入10个字',
        icon: 'none'
      })
      return
    }
    this.setData({
      submitting: true
    })
    try {
      await request('/feedback', 'POST', {
        content
      })
      wx.showToast({
        title: '提交成功',
        icon: 'success'
      })
      setTimeout(() => wx.navigateBack(), 1200)
    } catch (error) {
      this.setData({
        submitting: false
      })
      wx.showToast({
        title: error.message || '提交失败，请重试',
        icon: 'none'
      })
    }
  }
})
