@echo off
setlocal enabledelayedexpansion

rem ==== Leer configuración ====
for /f "tokens=1,2 delims==" %%a in (config_drivers.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)

echo Iniciando %num_drivers% drivers en el broker %ip_broker%:9092
echo.

rem ==== Ejecutar cada driver ====
for /L %%i in (1,1,%num_drivers%) do (
    set name_driver=Driver_0%%i
    echo Iniciando !name_driver! ...
    start "Driver_%%i" java -jar EV_DRIVER.jar %ip_broker%:9092 !name_driver!
)

echo.
echo === Todos los drivers han sido lanzados ===
pause