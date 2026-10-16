// 旧版本的本地演示账单；当前账单页面调用后端，不代表微信支付或真实账户余额。
// 本地演示账单，不代表真实支付或退款。
const {
  getCurrentUser,
  readOrders
}
= require('./order-store')
const pad = value => String(value).padStart(2, '0')
function formatTime(value) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
// decorate：给旧本地演示账单补金额展示、月份和订单号。
function decorate(record) {
  return {
    ...record,
    time: record.fullTime.slice(5, 16),
    month: record.fullTime.slice(0, 7),
    amount: `${record.amountCents >= 0 ? '+' : '-'}¥${(Math.abs(record.amountCents) / 100).toFixed(2)}`,
    icon: record.kind === 'refund' ? '退' : '¥',
    transactionType: record.kind === 'refund' ? '订单退款' : record.kind === 'income' ? '订单收入' : '订单发布',
    status: '成功'
  }
}
// getTransactions：旧本地演示从订单推导发布支出、取消退回、完成收入；无真实支付。
function getTransactions() {
  const currentId = getCurrentUser().id
  const records = []
  for (const [type, key] of [['meal', 'campusHelpMealOrders'], ['parcel', 'campusHelpPickupOrders']]) {
    for (const order of readOrders(key)) {
      const amountCents = Math.round(Number(order.reward) * 100)
      const fullTime = formatTime(order.createdAt || Number(order.id))
      if (!Number.isFinite(amountCents) || amountCents <= 0 || !fullTime) continue
      const common = {
        orderNumber: `UO${order.id}`,
        orderId: String(order.id),
        orderType: type
      }
      const isPublisher = order.publisherId ? order.publisherId === currentId : order.myRole !== 'runner'
      if (isPublisher) {
        records.push(decorate({
          ...common,
          id: `${type}-${order.id}-publish`,
          title: type === 'meal' ? '发布取餐订单' : '发布取件订单',
          fullTime,
          amountCents: -amountCents,
          kind: 'publish'
        }))
        if (order.status === '已取消' && order.cancelledAt) {
          const cancelledTime = formatTime(order.cancelledAt)
          if (cancelledTime) records.push(decorate({
            ...common,
            id: `${type}-${order.id}-refund`,
            title: `订单取消退款：${order.title || (type === 'meal' ? '取餐订单' : '取件订单')}`,
            fullTime: cancelledTime,
            amountCents,
            kind: 'refund'
          }))
        }
      }
      if (order.runnerId === currentId && order.status === '已完成' && order.completedAt) {
        const completedTime = formatTime(order.completedAt)
        if (completedTime) records.push(decorate({
          ...common,
          id: `${type}-${order.id}-income`,
          title: `订单完成收入：${order.title || '订单'}`,
          fullTime: completedTime,
          amountCents,
          kind: 'income'
        }))
      }
    }
  }
  return records.sort((a, b) => b.fullTime.localeCompare(a.fullTime))
}
// getIncomeTotal：累加旧本地演示账单中的完成收入并保留两位小数。
function getIncomeTotal() {
  const cents = getTransactions().filter(item => item.kind === 'income').reduce((sum, item) => sum + item.amountCents, 0)
  return (cents / 100).toFixed(2)
}
module.exports = {
  getTransactions,
  getIncomeTotal
}
