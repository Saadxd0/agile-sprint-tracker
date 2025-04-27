#!/bin/bash

echo "Downloading dependencies for Agile Team Sprint Tracker..."

# Create a temporary directory for downloads
mkdir -p tmp
cd tmp

echo "Downloading Gson..."
curl -L "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -o "gson-2.10.1.jar"

echo "Downloading HttpClient..."
curl -L "https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.14/httpclient-4.5.14.jar" -o "httpclient-4.5.14.jar"

echo "Downloading HttpCore..."
curl -L "https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar" -o "httpcore-4.4.16.jar"

echo "Downloading Commons Logging..."
curl -L "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar" -o "commons-logging-1.2.jar"

echo "Downloading Commons Codec..."
curl -L "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.11/commons-codec-1.11.jar" -o "commons-codec-1.11.jar"

# Move the downloaded files to the libs directory
mv *.jar ..

# Return to the libs directory and clean up
cd ..
rm -rf tmp

echo ""
echo "All dependencies downloaded successfully!"
echo "" 