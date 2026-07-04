@echo off
chcp 65001 >nul
echo Zuhause am Bach Mobile Android V1.0 - APK bauen
echo.
if not exist "gradlew.bat" (
 echo Gradle Wrapper fehlt. Öffne den Ordner bitte in Android Studio.
 echo Android Studio lädt Gradle automatisch und baut die APK.
 pause
 exit /b 1
)
call gradlew.bat assembleDebug
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
