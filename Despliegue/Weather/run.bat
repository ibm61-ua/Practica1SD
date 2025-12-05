@echo off
title EV_W
setlocal enabledelayedexpansion

for /f "tokens=1,2 delims==" %%a in (args.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)

java -jar EV_W.jar %API_STATUS_CP% %API_ALERT% %API_KEY_OPENWEATHER%