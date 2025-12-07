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
javaw -jar EV_DRIVER.jar %ip_broker%:9092 %name_driver%
