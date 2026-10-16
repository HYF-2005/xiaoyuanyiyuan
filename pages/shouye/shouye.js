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
    avatarInitial: '同'
  },

  // onLoad：读取路由 tab/type 参数；发布完成回来时可以直接定位到我的订单区域。
  onLoad(options) {
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
    this.clearCountdownTimer()
    this.countdownTimer = setInterval(() => {
      this.loadOrders(); this.loadMarketOrders()
    }, 60000)
  },

  async onPullDownRefresh() {
    try {
      await Promise.all([this.loadOrders(), this.loadMarketOrders(), this.loadAccountSummary()])
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

  // onHide：页面隐藏/销毁时清理计时器，防止离开后仍定时请求。
  onHide() {
    this.clearCountdownTimer()
  },

  // onUnload：页面隐藏/销毁时清理计时器，防止离开后仍定时请求。
  onUnload() {
    this.clearCountdownTimer()
  },

  // clearCountdownTimer：停止首页每分钟刷新订单的计时器；离开页面时清理，避免重复请求。
  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
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
