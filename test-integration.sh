#!/bin/bash

echo "=============================================="
echo "  刀具磨损预测与库存联动系统 - 集成测试"
echo "=============================================="
echo ""

echo "[1/5] 检查 Java 服务健康状态..."
JAVA_HEALTH=$(curl -s http://localhost:8080/api/health 2>/dev/null)
if [ $? -eq 0 ] && echo "$JAVA_HEALTH" | grep -q "healthy"; then
    echo "✅ Java 服务运行正常"
    echo "   $JAVA_HEALTH"
else
    echo "❌ Java 服务未启动，请先运行 ./start-java.sh"
    exit 1
fi
echo ""

echo "[2/5] 检查 Python 服务健康状态..."
PYTHON_HEALTH=$(curl -s http://localhost:5002/api/health 2>/dev/null)
if [ $? -eq 0 ] && echo "$PYTHON_HEALTH" | grep -q "healthy"; then
    echo "✅ Python 服务运行正常"
    echo "   $PYTHON_HEALTH"
else
    echo "❌ Python 服务未启动，请先运行 ./start-python.sh"
    exit 1
fi
echo ""

echo "[3/5] 查询当前刀具库存..."
echo "   库存列表:"
curl -s http://localhost:8080/api/inventory | python3 -m json.tool 2>/dev/null | head -50
echo ""

echo "[4/5] 查询 Python 服务监控的刀具状态..."
echo "   刀具列表:"
curl -s http://localhost:5002/api/tools | python3 -m json.tool 2>/dev/null
echo ""

echo "[5/5] 测试服务间通信 - 手动触发刀具寿命预警..."
echo "   模拟 TOOL-001 刀具寿命低于 15%，发送通知给 Java 服务..."
MANUAL_NOTIFY=$(curl -s -X POST http://localhost:5002/api/manual-notify/TOOL-001 2>/dev/null)
echo "   通知结果:"
echo "$MANUAL_NOTIFY" | python3 -m json.tool 2>/dev/null
echo ""

echo "=============================================="
echo "  查询生成的工单列表:"
echo "=============================================="
curl -s http://localhost:8080/api/work-orders | python3 -m json.tool 2>/dev/null
echo ""

echo "=============================================="
echo "  查询库存变化 (预扣库存后):"
echo "=============================================="
curl -s http://localhost:8080/api/inventory/model/CNMG-120408 | python3 -m json.tool 2>/dev/null
echo ""

echo "=============================================="
echo "  测试完成！"
echo "=============================================="
echo ""
echo "常用 API 接口:"
echo "  Python 服务 (端口 5002):"
echo "    GET  /api/tools              - 获取所有刀具状态"
echo "    GET  /api/tools/<id>/predict - 获取单把刀具预测结果"
echo "    POST /api/manual-notify/<id> - 手动触发通知"
echo ""
echo "  Java 服务 (端口 8080):"
echo "    GET  /api/inventory              - 获取库存列表"
echo "    GET  /api/work-orders            - 获取工单列表"
echo "    POST /api/work-orders/<id>/complete - 完成工单"
echo "    GET  /h2-console                 - H2 数据库控制台"
echo ""
