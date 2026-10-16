// 取餐订单详情：订单时间线、照片凭证与角色相关操作
const {
  chooseCompletionPhoto
}
= require('../../utils/order-photo')
const {
  request,
  uploadProof,
  downloadProof
}
= require('../../utils/api')
const {
  orderView
}
= require('../../utils/api-format')
const pad = number => String(number).padStart(2, '0')

// formatDateTime：将接口日期转成 MM-DD HH:mm，空值显示占位文本。
function formatDateTime(value) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return '时间未记录'
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

Page({
  data: {
    // orderId：路由传入的订单编号
    orderId: '',
    // order：本页当前读取的后端订单
    order: null,
    // publishedAtText：订单发布时间
    publishedAtText: '',
    // acceptedAtText：接单时间
    acceptedAtText: '',
    // submittedAtText：提交照片时间
    submittedAtText: '',
    // completedAtText：确认完成时间
    completedAtText: '',
    // cancelledAtText：取消时间
    cancelledAtText: '',
    // deadlineText：截止时间展示字符串
    deadlineText: '',
    // displayId：界面展示的 UO 前缀编号，真实 API 使用 orderId
    displayId: '',
    // cancelVisible：取消订单弹窗开关
    cancelVisible: false,
    // completionBusy：照片上传或完成确认请求锁，同时控制 loading/disabled
    completionBusy: false,
    // confirmVisible：完成确认弹窗开关
    confirmVisible: false,
    // acceptVisible：接单确认弹窗开关
    acceptVisible: false,
    // role：后端返回的 publisher 发布者 / runner 接单者 / visitor 非参与者
    role: 'visitor',
    // isRunner：当前用户是否是本单接单者
    isRunner: false
  },

  // onLoad：保存路由 options.id，供详情、上传照片和状态变更请求使用。
  onLoad(options) {
    this.setData({
      orderId: String(options.id || '')
    })
  },

  // onShow：页面每次显示都会执行；此页重新读取数据，保证返回页面后显示最新内容。
  onShow() {
    this.loadOrder()
  },

  // onPullDownRefresh：用户下拉触发：重新读取后端数据，finally 停止刷新动画；需要页面 JSON 开启下拉刷新。
  async onPullDownRefresh() {
    try {
      await this.loadOrder()
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  // loadOrder：按 orderId 请求最新订单；后端返回 myRole，再映射成详情页时间、数量和按钮状态。
  async loadOrder() {
    try {
      const order = orderView(await request(`/orders/${this.data.orderId}`))
      const role = order.myRole
      this.setData({
        order,
        displayId: `UO${order.id}`,
        publishedAtText: formatDateTime(order.createdAt),
        acceptedAtText: order.acceptedAt ? formatDateTime(order.acceptedAt) : '已接单',
        submittedAtText: order.submittedAt ? formatDateTime(order.submittedAt) : '已提交照片',
        completedAtText: order.completedAt ? formatDateTime(order.completedAt) : '已确认',
        role,
        isRunner: role === 'runner',
        cancelledAtText: order.cancelledAt ? formatDateTime(order.cancelledAt) : '',
        deadlineText: order.deadlineDate ? `${order.deadlineDate.slice(5)} ${order.deadlineTime || '23:59'}` : '未设置'
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取订单失败',
        icon: 'none'
      })
    }
  },

  // openAccept：只为 visitor 且待接单的订单显示接单确认弹窗；真正权限仍由后端校验。
  openAccept() {
    if (this.data.order && this.data.order.status === '待接单' && this.data.role === 'visitor') {
      this.setData({
        acceptVisible: true
      })
    }
  },

  // closeAccept：关闭接单确认弹窗。
  closeAccept() {
    this.setData({
      acceptVisible: false
    })
  },

  // confirmAccept：POST 当前订单的 /accept；成功或失败后重读订单，防止多人抢单导致旧状态残留。
  async confirmAccept() {
    if (!this.data.acceptVisible) return
    try {
      await request(`/orders/${this.data.orderId}/accept`, 'POST')
      this.setData({
        acceptVisible: false
      })
      await this.loadOrder()
      wx.showToast({
        title: '接单成功',
        icon: 'success'
      })
    } catch (error) {
      this.setData({
        acceptVisible: false
      })
      await this.loadOrder()
      wx.showToast({
        title: error.message || '接单失败',
        icon: 'none'
      })
    }
  },

  // takeCompletionPhoto：仅接单者在已接单时选/拍照片；上传 /proof 后，后端把订单改为待确认。
  takeCompletionPhoto() {
    if (!this.data.order || this.data.order.status !== '已接单' || !this.data.isRunner || this.data.completionBusy) return
    chooseCompletionPhoto(async path => {
      this.setData({
        completionBusy: true
      })
      try {
        await uploadProof(this.data.orderId, path)
        this.setData({
          completionBusy: false
        })
        await this.loadOrder()
        wx.showToast({
          title: '已提交，等待确认',
          icon: 'success'
        })
      } catch (error) {
        this.setData({
          completionBusy: false
        })
        await this.loadOrder()
        wx.showToast({
          title: error.message || '上传失败',
          icon: 'none'
        })
      }
    })
  },

  // previewCompletionPhoto：携带登录令牌下载后端照片，再调用微信预览；不是公开静态图片地址。
  async previewCompletionPhoto() {
    if (!this.data.order || !this.data.order.hasCompletionPhoto) return
    try {
      const path = await downloadProof(this.data.orderId)
      wx.previewImage({
        current: path,
        urls: [path]
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  // contactOther：按当前身份选择对方手机号并拨号；号码是否能返回由服务器控制。
  contactOther() {
    const order = this.data.order
    const phone = order && (this.data.isRunner ? order.publisherPhone : order.runnerPhone)
    if (phone) wx.makePhoneCall({
      phoneNumber: String(phone)
    })
    else wx.showToast({
      title: '暂无对方联系方式',
      icon: 'none'
    })
  },

  // openConfirm：仅发布者在待确认且有凭证时打开确认完成弹窗。
  openConfirm() {
    if (this.data.order && this.data.order.status === '待确认' && this.data.role === 'publisher' && this.data.order.hasCompletionPhoto) {
      this.setData({
        confirmVisible: true
      })
    }
  },

  // closeConfirm：关闭确认完成弹窗。
  closeConfirm() {
    this.setData({
      confirmVisible: false
    })
  },

  // confirmOrder：POST /confirm 由发布者确认完成；请求锁防重复，信用分/收入由后端订单状态推导。
  async confirmOrder() {
    if (!this.data.confirmVisible || this.data.completionBusy) return
    this.setData({
      completionBusy: true
    })
    try {
      await request(`/orders/${this.data.orderId}/confirm`, 'POST')
      this.setData({
        confirmVisible: false,
        completionBusy: false
      })
      await this.loadOrder()
      wx.showToast({
        title: '订单已完成',
        icon: 'success'
      })
    } catch (error) {
      this.setData({
        confirmVisible: false,
        completionBusy: false
      })
      await this.loadOrder()
      wx.showToast({
        title: error.message || '确认失败，请重试',
        icon: 'none'
      })
    }
  },

  // openCancel：在允许的身份/状态下打开取餐取消弹窗，接单者取消会扣信用分。
  openCancel() {
    if (this.data.order && ((this.data.order.status === '待接单' && this.data.role === 'publisher') || (this.data.order.status === '已接单' && this.data.role === 'runner'))) {
      this.setData({
        cancelVisible: true
      })
    }
  },

  // closeCancel：关闭取餐取消弹窗。
  closeCancel() {
    this.setData({
      cancelVisible: false
    })
  },

  noop() {},

  // confirmCancel：POST /cancel 让后端按身份与状态取消取餐订单，然后刷新详情。
  async confirmCancel() {
    this.setData({
      cancelVisible: false
    })
    try {
      await request(`/orders/${this.data.orderId}/cancel`, 'POST')
      await this.loadOrder()
      wx.showToast({
        title: '订单已取消',
        icon: 'success'
      })
    } catch (error) {
      await this.loadOrder()
      wx.showToast({
        title: error.message || '取消失败，请重试',
        icon: 'none'
      })
    }
  }
})
