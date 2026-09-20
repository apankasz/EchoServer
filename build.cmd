@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac -encoding UTF-8 -source 8 -target 8 -d build EchoServer.java
if errorlevel 1 exit /b 1
jar cfe EchoServer.jar EchoServer -C build .
if errorlevel 1 exit /b 1
echo Fertig: EchoServer.jar
