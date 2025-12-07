@echo off

for /f "delims=" %%x in (args.txt) do (set %%x)

echo Configuracion cargada:
echo PORT=%PORT%
echo HOST=%HOST%
echo API_CP=%API_CP%
echo API_LOG=%API_LOG%
echo API_DRIVER=%API_DRIVER%
echo.
echo Iniciando servidor Node.js...
echo ------------------------------------------

node server.js

pause