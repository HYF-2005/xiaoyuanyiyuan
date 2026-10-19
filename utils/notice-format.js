// 公告时间格式化：后端 LocalDateTime 序列化成 2026-09-28T18:00:00，这里只做字符串截取，避免不同设备解析时区不一致。
function toText(value) {
  return String(value || '').replace('T', ' ')
}

// formatNoticeTime：详情页展示完整日期和时间，例如 2026-09-28 18:00。
function formatNoticeTime(value) {
  const text = toText(value)
  return text.length >= 16 ? text.slice(0, 16) : text
}

// formatNoticeDate：列表只展示月日，例如 09-28；长度不足时原样返回。
function formatNoticeDate(value) {
  const text = toText(value)
  return text.length >= 10 ? text.slice(5, 10) : text
}

// formatNoticeDateTime：弹窗展示月日和时间，例如 09-28 18:00。
function formatNoticeDateTime(value) {
  const text = toText(value)
  return text.length >= 16 ? text.slice(5, 16) : text
}

module.exports = {
  formatNoticeTime,
  formatNoticeDate,
  formatNoticeDateTime
}
