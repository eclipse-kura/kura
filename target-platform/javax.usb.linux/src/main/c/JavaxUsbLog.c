
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

#define JAVAXUSB_CLASSNAME "com/ibm/jusb/os/linux/JavaxUsb"

jboolean tracing = JNI_TRUE;
jboolean trace_default = JNI_TRUE;
jboolean trace_hotplug = JNI_TRUE;
jboolean trace_xfer = JNI_TRUE;
jboolean trace_urb = JNI_FALSE;
int trace_level = LOG_CRITICAL;
FILE *trace_output = NULL;

//FIXME - add parameter to modify this!!!
jboolean trace_flush = JNI_TRUE;

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTracing
(JNIEnv *env, jclass JavaxUsb, jboolean enable)
{
	tracing = enable;
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceType
(JNIEnv *env, jclass JavaxUsb, jboolean setting, jstring jname)
{
	const char *name = (*env)->GetStringUTFChars( env, jname, NULL );
	if (!strcmp("default", name))
		trace_default = setting;
	else if (!strcmp("hotplug", name))
		trace_hotplug = setting;
	else if (!strcmp("xfer", name))
		trace_xfer = setting;
	else if (!strcmp("urb", name))
		trace_urb = setting;
	else
		log( LOG_ERROR, "No match for log type %s", name );
	(*env)->ReleaseStringUTFChars( env, jname, name );
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceLevel
(JNIEnv *env, jclass JavaxUsb, jint level)
{
	if (LOG_LEVEL_MIN > level || LOG_LEVEL_MAX < level)
		log( LOG_ERROR, "Invalid trace level %d", level );
	else
		trace_level = level;
}

JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeSetTraceOutput
(JNIEnv *env, jclass JavaxUsb, jint output, jstring filename)
{
	switch (output) {
		case 1:
			trace_output = stdout;
			break;

		case 2:
			trace_output = stderr;
			break;

		case 3:
		case 4:
			{
				const char *name = (*env)->GetStringUTFChars( env, filename, NULL );
				FILE *f = NULL;
				const char *mode = (3 == output ? "w" : "a"); /* w = trunc, a = append */

				if ((f = fopen(name, mode)))
					trace_output = f;
				else
					log( LOG_ERROR, "Could not open file %s for JNI tracing : %s\n", name, strerror(errno) );

				(*env)->ReleaseStringUTFChars( env, filename, name );
			}
			break;

		default:
			log( LOG_ERROR, "Invalid trace output setting %d\n", output );
			break;
	}
}

