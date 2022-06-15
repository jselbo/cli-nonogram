@echo off
call gradlew.bat assemble
if %errorlevel% neq 0 exit /b %errorlevel%

setlocal

for /f "tokens=*" %%i in ('gradlew.bat -q printClasspath') do set CLASSPATH=%%i

java -cp %CLASSPATH% com.joshuaselbo.nonogram.MainKt %*
