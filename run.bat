@echo off
call gradlew.bat assemble

setlocal

for /f "tokens=*" %%i in ('gradlew.bat -q printClasspath') do set CLASSPATH=%%i

java -cp %CLASSPATH% com.joshuaselbo.nonogram.MainKt
