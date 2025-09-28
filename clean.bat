@echo off
echo 清理构建文件...
cd /d "D:\Program Files\auto click"

if exist "build" rmdir /s /q "build"
if exist "app\build" rmdir /s /q "app\build"
if exist ".gradle" rmdir /s /q ".gradle"

echo 构建文件已清理完成
pause
