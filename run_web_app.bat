@echo off
echo Starting Agile Sprint Tracker Web Application
echo ============================================

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Maven is not installed or not in PATH.
    echo Please install Maven and add it to your PATH.
    goto :eof
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java is not installed or not in PATH.
    echo Please install Java 11 or higher and add it to your PATH.
    goto :eof
)

REM Build the application
echo Building the application...
call mvn clean package

if %ERRORLEVEL% neq 0 (
    echo Build failed! Please check the errors above.
    goto :eof
)

echo.
echo Build successful!
echo.

REM Run the application in API mode
echo Starting the application with web interface...
echo Open your browser and navigate to http://localhost:8080
echo Press Ctrl+C to stop the application.
echo.

java -jar target/sprint-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar --api

echo Application stopped. 