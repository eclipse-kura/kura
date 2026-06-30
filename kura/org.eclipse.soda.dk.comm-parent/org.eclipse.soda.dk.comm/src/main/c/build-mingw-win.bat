@REM MUST HAVE mingw32 and mingw-w64 in PATH
del Objs\x86\*.o
del Objs\x64\*.o
mkdir Objs\x86
mkdir Objs\x64
del Release\win32\x86\*.dll
del Release\win32\x64\*.dll
mkdir Release\win32\x86
mkdir Release\win32\x64

mingw32-make.exe -f makefile.mingw SodaDkComm32
mingw32-make.exe -f makefile.mingw SodaDkComm64
