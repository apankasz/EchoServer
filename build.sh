#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -source 8 -target 8 -d build EchoServer.java
jar cfe EchoServer.jar EchoServer -C build .
echo 'Fertig: EchoServer.jar'
