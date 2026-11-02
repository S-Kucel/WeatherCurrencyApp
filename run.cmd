@echo off
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
if errorlevel 1 (
  echo.
  echo Nie udalo sie uruchomic projektu. Sprawdz komunikat powyzej.
  pause
  exit /b 1
)
