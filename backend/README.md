# 校园易援后端

这套后端对应当前微信小程序的数据：取件、取餐、发布/接单、照片提交、发布者确认、取消、信用分、账单、账号设置和反馈。

技术：Java 17、Spring Boot 3.3.12、MyBatis 3.0.4、MySQL 8。Controller 接收请求，Service 校验业务规则，Mapper 执行 SQL。

## 当前完成情况

- 已有后端源码、数据库表结构、接口与业务测试。
- 账号密码使用 BCrypt；登录使用随机 token，数据库只保存 token 的 SHA-256 摘要。
- 订单状态更新在 SQL 的 WHERE 中同时检查身份和旧状态，防止重复接单、重复确认。
- 信用分和账单按照订单状态计算；账单不代表微信支付已完成。
- 验证码暂为本地演示，接口明确返回 demoCode；没有短信供应商接入。
- 小程序主要页面已通过 `utils/api.js` 请求后端，包括登录、订单、照片、信用分、账单、设置和反馈。
- 用户协议和隐私政策 PDF 位于 `src/main/resources/static/docs`，后端启动后可通过 `/docs/user-agreement.pdf` 和 `/docs/privacy-policy.pdf` 公开预览。
- 注销账号会校验密码和未完成订单，再停用账号并撤销登录会话；历史订单保留用于审计。
- 未实现微信支付、提现；后台管理由独立的 `admin-web` 提供，小程序端不包含后台页面。

## 启动

使用微信开发者工具打开本项目目录，在本机模拟器中编译。`project.private.config.json` 已关闭本地调试的合法域名校验；前端请求地址集中在 `utils/api-config.js`：模拟器默认使用 `http://127.0.0.1:8080/api`，真机调试时改成电脑与手机同一局域网的 IPv4 地址，正式发布前改成已备案并配置到微信后台合法域名的 HTTPS 地址。登录页可注册演示账号。旧版本存在微信本地缓存中的订单不会自动写入数据库。

1. 在 MySQL Workbench 中执行一次：

```sql
CREATE DATABASE IF NOT EXISTS campus_help CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 设置数据库连接。数据库密码不要写入源码或发到聊天中。

```powershell
$env:DB_USER = 'root'
$env:DB_PASSWORD = '你自己的数据库密码'
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.14.7-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

3. 在 `backend` 目录执行：

```powershell
mvn spring-boot:run
```

如果 `mvn` 没有加入 PATH，本机已有 Maven，可用：

```powershell
& 'C:\Users\86151\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd' spring-boot:run
```

macOS 没有全局 Maven 时，可直接使用项目内已安装的 Maven：

```bash
cd backend
./.tools/apache-maven-3.9.11/bin/mvn -Dmaven.repo.local=../.m2-local spring-boot:run
```

启动前需要先运行 MySQL，并设置数据库密码；只设置 `DB_PASSWORD` 不会自动安装或启动 MySQL。后端启动后用 `curl http://127.0.0.1:8080/api/health` 检查服务。

macOS 已配置用户级 LaunchAgent：登录系统后会自动运行当前项目目录中的 Spring Boot JAR，MySQL 连接使用本机的 `root` 账号。服务配置安装在 `~/Library/LaunchAgents/com.campushelp.backend.plist`，日志在 `backend/logs/`。MySQL 使用安装程序提供的系统级 LaunchDaemon，服务名为 `com.oracle.oss.mysql.mysqld`。手动停止/启动后端可执行：

```bash
launchctl bootout "gui/$(id -u)/com.campushelp.backend"
launchctl bootstrap "gui/$(id -u)" "$HOME/Library/LaunchAgents/com.campushelp.backend.plist"
```

LaunchAgent 当前直接调用 `/Users/feng/Downloads/xiaochengxu/backend/target/campus-help-backend-1.0.0-SNAPSHOT.jar`，避免项目目录变更或旧副本导致启动失败。

首次启动会通过 `schema.sql` 创建缺失表，不会清空已有表。服务地址是 `http://localhost:8080`；健康检查为 `GET /api/health`。

## 接口清单

