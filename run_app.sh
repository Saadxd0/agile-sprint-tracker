#!/bin/bash

echo "Agile Team Sprint Tracker"
echo "-----------------------------------"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed or not in PATH. Please install Java JDK 11+."
    echo "See SETUP_INSTRUCTIONS.md for details."
    exit 1
fi

# Check if target directory exists
if [ ! -d "target/classes" ]; then
    echo "Compiled classes not found. Compiling..."
    
    # Create directories
    mkdir -p target/classes
    
    # Check if libs directory exists
    if [ ! -d "libs" ]; then
        echo "libs directory not found. Please download the required dependencies."
        echo "See SETUP_INSTRUCTIONS.md for details."
        exit 1
    fi
    
    # Compile the code
    javac -d target/classes -cp "libs/*" src/main/java/com/agileteam/sprinttracker/*.java src/main/java/com/agileteam/sprinttracker/*/*.java
    
    if [ $? -ne 0 ]; then
        echo "Compilation failed. Please check the error messages."
        exit 1
    fi
    
    echo "Compilation successful."
fi

# Run the application
echo "Running Agile Team Sprint Tracker..."
java -cp "target/classes:libs/*" com.agileteam.sprinttracker.Main 