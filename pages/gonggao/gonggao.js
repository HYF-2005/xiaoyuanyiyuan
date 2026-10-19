// 公告中心：读取全部公告后在本地做未读/系统筛选，减少切换筛选时的网络请求。
const {
  request
}
= require('../../utils/api')
const {
  formatNoticeDate
}
= require('../../utils/notice-format')

Page({
  data: {
    // announcements：后端返回的全部公告，已补 dateText
    announcements: [],
    // filteredAnnouncements：当前筛选条件下真正渲染的公告
    filteredAnnouncements: [],
    // filter：all 全部 / unread 未读 / system 系统
    filter: 'all',
    // unreadCount：未读数量，同时用于“未读”筛选角标和“全部已读”状态
    unreadCount: 0,
    // summaryText：随筛选变化的小结，避免“未读”筛选时仍显示全部公告数量
    summaryText: '',
    // loading：首屏加载中，避免空列表先闪一下
    loading: true
  },

  // onShow：从详情页返回时重新读取，保证已读红点和未读数及时更新。
  onShow() {
    this.loadAnnouncements()
  },

  // loadAnnouncements：读取全部公告并补日期；接口失败时保留旧列表，只结束加载态。
  async loadAnnouncements() {
    this.setData({
      loading: true
    })
    try {
      const list = await request('/announcements?filter=all')
      const announcements = (Array.isArray(list) ? list : []).map(item => Object.assign({}, item, {
        dateText: formatNoticeDate(item.publishedAt)
      }))
      this.setData({
        announcements,
        unreadCount: announcements.filter(item => !item.read).length,
        loading: false
      })
      this.applyFilter()
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

  // applyFilter：未读只保留 read=false；系统按分类匹配“系统公告”。
  applyFilter() {
    const announcements = this.data.announcements
    const filter = this.data.filter
    let filteredAnnouncements = announcements
    if (filter === 'unread') {
      filteredAnnouncements = announcements.filter(item => !item.read)
    } else if (filter === 'system') {
      filteredAnnouncements = announcements.filter(item => item.category === '系统公告')
    }
    let summaryText = `共 ${announcements.length} 条公告`
    if (filter === 'unread') {
      summaryText = `共 ${filteredAnnouncements.length} 条未读公告`
    } else if (filter === 'system') {
      summaryText = `共 ${filteredAnnouncements.length} 条系统公告`
    } else if (this.data.unreadCount > 0) {
      summaryText += ` · ${this.data.unreadCount} 条未读`
    }
    this.setData({
      filteredAnnouncements,
      summaryText
    })
  },

  // switchFilter：切换本地筛选；点击当前项不重复 setData。
  switchFilter(e) {
    const filter = e.currentTarget.dataset.filter
    if (filter === this.data.filter) return
    this.setData({
      filter
    }, () => this.applyFilter())
  },

  // openDetail：只传公告 id，详情页自己请求完整正文，避免列表数据过期。
  openDetail(e) {
    wx.navigateTo({
      url: `/pages/gonggaoxiangqing/gonggaoxiangqing?id=${encodeURIComponent(e.currentTarget.dataset.id)}`
    })
  },

  // markAllRead：先调用后端批量写已读，成功后再把本地列表全部标为已读。
  async markAllRead() {
    if (this.data.unreadCount === 0) return
    wx.showLoading({
      title: '处理中',
      mask: true
    })
    try {
      const result = await request('/announcements/read-all', 'POST')
      wx.hideLoading()
      const unreadCount = Number(result && result.unreadCount) || 0
      const announcements = this.data.announcements.map(item => Object.assign({}, item, {
        read: true
      }))
      this.setData({
        announcements,
        unreadCount
      })
      this.applyFilter()
      wx.showToast({
        title: '已全部标记为已读',
        icon: 'success'
      })
      // 并发出现新公告时服务端未读数会大于 0，再拉一次列表把它补进来。
      if (unreadCount > 0) this.loadAnnouncements()
    } catch (error) {
      wx.hideLoading()
      wx.showToast({
        title: error.message || '操作失败，请稍后重试',
        icon: 'none'
      })
    }
  },

  // onPullDownRefresh：下拉时重新拉取公告，无论成功失败都结束下拉动画。
  async onPullDownRefresh() {
    try {
      await this.loadAnnouncements()
    } finally {
      wx.stopPullDownRefresh()
    }
  }
})
