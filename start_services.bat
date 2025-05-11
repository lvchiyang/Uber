@echo off
echo 正在启动Uber服务系统...

REM 设置标题
title Uber服务启动脚本

REM 创建临时目录用于保存日志
mkdir logs 2>nul

echo 正在编译 Eureka 服务...
cd eureka-server
call gradlew.bat clean build
cd ..

echo 正在编译 API Gateway 服务...
cd api-gateway
call gradlew.bat clean build
cd ..

echo 正在编译 平台服务...
cd platform
call gradlew.bat clean build
cd ..

echo 正在编译 司机服务...
cd Driver
call gradlew.bat clean build
cd ..

echo 正在编译 乘客服务...
cd Passenger
call gradlew.bat clean build
cd ..

echo 编译完成，开始启动服务...

REM 启动Eureka服务
echo 正在启动 Eureka 服务...
start "Eureka Service" cmd /c "cd eureka-server && gradlew.bat bootRun > ..\logs\eureka.log 2>&1"

REM 等待Eureka启动
echo 等待Eureka服务启动 (10秒)...
timeout /t 10 /nobreak > nul

REM 启动API Gateway
echo 正在启动 API Gateway...
start "API Gateway" cmd /c "cd api-gateway && gradlew.bat bootRun > ..\logs\gateway.log 2>&1"

REM 等待API Gateway启动
echo 等待API Gateway启动 (10秒)...
timeout /t 10 /nobreak > nul

REM 启动平台服务
echo 正在启动 平台服务...
start "Platform Service" cmd /c "cd platform && gradlew.bat bootRun > ..\logs\platform.log 2>&1"

REM 等待平台服务启动
echo 等待平台服务启动 (10秒)...
timeout /t 10 /nobreak > nul

REM 启动司机服务
echo 正在启动 司机服务...
start "Driver Service" cmd /c "cd Driver && gradlew.bat bootRun > ..\logs\driver.log 2>&1"

REM 等待司机服务启动
echo 等待司机服务启动 (10秒)...
timeout /t 10 /nobreak > nul

REM 启动乘客服务
echo 正在启动 乘客服务...
start "Passenger Service" cmd /c "cd Passenger && gradlew.bat bootRun > ..\logs\passenger.log 2>&1"

REM 等待乘客服务启动
echo 等待乘客服务启动 (10秒)...
timeout /t 10 /nobreak > nul

echo 所有服务已启动，正在打开前端界面...

REM 打开前端页面
start http://localhost:8083
start http://localhost:8082
start http://localhost:8081

echo 系统启动完成！
echo 平台管理界面: http://localhost:8083
echo 司机应用界面: http://localhost:8082
echo 乘客应用界面: http://localhost:8081
echo.
echo 服务日志保存在 logs 目录下
echo 按任意键退出此窗口...

pause > nul 