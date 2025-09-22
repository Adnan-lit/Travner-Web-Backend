@echo off
echo.
echo ===================================
echo  TRAVNER SECURITY SETUP SCRIPT
echo ===================================
echo.

echo [1/4] Creating .env file from template...
if not exist .env (
    copy .env.example .env
    echo ✓ .env file created successfully
) else (
    echo ! .env file already exists
)

echo.
echo [2/4] Checking .gitignore...
findstr /C:".idea/" .gitignore >nul
if %errorlevel%==0 (
    echo ✓ .idea/ is properly ignored
) else (
    echo ! Warning: .idea/ might not be properly ignored
)

echo.
echo [3/4] Checking git status...
git status --porcelain | findstr ".idea" >nul
if %errorlevel%==1 (
    echo ✓ No .idea files are tracked by git
) else (
    echo ! Warning: Some .idea files might still be tracked
)

echo.
echo [4/4] CRITICAL SECURITY REMINDER
echo ================================
echo.
echo 🚨 CHANGE YOUR MONGODB PASSWORD IMMEDIATELY!
echo.
echo Steps:
echo 1. Go to https://cloud.mongodb.com/
echo 2. Database Access → Find user "madnan221314"
echo 3. Edit → Change Password
echo 4. Update the MONGODB_URI in your .env file
echo.
echo Your old password was exposed in git history!
echo.
echo ===================================
echo  SETUP COMPLETE
echo ===================================
echo.
echo Next: Edit .env file with your NEW MongoDB credentials
echo Then: Run "mvn spring-boot:run" to start the application
echo.
pause