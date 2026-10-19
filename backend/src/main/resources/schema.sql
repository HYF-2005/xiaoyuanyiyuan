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
