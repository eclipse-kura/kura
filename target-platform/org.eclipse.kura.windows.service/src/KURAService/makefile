
all : Release/x86/KURAService.exe Release/x64/KURAService.exe


Objs/main32.o : main.c
	i686-w64-mingw32-gcc -c main.c -o Objs/main32.o

Objs/utils32.o : utils.c
	i686-w64-mingw32-gcc -c utils.c -o Objs/utils32.o

Objs/service32.o : service.c
	i686-w64-mingw32-gcc -c service.c -o Objs/service32.o

Objs/main64.o : main.c
	x86_64-w64-mingw32-gcc -c main.c -o Objs/main64.o

Objs/utils64.o : utils.c
	x86_64-w64-mingw32-gcc -c utils.c -o Objs/utils64.o

Objs/service64.o : service.c
	x86_64-w64-mingw32-gcc -c service.c -o Objs/service64.o


Release/x86/KURAService.exe : Objs/main32.o Objs/utils32.o Objs/service32.o
	i686-w64-mingw32-gcc Objs/main32.o Objs/utils32.o Objs/service32.o -o Release/x86/KURAService.exe

Release/x64/KURAService.exe : Objs/main64.o Objs/utils64.o Objs/service64.o
	x86_64-w64-mingw32-gcc Objs/main64.o Objs/utils64.o Objs/service64.o -o Release/x64/KURAService.exe


