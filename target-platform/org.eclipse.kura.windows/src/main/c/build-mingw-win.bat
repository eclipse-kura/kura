@REM MUST SET PATH TO mingw32 install folder \bin
del *.o
mkdir Release\win32\x86
mingw32-make.exe -f makefile.mingw_win32
