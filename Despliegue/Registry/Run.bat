for /f "tokens=1,2 delims==" %%a in (args.txt) do (
    if not "%%a"=="" (
        if not "%%a"=="#" (
            set %%a=%%b
        )
    )
)
start "EV_Registry" java -jar EV_Registry.jar %APIPort% %DatabaseIP%
