
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#ifndef _JAVAXUSBUTIL_H
#define _JAVAXUSBUTIL_H

#include "com_ibm_jusb_os_linux_JavaxUsb.h"
#include "JavaxUsbLog.h"
#include "JavaxUsbChecks.h"

#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/poll.h>
#include <sys/time.h>
#include <sys/dir.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <pthread.h>
#include <errno.h>

/* Need to include this last or gcc will give warnings */
#include "JavaxUsbKernel.h"

#define MAX_LINE_LENGTH 255
#define MAX_KEY_LENGTH 255
#define MAX_PATH_LENGTH 255

#define MAX_POLLING_ERRORS 64

/* These must match the defines in JavaxUsb.java */
#define SPEED_UNKNOWN 0
#define SPEED_LOW 1
#define SPEED_FULL 2

//******************************************************************************
// Descriptor structs 

struct jusb_device_descriptor {
	unsigned char bLength;
	unsigned char bDescriptorType;
	unsigned short bcdUSB;
	unsigned char bDeviceClass;
	unsigned char bDeviceSubClass;
	unsigned char bDeviceProtocol;
	unsigned char bMaxPacketSize0;
	unsigned short idVendor;
	unsigned short idProduct;
	unsigned short bcdDevice;
	unsigned char iManufacturer;
	unsigned char iProduct;
	unsigned char iSerialNumber;
	unsigned char bNumConfigurations;
};

struct jusb_config_descriptor {
	unsigned char bLength;
	unsigned char bDescriptorType;
	unsigned short wTotalLength;
	unsigned char bNumInterfaces;
	unsigned char bConfigurationValue;
	unsigned char iConfiguration;
	unsigned char bmAttributes;
	unsigned char bMaxPower;
};

struct jusb_interface_descriptor {
	unsigned char bLength;
	unsigned char bDescriptorType;
	unsigned char bInterfaceNumber;
	unsigned char bAlternateSetting;
	unsigned char bNumEndpoints;
	unsigned char bInterfaceClass;
	unsigned char bInterfaceSubClass;
	unsigned char bInterfaceProtocol;
	unsigned char iInterface;
};

struct jusb_endpoint_descriptor {
	unsigned char bLength;
	unsigned char bDescriptorType;
	unsigned char bEndpointAddress;
	unsigned char bmAttributes;
	unsigned short wMaxPacketSize;
	unsigned char bInterval;
};

struct jusb_string_descriptor {
	unsigned char bLength;
	unsigned char bDescriptorType;
	unsigned char bString[254];
};

//******************************************************************************
// Request methods

int pipe_request( JNIEnv *env, int fd, jobject linuxRequest );
int isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

void cancel_pipe_request( JNIEnv *env, int fd, jobject linuxRequest );
void cancel_isochronous_request( JNIEnv *env, int fd, jobject linuxRequest );

int complete_pipe_request( JNIEnv *env, jobject linuxRequest );
int complete_isochronous_request( JNIEnv *env, jobject linuxRequest );

int set_configuration( JNIEnv *env, int fd, jobject linuxRequest );
int set_interface( JNIEnv *env, int fd, jobject linuxRequest );

int claim_interface( JNIEnv *env, int fd, int claim, jobject linuxRequest );
int is_claimed( JNIEnv *env, int fd, jobject linuxRequest );

int control_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int bulk_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int interrupt_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int isochronous_pipe_request( JNIEnv *env, int fd, jobject linuxPipeRequest, struct usbdevfs_urb *urb );

int complete_control_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_bulk_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_interrupt_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );
int complete_isochronous_pipe_request( JNIEnv *env, jobject linuxPipeRequest, struct usbdevfs_urb *urb );

//******************************************************************************
// Config and Interface active checking methods

/* Pick a way to determine active config.
 *
 * Most of these generate bus traffic to one or more devices.
 * This is BAD when using non-queueing (up to 2.5.44) UHCI Host Controller Driver,
 * as it can interfere with other drivers and the results are unpredictable - ranging
 * from nothing to complete loss of use of the device(s).
 *
 * CONFIG_SETTING_ASK_DEVICE:
 * Asking the device directly is the best available way,
 * as bus traffic is generated only for the specific device in question,
 * and only 1 standard request.
 *
 * CONFIG_SETTING_USE_DEVICES_FILE:
 * Reading/parsing the /proc/bus/usb/devices file generates bus traffic,
 * by asking ALL connected devices for their 3 standard String-descriptors;
 * Manufacturer, Product, and SerialNumber.  This is a lot of bus traffic and
 * can cause problems with any or all connected devices (if using a non-queueing UHCI driver).
 *
 * CONFIG_SETTING_1_ALWAYS_ACTIVE:
 * This does not communicate with the device at all, but always marks the first
 * configuration (number 1, as configs must be numbered consecutively starting with 1)
 * as active.  This should work for all devices, but will produce incorrect results
 * for devices whose active configuration has been changed outside of the current javax.usb
 * instance.
 *
 * All or none may be used, attempts are in order shown, failure moves to the next one.
 * If none are defined (or all fail) then the result will be no configs active, i.e.
 * the device will appear to be (but will not really be) in a Not Configured state.
 *
 * Most people want at least the CONFIG_1_ALWAYS_ACTIVE define, as it is always
 * the last attempted and will do the right thing in many more cases than leaving the
 * device to appear as Not Configured.
 */
