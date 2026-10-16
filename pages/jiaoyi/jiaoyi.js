// 交易明细：按月份和收入/支出筛选后端推导的演示账单
const {
  request
}
= require('../../utils/api')
const {
  transactionView
}
= require('../../utils/api-format')

Page({
  // incomeTotal：累计完成收入展示值，不是可提现余额
  // expenseTotal：当前月份支出统计
  // month：选择的 YYYY-MM 月份
  // monthLabel：显示用年月文字
  // filter：账单收支筛选选项
  // transactions：筛选后可见的账单数组
  // allTransactions：本用户全部格式化账单
  data: {
    incomeTotal: '0.00',
    expenseTotal: '0.00',
    month: '',
    monthLabel: '',
    filter: '全部账单',
    transactions: [],
    allTransactions: []
  },
  // onShow：每次显示初始化年月并重新拉取账单，避免返回时仍显示旧数据。
  onShow() {
    const now = new Date()
    this.setData({
      month: this.data.month || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    })
    this.loadTransactions()
  },
  // loadTransactions：读取 /me/transactions 后格式化，再按月份和收支筛选；这些是订单推导账单。
  async loadTransactions() {
    try {
      this.setData({
        allTransactions: (await request('/me/transactions')).map(transactionView)
      })
      this.applyFilter()
    } catch (error) {
      wx.showToast({
        title: error.message || '读取账单失败，请重试',
        icon: 'none'
      })
    }
  },
  // applyFilter：先按月份选取，再算收入/支出并按类型显示；用分为单位累计，减少浮点误差。
  applyFilter() {
    const monthly = this.data.allTransactions.filter(item => item.month === this.data.month)
    const incomeCents = monthly.filter(item => item.kind === 'income' || item.kind === 'refund').reduce((sum, item) => sum + item.amountCents, 0)
    const expenseCents = monthly.filter(item => item.kind === 'publish').reduce((sum, item) => sum - item.amountCents, 0)
    const transactions = monthly.filter(item => this.data.filter === '全部账单' || (this.data.filter === '收入' ? item.kind === 'income' || item.kind === 'refund' : item.kind === 'publish'))
    const [year,
    month] = this.data.month.split('-')
    this.setData({
      transactions,
      monthLabel: `${year}年${Number(month)}月`,
      incomeTotal: (incomeCents / 100).toFixed(2),
      expenseTotal: (expenseCents / 100).toFixed(2)
    })
  },
  // chooseFilter：弹出全部账单/收入/支出选项并重新筛选。
  chooseFilter() {
    const filters = ['全部账单',
    '收入',
    '支出']
    wx.showActionSheet({
      itemList: filters,
      success: result => {
        this.setData({
          filter: filters[result.tapIndex]
        }); this.applyFilter()
      }
    })
  },
  // onMonthChange：读取年月选择结果，更新月份标签与账单列表。
  onMonthChange(e) {
    this.setData({
      month: e.detail.value.slice(0, 7)
    })
    this.applyFilter()
  },
  // openDetail：把账单 id 编码传给详情页，详情页再查找对应记录。
  openDetail(e) {
    wx.navigateTo({
      url: `/pages/jiaoyixiangqing/jiaoyixiangqing?id=${encodeURIComponent(e.currentTarget.dataset.id)}`
    })
  }
})
