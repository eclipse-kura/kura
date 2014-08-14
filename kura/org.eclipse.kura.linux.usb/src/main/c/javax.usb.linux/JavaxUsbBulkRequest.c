
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/**
 * Submit a bulk pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int bulk_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	int offset = 0;
	int ret = 0;

	jclass LinuxPipeRequest = CheckedGetObjectClass( env, linuxPipeRequest );
	jmethodID getData = CheckedGetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	jmethodID getOffset = CheckedGetMethodID( env, LinuxPipeRequest, "getOffset", "()I" );
	jmethodID getLength = CheckedGetMethodID( env, LinuxPipeRequest, "getLength", "()I" );
	jbyteArray data = CheckedCallObjectMethod( env, linuxPipeRequest, getData );
	CheckedDeleteLocalRef( env, LinuxPipeRequest );

	offset = (unsigned int)CheckedCallIntMethod( env, linuxPipeRequest, getOffset );
	urb->buffer_length = (unsigned int)CheckedCallIntMethod( env, linuxPipeRequest, getLength );

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto END_SUBMIT;
	}

	CheckedGetByteArrayRegion( env, data, offset, urb->buffer_length, urb->buffer );

	urb->type = getBulkType();
	urb->flags = getBulkFlags(urb->flags);

	debug_urb( env, "bulk_pipe_request", urb );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_SUBMITURB, urb )))
		ret = -errno;

END_SUBMIT:
	if (ret)
		if (urb->buffer) free(urb->buffer);

	if (data) CheckedDeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a bulk pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_bulk_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxPipeRequest = CheckedGetObjectClass( env, linuxPipeRequest );
	jmethodID setActualLength = CheckedGetMethodID( env, LinuxPipeRequest, "setActualLength", "(I)V" );
	jmethodID getData = CheckedGetMethodID( env, LinuxPipeRequest, "getData", "()[B" );
	jmethodID getOffset = CheckedGetMethodID( env, LinuxPipeRequest, "getOffset", "()I" );
	jmethodID getLength = CheckedGetMethodID( env, LinuxPipeRequest, "getLength", "()I" );
	jbyteArray data = CheckedCallObjectMethod( env, linuxPipeRequest, getData );
	unsigned int offset = (unsigned int)CheckedCallIntMethod( env, linuxPipeRequest, getOffset );
	unsigned int length = (unsigned int)CheckedCallIntMethod( env, linuxPipeRequest, getLength );
	CheckedDeleteLocalRef( env, LinuxPipeRequest );

	if (length < urb->actual_length) {
		log( LOG_XFER_ERROR, "Actual length %d greater than requested length %d", urb->actual_length, length );
		urb->actual_length = length;
	}

	CheckedSetByteArrayRegion( env, data, offset, urb->actual_length, urb->buffer );

	CheckedCallVoidMethod( env, linuxPipeRequest, setActualLength, urb->actual_length );

	if (data) CheckedDeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

	return urb->status;
}
