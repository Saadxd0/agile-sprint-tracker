#!/bin/bash

echo "Starting Agile Sprint Tracker Web Application"
echo "============================================"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed or not in PATH."
    echo "Please install Maven and add it to your PATH."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed or not in PATH."
    echo "Please install Java 11 or higher and add it to your PATH."
    exit 1
fi

# Build the application
echo "Building the application..."
mvn clean package

if [ $? -ne 0 ]; then
    echo "Build failed! Please check the errors above."
    exit 1
fi

echo
echo "Build successful!"
echo

# Run the application in API mode
echo "Starting the application with web interface..."
echo "Open your browser and navigate to http://localhost:8080"
echo "Press Ctrl+C to stop the application."
echo

java -jar target/sprint-tracker-1.0-SNAPSHOT-jar-with-dependencies.jar --api

echo "Application stopped." 