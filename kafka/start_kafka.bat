@echo off
setlocal enabledelayedexpansion

for /f "tokens=2 delims=:" %%A in ('ipconfig ^| findstr "IPv4"') do (
    set ip=%%A
    goto :found
)

:found
set ip=%ip: =%

echo Detectada IP: %ip%

echo HOST_IP=%ip% > .env

docker-compose up -d
PAUSE