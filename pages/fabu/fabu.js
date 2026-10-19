// 发布取件需求：驿站、取件码、包裹大小、送达地址、期限和报酬
const { request } = require('../../utils/api')

// 后台地址接口不可用时仍保留原有本地字典，避免断网无法发布。
const BUILDING_OPTIONS = {
  宿舍楼: [
  '一号学生宿舍',
  '二号学生宿舍',
  '三号学生宿舍',
  '四号学生宿舍',
  '五号学生宿舍',
  '六号学生宿舍',
  '七号学生宿舍',
  '八号学生宿舍',
  '九号学生宿舍',
  '十号学生宿舍',
  '十一号学生宿舍',
  '十二号学生宿舍',
  '十三号教师公寓',
  '十四号教师公寓',
  '十五号教师公寓',
  '十六号学生宿舍',
  '十七号学生宿舍-A',
  '十七号学生宿舍-B',
  '十七号学生宿舍-C'
  ],
  教学楼: [
  '学术中心',
  '学生活动中心',
  '一号教学楼',
  '二号教学楼',
  '三号教学楼',
  '四号教学楼'
  ],
  实训楼: ['一号实训楼',
  '二号实训楼',
  '三号实训楼'],
  餐厅: ['北苑食堂(一饭)',
  '第二饭堂',
  '南苑食堂(三饭)']
}
Page({
  data: {
    // station：已经选中的取件驿站
    station: '',
    // stations：驿站选项；优先读取后台地址字典，本地数组只作兜底
    stations: ['北苑菜鸟驿站',
    '南苑菜鸟驿站'],
    // stationPickerVisible：驿站弹窗开关
    stationPickerVisible: false,
    // packageSizes：包裹大小的可选值
    packageSizes: ['小件',
    '中件',
    '大件'],
    // packageRows：取件码输入行，每行包含 id、code、size
    packageRows: [
    {
      id: 1,
      code: '',
      size: '小件'
    }
    ],
    // nextRowId：新包裹输入行使用的递增 id
    nextRowId: 2,
    // summary：有效取件码对应的小/中/大件数量汇总
    summary: {
      total: 0,
      small: 0,
      medium: 0,
      large: 0
    },
    // suggestedReward: 按已填写包裹的大小累计计算的推荐报酬
    suggestedReward: '0.00',
    deliveryBuilding: '',
    deliveryAddress: '',
    deadlinePickerVisible: false,
    // deadlineDates：从今天起的可选日期列表
    deadlineDates: [],
    // selectedDeadlineDate：弹窗内临时选择日期
    selectedDeadlineDate: '',
    selectedDeadlineTime: '',
    // rewardAmount：取件表单报酬字符串；提交前再转成数字
    rewardAmount: '',
    // deadlineTimeMode：常用时段或自定义滚轮模式
    deadlineTimeMode: 'common',
    customHours: Array.from({
      length: 24
    }, (_, i) => String(i).padStart(2, '0')),
    customMinutes: Array.from({
      length: 12
    }, (_, i) => String(i * 5).padStart(2, '0')),
    customTimeIndexes: [0,
    0],
    // confirmedDeadlineTime：已经点击确定的截止时分
    confirmedDeadlineTime: '',
    // commonDeadlineTimes：本地预置常用时段
    commonDeadlineTimes: [
    {
      name: '早上',
      time: '13:00'
    },
    {
      name: '下午',
      time: '18:00'
    },
    {
      name: '傍晚',
      time: '20:00'
    },
    {
      name: '晚上',
      time: '23:59'
    }
    ],
    // confirmedDeadlineDate：已经确认的截止日期
    confirmedDeadlineDate: '',
    // confirmedDeadlineLabel：界面显示的日期/星期/时间描述
    confirmedDeadlineLabel: '',
    buildingPickerVisible: false,
    buildingCategoryMode: false,
    buildingKeyword: '',
    buildingCategory: '宿舍楼',
    buildingCategories: ['宿舍楼',
    '教学楼',
    '实训楼',
    '餐厅'],
    buildingOptions: BUILDING_OPTIONS['宿舍楼'],
    // buildingMap：分类 -> 建筑名称；后台地址读取成功后覆盖本地兜底
    buildingMap: BUILDING_OPTIONS

  },

  // onLoad：进入发布页时读取后台启用的驿站和送达建筑。
  onLoad() {
    this.loadLocations()
  },

  // loadLocations：后台地址成功时更新选项；失败保留本地字典，不阻塞发布。
  async loadLocations() {
    try {
      const data = await request('/locations')
      const stations = (data.stations || []).map(item => item.name)
      const buildingMap = {}
      ;(data.buildingCategories || []).forEach(category => {
        buildingMap[category.name] = (category.buildings || []).map(item => item.name)
      })
      const categories = Object.keys(buildingMap)
      const currentCategory = categories.includes(this.data.buildingCategory) ? this.data.buildingCategory : (categories[0] || '')
      this.setData({
        stations,
        buildingMap,
        buildingCategories: categories,
        buildingCategory: currentCategory,
        buildingOptions: currentCategory ? buildingMap[currentCategory] : []
      })
    } catch (error) {
      console.warn('后台地址读取失败，使用发布页本地兜底', error)
    }
  },

  // chooseStation：打开驿站选择弹窗。
  chooseStation() {
    this.setData({
      stationPickerVisible: true
    })
  },
  // closeStationPicker：关闭驿站选择弹窗。
  closeStationPicker() {
    this.setData({
      stationPickerVisible: false
    })
  },
  // selectStation：读取所选驿站 data-name 并关闭弹窗。
  selectStation(e) {
    this.setData({
      station: e.currentTarget.dataset.name,
      stationPickerVisible: false
    })
  },

  // updateRows：统一更新包裹行并重新统计小/中/大件数量；空取件码不计数。
  updateRows(rows) {
    const filled = rows.filter(row => row.code.trim())
    const suggestedCents = filled.reduce((sum, row) => {
      const rewardBySize = {
        小件: 310,
        中件: 510,
        大件: 810
      }
      return sum + (rewardBySize[row.size] || 0)
    }, 0)

    this.setData({
      packageRows: rows,
      summary: {
        total: filled.length,
        small: filled.filter(row => row.size === '小件').length,
        medium: filled.filter(row => row.size === '中件').length,
        large: filled.filter(row => row.size === '大件').length
      },
      suggestedReward: (suggestedCents / 100).toFixed(2)
    })
  },

  // onCodeInput：通过 data-id 找到对应包裹行，用 map 复制更新 code，再重算数量。
  onCodeInput(e) {
    const id = Number(e.currentTarget.dataset.id)
    const rows = this.data.packageRows.map(row =>
    row.id === id
    ? {
      ...row,
      code: e.detail.value
    }
    : row
    )
    this.updateRows(rows)
  },

  // choosePackageSize：给指定 id 的包裹选择小件/中件/大件，然后重算数量。
  choosePackageSize(e) {
    const id = Number(e.currentTarget.dataset.id)

    wx.showActionSheet({
      itemList: this.data.packageSizes,
      success: (res) => {
        const size = this.data.packageSizes[res.tapIndex]
        const rows = this.data.packageRows.map(row =>
        row.id === id ? {
          ...row,
          size
        } : row
        )
        this.updateRows(rows)
      }
    })
  },

  addCode() {
    const rows = [
    ...this.data.packageRows,
    {
      id: this.data.nextRowId,
      code: '',
      size: '小件'
    }
    ]

    this.setData({
      nextRowId: this.data.nextRowId + 1
    })
    this.updateRows(rows)
  },

  removeCode(e) {
    if (this.data.packageRows.length === 1) return

    const id = Number(e.currentTarget.dataset.id)
    const rows = this.data.packageRows.filter(row => row.id !== id)
    this.updateRows(rows)
  },
  openBuildingPicker() {
    this.setData({
      buildingPickerVisible: true,
      buildingCategoryMode: false,
      buildingKeyword: '',
      buildingOptions: this.data.buildingMap[this.data.buildingCategory] || []
    })
  },

  closeBuildingPicker() {
    this.setData({
      buildingPickerVisible: false
    })
  },

  showBuildingCategories() {
    this.setData({
      buildingCategoryMode: true
    })
  },

  showBuildingOptions() {
    this.setData({
      buildingCategoryMode: false
    })
  },

  chooseBuildingCategory(e) {
    const category = e.currentTarget.dataset.category

    this.setData({
      buildingCategory: category,
      buildingOptions: this.data.buildingMap[category] || [],
      buildingCategoryMode: false,
      buildingKeyword: ''
    })
  },
  onBuildingSearch(e) {
    const value = e.detail.value
    const keyword = value.trim()
    const options = this.data.buildingMap[this.data.buildingCategory] || []

    this.setData({
      buildingKeyword: value,
      buildingCategoryMode: false,
      buildingOptions: options.filter(name => name.includes(keyword))
    })
  },
  onAddressInput(e) {
    this.setData({
      deliveryAddress: e.detail.value
    })
  },
  onRewardInput(e) {
    this.setData({
      rewardAmount: e.detail.value
    })
  },
  chooseBuilding(e) {
    this.setData({
      deliveryBuilding: e.currentTarget.dataset.name,
      buildingPickerVisible: false
    })
  },
  openDeadlinePicker() {
    const weekdays = ['周日',
    '周一',
    '周二',
    '周三',
    '周四',
    '周五',
    '周六']
    const today = new Date()
    const dates = []

    for (let i = 0; i < 7; i++) {
      const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i)
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')

      dates.push({
        value: `${date.getFullYear()}-${month}-${day}`,
        label: `${month}月${day}日`,
        weekday: weekdays[date.getDay()]
      })
    }

    this.setData({
      deadlinePickerVisible: true,
      deadlineDates: dates,
      selectedDeadlineDate: this.data.confirmedDeadlineDate || dates[0].value,
      selectedDeadlineTime: this.data.confirmedDeadlineTime
    })
  },
  // chooseDeadlineDate：暂存点击的日期，只有点确定才写入最终期限。
  chooseDeadlineDate(e) {
    this.setData({
      selectedDeadlineDate: e.currentTarget.dataset.date
    })
  },
  // changeDeadlineTimeMode：切换常用时间/自定义滚轮模式，更新临时选择时间。
  changeDeadlineTimeMode(e) {
    const mode = e.currentTarget.dataset.mode
    const [hourIndex,
    minuteIndex] = this.data.customTimeIndexes
    const customTime = `${this.data.customHours[hourIndex]}:${this.data.customMinutes[minuteIndex]}`

    this.setData({
      deadlineTimeMode: mode,
      selectedDeadlineTime: mode === 'custom' ? customTime : ''
    })
  },

  onCustomTimeChange(e) {
    const indexes = e.detail.value
    this.setData({
      customTimeIndexes: indexes,
      selectedDeadlineTime: `${this.data.customHours[indexes[0]]}:${this.data.customMinutes[indexes[1]]}`
    })
  },
  // chooseDeadlineTime：选择或取消选中某个常用时间，仍然只是临时值。
  chooseDeadlineTime(e) {
    const time = e.currentTarget.dataset.time
    this.setData({
      selectedDeadlineTime: this.data.selectedDeadlineTime === time ? '' : time
    })
  },
  // confirmDeadlineDate：把临时日期和时间保存为正式期限及界面标签，然后关闭弹窗。
  confirmDeadlineDate() {
    const selected = this.data.deadlineDates.find(
    item => item.value === this.data.selectedDeadlineDate
    )
    if (!selected) return

    const time = this.data.selectedDeadlineTime
    this.setData({
      confirmedDeadlineDate: selected.value,
      confirmedDeadlineTime: time,
      confirmedDeadlineLabel: `${selected.label} ${selected.weekday}${time ? ' ' + time : ''}`,
      deadlinePickerVisible: false
    })
  },
  // onNextStep：校验取件表单、有效取件码、报酬和未来期限；通过 eventChannel 把草稿交给确认页，此处不写数据库。
  onNextStep() {
    const data = this.data
    const reward = data.rewardAmount.trim()
    const packages = data.packageRows
    .filter(row => row.code.trim())
    .map(row => ({
      code: row.code.trim(),
      size: row.size
    }))

    let message = ''
    if (!data.station) message = '请选择取件驿站'
    else if (packages.length === 0) message = '请输入取件码'
    else if (!data.deliveryBuilding) message = '请选择送达建筑'
    else if (!data.deliveryAddress.trim()) message = '请输入详细地址'
    else if (!data.confirmedDeadlineDate) message = '请选择截止时间'
    else if (!/^\d+(\.\d{1,2})?$/.test(reward) || Number(reward) <= 0) {
      message = '请输入有效报酬'
    }

    if (message) {
      wx.showToast({
        title: message,
        icon: 'none'
      })
      return
    }

    const [year,
    month,
    day] = data.confirmedDeadlineDate.split('-').map(Number)
    const [hour,
    minute] = (data.confirmedDeadlineTime || '23:59').split(':').map(Number)
    if (new Date(year, month - 1, day, hour, minute).getTime() <= Date.now()) {
      wx.showToast({
        title: '请选择晚于当前时间的截止时间',
        icon: 'none'
      })
      return
    }

    const draft = {
      station: data.station,
      packages,
      deliveryBuilding: data.deliveryBuilding,
      deliveryAddress: data.deliveryAddress.trim(),
      deadlineDate: data.confirmedDeadlineDate,
      deadlineTime: data.confirmedDeadlineTime || '23:59',
      deadlineLabel: data.confirmedDeadlineLabel,
      reward
    }

    wx.navigateTo({
      url: '/pages/queren/queren',
      success: res => {
        res.eventChannel.emit('publishDraft', draft)
      }
    })
  },
  closeDeadlinePicker() {
    this.setData({
      deadlinePickerVisible: false
    })
  },
})
