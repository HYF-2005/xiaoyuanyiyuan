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
