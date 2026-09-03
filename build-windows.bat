@echo off
REM PDFchemy Windows Installer Build Script
echo ===================================================
echo Building PDFchemy Windows Standalone Installer (MSI)
echo ===================================================

if not defined JAVA_HOME (
    set JAVA_HOME=E:\Android_Studio\jbr
)

call .\gradlew.bat :desktop:packageMsi
if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================
    echo SUCCESS! MSI installer generated at:
    echo desktop\build\compose\binaries\main\msi\
    echo ===================================================
) else (
    echo.
    echo Build failed with error code %ERRORLEVEL%
)
