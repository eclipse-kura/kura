
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

static int kernel_version = 0;

#define KERNEL_VERSION(version,patchlevel,sublevel) ((version<<16) | (patchlevel<<8) | sublevel)

// This is defined as various names in different kernel versions but the number is always the same
#define NO_ACCEPT_SHORT_PACKET 0x0001

// This is defined only in 2.4 kernels.
#define QUEUE_BULK 0x10
#define QUEUE_BULK_LAST_KERNEL_VERSION KERNEL_VERSION(2,4,29)

// 2.4 USB subsystems do not allow queued interrupt transfers but do allow (encourage?) interrupt-using-bulk
#define INTERRUPT_USES_BULK_LAST_KERNEL_VERSION KERNEL_VERSION(2,4,99)

static void setKernelVersion(void)
{
	struct utsname name;

	if (!uname(&name)) {
		char *p = name.release;
		int num[3], i;

		for (i=0; i<3; i++) {
		  			errno = 0;
 			if (((int)*p < '0') || ((int)*p > '9')) {
		  				log(LOG_ERROR, "Could not parse release string %s : %s", name.release, strerror(errno));
		  				break;
		  			} else {
                        num[i] = strtol(p, &p,0);
		  				p++;
			}
		}
		if (3 == i) {
			log(LOG_INFO, "Kernel version string %s parsed as %d.%d.%d",name.release,num[0],num[1],num[2]);
			kernel_version = KERNEL_VERSION(num[0],num[1],num[2]);
			return;
		}
	}

	log(LOG_CRITICAL, "Could not determine kernel version : %s", strerror(errno));
	log(LOG_ERROR, "Using (most likely wrong) kernel version of 2.4.0");
	kernel_version = KERNEL_VERSION(2,4,0);
}

static int getKernelVersion(void)
{
	if (!kernel_version)
		setKernelVersion();

	return kernel_version;
}

/* Get the flag for queueing bulk transfers, only used on older kernels. */
static int getQueueBulkFlag(void)
{
	return (QUEUE_BULK_LAST_KERNEL_VERSION >= getKernelVersion() ? QUEUE_BULK : 0);
}

/* Get the flag for accepting/rejecting short packets.
 * The parameter is whether short packets should be accepted or not.
 */
int getShortPacketFlag(int accept) { return ( accept ? 0 : NO_ACCEPT_SHORT_PACKET ); }

/* This sets/clears flags as appropriate to the transfer type.
 * The parameter is the existing flags, the return is the modified flags.
 */
int getIsochronousFlags(int flags) { return (~NO_ACCEPT_SHORT_PACKET & (USBDEVFS_URB_ISO_ASAP | flags)); }
int getControlFlags(int flags) { return flags; }
int getBulkFlags(int flags) { return getQueueBulkFlag() | flags; }
int getInterruptFlags(int flags)
{
	return ((INTERRUPT_USES_BULK_LAST_KERNEL_VERSION >= getKernelVersion()) ? (getBulkFlags(flags)) : flags);
}

// These #defined values have never changed name
int getIsochronousType(void) { return USBDEVFS_URB_TYPE_ISO; }
int getControlType(void) { return USBDEVFS_URB_TYPE_CONTROL; }
int getBulkType(void) { return USBDEVFS_URB_TYPE_BULK; }
int getInterruptType(void)
{
	return (INTERRUPT_USES_BULK_LAST_KERNEL_VERSION >= getKernelVersion() ? getBulkType() : USBDEVFS_URB_TYPE_INTERRUPT);
}


// These are probably not the best way to determine where the usbfs is but it's simple.

#define USBDEVFS_PATH_OLD "/proc/bus/usb"
#define USBDEVFS_DEVICES_OLD "/proc/bus/usb/devices"
#define USBDEVFS_SPRINTF_NODE_OLD "/proc/bus/usb/%3.03d/%3.03d"
#define USBDEVFS_SSCANF_NODE_OLD "/proc/bus/usb/%3d/%3d"

#define USBDEVFS_PATH_NEW "/dev/bus/usb"
#define USBDEVFS_DEVICES_NEW "/dev/bus/usb/devices"
#define USBDEVFS_SPRINTF_NODE_NEW1 "/dev/bus/usb/%3.03d/%3.03d"
#define USBDEVFS_SPRINTF_NODE_NEW2 "/dev/bus/usb/%d/%d"
#define USBDEVFS_SSCANF_NODE_NEW1 "/dev/bus/usb/%3d/%3d"
#define USBDEVFS_SSCANF_NODE_NEW2 "/dev/bus/usb/%d/%d"
#define USBDEVFS_CHECK_NODE_NEW1 "/dev/bus/usb/001/001"
#define USBDEVFS_CHECK_NODE_NEW2 "/dev/bus/usb/1/1"

char *usbdevfs_path()
{
	struct stat buf;
	if ((0 == stat(USBDEVFS_PATH_NEW, &buf)) && S_ISDIR(buf.st_mode))
		return USBDEVFS_PATH_NEW;
	else
		return USBDEVFS_PATH_OLD;
}

char *usbdevfs_devices_filename()
{
	struct stat buf;
	if (0 == stat(USBDEVFS_DEVICES_NEW, &buf))
		return USBDEVFS_DEVICES_NEW;
	else
		return USBDEVFS_DEVICES_OLD;
}

char *usbdevfs_sscanf_node()
{
	struct stat buf;
	if ((0 == stat(USBDEVFS_CHECK_NODE_NEW1, &buf)))
		return USBDEVFS_SSCANF_NODE_NEW1;
	else if ((0 == stat(USBDEVFS_CHECK_NODE_NEW2, &buf)))
		return USBDEVFS_SSCANF_NODE_NEW2;
	else
		return USBDEVFS_SSCANF_NODE_OLD;
}

char *usbdevfs_sprintf_node()
{
	struct stat buf;
	if ((0 == stat(USBDEVFS_CHECK_NODE_NEW1, &buf)))
		return USBDEVFS_SPRINTF_NODE_NEW1;
	else if ((0 == stat(USBDEVFS_CHECK_NODE_NEW2, &buf)))
		return USBDEVFS_SPRINTF_NODE_NEW2;
	else
		return USBDEVFS_SPRINTF_NODE_OLD;
}

