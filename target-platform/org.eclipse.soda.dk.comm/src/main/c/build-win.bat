call "C:\Program Files\Microsoft Visual Studio 11.0\VC\vcvarsall.bat" x86
call "C:\Program Files (x86)\Microsoft Visual Studio 11.0\VC\vcvarsall.bat" x86

@REM set path to SDK include if VS used doesn't include Win32.Mak
set INCLUDE=%INCLUDE%;c:\Program Files\Microsoft SDKs\Windows\v7.1\Include

nmake -f makefile.win32