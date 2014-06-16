
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAXUSBCHECKS_H
#define _JAVAXUSBCHECKS_H

/* exception checks */

#define check_for_exception(env) get_exception(env,1)
#define check_for_exception_noexit(env) get_exception(env,0)

/* Non-static checked JNI function wrappers */

#define CheckedGetObjectClass(env,object) debugGetObjectClass(__FILE__,__func__,__LINE__,env,object,#env","#object)
#define CheckedGetMethodID(env,class,name,id) debugGetMethodID(__FILE__,__func__,__LINE__,env,class,name,id,#env","#class","#name","#id)
#define CheckedGetFieldID(env,class,name,id) debugGetFieldID(__FILE__,__func__,__LINE__,env,class,name,id,#env","#class","#name","#id)
#define CheckedNewStringUTF(env,string) debugNewStringUTF(__FILE__,__func__,__LINE__,env,string,#env","#string)
#define CheckedNewGlobalRef(env,object) debugNewGlobalRef(__FILE__,__func__,__LINE__,env,object,#env","#object)
#define CheckedDeleteLocalRef(env,object) debugDeleteLocalRef(__FILE__,__func__,__LINE__,env,object,#env","#object)
#define CheckedDeleteGlobalRef(env,object) debugDeleteGlobalRef(__FILE__,__func__,__LINE__,env,object,#env","#object)

#define CheckedGetByteArrayRegion(env,array,offset,length,buffer) debugGetByteArrayRegion(__FILE__,__func__,__LINE__,env,array,offset,length,buffer,#env","#array","#offset","#length","#buffer)
#define CheckedSetByteArrayRegion(env,array,offset,length,buffer) debugSetByteArrayRegion(__FILE__,__func__,__LINE__,env,array,offset,length,buffer,#env","#array","#offset","#length","#buffer)
#define CheckedGetArrayLength(env,array) debugGetArrayLength(__FILE__,__func__,__LINE__,env,array,#env","#array)

//FIXME - implement
#define CheckedCallObjectMethod(env,object,method,args...) (*env)->CallObjectMethod(env,object,method,##args)
#define CheckedCallLongMethod(env,object,method,args...) (*env)->CallLongMethod(env,object,method,##args)
#define CheckedCallIntMethod(env,object,method,args...) (*env)->CallIntMethod(env,object,method,##args)
#define CheckedCallShortMethod(env,object,method,args...) (*env)->CallShortMethod(env,object,method,##args)
#define CheckedCallByteMethod(env,object,method,args...) (*env)->CallByteMethod(env,object,method,##args)
#define CheckedCallBooleanMethod(env,object,method,args...) (*env)->CallBooleanMethod(env,object,method,##args)
#define CheckedCallVoidMethod(env,object,method,args...) (*env)->CallVoidMethod(env,object,method,##args)

/* Static checked JNI function wrappers */

#define CheckedGetStaticMethodID(env,class,name,id) debugGetStaticMethodID(__FILE__,__func__,__LINE__,env,class,name,id,#env","#class","#name","#id)
#define CheckedGetStaticFieldID(env,class,name,id) debugGetStaticFieldID(__FILE__,__func__,__LINE__,env,class,name,id,#env","#class","#name","#id)

//FIXME - implement
#define CheckedCallStaticObjectMethod(env,class,method,args...) (*env)->CallStaticObjectMethod(env,class,method,##args)
#define CheckedCallStaticLongMethod(env,class,method,args...) (*env)->CallStaticLongMethod(env,class,method,##args)
#define CheckedCallStaticIntMethod(env,class,method,args...) (*env)->CallStaticIntMethod(env,class,method,##args)
#define CheckedCallStaticShortMethod(env,class,method,args...) (*env)->CallStaticShortMethod(env,class,method,##args)
#define CheckedCallStaticByteMethod(env,class,method,args...) (*env)->CallStaticByteMethod(env,class,method,##args)
#define CheckedCallStaticBooleanMethod(env,class,method,args...) (*env)->CallStaticBooleanMethod(env,class,method,##args)
#define CheckedCallStaticVoidMethod(env,class,method,args...) (*env)->CallStaticVoidMethod(env,class,method,##args)

/*
 * Check for and return an exception, or null.
 *
 * @should_exit If the JVM should exit immediately and without warning if there is an Exception.
 */
