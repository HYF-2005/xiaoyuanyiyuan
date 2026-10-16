// 前后端通信入口；管理本地令牌，封装 HTTP 请求、登录和订单照片上传/下载。
// 本机微信开发者工具模拟器连接本机后端；真机调试需改成手机可访问的地址。
const BASE_URL = require('./api-config')
const TOKEN_KEY = 'campusHelpApiToken'
const USER_KEY = 'campusHelpApiUser'

// token：读取本地保存的后端登录令牌；令牌有效性仍需服务器检验。
function token() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}
// currentUser：读取本地保存的 userId 和显示姓名；只是界面缓存，不是权限判断依据。
function currentUser() {
  return wx.getStorageSync(USER_KEY) || null
}
// saveSession：保存后端登录返回的 token 和用户摘要，后续请求自动携带 token。
function saveSession(result) {
  wx.setStorageSync(TOKEN_KEY, result.token)
  wx.setStorageSync(USER_KEY, {
    userId: result.userId,
    displayName: result.displayName
  })
}
// clearSession：只删除本机令牌与用户缓存；不删除 MySQL 用户。
function clearSession() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

function request(path, method = 'GET', data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json; charset=utf-8',
        ...(token() ? {
          Authorization: `Bearer ${token()}`
        } : {})
      },
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(res.data)
        else reject(new Error((res.data && res.data.message) || `请求失败：${res.statusCode}`))
      },
      fail(error) {
        reject(new Error(error.errMsg || '无法连接后端，请检查服务是否启动'))
      }
    })
  })
}

async function login(username, password) {
  const result = await request('/auth/login', 'POST', {
    username,
    password
  })
  saveSession(result)
  return result
}
// phoneLogin：POST 手机号和验证码到 /auth/phone-login，保存服务器返回的登录会话。
async function phoneLogin(phone, code) {
  const result = await request('/auth/phone-login', 'POST', {
    phone,
    code
  })
  saveSession(result)
  return result
}
// logout：通知后端删除当前 token 对应会话；finally 无论请求成败都清除本机登录缓存。
async function logout() {
  try {
    if (token()) await request('/auth/logout', 'POST')
  }
  finally {
    clearSession()
  }
}

function uploadProof(orderId, filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${BASE_URL}/orders/${encodeURIComponent(orderId)}/proof`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${token()}`
      },
      success(res) {
        let body
        try {
          body = JSON.parse(res.data)
        } catch (_) {
          reject(new Error('后端返回格式错误')); return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) resolve(body)
        else reject(new Error(body.message || '照片上传失败'))
      },
      fail(error) {
        reject(new Error(error.errMsg || '照片上传失败'))
      }
    })
  })
}

// downloadProof：带 token 下载受保护的订单凭证，返回临时文件路径供 wx.previewImage 使用。
function downloadProof(orderId) {
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url: `${BASE_URL}/orders/${encodeURIComponent(orderId)}/proof`,
      header: {
        Authorization: `Bearer ${token()}`
      },
      success(res) {
        if (res.statusCode === 200) resolve(res.tempFilePath)
        else reject(new Error('照片下载失败'))
      },
      fail(error) {
        reject(new Error(error.errMsg || '照片下载失败'))
      }
    })
  })
}

// openPolicyDocument：从后端下载完整协议 PDF，再交给微信文档预览器打开。
// PDF 放在后端 resources/static/docs 中，服务启动后对应 /docs/... 地址；预览使用临时文件，不写入业务数据库。
function openPolicyDocument(type) {
  const fileName = type === 'privacy' ? 'privacy-policy.pdf' : 'user-agreement.pdf'
  const baseUrl = BASE_URL.replace(/\/api\/?$/, '')
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url: `${baseUrl}/docs/${fileName}`,
      success(res) {
        if (res.statusCode !== 200) {
          reject(new Error('协议文件下载失败'))
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType: 'pdf',
          showMenu: true,
          success: resolve,
          fail: error => reject(new Error(error.errMsg || '暂时无法打开协议文件'))
        })
      },
      fail(error) {
        reject(new Error(error.errMsg || '无法连接协议文件服务'))
      }
    })
  })
}

module.exports = {
  request,
  login,
  phoneLogin,
  logout,
  token,
  currentUser,
  clearSession,
  uploadProof,
  downloadProof,
  openPolicyDocument
}
