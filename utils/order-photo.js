// 照片选择工具；chooseCompletionPhoto 用于当前后端版，另外两个函数保留旧版本地演示逻辑。
const {
  getRole
}
= require('./order-store')

// chooseCompletionPhoto：使用 wx.chooseMedia 获取一张相机/相册图片，回调返回临时路径；取消选择不当作错误。
function chooseCompletionPhoto(onSelected) {
  wx.chooseMedia({
    count: 1,
    mediaType: ['image'],
    sourceType: ['camera',
    'album'],
    success(result) {
      const file = result.tempFiles && result.tempFiles[0]
      if (!file || !file.tempFilePath) {
        wx.showToast({
          title: '没有获取到照片，请重试',
          icon: 'none'
        })
        return
      }
      onSelected(file.tempFilePath)
    },
    fail(error) {
      if (!/cancel/i.test((error && error.errMsg) || '')) {
        wx.showToast({
          title: '拍照失败，请重试',
          icon: 'none'
        })
      }
    }
  })
}

// submitCompletionPhoto：旧本地演示：把照片保存到微信本机文件系统，再把缓存订单改为待确认；不是上传服务器。
function submitCompletionPhoto(orderKey, orderId, tempFilePath, onSuccess, onFailure) {
  const fileSystem = wx.getFileSystemManager()
  fileSystem.saveFile({
    tempFilePath,
    success(result) {
      const savedFilePath = result.savedFilePath
      try {
        const stored = wx.getStorageSync(orderKey)
        const orders = Array.isArray(stored) ? stored : []
        const index = orders.findIndex(item => String(item.id) === String(orderId))
        if (index < 0 || orders[index].status !== '已接单' || getRole(orders[index]) !== 'runner') {
          throw new Error('订单状态已变化')
        }
        orders[index] = {
          ...orders[index],
          status: '待确认',
          completionPhotoPath: savedFilePath,
          submittedAt: Date.now()
        }
        wx.setStorageSync(orderKey, orders)
        onSuccess()
      } catch (error) {
        fileSystem.unlink({
          filePath: savedFilePath,
          fail() {}
        })
        onFailure(error.message === '订单状态已变化' ? error.message : '提交失败，请重试')
      }
    },
    fail() {
      onFailure('保存照片失败，请重试')
    }
  })
}

// confirmCompletedOrder：旧本地演示：核对发布者、待确认与照片后，仅把缓存状态改为已完成。
function confirmCompletedOrder(orderKey, orderId) {
  const stored = wx.getStorageSync(orderKey)
  const orders = Array.isArray(stored) ? stored : []
  const index = orders.findIndex(item => String(item.id) === String(orderId))
  if (index < 0 || orders[index].status !== '待确认' || !orders[index].completionPhotoPath || getRole(orders[index]) !== 'publisher') {
    throw new Error('订单状态已变化，无法确认')
  }
  orders[index] = {
    ...orders[index],
    status: '已完成',
    completedAt: Date.now()
  }
  wx.setStorageSync(orderKey, orders)
}

module.exports = {
  chooseCompletionPhoto,
  submitCompletionPhoto,
  confirmCompletedOrder
}
