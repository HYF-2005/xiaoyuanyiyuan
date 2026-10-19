-- 初始化建表脚本：IF NOT EXISTS 只建缺失表，不删除旧表；InnoDB 支持事务/外键，utf8mb4 支持中文和表情。
-- 用户表：唯一账号、BCrypt 密码、可选手机号、基础信用分与有效状态。
CREATE TABLE IF NOT EXISTS users (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 账号非空且唯一，避免重复注册。
  username VARCHAR(64) NOT NULL UNIQUE,
  -- BCrypt 密码哈希，不存明文。
  password_hash VARCHAR(100) NOT NULL,
  -- 可为空且唯一；验证码表的号码不要求唯一。
  phone VARCHAR(11) NULL UNIQUE,
  -- 姓名展示字段。
  display_name VARCHAR(64) NOT NULL,
  -- 批量导入时记录学院、专业、年级、班级和宿舍；手工注册用户可为空。
  college VARCHAR(64) NULL,
  major VARCHAR(64) NULL,
  grade VARCHAR(32) NULL,
  class_name VARCHAR(64) NULL,
  dormitory VARCHAR(64) NULL,
  -- 基础信用分默认 61，最终分数由业务层重算。
  credit_base INT NOT NULL DEFAULT 61,
  -- 1 有效 / 0 已停用，注销不会删除用户历史主键。
  active TINYINT(1) NOT NULL DEFAULT 1,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登录会话：随机 token 的 SHA-256 哈希、关联用户和过期时刻。
CREATE TABLE IF NOT EXISTS login_sessions (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 关联 users.id 的操作者。
  user_id BIGINT NOT NULL,
  -- 令牌 SHA-256 哈希，64 位十六进制字符。
  token_hash CHAR(64) NOT NULL UNIQUE,
  -- 凭证有效截止时间。
  expires_at DATETIME(3) NOT NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 外键确保关联记录存在，不能随意删掉仍被引用的用户。
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_session_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 演示验证码：BCrypt 哈希、用途、过期/已使用时刻及尝试次数。
CREATE TABLE IF NOT EXISTS verification_codes (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 可为空且唯一；验证码表的号码不要求唯一。
  phone VARCHAR(11) NOT NULL,
  -- 验证码用途 login/reset/bind。
  purpose VARCHAR(20) NOT NULL,
  -- BCrypt 验证码哈希，通常 60 字符，可放入 CHAR(64)。
  code_hash CHAR(64) NOT NULL,
  -- 凭证有效截止时间。
  expires_at DATETIME(3) NOT NULL,
  -- null 未用，非 null 已用。
  consumed_at DATETIME(3) NULL,
  -- 尝试次数，服务及条件更新限制最多 5 次。
  attempts INT NOT NULL DEFAULT 0,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_code_phone_purpose (phone, purpose, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单：取件/取餐类型、资料、报酬、参与者、状态和阶段时刻。
CREATE TABLE IF NOT EXISTS orders (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- parcel 取件 / meal 取餐。
  type VARCHAR(10) NOT NULL,
  -- 服务器根据数量生成标题。
  title VARCHAR(120) NOT NULL,
  -- 取件驿站，取餐订单可以为空。
  station VARCHAR(120) NULL,
  -- 取餐餐厅，取件订单可以为空。
  restaurant VARCHAR(120) NULL,
  -- JSON 包裹数组，包含大小与取件码。
  packages_json JSON NULL,
  -- JSON 取餐码数组。
  codes_json JSON NULL,
  -- 送达建筑。
  delivery_building VARCHAR(120) NOT NULL,
  -- 送达详细地址，不对非参与者暴露。
  delivery_address VARCHAR(255) NOT NULL,
  -- DECIMAL 固定两位小数报酬，避免浮点存储误差。
  reward DECIMAL(10,2) NOT NULL,
  -- 完整截止日期时间，取餐可选。
  deadline_at DATETIME(3) NULL,
  -- 订单生命周期状态，初始待接单。
  status VARCHAR(20) NOT NULL DEFAULT '待接单',
  -- 发布者用户 id。
  publisher_id BIGINT NOT NULL,
  -- 接单者用户 id，尚未接单为空。
  runner_id BIGINT NULL,
  -- 随机 JPEG 文件名，实际文件保存在 uploads。
  proof_file VARCHAR(80) NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 接单时间。
  accepted_at DATETIME(3) NULL,
  -- 提交完成照片时间，判断是否按时。
  submitted_at DATETIME(3) NULL,
  -- 发布者确认完成时间。
  completed_at DATETIME(3) NULL,
  -- 取消时间。
  cancelled_at DATETIME(3) NULL,
  -- 取消用户 id，信用分计算用它区分接单者取消。
  cancelled_by BIGINT NULL,
  -- 外键确保关联记录存在，不能随意删掉仍被引用的用户。
  CONSTRAINT fk_order_publisher FOREIGN KEY (publisher_id) REFERENCES users(id),
  -- 外键确保关联记录存在，不能随意删掉仍被引用的用户。
  CONSTRAINT fk_order_runner FOREIGN KEY (runner_id) REFERENCES users(id),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_order_market (type, status, deadline_at),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_order_publisher (publisher_id, created_at),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_order_runner (runner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 反馈：关联认证用户与正文。
CREATE TABLE IF NOT EXISTS feedback (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 关联 users.id 的操作者。
  user_id BIGINT NOT NULL,
  -- 反馈正文，最多 2000 字。
  content VARCHAR(2000) NOT NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 外键确保关联记录存在，不能随意删掉仍被引用的用户。
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 轮播位：首页可配置的公告、活动、广告海报；link_type/link_value 决定点击去向。
CREATE TABLE IF NOT EXISTS banners (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 后台识别用稳定编码；即使标题修改也不影响配置。
  code VARCHAR(64) NOT NULL UNIQUE,
  -- 海报地址；可以是包内 /assets 路径或 HTTPS 地址。
  image_url VARCHAR(500) NOT NULL,
  -- 左上角角标：公告 / 活动 / 推广。
  tag VARCHAR(20) NOT NULL DEFAULT '公告',
  -- 兜底标题，图片失败时展示，也便于后台识别。
  title VARCHAR(120) NOT NULL,
  -- 兜底副标题，海报通常已包含文案。
  subtitle VARCHAR(255) NULL,
  -- notice / page / web / miniProgram / none。
  link_type VARCHAR(20) NOT NULL DEFAULT 'none',
  -- 公告 id、站内路径、H5 地址或目标 appId。
  link_value VARCHAR(500) NULL,
  -- 数字越小越靠前。
  sort_order INT NOT NULL DEFAULT 0,
  -- 1 展示 / 0 停用。
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  -- 可选生效时间；null 表示立即生效。
  start_at DATETIME(3) NULL,
  -- 可选失效时间；null 表示长期有效。
  end_at DATETIME(3) NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 为常见筛选条件建立索引，加快查询；不是新增业务数据。
  INDEX idx_banner_active (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告：标题、摘要、正文、分类、重要级别、置顶和强提醒标记。
CREATE TABLE IF NOT EXISTS announcements (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 公告标题，列表和弹窗共用。
  title VARCHAR(120) NOT NULL,
  -- 列表摘要；为空时由业务层从正文截取。
  summary VARCHAR(255) NULL,
  -- 公告正文，纯文本保留换行；前端不做 HTML 注入。
  content TEXT NOT NULL,
  -- 系统公告 / 活动。
  category VARCHAR(20) NOT NULL DEFAULT '系统公告',
  -- 普通 / 重要 / 紧急。
  level VARCHAR(10) NOT NULL DEFAULT '普通',
  -- 1 置顶，排序优先于发布时间。
  pinned TINYINT(1) NOT NULL DEFAULT 0,
  -- 1 登录后有未读时自动弹出，并执行 5 秒强制阅读。
  force_read TINYINT(1) NOT NULL DEFAULT 1,
  -- 1 已发布 / 0 草稿；未来 published_at 可做定时发布。
  is_published TINYINT(1) NOT NULL DEFAULT 1,
  -- 发布时间；首页和列表只读取已到时间的公告。
  published_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 为列表和强提醒查询建立索引；不是新增业务数据。
  INDEX idx_announcement_feed (is_published, published_at),
  INDEX idx_announcement_pending (is_published, force_read, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告已读：一个用户对一条公告最多一条记录；唯一键让重复标记幂等。
CREATE TABLE IF NOT EXISTS announcement_reads (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 关联 users.id 的阅读者。
  user_id BIGINT NOT NULL,
  -- 关联 announcements.id 的公告。
  announcement_id BIGINT NOT NULL,
  -- 首次标记已读时间。
  read_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 同一用户同一公告只保留一条，重复写入由 INSERT IGNORE 跳过。
  UNIQUE KEY uk_announcement_read (user_id, announcement_id),
  -- 公告被删除时一并清理已读记录。
  CONSTRAINT fk_read_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_read_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
  -- 为按公告反查已读人数建立索引；不是新增业务数据。
  INDEX idx_read_announcement (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校园地址字典：驿站、食堂/楼层/档口、建筑分类/建筑统一存放；后台可增改，发布页读取启用项。
CREATE TABLE IF NOT EXISTS locations (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- station 取件驿站；canteen 食堂；floor 楼层；vendor 档口；building_category 建筑分类；building 送达建筑。
  kind VARCHAR(32) NOT NULL,
  -- 父节点 id；根节点固定为 0，避免 MySQL 唯一键对 NULL 的宽松处理。
  parent_id BIGINT NOT NULL DEFAULT 0,
  -- 展示名称；同一父节点下不允许重名。
  name VARCHAR(120) NOT NULL,
  -- 同级排序，越小越靠前。
  sort_order INT NOT NULL DEFAULT 0,
  -- 1 启用 / 0 停用；停用后小程序发布页不返回。
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  -- 同一类型、同一父节点下名称唯一；不同食堂可以有同名档口。
  UNIQUE KEY uk_location_parent_name (kind, parent_id, name),
  -- 按层级读取发布页地址树。
  INDEX idx_location_parent (kind, parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后台管理员：独立于小程序 users，后台账号不能登录小程序，普通用户也不能登录后台。
CREATE TABLE IF NOT EXISTS admins (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 后台登录账号，唯一；与 users.username 不共享身份。
  username VARCHAR(64) NOT NULL UNIQUE,
  -- BCrypt 密码哈希，不存明文；初始账号由 ADMIN_INIT_PASSWORD 环境变量创建。
  password_hash VARCHAR(100) NOT NULL,
  -- 后台顶部栏展示名称，也作为真实姓名。
  display_name VARCHAR(64) NOT NULL,
  -- 管理员手机号，仅后台联系使用，不参与登录。
  phone VARCHAR(20) NULL,
  -- 管理员邮箱，仅后台联系使用，不参与登录。
  email VARCHAR(128) NULL,
  -- super_admin / operator / finance / content，用于计算默认菜单权限。
  role VARCHAR(20) NOT NULL DEFAULT 'operator',
  -- 1 可登录 / 0 已停用；停用后现有会话也查不到管理员。
  active TINYINT(1) NOT NULL DEFAULT 1,
  -- 创建/编辑时填写的备注，便于交接和权限说明。
  remark VARCHAR(500) NULL,
  -- 自定义菜单权限 JSON；NULL 表示沿用角色默认权限。
  permission_codes TEXT NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 最近一次登录时间；仅用于后台展示，不参与鉴权。
  last_login_at DATETIME(3) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后台登录会话：只保存随机 token 的 SHA-256 哈希，不保存可用明文 token。
CREATE TABLE IF NOT EXISTS admin_sessions (
  -- BIGINT 自增主键；同表内唯一。
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 关联 admins.id，管理员删除时会话一并清理。
  admin_id BIGINT NOT NULL,
  -- 64 位十六进制 SHA-256 摘要，唯一；泄漏数据库也不能直接当 Bearer token。
  token_hash CHAR(64) NOT NULL UNIQUE,
  -- 会话过期时间；后台默认 12 小时。
  expires_at DATETIME(3) NOT NULL,
  -- 数据库默认记录创建时刻，精度到毫秒。
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 外键确保会话不会指向不存在的管理员。
  CONSTRAINT fk_admin_session_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE,
  -- 为清理过期会话和按管理员反查建立索引。
  INDEX idx_admin_session_expiry (expires_at),
  INDEX idx_admin_session_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 风险订单快照：规则扫描后的分数、命中原因和人工审核状态；不自动处罚，供后台风险队列使用。
CREATE TABLE IF NOT EXISTS order_risks (
  -- 关联 orders.id；一单一条风险快照。
  order_id BIGINT PRIMARY KEY,
  -- 规则累计分，0–100；分数越高越可疑。
  risk_score INT NOT NULL DEFAULT 0,
  -- none / low / medium / high。
  risk_level VARCHAR(10) NOT NULL DEFAULT 'none',
  -- JSON 字符串数组，保存可解释的命中原因，例如“发布者与接单者相同 (+100)”。
  risk_flags JSON NULL,
  -- 待审核 / 已放行 / 已确认风险；只代表人工结论，不直接改订单或用户状态。
  review_status VARCHAR(20) NOT NULL DEFAULT '待审核',
  -- 审核备注，便于后续复核。
  review_note VARCHAR(500) NULL,
  -- 审核管理员，关联 admins.id。
  reviewed_by BIGINT NULL,
  -- 最近一次人工审核时间。
  reviewed_at DATETIME(3) NULL,
  -- 最近一次规则扫描时间。
  scanned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  -- 外键确保风险记录不会指向不存在的订单或管理员。
  CONSTRAINT fk_order_risk_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_risk_admin FOREIGN KEY (reviewed_by) REFERENCES admins(id),
  -- 为风险队列按等级、审核状态筛选建立索引。
  INDEX idx_order_risk_level (risk_level, risk_score),
  INDEX idx_order_risk_review (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 反馈处理：后台回复/处理状态独立于用户提交的反馈正文；一个反馈最多一条处理记录。
CREATE TABLE IF NOT EXISTS feedback_handling (
  -- 关联 feedback.id，一条反馈只保留一条处理状态。
  feedback_id BIGINT PRIMARY KEY,
  -- 待处理 / 已处理。
  status VARCHAR(20) NOT NULL DEFAULT '待处理',
  -- 后台回复正文；可以不填，仅标记处理。
  reply VARCHAR(2000) NULL,
  -- 最后处理的管理员 id；管理员删除后保留处理记录。
  handled_by BIGINT NULL,
  -- 最后处理时间。
  handled_at DATETIME(3) NULL,
  -- 最近更新时间；后台每次保存刷新。
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  -- 反馈删除时同步清理处理记录。
  CONSTRAINT fk_feedback_handling_feedback FOREIGN KEY (feedback_id) REFERENCES feedback(id) ON DELETE CASCADE,
  -- 管理员删除后不阻塞处理记录。
  CONSTRAINT fk_feedback_handling_admin FOREIGN KEY (handled_by) REFERENCES admins(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 协议：后台维护服务协议/隐私政策等文本，code 稳定，version 用于展示更新批次。
CREATE TABLE IF NOT EXISTS agreements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  -- 稳定编码：service / privacy / refund 等。
  code VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(120) NOT NULL,
  version VARCHAR(40) NOT NULL DEFAULT '1.0',
  content MEDIUMTEXT NOT NULL,
  -- 1 发布 / 0 草稿。
  is_published TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员操作审计：记录写操作和登录/退出，不记录每次只读查询。
CREATE TABLE IF NOT EXISTS admin_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_id BIGINT NULL,
  -- 冗余管理员名称，账号改名后历史日志仍可追溯。
  admin_name VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NULL,
  target_id VARCHAR(64) NULL,
  detail VARCHAR(500) NULL,
  ip VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE SET NULL,
  INDEX idx_audit_created (created_at),
  INDEX idx_audit_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
