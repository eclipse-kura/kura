
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
 * Listener for connect/disconnect events
 * @author Dan Streetman
 */
JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeTopologyListener
			( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices ) {
	struct pollfd devpoll;
	int poll_timeout = -1;
	int descriptor = 0;
	int error = 0;
	unsigned int pollingError = 0;

	jclass LinuxUsbServices = CheckedGetObjectClass( env, linuxUsbServices );

	jmethodID topologyChange = CheckedGetMethodID( env, LinuxUsbServices, "topologyChange", "()V" );

	errno = 0;
	descriptor = open( usbdevfs_devices_filename(), O_RDONLY, 0 );
	if ( 0 >= descriptor ) {
		log( LOG_HOTPLUG_CRITICAL, "Could not open %s", usbdevfs_devices_filename() );
		error = errno;
		goto TOPOLOGY_LISTENER_CLEANUP;
	}

	devpoll.fd = descriptor;
	devpoll.events = POLLIN;

	while ( 1 ) {
		poll(&devpoll, 1, poll_timeout);

		// Skip empty wake-ups
		if ( 0x0 == devpoll.revents ) continue;

		// Polling Error...strange...
		if ( devpoll.revents & POLLERR ) {
			log( LOG_HOTPLUG_ERROR, "Topology Polling error." );
			if (MAX_POLLING_ERRORS < ++pollingError) {
				log( LOG_HOTPLUG_CRITICAL, "%d polling errors; aborting!", pollingError );
				error = -ENOLINK; /* gotta pick one of 'em */
				break;
			} else continue;
		}

		// Connect/Disconnect event...
		if ( devpoll.revents & POLLIN ) {
			log( LOG_HOTPLUG_CHANGE, "Got topology change event." );
			CheckedCallVoidMethod( env, linuxUsbServices, topologyChange );
			continue;
		}

		// Freak event...
		log( LOG_HOTPLUG_CHANGE, "Unknown event received = 0x%x", devpoll.revents );
	}

	/* This should NOT happen! */
	log( LOG_HOTPLUG_CRITICAL, "TopologyListener Exiting!" );
	close( descriptor );

TOPOLOGY_LISTENER_CLEANUP:
	CheckedDeleteLocalRef( env, LinuxUsbServices );

	return error;
}

