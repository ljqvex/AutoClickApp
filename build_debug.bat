@echo off
echo 开始编译自动点击应用...
echo.

echo 清理项目...
call gradlew clean

echo.
echo 编译调试版本...
call gradlew assembleDebug

echo.
echo 编译完成！
echo APK文件位置: app\build\outputs\apk\debug\app-debug.apk

pause
