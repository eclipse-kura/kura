
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
 * JavaxUsbInterfaceRequest.c
 *
 * This handles requests to claim/release interfaces
 *
 */

/**
 * Disconnect the driver from the specified interface.
 * @param end The JNIEnv.
 * @param fd The file descriptor.
 * @param interface The interface number.
 */
void disconnect_interface_driver(JNIEnv *env, int fd, int interface)
{
	struct usbdevfs_ioctl *disc_ioctl = NULL;

	if (!(disc_ioctl = malloc(sizeof(*disc_ioctl)))) {
		log( LOG_CRITICAL, "Out of memory!" );
		return;
	}

	disc_ioctl->ifno = interface;
	disc_ioctl->ioctl_code = USBDEVFS_DISCONNECT;
	disc_ioctl->data = NULL;

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_IOCTL, disc_ioctl ))) {
		if (ENODATA == errno)
			log( LOG_ERROR, "No driver associated with interface %d.", interface );
		else if (ENOSYS == errno)
			log( LOG_ERROR, "This kernel does not support driver disconnection via USBDEVFS_DISCONNECT." );
		else
			log( LOG_ERROR, "Could not disconnect driver from interface %d : %s", interface, strerror(errno) );
	} else {
		log( LOG_INFO, "Disconnected driver from interface %d", interface );
	}

	free(disc_ioctl);
}

/**
 * Claim or release a specified interface.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param claim Whether to claim or release.
 * @param linuxRequest The request.
 * @return error.
 */
int claim_interface( JNIEnv *env, int fd, int claim, jobject linuxRequest )
{
	int ret = 0, *interface = NULL;
	int triedDisconnect = 0;

	jclass LinuxRequest = NULL;
	jmethodID getInterfaceNumber, getForceClaim;
	jboolean forceClaim;

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getInterfaceNumber = CheckedGetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	getForceClaim = CheckedGetMethodID( env, LinuxRequest, "getForceClaim", "()Z" );
	CheckedDeleteLocalRef( env, LinuxRequest );
	forceClaim = CheckedCallBooleanMethod( env, linuxRequest, getForceClaim );

	if (!(interface = malloc(sizeof(*interface)))) {
		log( LOG_CRITICAL, "Out of memory!" );
		return -ENOMEM;
	}

	*interface = CheckedCallIntMethod( env, linuxRequest, getInterfaceNumber );

	while(1) {
		ret = 0;

		log( LOG_FUNC, "%s interface %d", claim ? "Claiming" : "Releasing", *interface );

		errno = 0;
		if (0 > (ioctl( fd, claim ? USBDEVFS_CLAIMINTERFACE : USBDEVFS_RELEASEINTERFACE, interface )))
			ret = -errno;

		if (ret)
			log( LOG_ERROR, "Could not %s interface %d : errno %d", claim ? "claim" : "release", *interface, ret );
		else
			log( LOG_FUNC, "%s interface %d", claim ? "Claimed" : "Released", *interface );

		if (ret && claim && !triedDisconnect && (JNI_TRUE == forceClaim)) {
			triedDisconnect = 1;
			disconnect_interface_driver(env, fd, *interface);
		} else
			break;
	}

	free(interface);

	return ret;
}

/**
 * Check if an interface is claimed.
 * @param env The JNIEnv.
 * @param fd The file descriptor.
 * @param linuxRequest The LinuxRequest.
 */
int is_claimed( JNIEnv *env, int fd, jobject linuxRequest )
{
	struct usbdevfs_getdriver *gd;
	int ret = 0;

	jclass LinuxRequest;
	jmethodID getInterfaceNumber, setClaimed;

	LinuxRequest = CheckedGetObjectClass( env, linuxRequest );
	getInterfaceNumber = CheckedGetMethodID( env, LinuxRequest, "getInterfaceNumber", "()I" );
	setClaimed = CheckedGetMethodID( env, LinuxRequest, "setClaimed", "(Z)V" );
	CheckedDeleteLocalRef( env, LinuxRequest );

	if (!(gd = malloc(sizeof(*gd)))) {
		log( LOG_CRITICAL, "Out of memory!");
		return -ENOMEM;
	}

	memset(gd, 0, sizeof(*gd));

	gd->interface = CheckedCallIntMethod( env, linuxRequest, getInterfaceNumber );

	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_GETDRIVER, gd ))) {
		ret = -errno;

		if (-ENODATA == ret)
			log( LOG_INFO, "Interface %d is not claimed.", gd->interface );
		else
			log( LOG_ERROR, "Could not determine if interface %d is claimed.", gd->interface );
	} else {
		log( LOG_INFO, "Interface %d is claimed by driver %s.", gd->interface, gd->driver );
	}

	CheckedCallVoidMethod( env, linuxRequest, setClaimed, (ret ? JNI_FALSE : JNI_TRUE) );

	free(gd);

	return (-ENODATA == ret ? 0 : ret);
}

