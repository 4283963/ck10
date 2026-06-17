#!/bin/bash
cd "$(dirname "$0")/python-prediction-service"
echo "检查并安装 Python 依赖..."
if [ ! -d "venv" ]; then
    python3 -m venv venv
fi
source venv/bin/activate
pip install -q -r requirements.txt
echo "启动 Python 数据采集与预测服务 (端口 5000)..."
python app.py
