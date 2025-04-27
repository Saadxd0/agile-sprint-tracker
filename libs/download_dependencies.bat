@echo off
echo Downloading dependencies for Agile Team Sprint Tracker...

REM Create a temporary directory for downloads
mkdir tmp 2>nul
cd tmp

echo Downloading Gson...
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'gson-2.10.1.jar'}"

echo Downloading HttpClient...
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.14/httpclient-4.5.14.jar' -OutFile 'httpclient-4.5.14.jar'}"

echo Downloading HttpCore...
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar' -OutFile 'httpcore-4.4.16.jar'}"

echo Downloading Commons Logging...
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar' -OutFile 'commons-logging-1.2.jar'}"

echo Downloading Commons Codec...
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/commons-codec/commons-codec/1.11/commons-codec-1.11.jar' -OutFile 'commons-codec-1.11.jar'}"

REM Move the downloaded files to the libs directory
move *.jar ..

REM Return to the libs directory and clean up
cd ..
rmdir /s /q tmp

echo.
echo All dependencies downloaded successfully!
echo.
pause 