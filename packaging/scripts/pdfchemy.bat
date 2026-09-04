@echo off
setlocal enabledelayedexpansion

set "APP_NAME=PDFchemy Tools"
set "JAR_NAME=PDFchemy-universal-1.0.0.jar"
set "SCRIPT_DIR=%~dp0"
set "JAR_PATH=%SCRIPT_DIR%%JAR_NAME%"

:: 1. Check if java is on PATH
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ==================================================================
    echo  [!] %APP_NAME%: Java Runtime Missing
    echo ==================================================================
    echo  PDFchemy requires Java 17 or higher to run this portable JAR.
    echo.
    echo  Tip: You can download our official Windows installer (.msi)
    echo  which already has Java bundled inside with zero dependencies!
    echo.
    where winget >nul 2>nul
    if !errorlevel! equ 0 (
        set /p choice="Would you like to install Microsoft OpenJDK 21 via winget now? (Y/N): "
        if /i "!choice!"=="Y" (
            echo.
            echo Installing Microsoft OpenJDK 21...
            winget install Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements
            echo.
            echo Java installed! Please restart this script to launch PDFchemy.
            pause
            exit /b 0
        )
    )
    echo.
    echo Please install Java 17+ or download the self-contained PDFchemy-windows-x64-1.0.0.msi installer.
    pause
    exit /b 1
)

:: 2. Launch JAR
if exist "%JAR_PATH%" (
    start "" javaw -jar "%JAR_PATH%" %*
) else (
    echo Error: %JAR_NAME% not found in %SCRIPT_DIR%
    pause
    exit /b 1
)
