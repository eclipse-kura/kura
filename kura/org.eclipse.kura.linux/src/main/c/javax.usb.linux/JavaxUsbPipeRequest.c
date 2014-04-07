
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* These MUST match those defined in com/ibm/jusb/os/linux/LinuxPipeRequest.java */
#define PIPE_CONTROL 1
#define PIPE_BULK 2
#define PIPE_INTERRUPT 3
#define PIPE_ISOCHRONOUS 4

/**
 * Submit a pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 * @return The error, or 0.
 */
int pipe_request( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, type, urbsize;

	jclass LinuxPipeRequest = NULL;
	jobject linuxPipeRequest = NULL;
	jmethodID setUrbAddress, getAcceptShortPacket, getEndpointAddress, getPipeType;
	jboolean acceptShortPacket;

	linuxPipeRequest = CheckedNewGlobalRef( env, linuxRequest );
	LinuxPipeRequest = CheckedGetObjectClass( env, linuxPipeRequest );
	getEndpointAddress = CheckedGetMethodID( env, LinuxPipeRequest, "getEndpointAddress", "()B" );
	getPipeType = CheckedGetMethodID( env, LinuxPipeRequest, "getPipeType", "()I" );
	type = CheckedCallIntMethod( env, linuxPipeRequest, getPipeType );
	setUrbAddress = CheckedGetMethodID( env, LinuxPipeRequest, "setUrbAddress", "(J)V" );
	getAcceptShortPacket = CheckedGetMethodID( env, LinuxPipeRequest, "getAcceptShortPacket", "()Z" );
	acceptShortPacket = CheckedCallBooleanMethod( env, linuxPipeRequest, getAcceptShortPacket );
	CheckedDeleteLocalRef( env, LinuxPipeRequest );

	urbsize = sizeof(*urb);
	if (PIPE_ISOCHRONOUS == type)
		urbsize += sizeof(struct usbdevfs_iso_packet_desc);

	if (!(urb = malloc(urbsize))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto end;
	}

	memset(urb, 0, sizeof(*urb));

	urb->endpoint = (unsigned char)CheckedCallByteMethod( env, linuxPipeRequest, getEndpointAddress );
	urb->usercontext = linuxPipeRequest;
	urb->flags |= getShortPacketFlag(acceptShortPacket);

	log( LOG_XFER_REQUEST, "Submitting URB" );

	switch (type) {
	case PIPE_CONTROL: ret = control_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_BULK: ret = bulk_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_INTERRUPT: ret = interrupt_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	case PIPE_ISOCHRONOUS: ret = isochronous_pipe_request( env, fd, linuxPipeRequest, urb ); break;
	default: log( LOG_XFER_ERROR, "Unknown pipe type %d", type ); ret = -EINVAL; break;
	}

	if (ret) {
		log( LOG_XFER_ERROR, "Could not submit URB (errno %d)", ret );
	} else {
		log( LOG_XFER_REQUEST, "Submitted URB" );
		CheckedCallVoidMethod( env, linuxPipeRequest, setUrbAddress, urb );
	}

end:
	if (ret) {
			if (linuxPipeRequest) CheckedDeleteGlobalRef( env, linuxPipeRequest );
			if (urb) free(urb);
	}

	return ret;
}

/**
 * Complete a pipe request.
 * @param env The JNIEnv.
 * @param linuxRequest The LinuxRequest.
 * @return The error or 0.
 */
int complete_pipe_request( JNIEnv *env, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, type;

	jclass LinuxPipeRequest;
	jmethodID getPipeType, getUrbAddress;

	LinuxPipeRequest = CheckedGetObjectClass( env, linuxPipeRequest );
	getPipeType = CheckedGetMethodID( env, LinuxPipeRequest, "getPipeType", "()I" );
	getUrbAddress = CheckedGetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()J" );
	type = CheckedCallIntMethod( env, linuxPipeRequest, getPipeType );
	CheckedDeleteLocalRef( env, LinuxPipeRequest );

	if (!(urb = (struct usbdevfs_urb*)CheckedCallLongMethod( env, linuxPipeRequest, getUrbAddress ))) {
		log( LOG_XFER_ERROR, "No URB to complete." );
		return -EINVAL;
	}

	log( LOG_XFER_REQUEST, "Completing URB." );
	debug_urb( env, "complete_pipe_request", urb );

	switch (type) {
	case PIPE_CONTROL: ret = complete_control_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_BULK: ret = complete_bulk_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_INTERRUPT: ret = complete_interrupt_pipe_request( env, linuxPipeRequest, urb ); break;
	case PIPE_ISOCHRONOUS: ret = complete_isochronous_pipe_request( env, linuxPipeRequest, urb ); break;
	default: log( LOG_XFER_ERROR, "Unknown pipe type %d", type); ret = -EINVAL; break;
	}

	free(urb);

	log( LOG_XFER_REQUEST, "Completed URB." );

	return ret;
}

/**
 * Abort a pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 */
void cancel_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest )
{
	struct usbdevfs_urb *urb;

	jclass LinuxPipeRequest;
	jmethodID getUrbAddress;

	LinuxPipeRequest = CheckedGetObjectClass( env, linuxPipeRequest );
	getUrbAddress = CheckedGetMethodID( env, LinuxPipeRequest, "getUrbAddress", "()J" );
	CheckedDeleteLocalRef( env, LinuxPipeRequest );

	log( LOG_XFER_REQUEST, "Canceling URB." );

	urb = (struct usbdevfs_urb *)CheckedCallLongMethod( env, linuxPipeRequest, getUrbAddress );

	if (!urb) {
		log( LOG_XFER_ERROR, "No URB to cancel." );
		return;
	}

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_DISCARDURB, urb )))
		log( LOG_XFER_ERROR, "Could not unlink urb %p (error %d)", urb, -errno );
}
