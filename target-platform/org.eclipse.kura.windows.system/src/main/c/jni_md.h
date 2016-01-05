//====================================================================================================
//	Cross Platform Issue:
//	The jni.h header tries to include a file called jni_md.h to get some platform specific type info,
//	the file is different for Linux & Windows and exists in different directories. Since it's only a
//	small file and we know we will be built with mingw create a new jni_md.h in the local directory,
//	get the makefile to include it with -I ".", and define jlong as __int64

#ifndef _jni_md_h_
#define _jni_md_h_

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall

typedef long jint;

typedef __int64 jlong;

typedef signed char jbyte;

#endif
