@echo off
echo 🚀 Building Travner Backend for Railway...

REM Set Java version
set JAVA_HOME=C:\Program Files\Java\jdk-21

REM Build the application
echo 📦 Building JAR file...
call mvnw.cmd clean package -DskipTests

REM Check if build was successful
if %ERRORLEVEL% EQU 0 (
    echo ✅ Build successful!
    echo 📁 JAR file created: target\Travner-0.0.1-SNAPSHOT.jar
) else (
    echo ❌ Build failed!
    exit /b 1
)
