// 登录：账号密码登录、手机号演示验证码登录和登录方式切换
const {
  request,
  login,
  phoneLogin,
  token,
  clearSession,
  openPolicyDocument
}
= require('../../utils/api')
/* require 用来引入另一个 JavaScript 文件中的内容 */
/* request(...)通用请求函数，用于向后端发送请求，获取验证码时使用
login(...)账号密码登录
phoneLogin(...)手机号验证码登录
token()读取本地保存的登录令牌，它用于判断用户以前是否登录过
clearSession() 清除本地登录状态，当令牌已经失效时使用*/
Page({
  data: {
    // loginMode：当前登录方式：account 账号 / phone 手机号，决定 WXML 展示哪个表单
    loginMode: 'account',
    // hidePassword：true 时密码显示为圆点；false 时显示原文
    hidePassword: true,
    // agreed：是否勾选协议，提交前必须为 true
    agreed: false,
    // schoolName：当前选中的学校文字，只在本页显示
    schoolName: '广州华南商贸职业学院',
    // schools：本地学校选项数组
    schools: ['广州华南商贸职业学院',
    '其他学校'
    ],
    // account：输入的登录账号
    account: '',
    // password：密码输入值，只用于提交校验，不是数据库密码
    password: '',
    // phone：输入的手机号
    phone: '',
    // verificationCode：用户输入的验证码
    verificationCode: '',
    // demoCode：接口返回的演示码，不是真实短信回执
    demoCode: '',
    // codePhone：获取验证码时的手机号，用来防止改号码后沿用旧码
    codePhone: '',
  },
  // onLoad：页面首次创建时验证已有 token：/me 成功就进入首页，失败清除过期会话。
  async onLoad() {
    if (!token()) return
    try {
      await request('/me')
      wx.reLaunch({
        url: '/pages/shouye/shouye'
      })
    } catch (_) {
      clearSession()
    }
  },
  // switchLoginMode：读取按钮 data-mode，切换 account（账号）或 phone（手机号），WXML 条件区域随之切换。
  switchLoginMode(e) {
    this.setData({
      loginMode: e.currentTarget.dataset.mode/* 读取data-mode里的内容 */
    })
  },
  // togglePassword：切换密码可见性；布尔值取反只改变显示，不修改密码内容。
  togglePassword() {
    this.setData({
      hidePassword: !this.data.hidePassword /* ! 表示“取反” */
    })
  },
  toggleAgreement() {
    this.setData({
      agreed: !this.data.agreed
    })
  },
  // chooseSchool：弹出本地学校选项并更新显示；目前选中的学校没有作为登录参数发给后端。
  chooseSchool() {
    wx.showActionSheet({
      /* 弹出一个底部选项菜单 */
      itemList: this.data.schools,
      /* itemList 表示菜单中要显示的选项 */
      success: (res) => {
        this.setData({
          schoolName: this.data.schools[res.tapIndex] /* tapIndex 表示用户点击了第几个选项 */
        })
      }
    })
  },
  // onAccountInput：把输入框 e.detail.value 保存为账号，供点击登录时读取。
  onAccountInput(e) {
    this.setData({
      account: e.detail.value
    })/* <!--  每次输入变化，都把输入框当前的内容保存到页面数据 account 中 --> */
  },

  // onPasswordInput：保存密码输入值；修改密码页用 data-field 区分旧/新/确认密码。
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    })
  },
  // onPhoneInput：保存手机号输入值；提交时再检查格式。
  onPhoneInput(e) {
    this.setData({
      phone: e.detail.value
    })
  },

  onCodeInput(e) {
    this.setData({
      verificationCode: e.detail.value
    })
  },
  // getDemoCode：调用 /auth/code 获取演示验证码并提示；不是发送真实短信，用途由本页请求参数决定。
  async getDemoCode() {
    /* async 表示这个函数里面可能要执行需要等待的任务 */
    const phone = this.data.phone

    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请先输入正确的手机号',
        icon: 'none'
      })
      return
    }

    try {
      const result = await request('/auth/code', 'POST', {
        phone,
        purpose: 'login'
      })/* '/auth/code'这是后端接口地址  purpose: 'login' 表示：这个验证码是用于登录的*/
      this.setData({
        demoCode: result.demoCode || '',
        codePhone: phone
      })/* 后端返回的数据放在result中  result.demoCode表示读取后端返回的演示验证码，codePhone: phone
      记录这次验证码对应的手机号*/
      wx.showModal({
        title: '演示验证码',
        content: result.demoCode ? `本次验证码：${result.demoCode}` : result.message,
        showCancel: false
      })/* showCancel: false表示不显示“取消”按钮，只显示一个确定按钮 */
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  // submitPhoneLogin：检查手机号、验证码和协议，再请求手机号登录；成功后保存令牌并进入首页。
  async submitPhoneLogin() {
    /* async 表示这个函数里面可能要执行需要等待的任务 */
    const {
      phone,
      verificationCode,
      agreed
    }
    = this.data

    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return
    }

    if (!verificationCode.trim()) {
      wx.showToast({
        title: '请输入验证码',
        icon: 'none'
      })
      return
    }

    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    try {
      await phoneLogin(phone, verificationCode.trim())
      wx.reLaunch({
        url: '/pages/shouye/shouye'
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  // submitAccountLogin：检查账号、密码和协议，再请求后端验证密码；登录成功后清空页面栈进入首页。
  async submitAccountLogin() {
    /* async 表示这个函数里面可能要执行需要等待的任务 */
    const {
      account,
      password,
      agreed
    }
    = this.data /* 对象解构  相当于
    const account = this.data.account
    const password = this.data.password 
    const agreed = this.data.agreed */

    if (!account.trim()) {
      /* trim() 用来删除字符串前后的空格 */
      wx.showToast({
        title: '请输入账号',
        icon: 'none'
      })
      return
    }

    if (!password) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
      return
    }

    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    try {
      /* 用于处理可能成功或失败的操作 */
      await login(account.trim(), password) /* 调用登录函数，并等待它完成
      传入两个参数account.trim(), password只有登录请求成功，程序才会继续执行下一行 */
      wx.reLaunch({
        url: '/pages/shouye/shouye'
      })/* reLaunch 会关闭当前页面栈，然后打开新页面 */
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
    /* 如果 try 中的登录请求失败，就执行 catch */
  },
  // goToForgotPassword：打开忘记密码页面；navigateTo 保留当前页，方便返回。
  goToForgotPassword() {
    wx.navigateTo({
      url: '/pages/wangjimima/wangjimima'
    })
  },
  // goToRegister：打开注册页面，不在登录页面直接创建账号。
  goToRegister() {
    wx.navigateTo({
      url: '/pages/zhuce/zhuce'
    })
  },
  // openPolicy：登录前也可以查看完整的用户协议或隐私政策 PDF。
  async openPolicy(e) {
    wx.showLoading({ title: '正在打开' })
    try {
      await openPolicyDocument(e.currentTarget.dataset.type)
    } catch (error) {
      wx.showToast({
        title: error.message || '协议打开失败',
        icon: 'none'
      })
    } finally {
      wx.hideLoading()
    }
  },
})
