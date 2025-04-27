# Required Dependencies

This directory should contain the following JAR files needed to run the application without Maven:

## Core Dependencies

1. **Gson (2.10.1)** - For JSON parsing and serialization
   - Download from: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
   
2. **Apache HttpClient (4.5.14)** - For GitHub API integration
   - Download from: https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.14/httpclient-4.5.14.jar

## HttpClient Dependencies

HttpClient also requires the following dependencies:

3. **Apache HttpCore (4.4.16)**
   - Download from: https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar
   
4. **Commons Logging (1.2)**
   - Download from: https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar
   
5. **Commons Codec (1.11)**
   - Download from: https://repo1.maven.org/maven2/commons-codec/commons-codec/1.11/commons-codec-1.11.jar

## Download Instructions

1. Download all the JAR files using the links above.
2. Place all the JAR files in this directory.
3. Make sure there are no other JAR files in this directory that could cause conflicts.

## For Windows Users

If you're a Windows user, you can use the script `download_dependencies.bat` to automatically download these dependencies.

## For Linux/macOS Users

If you're a Linux or macOS user, you can use the script `download_dependencies.sh` to automatically download these dependencies. 
Make sure it's executable by running `chmod +x download_dependencies.sh` before executing it. 