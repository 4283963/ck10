#!/bin/bash
cd "$(dirname "$0")/java-inventory-service"
echo "正在编译 Java Spring Boot 服务..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误"
    exit 1
fi
echo "启动 Java 库存与工单管理服务 (端口 8080)..."
mvn spring-boot:run
