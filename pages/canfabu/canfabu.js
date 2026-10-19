// 发布取餐需求：餐厅三级联动、取餐码、地址、时间与建议报酬
const { request } = require('../../utils/api')

// 后台接口不可用时仍保留原有本地字典，避免断网无法发布。
// 本地选项字典：对象的键是分类/食堂，值是楼栋或下级选项；不是动态查询数据库。
const RESTAURANTS = {
  '北苑食堂(一饭)': {
    '1楼': ['匠心卤热',
    '蒙自源米线',
    '沃赛汉堡',
    '徕哩牛肉饭',
    '沙县小吃',
    '高校汤馆',
    '袁小饺',
    '蜜雪冰城',
    '东北麻辣烫',
    '隆江猪脚饭'],
    '2楼': ['瑞幸咖啡',
    '脆香鸭',
    '湘小悦',
    '泰卤下饭局',
    '肉太烤双拼饭',
    '悦轻荷',
    '塔斯汀中国汉堡',
    '播撒拌饭']
  },
  '南苑食堂(三饭)': {
    '1楼': ['小霞包点',
    '大西豪',
    '沙县小吃',
    '椒小咖',
    '幸运咖',
    '果然嗨'],
    '2楼': ['四季轻食',
    '港卤壹号',
    '五谷渔粉',
    '坤哥鸡腿饭',
    '牛常有牛肉饭',
    '米桥里牛肉饭'],
    '3楼': ['自助称重餐',
    '嫩斯双堡',
    '老坛酸菜',
    '滑蛋扒饭',
    '大众饭',
    '小街胡同']
  },
  第二饭堂: {
    '1楼': ['特色饭',
    '大众餐',
    '港式双拼饭']
  }
}

const BUILDINGS = {
  宿舍楼: ['一号学生宿舍',
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
  '十七号学生宿舍-C'],
  教学楼: ['学术中心',
  '学生活动中心',
  '一号教学楼',
  '二号教学楼',
  '三号教学楼',
  '四号教学楼'],
  实训楼: ['一号实训楼',
  '二号实训楼',
  '三号实训楼'],
  餐厅: ['北苑食堂(一饭)',
  '第二饭堂',
  '南苑食堂(三饭)']
}

// pad: 转为字符串后补两位，例如 3 → 03。
const pad = value => String(value).padStart(2, '0')

