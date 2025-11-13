@echo off
title Central
setlocal enabledelayedexpansion

rem Leer toda la línea del archivo args.txt
set /p args=<args.txt

echo Ejecutando con argumentos: %args%
java -jar EV_Central.jar %args%

pause
