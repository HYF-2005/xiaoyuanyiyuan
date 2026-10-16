// 日期格式工具；启动日志页面用它把 Date 变成人能读的字符串。
// formatTime: 将日期/时间转成人能读的文本；不同工具文件按自己的输入格式处理。
const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

// formatNumber: 将单个数字补成两位，例如 8 变成 08。
const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

module.exports = {
  formatTime
}
