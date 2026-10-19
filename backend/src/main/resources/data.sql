-- 演示内容种子：INSERT IGNORE 让重复启动保持幂等，不会覆盖后台后来修改的内容。
-- 首页轮播：当前保留“交易模式”和“平台协议”两张；背景图不带文字，标题/副标题由小程序动态渲染。
INSERT IGNORE INTO banners
(id, code, image_url, tag, title, subtitle, link_type, link_value, sort_order, enabled)
VALUES
(1, 'home-2026-09-notice', '/assets/banners/banner-trade-bg.jpg', '公告',
 '校园易援交易模式', '线下交易，详情请看公告', 'notice', '1', 1, 1),
(2, 'home-2026-10-protocol', '/assets/banners/banner-protocol-bg.jpg', '须知',
 '平台服务协议更新', '订单责任与隐私说明 · 请留意变更', 'notice', '2', 2, 1);

-- 公告：当前保留两条，id 1 交易方式说明，id 2 平台协议更新。
INSERT IGNORE INTO announcements
(id, title, summary, content, category, level, pinned, force_read, is_published, published_at)
VALUES
(1, '交易方式说明',
 '校园易援目前支持线下支付，骑手在接单的时候需要上传自己的收款码，买家在完成订单的时候需要上传支付完成的截图才允许完成订单',
 '校园易援目前支持线下支付，骑手在接单的时候需要上传自己的收款码，买家在完成订单的时候需要上传支付完成的截图才允许完成订单',
 '系统公告', '重要', 1, 1, 1, '2026-09-28 18:00:00'),
(2, '平台服务协议与隐私政策更新提醒',
 '我们更新了服务协议中的订单责任与隐私说明，请留意变更内容。',
 '我们更新了服务协议中的订单责任、取件码保管和隐私说明。继续使用校园易援即表示你同意更新后的条款。\n\n如对条款有疑问，可以通过「我的 - 问题反馈」联系我们。',
 '系统公告', '重要', 0, 1, 1, '2026-09-10 16:30:00');
