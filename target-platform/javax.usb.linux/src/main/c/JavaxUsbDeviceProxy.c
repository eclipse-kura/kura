
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* These MUST match those defined in com/ibm/jusb/os/linux/LinuxRequest.java */
#define LINUX_PIPE_REQUEST 1
#define LINUX_SET_INTERFACE_REQUEST 2
#define LINUX_SET_CONFIGURATION_REQUEST 3
#define LINUX_CLAIM_INTERFACE_REQUEST 4
#define LINUX_IS_CLAIMED_INTERFACE_REQUEST 5
#define LINUX_RELEASE_INTERFACE_REQUEST 6
#define LINUX_ISOCHRONOUS_REQUEST 7

static void submitRequest( JNIEnv *env, int fd, jobject linuxRequest );
static void cancelRequest( JNIEnv *env, int fd, jobject linuxRequest );
static void completeRequest( JNIEnv *env, jobject linuxRequest );

/*
 * Proxy for all I/O with a device
 * @author Dan Streetman
 */
JNIEXPORT void JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeDeviceProxy
  ( JNIEnv *env, jclass JavaxUsb, jobject linuxDeviceProxy )
{
	int fd = 0;
	struct usbdevfs_urb *urb;

	jclass LinuxDeviceProxy;
	jobject linuxRequest;
	jstring jkey;
	jmethodID startCompleted, isRequestWaiting, getReadyRequest, getCancelRequest;
	jmethodID getKey;

	LinuxDeviceProxy = CheckedGetObjectClass( env, linuxDeviceProxy );
	startCompleted = CheckedGetMethodID( env, LinuxDeviceProxy, "startCompleted", "(I)V" );
	isRequestWaiting = CheckedGetMethodID( env, LinuxDeviceProxy, "isRequestWaiting", "()Z" );
	getReadyRequest = CheckedGetMethodID( env, LinuxDeviceProxy, "getReadyRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getCancelRequest = CheckedGetMethodID( env, LinuxDeviceProxy, "getCancelRequest", "()Lcom/ibm/jusb/os/linux/LinuxRequest;" );
	getKey = CheckedGetMethodID( env, LinuxDeviceProxy, "getKey", "()Ljava/lang/String;" );
	jkey = CheckedCallObjectMethod( env, linuxDeviceProxy, getKey );
	CheckedDeleteLocalRef( env, LinuxDeviceProxy );

	errno = 0;
	fd = open_device( env, jkey, O_RDWR );
	CheckedDeleteLocalRef( env, jkey );

	if (0 > fd) {
		log( LOG_XFER_ERROR, "Could not open node for device!" );
		CheckedCallVoidMethod( env, linuxDeviceProxy, startCompleted, errno );
		return;
	}

	CheckedCallVoidMethod( env, linuxDeviceProxy, startCompleted, 0 );

	/* run forever...? */
	while (1) {
		usleep( 1000 ); // Sleep 1 ms to avoid too much polling

		if (JNI_TRUE == CheckedCallBooleanMethod( env, linuxDeviceProxy, isRequestWaiting )) {
			if ((linuxRequest = CheckedCallObjectMethod( env, linuxDeviceProxy, getReadyRequest ))) {
				log( LOG_XFER_REQUEST, "Got Request" );
				submitRequest( env, fd, linuxRequest );
				CheckedDeleteLocalRef( env, linuxRequest );
				log( LOG_XFER_REQUEST, "Completed Request" );
			}

			if ((linuxRequest = CheckedCallObjectMethod( env, linuxDeviceProxy, getCancelRequest ))) {
				log( LOG_XFER_REQUEST, "Got Abort Request" );
				cancelRequest( env, fd, linuxRequest );
				CheckedDeleteLocalRef( env, linuxRequest );
				log( LOG_XFER_REQUEST, "Completed Abort Request" );
			}
		}

		errno = 0;
		if (!(ioctl( fd, USBDEVFS_REAPURBNDELAY, &urb ))) {
			log( LOG_XFER_REQUEST, "Got completed URB" );
			linuxRequest = urb->usercontext;
			completeRequest( env, linuxRequest );
			CheckedDeleteGlobalRef( env, linuxRequest );
			log( LOG_XFER_REQUEST, "Finished completed URB" );
		} else if (ENODEV == errno) {
			break;
		}
	}

	log( LOG_XFER_OTHER, "Device Proxy exiting." );

	close( fd );
}

/**
 * Submit a LinuxRequest.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
static void submitRequest( JNIEnv *env, int fd, jobject linuxRequest )
{
	int type, err, sync = 0;

	jclass LinuxRequest;
	jmethodID getType, setError, setCompleted;

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getType = CheckedGetMethodID( env, LinuxRequest, "getType", "()I" );
	setCompleted = CheckedGetMethodID( env, LinuxRequest, "setCompleted", "(Z)V" );
	setError = CheckedGetMethodID( env, LinuxRequest, "setError", "(I)V" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	type = CheckedCallIntMethod( env, linuxRequest, getType );

	log( LOG_XFER_OTHER, "Submitting Request.");

	switch (type) {
	case LINUX_PIPE_REQUEST:
		log( LOG_XFER_OTHER, "Submitting Pipe Request.");
		err = pipe_request( env, fd, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
		log( LOG_XFER_OTHER, "Submitting SetInterface Request.");
		err = set_interface( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_SET_CONFIGURATION_REQUEST:
		log( LOG_XFER_OTHER, "Submitting SetConfiguration Request.");
		err = set_configuration( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_CLAIM_INTERFACE_REQUEST:
		log( LOG_XFER_OTHER, "Submitting ClaimInterface Request.");
		err = claim_interface( env, fd, 1, linuxRequest );
		sync = 1;
		break;
	case LINUX_RELEASE_INTERFACE_REQUEST:
		log( LOG_XFER_OTHER, "Submitting ReleaseInterface Request.");
		err = claim_interface( env, fd, 0, linuxRequest );
		sync = 1;
		break;
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
		log( LOG_XFER_OTHER, "Submitting IsClaimed Request.");
		err = is_claimed( env, fd, linuxRequest );
		sync = 1;
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		log( LOG_XFER_OTHER, "Submitting Isochronous Request.");
		err = isochronous_request( env, fd, linuxRequest );
		break;
	default: /* ? */
		log( LOG_XFER_ERROR, "Unknown Request type %d", type );
		err = -EINVAL;
		break;
	}

	if (err)
		CheckedCallVoidMethod( env, linuxRequest, setError, err );

	if (sync || err)
		CheckedCallVoidMethod( env, linuxRequest, setCompleted, JNI_TRUE );
}

