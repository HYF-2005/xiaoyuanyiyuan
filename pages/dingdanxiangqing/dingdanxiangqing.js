// 取件订单详情：按身份和状态显示接单、拍照、确认与取消操作
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

function formatDateTime(value) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return '时间未记录'
  const pad = number => String(number).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

Page({
  data: {
    orderId: '',
    order: null,
    publishedAtText: '',
    acceptedAtText: '',
    submittedAtText: '',
    completedAtText: '',
    deadlineText: '',
    // packageCount：包裹总件数
    packageCount: 0,
    displayId: '',
    completionBusy: false,
    confirmVisible: false,
    acceptVisible: false,
    role: 'visitor',
    isRunner: false,
    // actorLabel：详情页参与者字段标签
    actorLabel: '',
    // actorName：详情页参与者展示姓名
    actorName: ''
  },

  // onLoad：保存路由 options.id；显示页面后按该编号读取最新订单。
  onLoad(options) {
    this.setData({
      orderId: String(options.id || '')
    })
  },

  onShow() {
    this.loadOrder()
  },

  async onPullDownRefresh() {
    try {
      await this.loadOrder()
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  async loadOrder() {
    try {
      const order = orderView(await request(`/orders/${this.data.orderId}`))

      const deadlineDate = order.deadlineDate || ''
      const deadlineTime = order.deadlineTime || '23:59'
      const role = order.myRole
      const showRunner = role === 'publisher' && order.status !== '待接单'
      this.setData({
        order,
        displayId: `UO${order.id}`,
        packageCount: order.packageCount || (Array.isArray(order.packages) ? order.packages.length : 0),
        publishedAtText: formatDateTime(order.createdAt),
        acceptedAtText: order.acceptedAt ? formatDateTime(order.acceptedAt) : '已接单',
        submittedAtText: order.submittedAt ? formatDateTime(order.submittedAt) : '已提交照片',
        completedAtText: order.completedAt ? formatDateTime(order.completedAt) : '已确认',
        role,
        isRunner: role === 'runner',
        actorLabel: showRunner ? '接单者：' : '发布者：',
        actorName: showRunner ? (order.runnerName || '对方') : (order.publisherName || '何同学'),
        deadlineText: deadlineDate ? `${deadlineDate.slice(5)} ${deadlineTime}` : '未设置'
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '读取订单失败',
        icon: 'none'
      })
    }
  },

  openAccept() {
    if (this.data.order && this.data.order.status === '待接单' && this.data.role === 'visitor') {
      this.setData({
        acceptVisible: true
      })
    }
  },

  closeAccept() {
    this.setData({
      acceptVisible: false
    })
  },

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

  noop() {},

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

  openConfirm() {
    if (this.data.order && this.data.order.status === '待确认' && this.data.role === 'publisher' && this.data.order.hasCompletionPhoto) {
      this.setData({
        confirmVisible: true
      })
    }
  },

  closeConfirm() {
    this.setData({
      confirmVisible: false
    })
  },

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

  // cancelOrder：取件订单取消确认；发布者只能取消待接订单，接单者取消已接订单会产生扣分记录。
  cancelOrder() {
    if (!this.data.order) return
    const isPublisherCancel = this.data.order.status === '待接单' && this.data.role === 'publisher'
    const isRunnerCancel = this.data.order.status === '已接单' && this.data.role === 'runner'
    if (!isPublisherCancel && !isRunnerCancel) return

    wx.showModal({
      title: '取消订单',
      content: isRunnerCancel ? '确定取消已接订单吗？将扣除 7 信用分。' : '确定取消这笔订单吗？',
      success: async result => {
        if (!result.confirm) return

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
  }
})
