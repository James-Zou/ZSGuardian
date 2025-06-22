#!/bin/bash

# 检查是否以root权限运行
if [ "$(id -u)" != "0" ]; then
    echo "This script must be run as root. Attempting to use sudo..."
    sudo $0 "$@"
    exit $?
fi

# 获取脚本所在目录（兼容 macOS 和 Linux）
get_script_dir() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

SCRIPT_DIR=$(get_script_dir)
BASE_DIR=$(dirname "$SCRIPT_DIR")

# 切换到应用根目录
cd "$BASE_DIR"

# 检查端口是否被占用
check_port() {
    local port=$1
    if netstat -tuln | grep -q ":${port} "; then
        echo "Error: Port ${port} is already in use"
        return 1
    fi
    return 0
}

# 设置Java运行参数
JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$BASE_DIR/logs/heap_dump.hprof"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$BASE_DIR/logs/gc.log"

# 添加配置文件路径
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=file:$BASE_DIR/config/"

# 设置日志目录
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/startup.log"

# 创建日志目录（如果不存在）
mkdir -p "$LOG_DIR"
chown -R $(logname):$(logname) "$LOG_DIR"

# 获取主jar包
MAIN_JAR="$BASE_DIR/lib/zsguardian.jar"
if [ ! -f "$MAIN_JAR" ]; then
    echo "Error: Main JAR file not found at $MAIN_JAR"
    exit 1
fi

# 检查端口占用情况
PORT=12123
if ! check_port $PORT; then
    exit 1
fi

# 启动应用
echo "Starting application on port ${PORT}..."
SPRING_PROFILES_ACTIVE=prod SERVER_PORT=$PORT nohup java $JAVA_OPTS -jar "$MAIN_JAR" > "$LOG_FILE" 2>&1 &

# 打印进程ID
PID=$!
echo $PID > "$BASE_DIR/bin/pid"
chown $(logname):$(logname) "$BASE_DIR/bin/pid"
echo "Application started with PID: $PID"

# 监控启动状态并查看日志
tail -f "$LOG_FILE" &
TAIL_PID=$!

# 等待应用启动
TIMEOUT=30
COUNTER=0
while [ $COUNTER -lt $TIMEOUT ]; do
    if grep -q "Started .* in .* seconds" "$LOG_FILE"; then
        echo "Application started successfully"
        kill $TAIL_PID
        exit 0
    fi
    if ! ps -p $PID > /dev/null; then
        echo "Application failed to start. Check logs for details:"
        cat "$LOG_FILE"
        kill $TAIL_PID
        exit 1
    fi
    sleep 1
    let COUNTER=COUNTER+1
done

echo "Timeout waiting for application to start. Check logs for details:"
cat "$LOG_FILE"
kill $TAIL_PID
exit 1 