除注册、登录、演示验证码、重置密码和健康检查外，都需要请求头：`Authorization: Bearer <token>`。

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /api/auth/register | 注册账号，返回 token |
| POST | /api/auth/login | 账号密码登录 |
| POST | /api/auth/code | 获取演示验证码；purpose 为 login、reset 或 bind |
| POST | /api/auth/phone-login | 已绑定手机号验证码登录 |
| POST | /api/auth/reset-password | 账号、绑定手机、验证码重置密码 |
| POST | /api/auth/logout | 撤销当前 token |
| POST | /api/me/deactivate | 核验密码与未完成订单后停用账号 |
| GET | /api/me | 个人资料、信用分、总收入 |
| POST | /api/me/password | 修改密码并撤销该账号全部登录会话 |
| POST | /api/me/phone | 用 bind 验证码绑定手机号 |
| GET | /api/me/credit | 信用分和增减记录 |
| GET | /api/me/transactions | 本人账单；前端按月份筛选 |
| POST | /api/orders | 发布订单 |
| GET | /api/orders/market?type=parcel | 首页待接订单；type 可为 meal |
| GET | /api/orders/mine?type=parcel | 本人发布和接单的订单 |
| GET | /api/orders/{id} | 订单详情 |
| POST | /api/orders/{id}/accept | 接单 |
| POST | /api/orders/{id}/proof | multipart 上传照片，字段名 file，最大 5MB |
| GET | /api/orders/{id}/proof | 读取照片，仅订单双方可访问 |
| POST | /api/orders/{id}/confirm | 发布者确认完成 |
| POST | /api/orders/{id}/cancel | 发布者取消待接订单，或接单者取消已接订单 |
| GET | /api/locations | 发布页启用的驿站/食堂档口/送达建筑树 |
| GET | /api/admin/risk-orders | 风险订单分页、汇总 |
| POST | /api/admin/risk-orders/scan | 手动重扫风险规则 |
| PATCH | /api/admin/risk-orders/{id}/review | 标记放行/确认风险，不自动处罚 |
| POST | /api/feedback | 保存反馈 |

## 请求示例

注册时可先不绑定手机号：

```json
{"username":"student01","password":"Campus123","displayName":"测试同学"}
```

登录：

```json
{"username":"student01","password":"Campus123"}
```

发布取件：

```json
{
  "type":"parcel",
  "station":"北苑菜鸟驿站",
  "packages":[{"code":"A123","size":"小件"}],
  "deliveryBuilding":"三号学生宿舍",
  "deliveryAddress":"301",
  "reward":3.10,
  "deadlineDate":"2099-01-01",
  "deadlineTime":"18:00"
}
```

发布取餐：

```json
{
  "type":"meal",
  "restaurant":"北苑食堂(一饭) 1楼 沙县小吃",
  "codes":["18"],
  "deliveryBuilding":"三号学生宿舍",
  "deliveryAddress":"301",
  "reward":2.00
}
```

## 与前端衔接时的字段差异

- ID 是数据库生成的数字；前端比较时使用 `String(id)`。
- 时间字段是 ISO 格式，如 `2026-09-28T23:00:00`，前端应转换后显示，不再把 `createdAt` 当毫秒数使用。
- 订单返回真实 publisherCreditScore，以及服务端判断的 myRole。前端不能自行指定 publisherId 或 runnerId。
- 照片使用带 Authorization 的 `wx.downloadFile` 下载到临时文件后再显示，不把 token 放入 URL。
- 发布者和接单者的绑定手机仅对订单双方返回，供“联系对方”按钮使用；未绑定手机号则为空。
- API 返回普通 JSON；失败使用 HTTP 400/401/404/409 和 `{ "message": "原因" }`。
- 当前 `wx.getStorageSync` 的旧订单没有自动迁移。切换接口前需单独决定如何保留旧演示记录。

## 业务规则与面试解释

订单状态为：待接单 → 已接单 → 待确认 → 已完成。照片上传成功才会进入待确认，发布者确认后才产生收入。接单者取消取件扣 7 分、取消取餐扣 4 分；按时提交并被确认完成加 1 分，信用分限制在 0–100。

金额使用 BigDecimal 和 MySQL DECIMAL，避免浮点数误差。数据库对接单采用一次条件 UPDATE，返回影响行数为 0 时接口返回冲突，因此并发请求不会同时抢到同一订单。

## 验证

```powershell
mvn test
```

2026-09-29 已通过 8 项后端单元测试，并在本机 MySQL 8 上验证注册、登录、发布取件、接单、上传照片、发布者确认、信用分与收入变化。已检查全部小程序 JavaScript 文件语法。开发者工具显示登录页；前端按钮的完整手动点击流程仍需在模拟器中复核。

重启后可先检查两个服务：

```bash
launchctl print system/com.oracle.oss.mysql.mysqld | rg 'state =|pid ='
launchctl print "gui/$(id -u)/com.campushelp.backend" | rg 'state =|pid ='
curl http://127.0.0.1:8080/api/health
```

如果 MySQL 未运行，先在“系统设置”或 MySQL 安装程序中启动 `com.oracle.oss.mysql.mysqld`，再重启用户级后端服务。

上线前还需要：短信服务、HTTPS、正式域名、备份、部署配置、请求限流和真实支付集成。`DEMO_CODES_ENABLED=false` 会禁用演示验证码，并明确提示尚未配置短信服务。
