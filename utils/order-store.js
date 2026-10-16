// 旧版本的本地订单演示工具；读写微信缓存，不是 MySQL，也不能代替服务器鉴权。
// 本地演示身份只用于区分发布者和接单者，不能替代后端登录鉴权。
const SESSION_KEY = 'campusHelpDemoSession'

// getCurrentUser：读取旧本地演示身份，缺省时返回演示用户；不能作为后端认证。
function getCurrentUser() {
  const stored = wx.getStorageSync(SESSION_KEY)
  return stored && stored.id ? stored : {
    id: 'local-demo',
    name: '何同学'
  }
}

// setCurrentUser：保存旧演示身份，并把没有发布者的历史本地订单归给该演示用户。
function setCurrentUser(id) {
  const userId = String(id).trim()
  wx.setStorageSync(SESSION_KEY, {
    id: userId,
    name: '同学' + userId.slice(-4)
  })
  // 首次登录时把旧版仅存于本机的发布订单归给当前演示账号。
  for (const key of ['campusHelpPickupOrders', 'campusHelpMealOrders']) {
    const orders = readOrders(key)
    if (orders.some(order => !order.publisherId && order.myRole !== 'runner')) {
      wx.setStorageSync(key, orders.map(order =>
      !order.publisherId && order.myRole !== 'runner'
      ? {
        ...order,
        publisherId: userId,
        publisherName: order.publisherName || '何同学'
      }
      : order
      ))
    }
  }
}

// readOrders：从指定微信缓存键读取旧订单；非数组时返回空列表。
function readOrders(key) {
  const stored = wx.getStorageSync(key)
  return Array.isArray(stored) ? stored : []
}

// getRole：旧本地演示中用发布者/接单者 id 区分 publisher、runner、visitor。
function getRole(order) {
  const currentId = getCurrentUser().id
  if (order.publisherId) {
    if (order.publisherId === currentId) return 'publisher'
    if (order.runnerId === currentId) return 'runner'
    return 'visitor'
  }
  // 保留旧版本本地订单的角色，旧数据默认由当前用户发布。
  return order.myRole === 'runner' ? 'runner' : 'publisher'
}

// updateOrder：旧本地演示先核对状态和角色，再合并变更写回缓存；不执行 SQL。
function updateOrder(key, id, expectedStatus, role, patch) {
  const orders = readOrders(key)
  const index = orders.findIndex(order => String(order.id) === String(id))
  if (index < 0 || orders[index].status !== expectedStatus || getRole(orders[index]) !== role) {
    throw new Error('订单状态已变化，请刷新')
  }
  orders[index] = {
    ...orders[index],
    ...patch
  }
  wx.setStorageSync(key, orders)
  return orders[index]
}

// acceptOrder：旧本地演示检查是否过期，将待接单改为已接单并记录接单者。
function acceptOrder(key, id) {
  const current = getCurrentUser()
  const order = readOrders(key).find(item => String(item.id) === String(id))
  if (!order) throw new Error('订单不存在')
  if (order.deadlineDate) {
    const parts = order.deadlineDate.split('-').map(Number)
    const time = (order.deadlineTime || '23:59').split(':').map(Number)
    const deadline = new Date(parts[0], parts[1] - 1, parts[2], time[0], time[1]).getTime()
    if (!Number.isFinite(deadline) || deadline <= Date.now()) throw new Error('订单已截止')
  }
  return updateOrder(key, id, '待接单', 'visitor', {
    status: '已接单',
    runnerId: current.id,
    runnerName: current.name,
    acceptedAt: Date.now()
  })
}

// cancelAcceptedOrder：旧本地演示将接单者的已接订单取消，并记录取消人和时间。
function cancelAcceptedOrder(key, id) {
  const currentId = getCurrentUser().id
  return updateOrder(key, id, '已接单', 'runner', {
    status: '已取消',
    cancelledAt: Date.now(),
    cancelledById: currentId
  })
}

module.exports = {
  getCurrentUser,
  setCurrentUser,
  readOrders,
  getRole,
  updateOrder,
  acceptOrder,
  cancelAcceptedOrder
}
