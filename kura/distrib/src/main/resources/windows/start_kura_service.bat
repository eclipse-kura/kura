@echo off

call set_kura_paths.bat

set KURA_ROOT=%ALLUSERSPROFILE%
set KURA_HOME=%KURA_ROOT%\kura\kura
set KURA_HOME_URI=/%KURA_HOME:\=/%
set KURA_DATA=%KURA_ROOT%\kura\data
set KURA_TEMP=%TEMP%\kura

if not exist %KURA_TEMP% mkdir %KURA_TEMP%
if not exist %KURA_TEMP%\Configuration mkdir %KURA_TEMP%\Configuration

copy %KURA_HOME%\config.ini %KURA_TEMP%\Configuration

java -Xms256m -Xmx256m -Dkura.os.version=win32 ^
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
	-jar "%~dp0\plugins\org.eclipse.osgi_3.8.1.v20120830-144521.jar" ^
	-configuration %KURA_TEMP%\Configuration ^
	-console ^
	-consoleLog	
