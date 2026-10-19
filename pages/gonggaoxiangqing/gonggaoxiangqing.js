// 公告详情：按 id 读取完整正文，展示后写已读；已读写入失败不回滚页面展示。
const {
  request
}
= require('../../utils/api')
const {
  formatNoticeTime
}
= require('../../utils/notice-format')

Page({
  data: {
    // notice：当前公告，加载完成前为 null
    notice: null,
    // loading：控制加载中/不存在两种状态
    loading: true
  },

  // onLoad：只接收公告 id；不把标题、正文放在 URL 里，避免详情被旧参数污染。
  onLoad(options) {
    this.noticeId = options && options.id ? String(options.id) : ''
    if (!this.noticeId) {
      this.setData({
        loading: false
      })
      return
    }
    this.loadNotice()
  },

  // loadNotice：读取详情并补展示时间；网络失败时进入空状态，同时给出轻提示。
  async loadNotice() {
    try {
      const notice = await request(`/announcements/${encodeURIComponent(this.noticeId)}`)
      this.setData({
        notice: Object.assign({}, notice, {
          publishedText: formatNoticeTime(notice.publishedAt)
        }),
        loading: false
      })
      this.markRead()
    } catch (error) {
      this.setData({
        loading: false
      })
      wx.showToast({
        title: error.message || '读取公告失败',
        icon: 'none'
      })
    }
  },

  // markRead：幂等写入；即使失败也只记录日志，不打断用户阅读。
  async markRead() {
    try {
      await request(`/announcements/${encodeURIComponent(this.noticeId)}/read`, 'POST')
    } catch (error) {
      console.warn('标记公告已读失败', error)
    }
  },

  // goBack：正常返回上一页；如果是从分享/冷启动直接进入，则回到公告中心。
  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => wx.reLaunch({
        url: '/pages/gonggao/gonggao'
      })
    })
  }
})
