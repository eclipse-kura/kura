
KURAService32 : Release/x86/KURAService.exe

KURAService64 : Release/x64/KURAService.exe


Objs/x86/main32.o : main.c
	i686-w64-mingw32-gcc -c main.c -o $@

Objs/x86/utils32.o : utils.c
	i686-w64-mingw32-gcc -c utils.c -o $@

Objs/x86/service32.o : service.c
	i686-w64-mingw32-gcc -c service.c -o $@

Objs/x64/main64.o : main.c
	x86_64-w64-mingw32-gcc -c main.c -o $@

Objs/x64/utils64.o : utils.c
	x86_64-w64-mingw32-gcc -c utils.c -o $@

Objs/x64/service64.o : service.c
	x86_64-w64-mingw32-gcc -c service.c -o $@


Release/x86/KURAService.exe : Objs/x86/main32.o Objs/x86/utils32.o Objs/x86/service32.o
	i686-w64-mingw32-gcc Objs/x86/main32.o Objs/x86/utils32.o Objs/x86/service32.o -o $@

Release/x64/KURAService.exe : Objs/x64/main64.o Objs/x64/utils64.o Objs/x64/service64.o
	x86_64-w64-mingw32-gcc Objs/x64/main64.o Objs/x64/utils64.o Objs/x64/service64.o -o $@


