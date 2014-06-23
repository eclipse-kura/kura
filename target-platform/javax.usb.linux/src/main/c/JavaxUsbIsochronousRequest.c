
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

/* simple isochronous functions */

/**
 * Submit a simple isochronous pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int isochronous_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
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

	urb->type = getIsochronousType();
	urb->flags = getIsochronousFlags(urb->flags);
	urb->number_of_packets = 1;
	urb->iso_frame_desc[0].length = urb->buffer_length;

	debug_urb( env, "isochronous_pipe_request", urb );

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
 * Complete a simple isochronous pipe request.
 * @param env The JNIEnv.
 * @param linuxPipeRequest The LinuxPipeRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_isochronous_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb )
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

	CheckedCallVoidMethod( env, linuxPipeRequest, setActualLength, urb->iso_frame_desc[0].actual_length );

	if (data) CheckedDeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

	return urb->iso_frame_desc[0].status;
}

/* Complex isochronous functions */

static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb );

/**
 * Submit a complex isochronous pipe request.
 * Note that this does not support _disabling_ short packets.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 * @return The error that occurred, or 0.
 */
int isochronous_request( JNIEnv *env, int fd, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;
	int ret = 0, npackets, bufsize, urbsize;

	jclass LinuxIsochronousRequest;
	jmethodID getAcceptShortPacket, getTotalLength, size, setUrbAddress, getEndpointAddress;

	LinuxIsochronousRequest = CheckedGetObjectClass( env, linuxIsochronousRequest );
	getAcceptShortPacket = CheckedGetMethodID( env, LinuxIsochronousRequest, "getAcceptShortPacket", "()Z" );
	getTotalLength = CheckedGetMethodID( env, LinuxIsochronousRequest, "getTotalLength", "()I" );
	size = CheckedGetMethodID( env, LinuxIsochronousRequest, "size", "()I" );
	setUrbAddress = CheckedGetMethodID( env, LinuxIsochronousRequest, "setUrbAddress", "(J)V" );
	getEndpointAddress = CheckedGetMethodID( env, LinuxIsochronousRequest, "getEndpointAddress", "()B" );
	npackets = (unsigned int)CheckedCallIntMethod( env, linuxIsochronousRequest, size );
	bufsize = (unsigned int)CheckedCallIntMethod( env, linuxIsochronousRequest, getTotalLength );
	CheckedDeleteLocalRef( env, LinuxIsochronousRequest );

	urbsize = sizeof(*urb) + (npackets * sizeof(struct usbdevfs_iso_packet_desc));

	if (!(urb = malloc(urbsize))) {
		log( LOG_CRITICAL, "Out of memory! (%d bytes needed)", urbsize );
		ret = -ENOMEM;
		goto ISOCHRONOUS_REQUEST_END;
	}

	memset(urb, 0, urbsize);

	urb->number_of_packets = npackets;
	urb->buffer_length = bufsize;

	if (!(urb->buffer = malloc(urb->buffer_length))) {
		log( LOG_CRITICAL, "Out of memory! (%d needed)", urb->buffer_length );
		ret = -ENOMEM;
		goto ISOCHRONOUS_REQUEST_END;
	}

	memset(urb->buffer, 0, urb->buffer_length);

	if ((ret = create_iso_buffer( env, linuxIsochronousRequest, urb )))
		goto ISOCHRONOUS_REQUEST_END;

	urb->type = getIsochronousType();
	urb->usercontext = CheckedNewGlobalRef( env, linuxIsochronousRequest );
	urb->endpoint = (unsigned char)CheckedCallByteMethod( env, linuxIsochronousRequest, getEndpointAddress );
	urb->flags = getIsochronousFlags(urb->flags);

	log( LOG_XFER_OTHER, "Submitting URB" );
	debug_urb( env, "isochronous_request", urb );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_SUBMITURB, urb )))
		ret = -errno;

	if (ret) {
		log( LOG_XFER_ERROR, "Could not submit URB (errno %d)", ret );
	} else {
		log( LOG_XFER_OTHER, "isochronous_request : Submitted URB" );
		CheckedCallVoidMethod( env, linuxIsochronousRequest, setUrbAddress, urb );
	}

ISOCHRONOUS_REQUEST_END:
	if (ret) {
		if (urb) {
			if (urb->usercontext) CheckedDeleteGlobalRef( env, urb->usercontext);
			if (urb->buffer) free(urb->buffer);
			free(urb);
		}
	}

	return ret;
}

/**
 * Cancel a complex isochronous request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 */
void cancel_isochronous_request( JNIEnv *env, int fd, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;

	jclass LinuxIsochronousRequest;
	jmethodID getUrbAddress;

	LinuxIsochronousRequest = CheckedGetObjectClass( env, linuxIsochronousRequest );
	getUrbAddress = CheckedGetMethodID( env, LinuxIsochronousRequest, "getUrbAddress", "()J" );
	CheckedDeleteLocalRef( env, LinuxIsochronousRequest );

	log( LOG_XFER_OTHER, "Canceling URB" );

	urb = (struct usbdevfs_urb *)CheckedCallLongMethod( env, linuxIsochronousRequest, getUrbAddress );

	if (!urb) {
		log( LOG_XFER_ERROR, "No URB to cancel" );
		return;
	}

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_DISCARDURB, urb )))
		log( LOG_XFER_ERROR, "Could not unlink urb %p (error %d)", urb, -errno );
}

