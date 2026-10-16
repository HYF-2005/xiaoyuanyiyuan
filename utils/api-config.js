// 小程序请求地址集中配置。
// 开发者工具模拟器可以访问本机回环地址；真机调试时改成电脑在同一局域网中的 IPv4 地址。
// 正式发布前必须替换成已备案并配置到微信后台 request/uploadFile/downloadFile 合法域名的 HTTPS 地址。
const API_BASE_URL = 'http://127.0.0.1:8080/api'

module.exports = API_BASE_URL
