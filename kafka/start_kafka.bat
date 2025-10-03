@echo off
setlocal enabledelayedexpansion

REM Obtener la primera IP local (descarta 127.* y direcciones IPv6)
for /f "tokens=2 delims=:" %%A in ('ipconfig ^| findstr "IPv4"') do (
    set ip=%%A
    goto :found
)

:found
REM Quitar espacios en blanco
set ip=%ip: =%

echo Detectada IP: %ip%

REM Guardar en .env
echo HOST_IP=%ip% > .env

REM Lanzar docker-compose
docker-compose up -d