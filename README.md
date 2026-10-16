# 校园易援（CampusHelp）

> 校园互助跑腿小程序 —— 取件代取 / 取餐代取，含完整的微信小程序前端与 Spring Boot 后端。

![技术栈](https://img.shields.io/badge/微信小程序-TypeScript%20%7C%20JavaScript-informational)
![后端](https://img.shields.io/badge/Java-17-orange)
![框架](https://img.shields.io/badge/Spring%20Boot-3.3.12-brightgreen)
![数据库](https://img.shields.io/badge/MySQL-8.0-blue)

---

## 一、项目简介

面向高校场景的互助跑腿平台。学生在平台发布取件或取餐需求，其他同学接单并完成配送，平台负责订单状态流转、身份校验、信用分与账单结算。

**本项目为完整的前后端分离实现**，非静态演示。包含：

- 微信小程序前端（19 个页面）
- Spring Boot 后端服务（RESTful API + 单元测试）
- MySQL 数据库设计（7 张表，含外键与索引）
- 部署配置（nginx / systemd / macOS LaunchAgent）

---

## 二、技术栈

| 层次 | 技术 |
|---|---|
| 前端 | 微信小程序原生框架（JS / WXML / WXSS） |
| 后端 | Java 17 · Spring Boot 3.3.12 |
| 持久层 | MyBatis 3.0.4 · MySQL 8 |
| 安全 | BCrypt 密码哈希 · 自定义 Token 鉴权拦截器 |
| 测试 | JUnit 5 · Mockito |
| 构建 | Maven |
| 部署 | nginx · systemd · macOS LaunchAgent |

---

## 三、核心设计

### 1. 订单状态机与并发安全

订单有完整生命周期：`待接单 → 已接单 → 已提交 → 已完成`，任意阶段可进入 `已取消`。

接单与确认等状态变更**不使用"先查询再更新"**，而是在 SQL 的 `WHERE` 条件中同时校验身份与旧状态：

```sql
UPDATE orders SET runner_id = ?, status = '已接单', accepted_at = NOW(3)
WHERE id = ? AND status = '待接单' AND runner_id IS NULL
```

依赖**受影响行数**判断成败。若两个用户同时接单，只有一个能成功，另一个收到 409 冲突。这避免了分布式场景下的超卖问题。

### 2. 凭证安全

- 密码使用 **BCrypt** 加盐哈希，绝不明文存储或传输
- 登录成功后生成**随机 token**，数据库**仅保存 token 的 SHA-256 摘要**。即使数据库泄露，也无法反推出可直接使用的凭证
- token 通过 `Authorization: Bearer` 头传递，由拦截器统一校验有效期

### 3. 隐私边界

取件码与详细地址属于高敏感信息，**仅订单发布者与接单者可见**。服务端在组装订单详情时按参与者身份过滤字段，未认证访问直接拒绝。前端的按钮禁用只是体验优化，服务端才是真正的防线。

### 4. 信用分与账单

信用分与账单均**由订单状态派生计算**，而非独立存储，从而避免数据不一致。同一订单只会结算一次。账单仅代表平台内部流水，**未接入微信支付**，不涉及真实资金转移。

### 5. 账号注销

注销需校验密码与是否存在未完成订单，通过后停用账号并撤销全部登录会话；历史订单保留以保证数据可审计，不做物理删除。

---

## 四、项目结构

```
.
├── app.js / app.json / app.wxss      # 小程序入口与全局配置
├── pages/                             # 19 个业务页面
│   ├── shouye/          首页与订单市场
│   ├── fabu/            发布订单
│   ├── quxiao?          （见 dingdanxiangqing）订单详情
│   ├── queren/          订单确认
│   ├── jiaoyi/          交易记录
│   ├── xinyongfen/      信用分
│   ├── denglu/ zhuce/   登录与注册
│   ├── shezhi/          账号设置与注销
│   └── ...
├── utils/
│   ├── api.js           # 请求封装、令牌管理、文件上传下载
│   ├── api-config.js    # 基础地址配置（环境相关）
│   ├── order-store.js   # 订单数据访问层
│   └── ...
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/campushelp/
│   │   ├── auth/        鉴权：登录、验证码、拦截器
│   │   ├── order/       订单：状态机、证明照片
│   │   ├── account/     账户：资料、信用分、注销
│   │   ├── mapper/      MyBatis 数据访问
│   │   ├── model/       领域模型
│   │   └── web/         全局异常处理
│   ├── src/main/resources/
│   │   ├── schema.sql       # 建表脚本（含逐行注释）
│   │   └── application.yml  # 配置（密码走环境变量）
│   ├── src/test/java/    # 单元测试
│   └── deploy/           # nginx、systemd、启动脚本
└── 面试资料/              # 简历与面试讲稿（本地保留，不入库）
```

---

## 五、数据库设计

共 7 张表：`users`、`login_sessions`、`verification_codes`、`orders`、`order_items`、`feedback` 等。

约定：

- 引擎统一 `InnoDB`，字符集 `utf8mb4`（支持中文与 emoji）
- 外键约束保证引用完整性
- 常用筛选条件建索引：`idx_order_market (type, status, deadline_at)` 等
- 金额使用 `DECIMAL`，**不用浮点数**避免精度误差
- 时间戳统一 `DATETIME(3)`，精确到毫秒
- 建表脚本 `IF NOT EXISTS`，可重复执行且不破坏数据

---

## 六、测试

后端包含 8 个单元测试，覆盖关键业务规则：

| 测试 | 验证内容 |
|---|---|
| `parcelCreateRequiresFutureDeadlineAndValidPackage` | 发布取件订单的校验规则 |
| `failedConditionalAcceptIsConflict` | 并发接单返回冲突 |
| `visitorCannotReadPickupCodesOrDetailedAddress` | 未授权访问敏感字段被拒 |
| `completedAndCancelledOrdersUpdateCreditAndIncomeOnce` | 信用分与账单只结算一次 |
| `loginRejectsWrongPasswordAndIssuesRandomTokenForValidPassword` | 登录鉴权 |
| `phoneCodeIsConsumedOnlyOnce` | 验证码防重放 |
| `refusesClosureWhileOrdersAreOpen` | 有未完成订单不可注销 |
| `deactivatesOnlyAfterPasswordCheck` | 注销需密码验证 |

```bash
cd backend
mvn test
```

---

## 七、本地运行

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8
- 微信开发者工具

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 2. 启动后端

```bash
cd backend
export DB_USER=root
export DB_PASSWORD=你的数据库密码
mvn spring-boot:run
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

> 数据库密码通过环境变量注入，**不写入源码**。

### 3. 启动前端

用微信开发者工具打开项目根目录。请求地址在 `utils/api-config.js` 中配置：

- 开发者工具模拟器：`http://127.0.0.1:8080/api`
- 真机调试：改为电脑在局域网内的 IPv4 地址
- 正式发布：需替换为已备案并配置到微信后台合法域名的 HTTPS 地址

---

## 八、已实现 / 未实现

**已实现**

- 注册、登录、验证码、找回与修改密码
- 订单发布（取件 / 取餐）、接单、提交照片、确认完成、取消
- 订单市场、我的订单、交易记录
- 信用分与账单统计
- 账号资料、反馈、账号注销
- 用户协议与隐私政策

**未实现（后续计划）**

- 微信支付与提现（当前账单仅为平台内部流水）
- 后台管理端
- 短信供应商接入（验证码为本地演示实现）
- 实时消息通知（站内信 / 订阅消息）

---

## 九、说明

本项目为个人学习与作品展示用途。订单、用户与账单数据均为演示数据，不构成任何真实的交易或金融服务。
