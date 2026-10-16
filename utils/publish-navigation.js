// 发布成功后的返回导航；尽量回到已有首页的订单区域，避免重复堆叠页面。
// returnToOrders：查找页面栈中的首页并切到订单区；找不到时 reLaunch 首页并传 tab/type 参数。
function returnToOrders(type) {
  const pages = getCurrentPages()
  const homeIndex = pages.findIndex(page => page.route === 'pages/shouye/shouye')
  const fallback = () => wx.reLaunch({
    url: `/pages/shouye/shouye?tab=orders&type=${type}`
  })
  if (homeIndex < 0) {
    fallback();
    return
  }
  const home = pages[homeIndex]
  home.setData({
    activeTab: 'orders',
    orderType: type,
    orderKeyword: '',
    orderStatusFilter: '未完成'
  })
  wx.navigateBack({
    delta: pages.length - 1 - homeIndex,
    success: () => home.showOrders(),
    fail: fallback
  })
}
module.exports = {
  returnToOrders
}
