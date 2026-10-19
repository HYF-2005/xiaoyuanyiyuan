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

-- 协议初始内容：后台可继续编辑；INSERT IGNORE 不会覆盖已有修改。
INSERT IGNORE INTO agreements (id, code, title, version, content, is_published)
VALUES
(1, 'service', '平台服务协议', '2026.10',
 '校园易援为校内同学提供取件、取餐等互助信息撮合服务。\n\n发布者应如实填写订单信息并妥善保管取件码；接单者应按约定完成服务并上传真实凭证。平台目前采用线下支付，完成金额仅用于平台内部演示统计，不代表真实资金托管。\n\n如发生纠纷，请通过用户反馈联系运营人员。', 1),
(2, 'privacy', '隐私政策', '2026.10',
 '我们仅收集完成校园互助服务所必需的账号、联系方式、订单地址和完成凭证。取件码与详细地址只向订单参与者展示，后台管理员仅在处理异常时查看必要信息。\n\n除法律法规要求外，我们不会向无关第三方出售或提供你的个人信息。', 1);

-- 校园地址初始字典：完整迁移小程序发布页原有驿站、食堂楼层档口和送达建筑；仅在表为空时初始化，
-- 后台删除/改名后不会在重启时被种子重新覆盖。
INSERT INTO locations (id, kind, parent_id, name, sort_order, enabled)
SELECT * FROM (
  SELECT 1 AS id, 'station' AS kind, 0 AS parent_id, '北苑菜鸟驿站' AS name, 1 AS sort_order, 1 AS enabled
  UNION ALL SELECT 2, 'station', 0, '南苑菜鸟驿站', 2, 1
  UNION ALL SELECT 10, 'canteen', 0, '北苑食堂(一饭)', 1, 1
  UNION ALL SELECT 11, 'canteen', 0, '南苑食堂(三饭)', 2, 1
  UNION ALL SELECT 12, 'canteen', 0, '第二饭堂', 3, 1
  UNION ALL SELECT 20, 'floor', 10, '1楼', 1, 1
  UNION ALL SELECT 21, 'floor', 10, '2楼', 2, 1
  UNION ALL SELECT 22, 'floor', 11, '1楼', 1, 1
  UNION ALL SELECT 23, 'floor', 11, '2楼', 2, 1
  UNION ALL SELECT 24, 'floor', 11, '3楼', 3, 1
  UNION ALL SELECT 25, 'floor', 12, '1楼', 1, 1
  UNION ALL SELECT 100, 'vendor', 20, '匠心卤热', 1, 1
  UNION ALL SELECT 101, 'vendor', 20, '蒙自源米线', 2, 1
  UNION ALL SELECT 102, 'vendor', 20, '沃赛汉堡', 3, 1
  UNION ALL SELECT 103, 'vendor', 20, '徕哩牛肉饭', 4, 1
  UNION ALL SELECT 104, 'vendor', 20, '沙县小吃', 5, 1
  UNION ALL SELECT 105, 'vendor', 20, '高校汤馆', 6, 1
  UNION ALL SELECT 106, 'vendor', 20, '袁小饺', 7, 1
  UNION ALL SELECT 107, 'vendor', 20, '蜜雪冰城', 8, 1
  UNION ALL SELECT 108, 'vendor', 20, '东北麻辣烫', 9, 1
  UNION ALL SELECT 109, 'vendor', 20, '隆江猪脚饭', 10, 1
  UNION ALL SELECT 110, 'vendor', 21, '瑞幸咖啡', 1, 1
  UNION ALL SELECT 111, 'vendor', 21, '脆香鸭', 2, 1
  UNION ALL SELECT 112, 'vendor', 21, '湘小悦', 3, 1
  UNION ALL SELECT 113, 'vendor', 21, '泰卤下饭局', 4, 1
  UNION ALL SELECT 114, 'vendor', 21, '肉太烤双拼饭', 5, 1
  UNION ALL SELECT 115, 'vendor', 21, '悦轻荷', 6, 1
  UNION ALL SELECT 116, 'vendor', 21, '塔斯汀中国汉堡', 7, 1
  UNION ALL SELECT 117, 'vendor', 21, '播撒拌饭', 8, 1
  UNION ALL SELECT 118, 'vendor', 22, '小霞包点', 1, 1
  UNION ALL SELECT 119, 'vendor', 22, '大西豪', 2, 1
  UNION ALL SELECT 120, 'vendor', 22, '沙县小吃', 3, 1
  UNION ALL SELECT 121, 'vendor', 22, '椒小咖', 4, 1
  UNION ALL SELECT 122, 'vendor', 22, '幸运咖', 5, 1
  UNION ALL SELECT 123, 'vendor', 22, '果然嗨', 6, 1
  UNION ALL SELECT 124, 'vendor', 23, '四季轻食', 1, 1
  UNION ALL SELECT 125, 'vendor', 23, '港卤壹号', 2, 1
  UNION ALL SELECT 126, 'vendor', 23, '五谷渔粉', 3, 1
  UNION ALL SELECT 127, 'vendor', 23, '坤哥鸡腿饭', 4, 1
  UNION ALL SELECT 128, 'vendor', 23, '牛常有牛肉饭', 5, 1
  UNION ALL SELECT 129, 'vendor', 23, '米桥里牛肉饭', 6, 1
  UNION ALL SELECT 130, 'vendor', 24, '自助称重餐', 1, 1
  UNION ALL SELECT 131, 'vendor', 24, '嫩斯双堡', 2, 1
  UNION ALL SELECT 132, 'vendor', 24, '老坛酸菜', 3, 1
  UNION ALL SELECT 133, 'vendor', 24, '滑蛋扒饭', 4, 1
  UNION ALL SELECT 134, 'vendor', 24, '大众饭', 5, 1
  UNION ALL SELECT 135, 'vendor', 24, '小街胡同', 6, 1
  UNION ALL SELECT 136, 'vendor', 25, '特色饭', 1, 1
  UNION ALL SELECT 137, 'vendor', 25, '大众餐', 2, 1
  UNION ALL SELECT 138, 'vendor', 25, '港式双拼饭', 3, 1
  UNION ALL SELECT 200, 'building_category', 0, '宿舍楼', 1, 1
  UNION ALL SELECT 201, 'building_category', 0, '教学楼', 2, 1
  UNION ALL SELECT 202, 'building_category', 0, '实训楼', 3, 1
  UNION ALL SELECT 203, 'building_category', 0, '餐厅', 4, 1
  UNION ALL SELECT 301, 'building', 200, '一号学生宿舍', 1, 1
  UNION ALL SELECT 302, 'building', 200, '二号学生宿舍', 2, 1
  UNION ALL SELECT 303, 'building', 200, '三号学生宿舍', 3, 1
  UNION ALL SELECT 304, 'building', 200, '四号学生宿舍', 4, 1
  UNION ALL SELECT 305, 'building', 200, '五号学生宿舍', 5, 1
  UNION ALL SELECT 306, 'building', 200, '六号学生宿舍', 6, 1
  UNION ALL SELECT 307, 'building', 200, '七号学生宿舍', 7, 1
  UNION ALL SELECT 308, 'building', 200, '八号学生宿舍', 8, 1
  UNION ALL SELECT 309, 'building', 200, '九号学生宿舍', 9, 1
  UNION ALL SELECT 310, 'building', 200, '十号学生宿舍', 10, 1
  UNION ALL SELECT 311, 'building', 200, '十一号学生宿舍', 11, 1
  UNION ALL SELECT 312, 'building', 200, '十二号学生宿舍', 12, 1
  UNION ALL SELECT 313, 'building', 200, '十三号教师公寓', 13, 1
  UNION ALL SELECT 314, 'building', 200, '十四号教师公寓', 14, 1
  UNION ALL SELECT 315, 'building', 200, '十五号教师公寓', 15, 1
  UNION ALL SELECT 316, 'building', 200, '十六号学生宿舍', 16, 1
  UNION ALL SELECT 317, 'building', 200, '十七号学生宿舍-A', 17, 1
  UNION ALL SELECT 318, 'building', 200, '十七号学生宿舍-B', 18, 1
  UNION ALL SELECT 319, 'building', 200, '十七号学生宿舍-C', 19, 1
  UNION ALL SELECT 401, 'building', 201, '学术中心', 1, 1
  UNION ALL SELECT 402, 'building', 201, '学生活动中心', 2, 1
  UNION ALL SELECT 403, 'building', 201, '一号教学楼', 3, 1
  UNION ALL SELECT 404, 'building', 201, '二号教学楼', 4, 1
  UNION ALL SELECT 405, 'building', 201, '三号教学楼', 5, 1
  UNION ALL SELECT 406, 'building', 201, '四号教学楼', 6, 1
  UNION ALL SELECT 501, 'building', 202, '一号实训楼', 1, 1
  UNION ALL SELECT 502, 'building', 202, '二号实训楼', 2, 1
  UNION ALL SELECT 503, 'building', 202, '三号实训楼', 3, 1
  UNION ALL SELECT 601, 'building', 203, '北苑食堂(一饭)', 1, 1
  UNION ALL SELECT 602, 'building', 203, '第二饭堂', 2, 1
  UNION ALL SELECT 603, 'building', 203, '南苑食堂(三饭)', 3, 1
) AS location_seed
WHERE NOT EXISTS (SELECT 1 FROM locations);