// Page 注册本页面；data 存可绑定数据，下面的方法响应 WXML 事件和页面生命周期。
Page({
  // data 是页面的初始状态；WXML 用 {{字段}} 读取，后续必须通过 this.setData 更新才能同步显示。
  data: {
    // restaurant：确认后的食堂-楼层-档口文字
    restaurant: '',
    // restaurantPickerVisible：餐厅三级滚轮弹窗开关
    restaurantPickerVisible: false,
    // restaurantNames：食堂名称列表；优先读取后台地址字典
    restaurantNames: Object.keys(RESTAURANTS),
    // restaurantMap：后台或本地食堂 -> 楼层 -> 档口字典
    restaurantMap: RESTAURANTS,
    // floorNames：当前食堂的楼层列表
    floorNames: [],
    // vendorNames：当前楼层的档口列表
    vendorNames: [],
    // restaurantIndexes：食堂/楼层/档口的临时下标
    restaurantIndexes: [0,
    0,
    0],
    // confirmedRestaurantIndexes：上次确认后的三个下标，用于再次打开恢复位置
    confirmedRestaurantIndexes: [0,
    0,
    0],
    // codes：取餐码输入行；每行有 id 和 value
    codes: [{
      id: 1,
      value: ''
    }],
    // nextCodeId：新增取餐码行的递增 id
    nextCodeId: 2,
    // mealCount：非空取餐码的份数
    mealCount: 0,
    // buildingPickerVisible：建筑选择弹窗开关
    buildingPickerVisible: false,
    // buildingCategory：当前建筑分类
    buildingCategory: '',
    // buildingCategoryMode：true 显示分类 / false 显示具体楼栋
    buildingCategoryMode: true,
    // buildingCategories：全部分类名称
    buildingCategories: Object.keys(BUILDINGS),
    // buildingMap：分类 -> 建筑名称；后台地址读取成功后覆盖本地兜底
    buildingMap: BUILDINGS,
    // buildingOptions：当前可选楼栋列表
    buildingOptions: [],
    // buildingKeyword：建筑搜索输入
    buildingKeyword: '',
    // deliveryBuilding：选中的送达建筑
    deliveryBuilding: '',
    // deliveryAddress：输入的房间等详细地址
    deliveryAddress: '',
    // deadlineTime：已确认的取餐截止时分，允许留空
    deadlineTime: '',
    // defaultTime：进入页面时的时分，用于初始化滚轮
    defaultTime: '',
    // deadlinePickerVisible：截止时间弹窗开关
    deadlinePickerVisible: false,
    // selectedDeadlineTime：弹窗内临时选择时间，未点确定不生效
    selectedDeadlineTime: '',
    // customHours：00 至 23 小时选项
    customHours: Array.from({
      length: 24
    }, (_, i) => pad(i)),
    // customMinutes：00 至 55 分钟选项，步长 5 分钟
    customMinutes: Array.from({
      length: 12
    }, (_, i) => pad(i * 5)),
    // customTimeIndexes：小时列和分钟列的选择下标，不是直接的时分数字
    customTimeIndexes: [0,
    0],
    // reward：实际填写的报酬，独立于推荐值
    reward: '',
    // suggestedReward：按餐品份数计算的建议价，不能代表实际报价
    suggestedReward: '0.00'
  },

  // onLoad：读取当前时分作为滚轮默认位置，并加载后台地址字典。
  onLoad() {
    const now = new Date()
    // setData 合并更新页面状态并触发 WXML 重绘；this.data 可读，直接改它不会可靠更新界面。
    this.setData({
      defaultTime: `${pad(now.getHours())}:${pad(now.getMinutes())}`
    })
    this.loadLocations()
  },

  // loadLocations：把后台食堂/楼层/档口和建筑分类转换成页面现有字典结构。
  async loadLocations() {
    try {
      const data = await request('/locations')
      const restaurantMap = {}
      ;(data.canteens || []).forEach(canteen => {
        restaurantMap[canteen.name] = {}
        ;(canteen.floors || []).forEach(floor => {
          restaurantMap[canteen.name][floor.name] = (floor.vendors || []).map(item => item.name)
        })
      })
      const buildingMap = {}
      ;(data.buildingCategories || []).forEach(category => {
        buildingMap[category.name] = (category.buildings || []).map(item => item.name)
      })
      this.setData({
        restaurantMap,
        restaurantNames: Object.keys(restaurantMap),
        buildingMap,
        buildingCategories: Object.keys(buildingMap)
      })
    } catch (error) {
      console.warn('后台地址读取失败，使用取餐页本地兜底', error)
    }
  },

  // openRestaurantPicker：打开食堂→楼层→档口三级滚轮，恢复上一次确认的选择。
  openRestaurantPicker() {
    const names = this.data.restaurantNames
    if (!names.length) return
    const indexes = this.data.confirmedRestaurantIndexes
    const restaurantIndex = Math.min(indexes[0] || 0, names.length - 1)
    const floorNames = Object.keys(this.data.restaurantMap[names[restaurantIndex]] || {})
    const floorIndex = Math.min(indexes[1] || 0, Math.max(0, floorNames.length - 1))
    const vendorNames = (this.data.restaurantMap[names[restaurantIndex]] || {})[floorNames[floorIndex]] || []
    const vendorIndex = Math.min(indexes[2] || 0, Math.max(0, vendorNames.length - 1))
    this.setData({
      restaurantPickerVisible: true,
      restaurantIndexes: [restaurantIndex, floorIndex, vendorIndex],
      floorNames,
      vendorNames
    })
  },

  // onRestaurantPickerChange：上级食堂/楼层变化时重置下级下标，并检查范围，防止访问不存在的档口。
  onRestaurantPickerChange(e) {
    const previous = this.data.restaurantIndexes
    // e.detail.value 是输入框/选择器本次产生的值；e 是微信传入的事件对象。
    const indexes = [...e.detail.value]
    if (indexes[0] !== previous[0]) {
      indexes[1] = 0
      indexes[2] = 0
    } else if (indexes[1] !== previous[1]) {
      indexes[2] = 0
    }
    const name = this.data.restaurantNames[indexes[0]]
    const floorNames = Object.keys(this.data.restaurantMap[name])
    indexes[1] = Math.min(indexes[1], floorNames.length - 1)
    const vendorNames = this.data.restaurantMap[name][floorNames[indexes[1]]]
    indexes[2] = Math.min(indexes[2], vendorNames.length - 1)
    this.setData({
      restaurantIndexes: indexes,
      floorNames,
      vendorNames
    })
  },

  // closeRestaurantPicker：取消餐厅选择，关闭滚轮但不确认临时值。
  closeRestaurantPicker() {
    this.setData({
      restaurantPickerVisible: false
    })
  },

  // confirmRestaurant：把食堂、楼层、档口拼成餐厅字符串并保存确认下标。
  confirmRestaurant() {
    const [nameIndex,
    floorIndex,
    vendorIndex] = this.data.restaurantIndexes
    const name = this.data.restaurantNames[nameIndex]
    const floor = this.data.floorNames[floorIndex]
    const vendor = this.data.vendorNames[vendorIndex]
    if (!name || !floor || !vendor) return
    this.setData({
      restaurant: `${name}-${floor}-${vendor}`,
      confirmedRestaurantIndexes: [...this.data.restaurantIndexes],
      restaurantPickerVisible: false
    })
  },

  // onCodeInput：通过 data-id 找到对应取餐码行，用 map 更新 value，再重算份数与建议价。
  onCodeInput(e) {
    // dataset 来自 WXML 的 data-id/data-type 等属性，用来确定点击了哪一项。
    const id = Number(e.currentTarget.dataset.id)
    // map 遍历数组并返回新数组；此处转换/复制每条数据，不是直接修改原数组。
    const codes = this.data.codes.map(item => item.id === id ? {
      ...item,
      value: e.detail.value
    } : item)
    this.updateCodes(codes)
  },

  // updateCodes：统计非空取餐码数量；按每份 2 元生成建议价，只作提示，不替用户决定实际报酬。
  updateCodes(codes) {
    // filter 返回满足条件的数据；不符合条件的项不会出现在结果里。
    const mealCount = codes.filter(item => item.value.trim()).length
    this.setData({
      codes,
      mealCount,
      suggestedReward: (mealCount * 2).toFixed(2)
    })
  },

  // addCode：增加一行取件码/取餐码，使用递增 id 作为列表稳定标识。
  addCode() {
    const id = this.data.nextCodeId
    this.setData({
      nextCodeId: id + 1
    })
    this.updateCodes([...this.data.codes, {
      id,
      value: ''
    }])
  },

  // removeCode：按 data-id 删除指定输入行；至少保留一行以便继续输入。
  removeCode(e) {
    if (this.data.codes.length < 2) return
    const id = Number(e.currentTarget.dataset.id)
    this.updateCodes(this.data.codes.filter(item => item.id !== id))
  },

  // openBuildingPicker：打开送达建筑弹窗并初始化可选列表；选项来自本页本地常量。
  openBuildingPicker() {
    this.setData({
      buildingPickerVisible: true,
      buildingCategory: '',
      buildingCategoryMode: true,
      buildingKeyword: '',
      buildingOptions: []
    })
  },

  // closeBuildingPicker：关闭建筑选择弹窗，不提交后端。
  closeBuildingPicker() {
    this.setData({
      buildingPickerVisible: false
    })
  },

  // chooseBuildingCategory：按 data-category 切换分类，刷新楼栋列表并清除旧搜索词。
  chooseBuildingCategory(e) {
    const category = e.currentTarget.dataset.category
    this.setData({
      buildingCategory: category,
      buildingCategoryMode: false,
      buildingOptions: this.data.buildingMap[category],
      buildingKeyword: ''
    })
  },

  // showBuildingCategories：回到建筑分类选择界面。
  showBuildingCategories() {
    this.setData({
      buildingCategoryMode: true,
      buildingKeyword: ''
    })
  },

  // showBuildingOptions：切回当前分类的建筑列表。
  showBuildingOptions() {
    if (!this.data.buildingCategory) return
    this.setData({
      buildingCategoryMode: false,
      buildingKeyword: '',
      buildingOptions: this.data.buildingMap[this.data.buildingCategory]
    })
  },

  // onBuildingSearch：按输入关键词过滤本地建筑选项；取件页查当前分类，取餐页未选分类时可查全部。
  onBuildingSearch(e) {
    const keyword = e.detail.value
    const source = this.data.buildingCategory ? this.data.buildingMap[this.data.buildingCategory] : Object.keys(this.data.buildingMap).reduce((all, category) => all.concat(this.data.buildingMap[category]), [])
    const options = source.filter(item => item.includes(keyword.trim()))
    this.setData({
      buildingKeyword: keyword,
      buildingOptions: options,
      buildingCategoryMode: !this.data.buildingCategory && !keyword.trim()
    })
  },

  // chooseBuilding：把选项 data-name 存为送达建筑并关闭弹窗。
  chooseBuilding(e) {
    this.setData({
      deliveryBuilding: e.currentTarget.dataset.name,
      buildingPickerVisible: false
    })
  },

  // onAddressInput：保存详细地址输入，与所选建筑一起构成送达地址。
  onAddressInput(e) {
    this.setData({
      deliveryAddress: e.detail.value
    })
  },

  // openDeadlinePicker：打开时间选择器并准备临时选择值；取消不会覆盖已确认的期限。
  openDeadlinePicker() {
    const [hour,
    minute] = (this.data.deadlineTime || this.data.defaultTime).split(':').map(Number)
    const indexes = [hour,
    Math.floor(minute / 5)]
    this.setData({
      deadlinePickerVisible: true,
      customTimeIndexes: indexes,
      selectedDeadlineTime: `${this.data.customHours[indexes[0]]}:${this.data.customMinutes[indexes[1]]}`
    })
  },

  // closeDeadlinePicker：关闭时间弹窗；不把未确认的临时值当成正式期限。
  closeDeadlinePicker() {
    this.setData({
      deadlinePickerVisible: false
    })
  },

  // noop：故意不做操作；配合 catchtap 阻止点击弹窗内容时冒泡关闭外层遮罩。
  noop() {},

  // onCustomTimeChange：将滚轮的小时/分钟下标转换成 HH:mm 文本。
  onCustomTimeChange(e) {
    const indexes = e.detail.value
    this.setData({
      customTimeIndexes: indexes,
      selectedDeadlineTime: `${this.data.customHours[indexes[0]]}:${this.data.customMinutes[indexes[1]]}`
    })
  },

  // confirmDeadline：检查所选时刻晚于今天当前时间，然后保存取餐截止时间；取餐期限是可选项。
  confirmDeadline() {
    const [hour,
    minute] = this.data.selectedDeadlineTime.split(':').map(Number)
    const now = new Date()
    if (new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute).getTime() <= now.getTime()) {
      wx.showToast({
        title: '请选择晚于当前时间的截止时间',
        icon: 'none'
      })
      return
    }
    this.setData({
      deadlineTime: this.data.selectedDeadlineTime,
      deadlinePickerVisible: false
    })
  },

  // onRewardInput：保存用户输入的报酬，下一步再检查金额格式和大小。
  onRewardInput(e) {
    this.setData({
      reward: e.detail.value
    })
  },

  // nextStep：验证取餐表单并生成 mealDraft；时间可选，金额取用户实际输入，通过 eventChannel 传给确认页。
  nextStep() {
    const data = this.data
    const codes = data.codes.map(item => item.value.trim()).filter(Boolean)
    let message = ''
    if (!data.restaurant) message = '请选择取餐餐厅'
    else if (codes.length === 0) message = '请输入取餐码'
    else if (!data.deliveryBuilding) message = '请选择送达建筑'
    else if (!data.deliveryAddress.trim()) message = '请输入详细地址'
    // 正则表达式校验输入格式；失败 return 只拦前端操作，服务器仍必须独立检查。
    else if (!/^\d+(\.\d{1,2})?$/.test(data.reward.trim()) || Number(data.reward) <= 0) message = '请输入有效金额'
    if (message) {
      wx.showToast({
        title: message,
        icon: 'none'
      })
      return
    }

    const now = new Date()
    const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    const deadlineDate = data.deadlineTime ? date : ''
    const deadlineTime = data.deadlineTime
    const [hour,
    minute] = (deadlineTime || '23:59').split(':').map(Number)
    if (deadlineTime && new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute).getTime() <= now.getTime()) {
      wx.showToast({
        title: '请选择晚于当前时间的截止时间',
        icon: 'none'
      })
      return
    }
    const draft = {
      type: 'meal',
      restaurant: data.restaurant,
      codes,
      deliveryBuilding: data.deliveryBuilding,
      deliveryAddress: data.deliveryAddress.trim(),
      deadlineDate,
      deadlineTime,
      deadlineLabel: data.deadlineTime ? `今天 ${deadlineTime}` : '未设置',
      reward: Number(data.reward).toFixed(2),
      title: `${codes.length}份餐`
    }
    // navigateTo 打开下一页并保留上一页；navigateBack 返回，reLaunch 则重建页面栈。
    wx.navigateTo({
      url: '/pages/canqueren/canqueren',
      // eventChannel 在前后两页之间传草稿对象；确认页监听同名事件，不是存数据库。
      success: result => result.eventChannel.emit('mealDraft', draft)
    })
  }
})
