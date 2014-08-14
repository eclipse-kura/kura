
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
 * Submit a control pipe request.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxControlRequest The LinuxControlRequest.
 * @param usb The usbdevfs_urb.
 * @return The error that occurred, or 0.
 */
int control_pipe_request( JNIEnv *env, int fd, jobject linuxControlRequest, struct usbdevfs_urb *urb )
{
	int offset = 0;
	int ret = 0;

	jclass LinuxControlRequest = NULL;
	jmethodID getSetupPacket, getData, getOffset, getLength;
	jbyteArray setupPacket = NULL, data = NULL;

	LinuxControlRequest = CheckedGetObjectClass( env, linuxControlRequest );
	getSetupPacket = CheckedGetMethodID( env, LinuxControlRequest, "getSetupPacket", "()[B" );
	getData = CheckedGetMethodID( env, LinuxControlRequest, "getData", "()[B" );
	getOffset = CheckedGetMethodID( env, LinuxControlRequest, "getOffset", "()I" );
	getLength = CheckedGetMethodID( env, LinuxControlRequest, "getLength", "()I" );
	setupPacket = CheckedCallObjectMethod( env, linuxControlRequest, getSetupPacket );
	data = CheckedCallObjectMethod( env, linuxControlRequest, getData );
	CheckedDeleteLocalRef( env, LinuxControlRequest );

	offset = (unsigned int)CheckedCallIntMethod( env, linuxControlRequest, getOffset );
	urb->buffer_length = (unsigned int)CheckedCallIntMethod( env, linuxControlRequest, getLength );

	if (!(urb->buffer = malloc(urb->buffer_length + 8))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto END_SUBMIT;
	}

	CheckedGetByteArrayRegion( env, setupPacket, 0, 8, urb->buffer );
	CheckedGetByteArrayRegion( env, data, offset, urb->buffer_length, urb->buffer + 8 );

	/* Add 8 for the setup packet */
	urb->buffer_length += 8;

	urb->type = getControlType();
	urb->flags = getControlFlags(urb->flags);

	debug_urb( env, "control_pipe_request", urb );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_SUBMITURB, urb )))
		ret = -errno;

END_SUBMIT:
	if (ret)
		if (urb->buffer) free(urb->buffer);

	if (setupPacket) CheckedDeleteLocalRef( env, setupPacket );
	if (data) CheckedDeleteLocalRef( env, data );

	return ret;
}

/**
 * Complete a control pipe request.
 * @param env The JNIEnv.
 * @param linuxControlRequest The LinuxControlRequest.
 * @param urb the usbdevfs_usb.
 * @return The error that occurred, or 0.
 */
int complete_control_pipe_request( JNIEnv *env, jobject linuxControlRequest, struct usbdevfs_urb *urb )
{
	jclass LinuxControlRequest = CheckedGetObjectClass( env, linuxControlRequest );
	jmethodID setActualLength = CheckedGetMethodID( env, LinuxControlRequest, "setActualLength", "(I)V" );
	jmethodID getData = CheckedGetMethodID( env, LinuxControlRequest, "getData", "()[B" );
	jmethodID getOffset = CheckedGetMethodID( env, LinuxControlRequest, "getOffset", "()I" );
	jmethodID getLength = CheckedGetMethodID( env, LinuxControlRequest, "getLength", "()I" );
	jbyteArray data = CheckedCallObjectMethod( env, linuxControlRequest, getData );
	unsigned int offset = (unsigned int)CheckedCallIntMethod( env, linuxControlRequest, getOffset );
	unsigned int length = (unsigned int)CheckedCallIntMethod( env, linuxControlRequest, getLength );
	CheckedDeleteLocalRef( env, LinuxControlRequest );

	if (length < urb->actual_length) {
		log( LOG_XFER_ERROR, "Actual length %d greater than requested length %d", urb->actual_length, length );
		urb->actual_length = length;
	}

	CheckedSetByteArrayRegion( env, data, offset, urb->actual_length, urb->buffer + 8 );

	CheckedCallVoidMethod( env, linuxControlRequest, setActualLength, urb->actual_length );

	if (data) CheckedDeleteLocalRef( env, data );
	if (urb->buffer) free(urb->buffer);

	return urb->status;
}

/**
 * Set a configuration.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxSetConfigurationRequest The LinuxSetConfigurationRequest.
 * @return The error, or 0.
 */
int set_configuration( JNIEnv *env, int fd, jobject linuxSetConfigurationRequest )
{
	unsigned int *configuration = NULL;
	int ret = 0;

	jclass LinuxSetConfigurationRequest;
	jmethodID getConfiguration;

	LinuxSetConfigurationRequest = CheckedGetObjectClass( env, linuxSetConfigurationRequest );
	getConfiguration = CheckedGetMethodID( env, LinuxSetConfigurationRequest, "getConfiguration", "()I" );
	CheckedDeleteLocalRef( env, LinuxSetConfigurationRequest );

	if (!(configuration = malloc(sizeof(*configuration)))) {
		log( LOG_CRITICAL, "Out of memory!" );
		return -ENOMEM;
	}

	*configuration = (unsigned int)CheckedCallIntMethod( env, linuxSetConfigurationRequest, getConfiguration );

	log( LOG_XFER_META, "Setting configuration to %d", *configuration );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_SETCONFIGURATION, configuration )))
		ret = -errno;

	if (ret)
		log( LOG_XFER_ERROR, "Could not set configuration (errno %d)", ret );
	else
		log( LOG_XFER_META, "set_configuration : Set configuration" );

	free(configuration);

	return ret;
}

/**
 * Set a interface setting.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxSetInterfaceRequest The LinuxSetInterfaceRequest.
 * @return The error, or 0.
 */
int set_interface( JNIEnv *env, int fd, jobject linuxSetInterfaceRequest )
{
	struct usbdevfs_setinterface *interface = NULL;
	int ret = 0;

	jclass LinuxSetInterfaceRequest;
	jmethodID getInterface, getSetting;

	LinuxSetInterfaceRequest = CheckedGetObjectClass( env, linuxSetInterfaceRequest );
	getInterface = CheckedGetMethodID( env, LinuxSetInterfaceRequest, "getInterface", "()I" );
	getSetting = CheckedGetMethodID( env, LinuxSetInterfaceRequest, "getSetting", "()I" );
	CheckedDeleteLocalRef( env, LinuxSetInterfaceRequest );

	if (!(interface = malloc(sizeof(*interface)))) {
		log( LOG_CRITICAL, "Out of memory!" );
		return -ENOMEM;
	}

	interface->interface = (unsigned int)CheckedCallIntMethod( env, linuxSetInterfaceRequest, getInterface );
	interface->altsetting = (unsigned int)CheckedCallIntMethod( env, linuxSetInterfaceRequest, getSetting );

	log( LOG_XFER_META, "Setting interface %d to setting %d", interface->interface, interface->altsetting );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_SETINTERFACE, interface )))
		ret = -errno;

	if (ret)
		log( LOG_XFER_ERROR, "Could not set interface (errno %d)", ret );
	else
		log( LOG_XFER_META, "Set interface" );

	free(interface);

	return ret;
}
