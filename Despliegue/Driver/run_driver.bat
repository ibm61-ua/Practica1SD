@echo off
setlocal enabledelayedexpansion

rem ==== Leer configuración del archivo ====
for /f "tokens=1,2 delims==" %%a in (config_driver.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)

rem ==== Mostrar configuración leída ====
echo IP Broker: %ip_broker%
echo Nombre Driver: %name_driver%
echo.

rem ==== Ejecutar Driver ====
echo Iniciando DRIVER...
java -jar EV_DRIVER.jar %ip_broker%:9092 %name_driver%

echo.
echo === DRIVER %name_driver% finalizado ===
pause