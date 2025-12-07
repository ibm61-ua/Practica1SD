@echo off
title Central
setlocal enabledelayedexpansion

for /f "tokens=1,2 delims==" %%a in (args.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)

javaw -jar EV_Central.jar %port_cps% %broker_kafka% %database_ip% %api_port% %api_auth%


