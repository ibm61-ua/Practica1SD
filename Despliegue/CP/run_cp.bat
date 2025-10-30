@echo off
setlocal enabledelayedexpansion

rem ==== Leer configuración del archivo ====
for /f "tokens=1,2 delims==" %%a in (config_cp.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)

rem ==== Mostrar configuración leída ====
echo IP Engine: %ip_engine%
echo Port Engine: %port_engine%
echo IP Broker: %ip_broker%
echo IP Central: %ip_central%
echo Port Central: %port_central%
echo Nombre CP: %nombre_cp%
echo.

rem ==== Ejecutar Engine ====
echo Iniciando ENGINE...
start "CP_ENGINE" java -jar EV_CP_E.jar %ip_broker%:9092 %port_engine%

rem ==== Ejecutar Monitor ====
echo Iniciando MONITOR...
start "CP_MONITOR" java -jar EV_CP_M.jar %ip_engine%:%port_engine% %ip_central%:%port_central% %nombre_cp%

echo.
echo === CP %nombre_cp% iniciado correctamente ===
pause