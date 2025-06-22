#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
PID_FILE="$SCRIPT_DIR/pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping application with PID: $PID"
        kill "$PID"
        rm "$PID_FILE"
        echo "Application stopped"
    else
        echo "Process $PID not found"
        rm "$PID_FILE"
    fi
else
    echo "PID file not found"
fi 