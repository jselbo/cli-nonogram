@echo off
setlocal

for /f "tokens=*" %%i in ('gradlew.bat -q printClasspath') do set CLASSPATH=%%i

@echo on
java -cp %CLASSPATH% MainKt