/**
 * Complete a complex isochronous pipe request.
 * @param env The JNIEnv.
 * @param linuxIsochronousRequest The LinuxIsochronousRequest.
 * @return The error that occurred, or 0.
 */
int complete_isochronous_request( JNIEnv *env, jobject linuxIsochronousRequest )
{
	struct usbdevfs_urb *urb;
	int ret;

	jclass LinuxIsochronousRequest;
	jmethodID getUrbAddress;

	LinuxIsochronousRequest = CheckedGetObjectClass( env, linuxIsochronousRequest );
	getUrbAddress = CheckedGetMethodID( env, LinuxIsochronousRequest, "getUrbAddress", "()J" );
	CheckedDeleteLocalRef( env, LinuxIsochronousRequest );

	if (!(urb = (struct usbdevfs_urb*)CheckedCallLongMethod( env, linuxIsochronousRequest, getUrbAddress ))) {
		log( LOG_XFER_ERROR, "No URB to complete!" );
		return -EINVAL;
	}

	log( LOG_XFER_OTHER, "Completing URB" );
	debug_urb( env, "complete_isochronous_request", urb );

	ret = destroy_iso_buffer( env, linuxIsochronousRequest, urb );

	free(urb->buffer);
	free(urb);

	log( LOG_XFER_OTHER, "Completed URB" );

	return ret;
}

/**
 * Create the multi-packet ISO buffer and iso_frame_desc's.
 */
static inline int create_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0, buffer_offset = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getDirection, getData, getOffset, getLength;
	jbyteArray jbuf;

	LinuxIsochronousRequest = CheckedGetObjectClass( env, linuxIsochronousRequest );
	getDirection = CheckedGetMethodID( env, LinuxIsochronousRequest, "getDirection", "()B" );
	getData = CheckedGetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	getOffset = CheckedGetMethodID( env, LinuxIsochronousRequest, "getOffset", "(I)I" );
	getLength = CheckedGetMethodID( env, LinuxIsochronousRequest, "getLength", "(I)I" );
	CheckedDeleteLocalRef( env, LinuxIsochronousRequest );

	for (i=0; i<urb->number_of_packets; i++) {
	  if (!(jbuf = CheckedCallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
		log( LOG_XFER_ERROR, "Could not access data at index %d", i );
		return -EINVAL;
	  }

	  offset = CheckedCallIntMethod( env, linuxIsochronousRequest, getOffset, i );
	  urb->iso_frame_desc[i].length = CheckedCallIntMethod( env, linuxIsochronousRequest, getLength, i );
	  CheckedGetByteArrayRegion( env, jbuf, offset, urb->iso_frame_desc[i].length, urb->buffer + buffer_offset );
	  buffer_offset += urb->iso_frame_desc[i].length;

	  CheckedDeleteLocalRef( env, jbuf );
	}

	return 0;
}

/**
 * Destroy the multi-packet ISO buffer and iso_frame_desc's.
 */
static inline int destroy_iso_buffer( JNIEnv *env, jobject linuxIsochronousRequest, struct usbdevfs_urb *urb )
{
	int i, offset = 0, buffer_offset = 0, actual_length = 0;

	jclass LinuxIsochronousRequest;
	jmethodID getDirection, getData, getOffset, setActualLength, setError;
	jbyteArray jbuf;

	LinuxIsochronousRequest = CheckedGetObjectClass( env, linuxIsochronousRequest );
	getDirection = CheckedGetMethodID( env, LinuxIsochronousRequest, "getDirection", "()B" );
	getData = CheckedGetMethodID( env, LinuxIsochronousRequest, "getData", "(I)[B" );
	getOffset = CheckedGetMethodID( env, LinuxIsochronousRequest, "getOffset", "(I)I" );
	setActualLength = CheckedGetMethodID( env, LinuxIsochronousRequest, "setActualLength", "(II)V" );
	setError = CheckedGetMethodID( env, LinuxIsochronousRequest, "setError", "(II)V" );
	CheckedDeleteLocalRef( env, LinuxIsochronousRequest );

	for (i=0; i<urb->number_of_packets; i++) {
	  if (!(jbuf = CheckedCallObjectMethod( env, linuxIsochronousRequest, getData, i ))) {
		log( LOG_XFER_ERROR, "Could not access data buffer at index %d", i );
		return -EINVAL;
	  }

	  offset = CheckedCallIntMethod( env, linuxIsochronousRequest, getOffset, i );
	  actual_length = urb->iso_frame_desc[i].actual_length;
	  if ((offset + actual_length) > CheckedGetArrayLength( env, jbuf )) {
		log( LOG_XFER_ERROR, "Data buffer %d too small, data truncated!", i );
		actual_length = CheckedGetArrayLength( env, jbuf ) - offset;
	  }
	  CheckedSetByteArrayRegion( env, jbuf, offset, actual_length, urb->buffer + buffer_offset );
	  CheckedCallVoidMethod( env, linuxIsochronousRequest, setActualLength, i, actual_length );
	  if (0 > urb->iso_frame_desc[i].status)
		CheckedCallVoidMethod( env, linuxIsochronousRequest, setError, i, urb->iso_frame_desc[i].status );
	  buffer_offset += urb->iso_frame_desc[i].length;

	  CheckedDeleteLocalRef( env, jbuf );
	}
			
//FIXME - what should we return here, this or something based on each packet's status?
	return urb->status;
}

