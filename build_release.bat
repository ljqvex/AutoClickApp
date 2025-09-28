@echo off
chcp 65001 >nul
echo ==========================================
echo    自动点击应用 - 构建发布版
echo ==========================================
echo.

cd /d "%~dp0"
echo 当前目录: %CD%

echo 检查环境...
echo.

rem 检查Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ 错误: 未找到Java，请确保已安装Android Studio或JDK
    echo.
    pause
    exit /b 1
)

rem 检查Gradle（尝试多种方式）
echo 检查Gradle...

rem 方式1: 尝试使用gradlew
if exist "gradlew.bat" (
    echo 使用项目Gradle Wrapper...
    gradlew.bat --version >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set GRADLE_CMD=gradlew.bat
        goto gradle_found
    )
)

rem 方式2: 尝试系统Gradle
gradle --version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo 使用系统Gradle...
    set GRADLE_CMD=gradle
    goto gradle_found
)

rem 方式3: 尝试Android Studio的Gradle
for /f "tokens=*" %%i in ('where gradle 2^>nul') do (
    set GRADLE_CMD=%%i
    goto gradle_found
)

echo ❌ 错误: 未找到Gradle
echo.
echo 请确保以下任一条件满足:
echo 1. Android Studio已安装并配置环境变量
echo 2. 手动安装了Gradle并添加到PATH
echo 3. 项目包含完整的Gradle Wrapper
echo.
pause
exit /b 1

:gradle_found
echo ✅ 找到Gradle: %GRADLE_CMD%
echo.

rem 清理旧的构建
echo 清理旧构建文件...
if exist "app\build\outputs" rmdir /s /q "app\build\outputs" 2>nul

echo.
echo 开始构建Release APK...
echo 命令: %GRADLE_CMD% assembleRelease
echo.

%GRADLE_CMD% assembleRelease

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==========================================
    echo ✅ 构建成功！
    echo ==========================================
    echo.
    
    rem 查找生成的APK
    if exist "app\build\outputs\apk\release\app-release.apk" (
        echo 📱 Release APK已生成:
        echo    app\build\outputs\apk\release\app-release.apk
        echo.
        
        rem 显示文件信息
        for %%F in ("app\build\outputs\apk\release\app-release.apk") do (
            echo 📊 文件大小: %%~zF 字节 ^(%.2f MB^)
            set /a size_mb=%%~zF/1024/1024
        )
        
        echo.
        echo 🚀 可以直接安装到Android设备:
        echo    adb install app\build\outputs\apk\release\app-release.apk
    ) else (
        echo ⚠️  未找到Release APK，检查debug版本...
        if exist "app\build\outputs\apk\debug\app-debug.apk" (
            echo 📱 Debug APK已生成:
            echo    app\build\outputs\apk\debug\app-debug.apk
        ) else (
            echo ❌ 未找到任何APK文件
        )
    )
) else (
    echo.
    echo ==========================================
    echo ❌ 构建失败
    echo ==========================================
    echo.
    echo 常见解决方案:
    echo 1. 检查Android SDK路径是否正确
    echo 2. 确保网络连接正常（下载依赖）
    echo 3. 清理项目后重试: %GRADLE_CMD% clean
    echo 4. 检查build.gradle文件是否有语法错误
)

echo.
echo 构建完成，按任意键退出...
pause >nul
