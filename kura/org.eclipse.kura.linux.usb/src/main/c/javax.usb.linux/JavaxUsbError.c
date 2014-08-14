
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/*
 * Get String message for specified error number
 * @author Dan Streetman
 */
JNIEXPORT jstring JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeGetErrorMessage
  (JNIEnv *env, jclass JavaxUsb, jint error) {
	if (0 > error) error = -error;

	switch (error) {
		case EPERM			: return CheckedNewStringUTF( env, "Operation not permitted" );
		case ENOENT		: return CheckedNewStringUTF( env, "Submission aborted" );
		case EINTR			: return CheckedNewStringUTF( env, "Interrupted system call" );
		case EIO			: return CheckedNewStringUTF( env, "I/O error" );
		case ENXIO			: return CheckedNewStringUTF( env, "Cannot queue certain submissions on Universal Host Controller (unsupported in Linux driver)" );
		case EAGAIN		: return CheckedNewStringUTF( env, "Temporarily busy, try again" );
		case ENOMEM		: return CheckedNewStringUTF( env, "Out of memory" );
		case EACCES		: return CheckedNewStringUTF( env, "Permission denied" );
		case EBUSY			: return CheckedNewStringUTF( env, "Device or resource busy" );
		case ENODEV		: return CheckedNewStringUTF( env, "Device removed (or no such device)" );
		case EINVAL		: return CheckedNewStringUTF( env, "Invalid" );
		case ENOSYS		: return CheckedNewStringUTF( env, "Function not implemented" );
		case ENODATA		: return CheckedNewStringUTF( env, "No data available" );
		case ERESTART		: return CheckedNewStringUTF( env, "Interrupted system call should be restarted" );
		case EOPNOTSUPP	: return CheckedNewStringUTF( env, "Operation not supported on transport endpoint" );
		case ECONNRESET	: return CheckedNewStringUTF( env, "Connection reset by peer" );
		case ENOBUFS 		: return CheckedNewStringUTF( env, "No buffer space available" );
		case ETIMEDOUT		: return CheckedNewStringUTF( env, "Timed out" );
		case ECONNREFUSED	: return CheckedNewStringUTF( env, "Connection refused" );
		case EALREADY		: return CheckedNewStringUTF( env, "Operation already in progress" );
		case EINPROGRESS	: return CheckedNewStringUTF( env, "Operation now in progress" );
		default				: {
			char err[32];
			sprintf(err, "Error %d", (int)error);
			return CheckedNewStringUTF( env, err );
		}
	}
}

/*
 * Check if specified error is serious (continued error condition)
 * @author Dan Streetman
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeIsErrorSerious
  (JNIEnv *env, jclass JavaxUsb, jint error) {
	if (0 < error) error = -error;

	switch (error) {
		case -ENODEV :
		case -EPIPE :
			return JNI_TRUE;
		default :
			return JNI_FALSE;
	}
}
