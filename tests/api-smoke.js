// 前端 API 冒烟测试脚本；用 Node 模拟 wx，需服务和测试账号。不是小程序运行入口。
// node tests/api-smoke.js (需先设置 DEMO_USERNAME 和 DEMO_PASSWORD)
const assert = require('node:assert/strict')
const storage = new Map()

global.wx = {
  getStorageSync(key) {
    return storage.get(key)
  },
  setStorageSync(key, value) {
    storage.set(key, value)
  },
  removeStorageSync(key) {
    storage.delete(key)
  },
  request(options) {
    fetch(options.url, {
      method: options.method,
      headers: options.header,
      body: options.method === 'GET' ? undefined : JSON.stringify(options.data || {})
    }).then(async response => {
      const data = await response.json()
      options.success({
        statusCode: response.status,
        data
      })
    }).catch(error => options.fail({
      errMsg: error.message
    }))
  }
}
global.Page = definition => {
  global.lastPage = definition
}

const {
  login,
  request,
  token
}
= require('../utils/api')
const {
  orderView,
  transactionView
}
= require('../utils/api-format')

// main：用测试账号登录真实本机后端并断言账户/列表一致；模拟 wx 不等于真实微信编译与点击验证。
async function main() {
  assert(process.env.DEMO_USERNAME && process.env.DEMO_PASSWORD, 'Set DEMO_USERNAME and DEMO_PASSWORD')
  const session = await login(process.env.DEMO_USERNAME, process.env.DEMO_PASSWORD)
  assert.equal(token(), session.token)
  const account = await request('/me')
  assert.equal(account.userId, session.userId)
  const market = (await request('/orders/market?type=parcel')).map(orderView)
  const mine = (await request('/orders/mine?type=parcel')).map(orderView)
  const transactions = (await request('/me/transactions')).map(transactionView)
  assert(Array.isArray(market) && Array.isArray(mine) && Array.isArray(transactions))
  assert(mine.every(order => typeof order.id === 'string'))
  assert(transactions.every(item => item.orderNumber.startsWith('UO')))
  require('../pages/shouye/shouye')
  const page = global.lastPage
  page.data = {
    ...page.data
  }
  page.setData = patch => Object.assign(page.data, patch)
  page.onShow()
  await new Promise(resolve => setTimeout(resolve, 150))
  page.onHide()
  assert.equal(page.data.activeTab, 'home')
  await page.loadAccountSummary()
  await page.loadMarketOrders()
  await page.loadOrders()
  assert.equal(page.data.creditScore, account.creditScore)
  assert.equal(page.data.marketOrders.length, market.length)
  assert.equal(page.data.orders.length, mine.length)
  console.log(`Frontend API smoke passed: market=${market.length}, mine=${mine.length}, transactions=${transactions.length}`)
}

main().catch(error => {
  console.error(error.message); process.exitCode = 1
})
