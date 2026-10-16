// 旧版本的本地信用分演示；当前信用分页面调用后端 AccountService，不使用此计算作为权威结果。
const {
  getCurrentUser,
  readOrders
}
= require('./order-store')

// 演示初始值。真实信用分须由后端按账号保存并裁定。
// 旧本地演示使用的信用分规则常量；当前权威规则在后端 AccountService。
const INITIAL_SCORE = 61
const MAX_SCORE = 100
const CANCEL_PENALTIES = {
  parcel: 7,
  meal: 4
}

// orderDeadline：把旧订单日期和时间组合成截止时刻；缺少日期时返回无期限。
function orderDeadline(order) {
  if (!order.deadlineDate) return null
  const [year,
  month,
  day] = order.deadlineDate.split('-').map(Number)
  const [hour,
  minute] = (order.deadlineTime || '23:59').split(':').map(Number)
  const deadline = new Date(year, month - 1, day, hour, minute).getTime()
  return Number.isFinite(deadline) ? deadline : null
}

function formatTime(value) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return ''
  const pad = number => String(number).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}月${pad(date.getDate())}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// getCreditSummary：旧本地演示按完成/取消记录重算信用分，不是当前后端页面的权威数据。
function getCreditSummary() {
  const currentId = getCurrentUser().id
  const records = []
  for (const [type, key] of [['parcel', 'campusHelpPickupOrders'], ['meal', 'campusHelpMealOrders']]) {
    for (const order of readOrders(key)) {
      if (order.runnerId !== currentId) continue
      const title = order.title || (type === 'meal' ? '取餐订单' : '取件订单')
      if (order.status === '已完成' && order.completedAt) {
        const deadline = orderDeadline(order)
        const serviceTime = Number(order.submittedAt || order.completedAt)
        if (deadline === null || serviceTime <= deadline) {
          records.push({
            id: `${type}-${order.id}-complete`,
            reason: `按时完成订单：${title}`,
            change: 1,
            at: order.completedAt
          })
        }
      }
      if (order.status === '已取消' && order.cancelledById === currentId && order.acceptedAt && order.cancelledAt) {
        records.push({
          id: `${type}-${order.id}-cancel`,
          reason: `接单后取消：${title}`,
          change: -CANCEL_PENALTIES[type],
          at: order.cancelledAt
        })
      }
    }
  }
  records.sort((a, b) => b.at - a.at)
  const score = Math.max(0, Math.min(MAX_SCORE, INITIAL_SCORE + records.reduce((sum, record) => sum + record.change, 0)))
  return {
    score,
    records: records.map(record => ({
      ...record,
      timeText: formatTime(record.at),
      changeText: `${record.change > 0 ? '+' : ''}${record.change}`
    }))
  }
}

module.exports = {
  getCreditSummary,
  CANCEL_PENALTIES
}
