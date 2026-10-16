// 早期接口接入示例；当前页面实际使用 utils/api.js，不要把示例当成第二套配置。
// 复制到小程序 utils/api.js 后再逐页接入。当前原页面仍使用本地数据。
// 后端接口根地址；127.0.0.1 指当前设备，模拟器可连本机，手机不能用它访问电脑。
const BASE_URL = 'http://localhost:8080/api'
// 微信本地缓存的键名；同一个键读写对应同一份缓存，不是 MySQL 表名。
const TOKEN_KEY = 'campusHelpApiToken'

// request：把 wx.request 包成 Promise；默认 GET，自动附带 Bearer 令牌，2xx 成功，其他状态或网络失败抛错。
function request(path, method = 'GET', data) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync(TOKEN_KEY)
    wx.request({
      url: BASE_URL + path,
      method,
      data,
      header: token ? {
        Authorization: `Bearer ${token}`
      } : {},
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data)
        else reject(new Error((res.data && res.data.message) || `请求失败：${res.statusCode}`))
      },
      fail(error) {
        reject(new Error(error.errMsg || '无法连接后端'))
      }
    })
  })
}

// login：POST 账号密码到 /auth/login，服务器验证成功后保存会话并返回结果。
async function login(username, password) {
  // await 等接口响应后再继续；失败抛到 catch。页面不能自己执行 SQL。
  const result = await request('/auth/login', 'POST', {
    username,
    password
  })
  wx.setStorageSync(TOKEN_KEY, result.token)
  return result
}

// uploadProof：multipart 上传本地照片；字段名 file 必须与后端一致，响应 JSON 字符串需解析。
function uploadProof(orderId, filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${BASE_URL}/orders/${encodeURIComponent(orderId)}/proof`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${wx.getStorageSync(TOKEN_KEY)}`
      },
      success(res) {
        // try 包住可能失败的请求/操作；catch 给用户提示，finally 用来无论成败都解除加载锁。
        try {
          const body = JSON.parse(res.data)
          if (res.statusCode >= 200 && res.statusCode < 300) resolve(body)
          else reject(new Error(body.message || '上传失败'))
        } catch (error) {
          reject(new Error('后端返回格式错误'))
        }
      },
      fail(error) {
        reject(new Error(error.errMsg || '照片上传失败'))
      }
    })
  })
}

// 导出本文件的公共函数，其他文件通过 require 导入；没有导出的函数只在本文件内使用。
module.exports = {
  request,
  login,
  uploadProof
}
