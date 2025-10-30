@echo off
echo ==========================================
echo   LANZANDO CPs MANUALMENTE
echo ==========================================

rem === CONFIGURACIÓN BASE ===
set KAFKA_ADDR=192.168.1.19:9092
set CENTRAL_ADDR=192.168.1.19:8888

rem === CPs DEFINIDOS MANUALMENTE ===
rem Formato:
rem start "TítuloVentana" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% <PUERTO>
rem start "TítuloVentana" cmd /C java -jar EV_CP_M.jar 192.168.1.19:<PUERTO> %CENTRAL_ADDR% <CP_ID>

rem === CPs de ALC ===
start "CP_E_ALC001" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 9999
start "CP_M_ALC001" cmd /C java -jar EV_CP_M.jar 192.168.1.19:9999 %CENTRAL_ADDR% ALC001

start "CP_E_ALC002" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10000
start "CP_M_ALC002" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10000 %CENTRAL_ADDR% ALC002

start "CP_E_ALC003" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10001
start "CP_M_ALC003" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10001 %CENTRAL_ADDR% ALC003


rem === CPs de SEV ===
start "CP_E_SEV001" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10002
start "CP_M_SEV001" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10002 %CENTRAL_ADDR% SEV001

start "CP_E_SEV002" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10003
start "CP_M_SEV002" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10003 %CENTRAL_ADDR% SEV002

start "CP_E_SEV003" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10004
start "CP_M_SEV003" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10004 %CENTRAL_ADDR% SEV003


rem === CPs de MAD ===
start "CP_E_MAD001" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10005
start "CP_M_MAD001" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10005 %CENTRAL_ADDR% MAD001

start "CP_E_MAD002" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10006
start "CP_M_MAD002" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10006 %CENTRAL_ADDR% MAD002

start "CP_E_MAD003" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10007
start "CP_M_MAD003" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10007 %CENTRAL_ADDR% MAD003

rem === CPs de BAR ===
start "CP_E_BAR001" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10008
start "CP_M_BAR001" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10008 %CENTRAL_ADDR% BAR001

start "CP_E_BAR002" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10009
start "CP_M_BAR002" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10009 %CENTRAL_ADDR% BAR002

start "CP_E_BAR003" cmd /C java -jar EV_CP_E.jar %KAFKA_ADDR% 10010
start "CP_M_BAR003" cmd /C java -jar EV_CP_M.jar 192.168.1.19:10010 %CENTRAL_ADDR% BAR003

echo ==========================================
echo  Todos los CPs definidos se han lanzado.
echo ==========================================
pause