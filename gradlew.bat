@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0"
set "PROPS=%ROOT%gradle\wrapper\gradle-wrapper.properties"
for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionUrl=" "%PROPS%"') do set "DIST_URL=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionSha256Sum=" "%PROPS%"') do set "DIST_SHA=%%B"
set "DIST_URL=!DIST_URL:\:=:!"
if /i not "!DIST_URL:~0,39!"=="https://services.gradle.org/distributions/" (
  echo Invalid distributionUrl 1>&2
  exit /b 2
)
for %%A in ("!DIST_URL!") do set "FILE=%%~nxA"
set "NAME=!FILE:.zip=!"
set "GRADLE_HOME_DEFAULT=%GRADLE_USER_HOME%"
if "!GRADLE_HOME_DEFAULT!"=="" set "GRADLE_HOME_DEFAULT=%USERPROFILE%\.gradle"
set "CACHE=!GRADLE_HOME_DEFAULT!\jarvis-wrapper\dists\!NAME!\!DIST_SHA!"
set "ZIP=!CACHE!\!NAME!.zip"
set "DIR=!CACHE!\!NAME!"
if exist "!DIR!\bin\gradle.bat" goto :run
mkdir "!CACHE!" >NUL 2>&1
if not exist "!ZIP!" (
  powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
    "$u='!DIST_URL!'; $o='!ZIP!'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile $o"
  if errorlevel 1 exit /b 3
)
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
  "$p='!ZIP!'; $expected='!DIST_SHA!'.ToLower(); $actual=(Get-FileHash -Algorithm SHA256 -Path $p).Hash.ToLower(); if($actual -ne $expected){throw 'Gradle distribution SHA-256 mismatch'}"
if errorlevel 1 exit /b 4
if exist "!DIR!" rmdir /s /q "!DIR!"
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command ^
  "$z='!ZIP!'; $d='!CACHE!'; Expand-Archive -LiteralPath $z -DestinationPath $d -Force"
if errorlevel 1 exit /b 5
if not exist "!DIR!\bin\gradle.bat" (
  echo Gradle distribution layout invalid 1>&2
  exit /b 6
)
:run
call "!DIR!\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