static inline jthrowable get_exception( JNIEnv *env, int should_exit )
{
	jthrowable e = (*env)->ExceptionOccurred( env );

	if (e) {
		log( LOG_CRITICAL, "Exception occured!\n" );
		if (should_exit)
			exit(1);
	}

	return e;
}

static inline void debug_exception( JNIEnv *env, const char *file, const char *func, int line, char *jnicall, char *args )
{
	if (JNI_TRUE == (*env)->ExceptionCheck(env)) {
		log( LOG_CRITICAL, "!! JNI Exception : file (%s) function (%s) line (%d)\n", file, func, line );
		log( LOG_CRITICAL, "!!!!! Failure at : (*env)->%s(%s)\n", jnicall, args );
		exit(1);
	}
}

static inline jclass debugGetObjectClass( const char *file, const char *func, int line, JNIEnv *env, jobject object, char *args )
{
	jclass class = (*env)->GetObjectClass( env, object );
	debug_exception( env, file, func, line, "GetObjectClass", args );
	return class;
}

static inline jmethodID debugGetMethodID( const char *file, const char *func, int line, JNIEnv *env, jclass class, char *name, char *id, char *args )
{
	jmethodID method = (*env)->GetMethodID( env, class, name, id );
	debug_exception( env, file, func, line, "GetMethodID", args );
	return method;
}

static inline jfieldID debugGetFieldID( const char *file, const char *func, int line, JNIEnv *env, jclass class, char *name, char *id, char *args )
{
	jfieldID field = (*env)->GetFieldID( env, class, name, id );
	debug_exception( env, file, func, line, "GetFieldID", args );
	return field;
}

static inline jstring debugNewStringUTF( const char *file, const char *func, int line, JNIEnv *env, char *str, char *args )
{
	jstring string = (*env)->NewStringUTF( env, str );
	debug_exception( env, file, func, line, "NewStringUTF", args );
	return string;
}

static inline jobject debugNewGlobalRef( const char *file, const char *func, int line, JNIEnv *env, jobject object, char *args )
{
	jobject newObject = (*env)->NewGlobalRef( env , object );
	debug_exception( env, file, func, line, "NewGlobalRef", args );
	return newObject;
}

static inline void debugDeleteLocalRef( const char *file, const char *func, int line, JNIEnv *env, jobject object, char *args )
{
	(*env)->DeleteLocalRef( env, object );
	debug_exception( env, file, func, line, "DeleteLocalRef", args );
}

static inline void debugDeleteGlobalRef( const char *file, const char *func, int line, JNIEnv *env, jobject object, char *args )
{
	(*env)->DeleteGlobalRef( env, object );
	debug_exception( env, file, func, line, "DeleteGlobalRef", args );
}

static inline void debugGetByteArrayRegion( const char *file, const char *func, int line, JNIEnv *env, jbyteArray array, jsize offset, jsize length, jbyte *buffer, char *args )
{
	(*env)->GetByteArrayRegion( env, array, offset, length, buffer );
	debug_exception( env, file, func, line, "GetByteArrayRegion", args );
}

static inline void debugSetByteArrayRegion( const char *file, const char *func, int line, JNIEnv *env, jbyteArray array, jsize offset, jsize length, jbyte *buffer, char *args )
{
	(*env)->SetByteArrayRegion( env, array, offset, length, buffer );
	debug_exception( env, file, func, line, "SetByteArrayRegion", args );
}

static inline jsize debugGetArrayLength( const char *file, const char *func, int line, JNIEnv *env, jarray array, char *args )
{
	jsize size = (*env)->GetArrayLength( env, array );
	debug_exception( env, file, func, line, "GetArrayLength", args );
	return size;
}

static inline jmethodID debugGetStaticMethodID( const char *file, const char *func, int line, JNIEnv *env, jclass class, char *name, char *id, char *args )
{
	jmethodID method = (*env)->GetStaticMethodID( env, class, name, id );
	debug_exception( env, file, func, line, "GetStaticMethodID", args );
	return method;
}

static inline jfieldID debugGetStaticFieldID( const char *file, const char *func, int line, JNIEnv *env, jclass class, char *name, char *id, char *args )
{
	jfieldID field = (*env)->GetStaticFieldID( env, class, name, id );
	debug_exception( env, file, func, line, "GetStaticFieldID", args );
	return field;
}

#endif /* _JAVAXUSBCHECKS_H */

