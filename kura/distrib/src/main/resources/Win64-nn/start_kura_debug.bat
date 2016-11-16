@echo off

set KURA_ROOT=c:\opt\eclipse
set KURA_HOME=%KURA_ROOT%\kura\kura
set KURA_HOME_URI=/%KURA_HOME:\=/%
set KURA_DATA=%KURA_ROOT%\kura\data
set KURA_TEMP=\tmp\.kura

if not exist %KURA_TEMP% mkdir %KURA_TEMP%
if not exist %KURA_TEMP%\Configuration mkdir %KURA_TEMP%\Configuration

@rem Temporary patch for hardcoded /tmp paths
if not exist c:\tmp mkdir c:\tmp

copy %KURA_HOME%\config.ini %KURA_TEMP%\Configuration

java	-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000 ^
	-Xms256m -Xmx256m -Dkura.os.version=win32 ^
	-Dkura.arch=x86_64 ^
	-Dtarget.device=windows ^
	-Declipse.ignoreApp=true ^
	-Dkura.home=%KURA_HOME% ^
	-Dkura.data.dir=%KURA_DATA% ^
	-Dkura.configuration=file:%KURA_HOME_URI%/kura.properties ^
	-Dkura.custom.configuration=file:%KURA_HOME_URI%/kura_custom.properties ^
	-Ddpa.configuration=%KURA_HOME%\dpa.properties ^
	-Dlog4j.configuration=file:%KURA_HOME_URI%/log4j.properties ^
	-Djdk.tls.trustNameService=true ^
	-jar "%~dp0\plugins\org.eclipse.osgi_3.11.1.v20160708-1632.jar" ^
	-configuration %KURA_TEMP%\Configuration ^
	-console ^
	-consoleLog	
