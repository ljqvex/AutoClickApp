@echo off
chcp 65001 >nul
echo ==========================================
echo    环境检查工具
echo ==========================================
echo.

echo 📁 当前目录: %CD%
echo.

echo 🔍 检查Java环境:
java -version 2>&1 | findstr "version"
if %ERRORLEVEL% neq 0 (
    echo ❌ Java未找到
) else (
    echo ✅ Java已安装
)
echo.

echo 🔍 检查Gradle:
gradle --version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✅ 系统Gradle可用
    gradle --version | findstr "Gradle"
) else (
    echo ❌ 系统Gradle不可用
)
echo.

echo 🔍 检查Android环境:
if defined ANDROID_HOME (
    echo ✅ ANDROID_HOME: %ANDROID_HOME%
) else (
    echo ❌ ANDROID_HOME未设置
)

if defined ANDROID_SDK_ROOT (
    echo ✅ ANDROID_SDK_ROOT: %ANDROID_SDK_ROOT%
) else (
    echo ❌ ANDROID_SDK_ROOT未设置
)
echo.

echo 🔍 检查项目文件:
if exist "build.gradle" (
    echo ✅ build.gradle存在
) else (
    echo ❌ build.gradle不存在
)

if exist "app\build.gradle" (
    echo ✅ app\build.gradle存在
) else (
    echo ❌ app\build.gradle不存在
)

if exist "gradlew.bat" (
    echo ✅ gradlew.bat存在
) else (
    echo ❌ gradlew.bat不存在
)
echo.

echo 🔍 检查源码文件:
if exist "app\src\main\java\com\example\autoclick\MainActivity.java" (
    echo ✅ MainActivity.java存在
) else (
    echo ❌ MainActivity.java不存在
)
echo.

echo 检查完成！
pause
