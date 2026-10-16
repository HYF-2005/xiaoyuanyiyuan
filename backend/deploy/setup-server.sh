#!/usr/bin/env bash
# 严格模式：命令失败、未定义变量或管道失败时退出；这是 Linux 部署脚本，不能直接在 Windows PowerShell 执行。
set -euo pipefail
# Run as root after installing Java 17, MySQL 8 and nginx.
cd "$(dirname "$0")"
# 检查 root 身份和待部署 JAR/SQL 是否存在，失败会终止。
test "$(id -u)" -eq 0
test -f app.jar
test -f schema.sql

# 创建只供服务使用的 Linux 系统用户，避免后端用 root 跑。
if ! id campushelp >/dev/null 2>&1; then
  useradd --system --home /var/lib/campushelp --shell /usr/sbin/nologin campushelp
fi
# 创建服务/配置/上传目录并指定权限；执行本脚本会改变服务器，不是单纯语法检查。
install -d -m 755 /opt/campushelp
install -d -m 700 /etc/campushelp
install -d -o campushelp -g campushelp -m 750 /var/lib/campushelp/uploads
# 把已构建 JAR 安装到 Linux 服务目录。
install -m 644 app.jar /opt/campushelp/app.jar

# 下面 heredoc 写 MySQL 配置，仅监听 127.0.0.1；配置正文内不能误插脚本语句。
install -m 644 /dev/stdin /etc/mysql/mysql.conf.d/campushelp.cnf <<'MYSQLCONFIG'
[mysqld]
bind-address = 127.0.0.1
mysqlx-bind-address = 127.0.0.1
default-time-zone = '+08:00'
max_connections = 40
innodb_buffer_pool_size = 134217728
MYSQLCONFIG
# 启用并重启 MySQL，让刚才配置生效。
systemctl enable mysql
systemctl restart mysql
# 建立 campus_help 数据库并随后执行建表 SQL，已有数据保留。
mysql --execute="CREATE DATABASE IF NOT EXISTS campus_help CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql campus_help < schema.sql

# 仅首次部署生成数据库用户密码和环境配置，重复部署不覆盖已有密钥。
if [ ! -f /etc/campushelp/backend.env ]; then
  # Keep the generated database password on the server; never print it.
  # 随机生成服务数据库密码，不打印；与 Windows 本机演示密码不是一回事。
  db_password=$(python3 -c 'import secrets; print(secrets.token_hex(32))')
  # 此 heredoc 给 MySQL 执行创建用户和最小业务权限 SQL。
  mysql <<SQL
CREATE USER IF NOT EXISTS 'campushelp'@'127.0.0.1' IDENTIFIED BY '${db_password}';
GRANT SELECT, INSERT, UPDATE, DELETE ON campus_help.* TO 'campushelp'@'127.0.0.1';
SQL
  umask 077
  # 此 heredoc 是部署脚本原有的环境文件生成逻辑，包含数据库密钥；不要当讲稿展示密钥。
  cat > /etc/campushelp/backend.env <<ENV
DB_URL=jdbc:mysql://127.0.0.1:3306/campus_help?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USER=campushelp
DB_PASSWORD=${db_password}
UPLOADS_DIR=/var/lib/campushelp/uploads
DEMO_CODES_ENABLED=false
ENV
  unset db_password
fi
chmod 600 /etc/campushelp/backend.env
# 安装 systemd 服务与 Nginx 配置，后面检查配置并启用服务。
install -m 644 campushelp.service /etc/systemd/system/campushelp.service
install -m 644 nginx-staging.conf /etc/nginx/conf.d/campushelp.conf
nginx -t
systemctl daemon-reload
systemctl enable --now campushelp
systemctl enable --now nginx
systemctl restart campushelp
systemctl reload nginx
# 最多检查 40 次服务健康；失败时输出服务日志便于定位。
for attempt in $(seq 1 40); do
  if curl -fsS http://127.0.0.1:8080/api/health; then
    printf '\nBackend startup verified.\n'
    exit 0
  fi
  sleep 2
done
journalctl -u campushelp -n 40 --no-pager
exit 1
