@echo off
if exist "%~dp0composeApp\build\compose\binaries\main\app\NuvioTvMod\NuvioTvMod.exe" (
    cd /d "%~dp0composeApp\build\compose\binaries\main\app\NuvioTvMod"
    start "" "NuvioTvMod.exe"
    exit
)
if exist "%~dp0composeApp\build\compose\binaries\main\app\Nuvio Alpha\Nuvio Alpha.exe" (
    cd /d "%~dp0composeApp\build\compose\binaries\main\app\Nuvio Alpha"
    start "" "Nuvio Alpha.exe"
    exit
)
if exist "%~dp0composeApp\build\compose\binaries\main\app\Nuvio\Nuvio.exe" (
    cd /d "%~dp0composeApp\build\compose\binaries\main\app\Nuvio"
    start "" "Nuvio.exe"
    exit
)
exit