/**
 * Cancel a LinuxRequest.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
static void cancelRequest( JNIEnv *env, int fd, jobject linuxRequest )
{
	int type;

	jclass LinuxRequest;
	jmethodID getType;

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getType = CheckedGetMethodID( env, LinuxRequest, "getType", "()I" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	type = CheckedCallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		cancel_pipe_request( env, fd, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
	case LINUX_SET_CONFIGURATION_REQUEST:
	case LINUX_CLAIM_INTERFACE_REQUEST:
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
	case LINUX_RELEASE_INTERFACE_REQUEST:
		/* cannot abort these synchronous requests */
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		cancel_isochronous_request( env, fd, linuxRequest );
		break;
	default: /* ? */
		log( LOG_XFER_ERROR, "Unknown Request type %d", type );
		break;
	}	
}

/**
 * Complete a LinuxRequest.
 * @param env The JNIEnv.
 * @param linuxRequest The LinuxRequest.
 */
static void completeRequest( JNIEnv *env, jobject linuxRequest )
{
	int type, err;

	jclass LinuxRequest;
	jmethodID getType, setError, setCompleted;

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getType = CheckedGetMethodID( env, LinuxRequest, "getType", "()I" );
	setCompleted = CheckedGetMethodID( env, LinuxRequest, "setCompleted", "(Z)V" );
	setError = CheckedGetMethodID( env, LinuxRequest, "setError", "(I)V" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	type = CheckedCallIntMethod( env, linuxRequest, getType );

	switch (type) {
	case LINUX_PIPE_REQUEST:
		err = complete_pipe_request( env, linuxRequest );
		break;
	case LINUX_SET_INTERFACE_REQUEST:
	case LINUX_SET_CONFIGURATION_REQUEST:
	case LINUX_CLAIM_INTERFACE_REQUEST:
	case LINUX_IS_CLAIMED_INTERFACE_REQUEST:
	case LINUX_RELEASE_INTERFACE_REQUEST:
		/* these are synchronous, completion happens during submit */
		break;
	case LINUX_ISOCHRONOUS_REQUEST:
		err = complete_isochronous_request( env, linuxRequest );
		break;
	default: /* ? */
		log( LOG_XFER_ERROR, "Unknown Request type %d", type );
		err = -EINVAL;
		break;
	}

	if (err)
		CheckedCallVoidMethod( env, linuxRequest, setError, err );

	CheckedCallVoidMethod( env, linuxRequest, setCompleted, JNI_TRUE );
}
