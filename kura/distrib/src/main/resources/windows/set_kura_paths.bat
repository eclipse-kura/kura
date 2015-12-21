@echo off

@rem Arg1 is the install directory
set INST_DIR=%1

@rem Replace start_kura.bat 
cd %INST_DIR%
set textfile=start_kura.bat
@echo off &setlocal
(for /f "delims=" %%i in (%textfile%) do (
    set "line=%%i"
    setlocal enabledelayedexpansion
    set "line=!line:c:\opt\eclipse=%ALLUSERSPROFILE%!"
    set "line=!line:\tmp\.kura=%TEMP%\kura!"
    echo(!line!
    endlocal
))>"repl_out.txt"
del %textfile%
rename repl_out.txt %textfile%

rem Config files will need to change path to Unix-style / and remove "
set INST_DIR_=%INST_DIR:"=%
set INST_DIR_=%INST_DIR_:\=/%
set DATA_DIR=%ALLUSERSPROFILE:\=/%
set TEMP_DIR=%TEMP:\=/%

@echo off &setlocal
cd %ALLUSERSPROFILE%\Kura\kura
set textfile=kura.properties
(for /f "delims=" %%i in (%textfile%) do (
    set "line=%%i"
    setlocal enabledelayedexpansion
    set "line=!line:/opt/eclipse/kura/kura/plugins=%INST_DIR_%/kura/plugins!"
    set "line=!line:/opt/eclipse/kura=%DATA_DIR%/kura!"
    set "line=!line:/tmp/.kura=%TEMP_DIR%/kura!"
    echo(!line!
    endlocal
))>"repl_out.txt"
del %textfile%
rename repl_out.txt %textfile%

@echo off &setlocal
cd %ALLUSERSPROFILE%\Kura\kura
set textfile=log4j.properties
(for /f "delims=" %%i in (%textfile%) do (
    set "line=%%i"
    setlocal enabledelayedexpansion
    set "line=!line:/var=%TEMP_DIR%/kura!"
    echo(!line!
    endlocal
))>"repl_out.txt"
del %textfile%
rename repl_out.txt %textfile%
