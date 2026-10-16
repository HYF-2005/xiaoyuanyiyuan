// 接口数据到页面数据的转换层；补默认值、格式化时间/金额与订单编号。
const pad = value => String(value).padStart(2, '0')
// formatTime：将日期/时间转成人能读的文本；不同工具文件按自己的输入格式处理。
function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
// orderView：复制接口订单，补空值、转换 id/金额/时间；只做展示转换，不改变数据库记录。
function orderView(order) {
  return {
    ...order,
    id: String(order.id),
    reward: Number(order.reward).toFixed(2),
    cardReward: String(Number(Number(order.reward).toFixed(2))),
    packages: order.packages || [],
    codes: order.codes || [],
    deadlineTime: order.deadlineTime ? order.deadlineTime.slice(0, 5) : '',
    publisherText: order.publisherName || '同学',
    publisherCreditText: order.publisherCreditScore == null ? '--' : String(order.publisherCreditScore)
  }
}
// transactionView：转换账单金额符号、年月与 UO 订单号，方便列表和月份筛选。
function transactionView(record) {
  const fullTime = formatTime(record.at)
  const amountCents = Math.round(Number(record.amount) * 100)
  return {
    ...record,
    orderId: String(record.orderId),
    orderNumber: `UO${record.orderId}`,
    fullTime,
    time: fullTime.slice(5, 16),
    month: fullTime.slice(0, 7),
    amountCents,
    amount: `${amountCents >= 0 ? '+' : '-'}¥${(Math.abs(amountCents) / 100).toFixed(2)}`,
    icon: record.kind === 'refund' ? '退' : '¥',
    transactionType: record.kind === 'refund' ? '订单退款' : record.kind === 'income' ? '订单收入' : '订单发布'
  }
}
// creditRecord：转换信用分变动的符号、时间和原因，便于 WXML 渲染。
function creditRecord(record) {
  return {
    ...record,
    timeText: formatTime(record.at),
    changeText: `${record.change > 0 ? '+' : ''}${record.change}`
  }
}
module.exports = {
  formatTime,
  orderView,
  transactionView,
  creditRecord
}
