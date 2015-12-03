@REM MUST SET PATH TO mingw-w64 install folder \bin
del *.o
mkdir Release\win32\x64
mingw32-make.exe -f makefile.mingw_win32_x64
