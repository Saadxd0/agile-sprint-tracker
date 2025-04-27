@echo off
echo Starting Agile Sprint Tracker Web Application (without Maven)
echo ============================================

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java is not installed or not in PATH.
    echo Please install Java 11 or higher and add it to your PATH.
    goto :eof
)

REM Create the classes directory if it doesn't exist
if not exist "target\classes" mkdir target\classes

REM Compile all Java files
echo Compiling Java files...
javac -d target\classes -cp ".;libs\*" src\main\java\com\agileteam\sprinttracker\*.java 

if %ERRORLEVEL% neq 0 (
    echo Failed on first compilation pass.
    goto :eof
)

REM Now compile all the rest with full classpath
javac -d target\classes -cp ".;target\classes;libs\*" src\main\java\com\agileteam\sprinttracker\api\*.java
javac -d target\classes -cp ".;target\classes;libs\*" src\main\java\com\agileteam\sprinttracker\model\*.java
javac -d target\classes -cp ".;target\classes;libs\*" src\main\java\com\agileteam\sprinttracker\manager\*.java
javac -d target\classes -cp ".;target\classes;libs\*" src\main\java\com\agileteam\sprinttracker\github\*.java
javac -d target\classes -cp ".;target\classes;libs\*" src\main\java\com\agileteam\sprinttracker\storage\*.java

if %ERRORLEVEL% neq 0 (
    echo Compilation failed! Please check the errors above.
    goto :eof
)

echo.
echo Compilation successful!
echo.

REM Copy resources
echo Copying resources...
if exist src\main\resources (
    xcopy /E /Y src\main\resources target\classes\
)

REM Run the application in API mode
echo Starting the application with web interface...
echo Open your browser and navigate to http://localhost:8080
echo Press Ctrl+C to stop the application.
echo.

java -cp "target\classes;libs\*" com.agileteam.sprinttracker.Main --api

echo Application stopped. 