#define CONFIG_SETTING_ASK_DEVICE
#undef CONFIG_SETTING_USE_DEVICES_FILE
#define CONFIG_SETTING_1_ALWAYS_ACTIVE

/* Pick a way to determine active interface alternate setting.
 *
 * INTERFACE_SETTING_ASK_DEVICE:
 * This directly asks the device in the same manner as above.  The only difference is,
 * to communicate with an interface, the interface must be claimed;
 * for a device that already has a driver (which is usually most devices)
 * this will not work since the interface will already be claimed.
 *
 * INTERFACE_SETTING_USE_DEVICES_FILE:
 * This uses the /proc/bus/usb/devices file in the same manner as above.
 * However, until kernel 2.5.XX, the devices file does not provide active
 * interface setting information, so this will fail on those kernels.
 *
 * If none are defined (or all fail) then the result will be first setting is active.
 */
#undef INTERFACE_SETTING_ASK_DEVICE
#undef INTERFACE_SETTING_USE_DEVICES_FILE

int getActiveConfig( JNIEnv *env, int fd, unsigned char bus, unsigned char dev );
int getActiveInterfaceSetting( JNIEnv *env, int fd, unsigned char bus, unsigned char dev, unsigned char interface );

//******************************************************************************
// Utility methods

static inline unsigned short bcd( unsigned char msb, unsigned char lsb ) 
{
    return ( (msb << 8) & 0xff00 ) | ( lsb & 0x00ff );
}

static inline int open_device( JNIEnv *env, jstring javaKey, int oflag ) 
{
	const char *node;
	int filed;

	node = (*env)->GetStringUTFChars( env, javaKey, NULL );
	log( LOG_INFO, "Opening node %s", node );
	if (0 > (filed = open( node, oflag )))
		log( LOG_ERROR, "Could not open node %s : %s", node, strerror(errno) );
	(*env)->ReleaseStringUTFChars( env, javaKey, node );
	return filed;
}

static inline int bus_node_to_name( int bus, int node, char *name )
{
	sprintf( name, usbdevfs_sprintf_node(), bus, node );
	return strlen( name );
}

static inline int get_busnum_from_name( const char *name )
{
	int bus, node;
	if (1 > (sscanf( name, usbdevfs_sscanf_node(), &bus, &node )))
		return -1;
	else return bus;
}

static inline int get_busnum_from_jname( JNIEnv *env, jstring jname )
{
	const char *name = (*env)->GetStringUTFChars( env, jname, NULL );
	int busnum = get_busnum_from_name( name );
	(*env)->ReleaseStringUTFChars( env, jname, name );
	return busnum;
}

static inline int get_devnum_from_name( const char *name )
{
	int bus, node;
	if (2 > (sscanf( name, usbdevfs_sscanf_node(), &bus, &node )))
		return -1;
	else return node;
}

static inline int get_devnum_from_jname( JNIEnv *env, jstring jname )
{
	const char *name = (*env)->GetStringUTFChars( env, jname, NULL );
	int devnum = get_devnum_from_name( name );
	(*env)->ReleaseStringUTFChars( env, jname, name );
	return devnum;
}

/**
 * Debug a URB.
 * @env The JNIEnv*.
 * @param calling_method The name of the calling method.
 * @param urb The usbdevfs_urb.
 */
static inline void debug_urb( JNIEnv *env, char *calling_method, struct usbdevfs_urb *urb )
{
	if (!tracing)
		return;

//FIXME - add device number and/or other dev info
	log( LOG_URB_METADATA, "%s : URB address = %p", calling_method, urb );
	log( LOG_URB_METADATA, "%s : URB endpoint = %x status = %d signal = %x", calling_method, urb->endpoint, urb->status, urb->signr );
	log( LOG_URB_METADATA, "%s : URB buffer length = %d actual length = %d", calling_method, urb->buffer_length, urb->actual_length );

	if (urb->buffer && (0 < urb->buffer_length)) {
		static const char hex[] = "0123456789abcdef";
		int i, loglen = (3*urb->buffer_length);
		char logbuf[loglen], *bufp = logbuf;
		char* p = (char *)urb->buffer;
		for (i=0; i<urb->buffer_length; i++) {
			int c = *p++;
			*bufp++ = hex[(c>>4)&0xf]; // index to array
			*bufp++ = hex[c&0xf]; // index to array
			*bufp++ = ' ';
		}
		logbuf[loglen-1] = 0; // null terminate string
		log( LOG_URB_DATA, "%s : URB data = %s", calling_method, logbuf );
 	} else {
 		log( LOG_URB_DATA, "%s : URB data empty", calling_method );
 	}
}

#endif /* _JAVAXUSBUTIL_H */

