#!/bin/zsh
set -euo pipefail

BACKEND_DIR="/Users/feng/Downloads/xiaochengxu/backend"
JAR_PATH="$BACKEND_DIR/target/campus-help-backend-1.0.0-SNAPSHOT.jar"

cd "$BACKEND_DIR"
exec /usr/bin/java -jar "$JAR_PATH"
