// 首页：待接订单市场、我的订单和个人中心共用这一个页面
const {
  request,
  token,
  logout
}
= require('../../utils/api')
const {
  orderView
}
= require('../../utils/api-format')
const {
  formatNoticeDateTime
}
= require('../../utils/notice-format')

Page({
  data: {
    // serviceType：首页市场订单类型：parcel 取件 / meal 取餐
    serviceType: 'parcel',
    // activeTab：home 首页 / orders 我的订单 / mine 我的；自定义导航状态
    activeTab: 'home',
    // orders：后端返回并格式化的本人订单
    orders: [],
    // filteredOrders：筛选和排序后实际展示的本人订单
    filteredOrders: [],
    // orderKeyword：我的订单搜索词
    orderKeyword: '',
    // orderType：我的订单区域当前类型
    orderType: 'parcel',
    // orderStatusFilter：未完成或全部
    orderStatusFilter: '未完成',
    // orderSort：near 截止近到远 / far 远到近
    orderSort: 'near',
    // publishTypeVisible：是否显示发布类型弹窗
    publishTypeVisible: false,
    // marketOrders：首页待接订单卡片数组
    marketOrders: [],
    // marketKeyword：首页市场搜索词
    marketKeyword: '',
    // creditScore：后端返回的信用分，初始值只是占位
    creditScore: 61,
    incomeTotal: '0.00',
    // displayName：用户展示姓名
    displayName: '',
    // username：用户账号
    username: '',
    // avatarInitial：姓名首字作为头像文字
    avatarInitial: '同',
    // banners：后端返回的当前有效轮播；图片和跳转都由后台配置
    banners: [],
    // bannerIndex：当前轮播下标，用于自定义指示点
    bannerIndex: 0,
    // latestNotice：最新一条公告，用于公告条和快捷打开详情
    latestNotice: null,
    // noticeBarText：公告条单行文案，加载中/空状态也由它承载
    noticeBarText: '公告加载中...',
    // hasUnreadNotice：控制公告中心入口的小红点
    hasUnreadNotice: false,
    // noticeModalVisible：登录后强提醒弹窗是否显示
    noticeModalVisible: false,
    // noticeModal：当前强制阅读的公告
    noticeModal: null,
    // noticeCountdown：剩余秒数，仅用于显示
    noticeCountdown: 5,
    // noticeCanClose：倒计时结束后才允许关闭
    noticeCanClose: false,
    // noticeProgress：倒计时进度条百分比
    noticeProgress: 0
  },

  // onLoad：读取路由 tab/type 参数；发布完成回来时可以直接定位到我的订单区域。
  onLoad(options) {
    // 每个页面实例只自动弹一次；关闭后不再连锁弹出下一条强提醒。
    this.noticePopupShown = false
    this.noticeDeadline = 0
    if (options && options.tab === 'orders') {
      this.setData({
        activeTab: 'orders',
        orderType: options.type === 'meal' ? 'meal' : 'parcel'
      })
      wx.setNavigationBarTitle({
        title: '我的订单'
      })
    }
  },

  // onShow：每次显示检查令牌并加载列表/个人数据；开启每 60 秒刷新，先清理旧定时器。
  onShow() {
    if (!token()) {
      wx.reLaunch({
        url: '/pages/denglu/denglu'
      }); return
    }
    wx.hideHomeButton()
    this.loadOrders()
    this.loadMarketOrders()
    this.loadAccountSummary()
    this.loadHomeContent()
    this.resumeNoticeCountdown()
    this.clearCountdownTimer()
    this.countdownTimer = setInterval(() => {
      this.loadOrders(); this.loadMarketOrders()
    }, 60000)
  },

  async onPullDownRefresh() {
    try {
      await Promise.all([this.loadOrders(), this.loadMarketOrders(), this.loadAccountSummary(), this.loadHomeContent()])
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  // loadAccountSummary：读取 /me，把后端信用分、收入和姓名写入 data，刷新个人中心显示。
  async loadAccountSummary() {
    try {
      const account = await request('/me')
      this.setData({
        creditScore: account.creditScore,
        incomeTotal: Number(account.incomeTotal).toFixed(2),
        displayName: account.displayName,
        username: account.username,
        avatarInitial: (account.displayName || '同').charAt(0)
      })
    } catch (error) {
      wx.showToast({
        title: '读取账户数据失败',
        icon: 'none'
      })
    }
  },

  // loadHomeContent：一次并发读取轮播和公告聚合；任一接口失败不拖垮另一个，也不打断订单列表。
  async loadHomeContent() {
    const [bannerResult, overviewResult] = await Promise.all([
      request('/banners').catch(error => {
        console.warn('读取轮播失败', error)
        return null
      }),
      request('/announcements/overview').catch(error => {
        console.warn('读取公告失败', error)
        return null
      })
    ])
    const banners = Array.isArray(bannerResult) ? bannerResult : this.data.banners
    if (!overviewResult) {
      this.setData({
        banners,
        noticeBarText: this.data.latestNotice ? this.data.latestNotice.title : '暂无公告'
      })
      return
    }
    const latestNotice = overviewResult.latest || null
    this.setData({
      banners,
      latestNotice,
      noticeBarText: latestNotice ? latestNotice.title : '暂无公告',
      hasUnreadNotice: Number(overviewResult.unreadCount) > 0
    })
    // 强提醒只在当前页面实例首次出现；关闭一条后不立即弹下一条。
    if (overviewResult.pending && !this.noticePopupShown) {
      this.noticePopupShown = true
      this.showNoticeModal(overviewResult.pending)
    }
  },

  // onHide：页面隐藏时停止计时器；保留截止时间，回来后可继续倒计时。
  onHide() {
    this.clearCountdownTimer()
    this.clearNoticeTimer()
  },

  // onUnload：页面卸载时清理所有计时器，防止离开后仍触发 setData。
  onUnload() {
    this.clearCountdownTimer()
    this.clearNoticeTimer()
  },

  // clearCountdownTimer：停止首页每分钟刷新订单的计时器；离开页面时清理，避免重复请求。
  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  // clearNoticeTimer：只停止每 200ms 的倒计时刷新，不清除 noticeDeadline。
  clearNoticeTimer() {
    if (this.noticeTimer) {
      clearInterval(this.noticeTimer)
      this.noticeTimer = null
    }
  },

  // startNoticeTimer：先立即刷新一次，再启动 200ms 定时器，让秒数和进度条平滑变化。
  startNoticeTimer() {
    this.clearNoticeTimer()
    this.updateNoticeCountdown()
    this.noticeTimer = setInterval(() => this.updateNoticeCountdown(), 200)
  },

  // updateNoticeCountdown：用绝对截止时间计算剩余量，避免切后台再回来时倒计时被暂停。
  updateNoticeCountdown() {
    if (!this.data.noticeModalVisible) {
      this.clearNoticeTimer()
      return
    }
    const remaining = Math.max(0, this.noticeDeadline - Date.now())
    const countdown = Math.ceil(remaining / 1000)
    const progress = Math.min(100, Math.max(0, Math.round((5000 - remaining) / 50)))
    if (remaining <= 0) {
      this.clearNoticeTimer()
      this.setData({
        noticeCountdown: 0,
        noticeCanClose: true,
        noticeProgress: 100
      })
      return
    }
    if (countdown !== this.data.noticeCountdown || progress !== this.data.noticeProgress) {
      this.setData({
        noticeCountdown: countdown,
        noticeCanClose: false,
        noticeProgress: progress
      })
    }
  },

  // resumeNoticeCountdown：从其他页面返回或切回前台时，沿用原截止时间继续倒计时。
  resumeNoticeCountdown() {
    if (!this.data.noticeModalVisible) return
    if (!this.noticeDeadline) this.noticeDeadline = Date.now() + this.data.noticeCountdown * 1000
    this.startNoticeTimer()
  },

  // showNoticeModal：展示强提醒卡片，并重置 5 秒强制阅读窗口。
  showNoticeModal(notice) {
    this.noticeDeadline = Date.now() + 5000
    this.setData({
      noticeModalVisible: true,
      noticeModal: Object.assign({}, notice, {
        publishedText: formatNoticeDateTime(notice.publishedAt)
      }),
      noticeCountdown: 5,
      noticeCanClose: false,
      noticeProgress: 0
    })
    this.startNoticeTimer()
  },

  // closeNoticeModal：倒计时未结束时点击无效；结束后关闭并把该公告标记已读。
  closeNoticeModal() {
    if (!this.data.noticeCanClose || !this.data.noticeModal) return
    const id = this.data.noticeModal.id
    this.clearNoticeTimer()
    this.noticeDeadline = 0
    this.setData({
      noticeModalVisible: false,
      noticeModal: null,
      noticeCanClose: false,
      noticeCountdown: 5,
      noticeProgress: 0
    })
    this.markNoticeRead(id)
  },

  // markNoticeRead：写已读后按服务端最新未读数更新红点；失败时保留红点，下次进入再同步。
  async markNoticeRead(id) {
    try {
      const result = await request(`/announcements/${encodeURIComponent(id)}/read`, 'POST')
      this.setData({
        hasUnreadNotice: Number(result.unreadCount) > 0
      })
    } catch (error) {
      console.warn('标记公告已读失败', error)
    }
  },

  // openLatestNotice：点公告条打开最新公告详情；没有公告时给出轻提示。
  openLatestNotice() {
    if (!this.data.latestNotice) {
      wx.showToast({
        title: '暂无公告',
        icon: 'none'
      })
      return
    }
    wx.navigateTo({
      url: `/pages/gonggaoxiangqing/gonggaoxiangqing?id=${encodeURIComponent(this.data.latestNotice.id)}`
    })
  },

  // openNoticeCenter：进入公告中心；点击事件用 catchtap 阻止冒泡到公告条。
  openNoticeCenter() {
    wx.navigateTo({
      url: '/pages/gonggao/gonggao'
    })
  },

  // onBannerChange：同步自定义指示点；swiper 自身负责轮播，不在这里改 current。
  onBannerChange(e) {
    this.setData({
      bannerIndex: Number(e.detail.current) || 0
    })
  },

  // onBannerImageError：单张海报加载失败时清空该图，WXML 自动切成同色兜底文案。
  onBannerImageError(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (!Number.isInteger(index) || !this.data.banners[index]) return
    this.setData({
      [`banners[${index}].imageUrl`]: ''
    })
  },

  // onBannerTap：按后台 linkType 分流；只允许站内 /pages 路径，避免任意路由注入。
  onBannerTap(e) {
    const index = Number(e.currentTarget.dataset.index)
    const banner = this.data.banners[index]
    if (!banner) return
    const linkType = banner.linkType
    const linkValue = banner.linkValue
    if (linkType === 'notice' && linkValue) {
      wx.navigateTo({
        url: `/pages/gonggaoxiangqing/gonggaoxiangqing?id=${encodeURIComponent(linkValue)}`
      })
      return
    }
    if (linkType === 'page' && linkValue && String(linkValue).indexOf('/pages/') === 0) {
      wx.navigateTo({
        url: linkValue
      })
      return
    }
    if (linkType === 'miniProgram' && linkValue) {
      wx.navigateToMiniProgram({
        appId: linkValue,
        fail: () => wx.showToast({
          title: '小程序跳转失败',
          icon: 'none'
        })
      })
      return
    }
    if (linkType === 'web') {
      wx.showToast({
        title: '网页跳转需先配置业务域名',
        icon: 'none'
      })
    }
  },

  // loadOrders：读取本人发布/接取的订单，再按关键词、状态、截止时间筛选排序；切换类型后丢弃旧类型响应。
  async loadOrders() {
    const type = this.data.orderType
    try {
      const orders = (await request(`/orders/mine?type=${type}`)).map(orderView)
      if (type !== this.data.orderType) return
      const keyword = this.data.orderKeyword.trim()
      const statusFilter = this.data.orderStatusFilter

      const filteredOrders = orders
      .filter(order => {
        const matchesKeyword =
        !keyword ||
        (order.title || '').includes(keyword) ||
        (order.station || '').includes(keyword) ||
        (order.restaurant || '').includes(keyword) ||
        (order.deliveryBuilding || '').includes(keyword)
        const matchesStatus =
        statusFilter === '全部' || (order.status !== '已完成' && order.status !== '已取消')

        return matchesKeyword && matchesStatus
      })
      .sort((a, b) => {
        const aTime = `${a.deadlineDate || '9999-12-31'} ${a.deadlineTime || '23:59'}`
        const bTime = `${b.deadlineDate || '9999-12-31'} ${b.deadlineTime || '23:59'}`
        const comparison = aTime.localeCompare(bTime)
        return this.data.orderSort === 'near' ? comparison : -comparison
      })
      .map(order => ({
        ...order,
        remainingText: this.formatRemaining(order)
      }))

      this.setData({
        orders,
        filteredOrders
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取订单失败',
        icon: 'none'
      })
    }
  },

  // loadMarketOrders：读取指定类型的待接订单；过滤关键词并补剩余时间，切换类型后不让旧响应覆盖新列表。
  async loadMarketOrders() {
    const type = this.data.serviceType
    try {
      const orders = (await request(`/orders/market?type=${type}`)).map(orderView)
      if (type !== this.data.serviceType) return
      const keyword = this.data.marketKeyword.trim()
      const marketOrders = orders.filter(order => order.status === '待接单')
      .filter(order => !keyword || [order.title, order.station, order.restaurant, order.deliveryBuilding].some(value => (value || '').includes(keyword)))
      .map(order => Object.assign({}, order, {
        remainingText: this.formatRemaining(order)
      }))
      this.setData({
        marketOrders
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取待接订单失败',
        icon: 'none'
      })
    }
  },

  // onMarketSearch：保存首页搜索词后重新读取待接订单。
  onMarketSearch(e) {
    this.setData({
      marketKeyword: e.detail.value
    })
    this.loadMarketOrders()
  },

  // openMarketDetail：读取卡片 data-id，按取件/取餐类型进入详情；id 经 URL 编码传递。
  openMarketDetail(e) {
    const page = this.data.serviceType === 'meal' ? 'canxiangqing/canxiangqing' : 'dingdanxiangqing/dingdanxiangqing'
    wx.navigateTo({
      url: `/pages/${page}?id=${encodeURIComponent(e.currentTarget.dataset.id)}`
    })
  },
  // formatRemaining：用截止时间减当前时间换算天/时/分；已完成、已取消、待确认不再展示倒计时。
  formatRemaining(order) {
    if (order.status === '已取消' || order.status === '已完成' || order.status === '待确认') return ''
    if (!order.deadlineDate) return '截止时间未设置'

    const dateParts = order.deadlineDate.split('-').map(Number)
    const timeParts = (order.deadlineTime || '23:59').split(':').map(Number)
    const deadline = new Date(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1])
    const remainingMinutes = Math.ceil((deadline.getTime() - Date.now()) / 60000)

    if (!Number.isFinite(remainingMinutes)) return '截止时间无效'
    if (remainingMinutes <= 0) return '已截止'

    const days = Math.floor(remainingMinutes / 1440)
    const hours = Math.floor((remainingMinutes % 1440) / 60)
    const minutes = remainingMinutes % 60

    if (!days && !hours) return `截止剩余 ${minutes}分钟`
    return `截止剩余 ${days ? `${days}天 ` : ''}${hours}时 ${minutes}分`
  },
  // onOrderSearch：保存我的订单搜索词后重新计算列表。
  onOrderSearch(e) {
    this.setData({
      orderKeyword: e.detail.value
    })
    this.loadOrders()
  },

  // chooseOrderStatus：让用户选择未完成或全部，然后刷新我的订单筛选结果。
  chooseOrderStatus() {
    wx.showActionSheet({
      itemList: ['未完成',
      '全部'],
      success: res => {
        const filter = ['未完成',
        '全部'][res.tapIndex]
        this.setData({
          orderStatusFilter: filter
        })
        this.loadOrders()
      }
    })
  },
  // chooseOrderSort：切换截止时间由近到远/由远到近；只是排序，不改变订单状态。
  chooseOrderSort() {
    wx.showActionSheet({
      itemList: ['截止时间从近到远',
      '截止时间从远到近'],
      success: res => {
        this.setData({
          orderSort: res.tapIndex === 0 ? 'near' : 'far'
        })
        this.loadOrders()
      }
    })
  },
  // switchService：修改首页市场的 parcel/meal 类型并加载对应订单。
  switchService(e) {
    this.setData({
      serviceType: e.currentTarget.dataset.type
    })
    this.loadMarketOrders()
  },
  // switchOrderType：修改我的订单区域的 parcel/meal 类型并加载本人订单。
  switchOrderType(e) {
    this.setData({
      orderType: e.currentTarget.dataset.type
    })
    this.loadOrders()
  },

  // showHome：把 activeTab 改为 home 并刷新市场；这里是自定义导航，不是原生 tabBar。
  showHome() {
    this.setData({
      activeTab: 'home'
    })
    this.loadMarketOrders()
    this.loadHomeContent()
    wx.setNavigationBarTitle({
      title: '首页'
    })
  },

  // showOrders：把 activeTab 改为 orders，显示我的订单并更新标题。
  showOrders() {
    this.loadOrders()
    this.setData({
      activeTab: 'orders'
    })
    wx.setNavigationBarTitle({
      title: '我的订单'
    })
  },
  // openOrderDetail：携带所点击订单的 id 打开对应类型详情，而不是传整个订单对象。
  openOrderDetail(e) {
    const id = e.currentTarget.dataset.id
    const page = this.data.orderType === 'meal' ? 'canxiangqing/canxiangqing' : 'dingdanxiangqing/dingdanxiangqing'
    wx.navigateTo({
      url: `/pages/${page}?id=${encodeURIComponent(id)}`
    })
  },
  // showMine：把 activeTab 改为 mine，读取账户汇总显示个人中心。
  showMine() {
    this.loadAccountSummary()
    this.setData({
      activeTab: 'mine'
    })
    wx.setNavigationBarTitle({
      title: '我的'
    })
  },
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
  },
  // goToCredit：导航到信用分记录页。
  goToCredit() {
    wx.navigateTo({
      url: '/pages/xinyongfen/xinyongfen'
    })
  },
  // goToTransactions：导航到交易明细页。
  goToTransactions() {
    wx.navigateTo({
      url: '/pages/jiaoyi/jiaoyi'
    })
  },
  // goToSettings：导航到账户设置页。
  goToSettings() {
    wx.navigateTo({
      url: '/pages/shezhi/shezhi'
    })
  },
  // goToFeedback：导航到问题反馈页。
  goToFeedback() {
    wx.navigateTo({
      url: '/pages/fankui/fankui'
    })
  },
  // goToPublish：显示发布类型弹窗，此时还没有创建订单。
  goToPublish() {
    this.setData({
      publishTypeVisible: true
    })
  },
  // closePublishType：关闭发布类型弹窗。
  closePublishType() {
    this.setData({
      publishTypeVisible: false
    })
  },
  noop() {},
  // choosePublishType：根据按钮 data-type 打开取件或取餐填写页。
  choosePublishType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      publishTypeVisible: false,
      serviceType: type
    })
    const page = type === 'meal' ? 'canfabu/canfabu' : 'fabu/fabu'
    wx.navigateTo({
      url: `/pages/${page}`
    })
  }
})
