-- ============================================================
-- 迁移脚本 V1.3.1：feedback 表增加 order_id
-- ============================================================
-- 背景：
--   v1.3.0 中"订单问题反馈"只是把订单号拼进 content 正文
--   （形如「订单 UO12：内容」），无法按订单检索。
--   v1.3.1 起改为结构化字段 order_id，正文只存用户输入。
--
-- 适用：
--   已在运行 v1.3.0 及更早版本的数据库。
--   全新安装直接执行 schema.sql 即可，不需要本文件。
--
-- 执行：
--   mysql -u root -p campus_help < backend/migrations/V1.3.1__feedback_order_id.sql
--
-- 注意：
--   MySQL 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报
--   1060 (Duplicate column name) / 1061 (Duplicate key name)，属正常，可忽略。
-- ============================================================

ALTER TABLE feedback
  ADD COLUMN order_id BIGINT NULL COMMENT '关联 orders.id；普通意见反馈为空' AFTER user_id,
  ADD INDEX idx_feedback_order (order_id),
  ADD CONSTRAINT fk_feedback_order
      FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;

-- ------------------------------------------------------------
-- 历史数据说明：
--   v1.3.1 之前提交的订单反馈没有 order_id，订单号仍混在 content 中。
--   若需要回填，可先人工核对再执行（正则表达式因 MySQL 版本差异大，此处不自动执行）：
--
--   SELECT id, content FROM feedback
--    WHERE order_id IS NULL AND content LIKE '订单 UO%';
-- ------------------------------------------------------------
