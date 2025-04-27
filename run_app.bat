@echo off
echo Agile Team Sprint Tracker
echo -----------------------------------

REM Check if Java is installed
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java is not installed or not in PATH. Please install Java JDK 11+.
    echo See SETUP_INSTRUCTIONS.md for details.
    pause
    exit /b 1
)

REM Check if target directory exists
if not exist target\classes (
    echo Compiled classes not found. Compiling...
    
    REM Create directories
    mkdir target\classes 2>nul
    
    REM Check if libs directory exists
    if not exist libs (
        echo libs directory not found. Please download the required dependencies.
        echo See SETUP_INSTRUCTIONS.md for details.
        pause
        exit /b 1
    )
    
    REM Compile the code
    javac -d target\classes -cp "libs\*" src\main\java\com\agileteam\sprinttracker\*.java src\main\java\com\agileteam\sprinttracker\*\*.java
    
    if %ERRORLEVEL% NEQ 0 (
        echo Compilation failed. Please check the error messages.
        pause
        exit /b 1
    )
    
    echo Compilation successful.
)

REM Run the application
echo Running Agile Team Sprint Tracker...
java -cp "target\classes;libs\*" com.agileteam.sprinttracker.Main

pause 