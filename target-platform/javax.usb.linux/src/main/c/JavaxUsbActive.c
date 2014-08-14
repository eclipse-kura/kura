
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/* This file uses getline(), a GNU extention */
#define _GNU_SOURCE

#include "JavaxUsb.h"

#ifdef CONFIG_SETTING_USE_DEVICES_FILE
static int config_use_devices_file( JNIEnv *env, unsigned char bus, unsigned char dev )
{
	FILE *file = NULL;
#define LINELEN 1024
	ssize_t linelen, len;
	char *line = NULL, busstr[32], devstr[32];
	int in_dev = 0;
	int ret = -1;
	int config;

	if (!(line = malloc(LINELEN))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto end;
	}

	linelen = LINELEN - 1;

	sprintf(busstr, "Bus=%2.2d", bus);
	sprintf(devstr, "Dev#=%3d", dev);
#define cfgstr "Cfg#=%2d"

	errno = 0;
	if (!(file = fopen(usbdevfs_devices_filename(), "r"))) {
		log( LOG_HOTPLUG_ERROR, "Could not open %s : %s", usbdevfs_devices_filename(), strerror(errno) );
		ret = -errno
		goto end;
	}

	log( LOG_HOTPLUG_OTHER, "Checking %s", usbdevfs_devices_filename() );

	while (1) {
		memset(line, 0, LINELEN);

		errno = 0;
		if (0 > (len = getline(&line, &linelen, file))) {
			log( LOG_HOTPLUG_ERROR, "Could not read from %s : %s", usbdevfs_devices_filename(), strerror(errno) );
			ret = -errno;
			break;
		}

		if (!len) {
			log( LOG_HOTPLUG_ERROR, "No device matching %s/%s found!", busstr, devstr );
			ret = -ENODEV;
			break;
		}

		if (strstr(line, "T:")) {
			if (in_dev) {
				log( LOG_HOTPLUG_ERROR, "No active config found in device %s/%s!", cfgstr, busstr, devstr );
				ret = -EINVAL;
				break;
			}
			if (strstr(line, busstr) && strstr(line, devstr)) {
				log( LOG_HOTPLUG_OTHER, "Found section for device %s/%s", busstr, devstr );
				in_dev = 1;
				continue;
			}
		}

		if (in_dev && strstr(line, "C:*")) {
			if (1 == sscanf(line, cfgstr, config)) {
				ret = config;
				break;
			}
		}
	}

end:
	if (line) free(line);
	if (file) fclose(file);

	return ret;
}
#endif /* CONFIG_SETTING_USE_DEVICES_FILE */

#ifdef INTERFACE_SETTING_USE_DEVICES_FILE
static int interface_use_devices_file( JNIEnv *env, unsigned char bus, unsigned char dev, unsigned char interface )
{
	FILE *file = NULL;
#define LINELEN 1024
	ssize_t linelen, len;
	char *line = NULL, busstr[32], devstr[32], cfgstr[32], ifstr[32];
	int in_dev = 0, in_cfg = 0;
	int ret = -1;
	int setting;

	if (!(line = malloc(LINELEN))) {
		log( LOG_CRITICAL, "Out of memory!" );
		goto end;
	}

	linelen = LINELEN - 1;

	sprintf(busstr, "Bus=%2.2d", bus);
	sprintf(devstr, "Dev#=%3d", dev);
	sprintf(ifstr, "If#=%2d", interface );
#define setstr "Alt=%2d"

	errno = 0;
	if (!(file = fopen(usbdevfs_devices_filename(), "r"))) {
		log( LOG_HOTPLUG_ERROR, "Could not open %s : %s", usbdevfs_devices_filename(), strerror(errno) );
		ret = -errno;
		goto end;
	}

	log( LOG_HOTPLUG_OTHER, "Checking %s", usbdevfs_devices_filename() );

	while (1) {
		memset(line, 0, LINELEN);

		errno = 0;
		if (0 > (len = getline(&line, &linelen, file))) {
			log( LOG_HOTPLUG_ERROR, "Could not read from %s : %s", usbdevfs_devices_filename(), strerror(errno) );
			ret = -errno;
			break;
		}

		if (!len) {
			log( LOG_HOTPLUG_ERROR, "No device matching %s/%s found!", busstr, devstr );
			ret = -ENODEV;
			break;
		}

		if (strstr(line, "T:")) {
			if (in_dev) {
				log( LOG_HOTPLUG_ERROR, "No config matching %s found in device %s/%s!", cfgstr, busstr, devstr );
				ret = -EINVAL;
				break;
			}
			if (strstr(line, busstr) && strstr(line, devstr)) {
				log( LOG_HOTPLUG_OTHER, "Found section for device %s/%s", busstr, devstr );
				in_dev = 1;
				continue;
			}
		}

		if (in_dev && strstr(line, "C:*")) {
			in_cfg = 1;
			continue;
		}

		if (in_cfg) {
			if (strstr(line, "C:")) {
				log( LOG_HOTPLUG_ERROR, "No active interface matching %s found in device %s/%s for active config!", ifstr, busstr, devstr );
				ret = -EINVAL;
				break;
			}
			if (strstr(line, ifstr) && strstr(line, "I:*")) {
				if (1 == sscanf(line, setstr, setting)) {
					ret = setting;
					break;
				}
			}
		}
	}

end:
	if (line) free(line);
	if (file) fclose(file);

	return ret;
}
#endif /* INTERFACE_SETTING_USE_DEVICES_FILE */

#ifdef CONFIG_SETTING_ASK_DEVICE
#define CONFIG_ASK_DEVICE_TIMEOUT 500 /* ms */
static int config_ask_device( JNIEnv *env, int fd )
{
	int ret = 0;

	struct usbdevfs_ctrltransfer *ctrl = NULL;
	unsigned char *actconfig = NULL;

	if (!(ctrl = malloc(sizeof(*ctrl))) || !(actconfig = malloc(1))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto CONFIG_ASK_DEVICE_END;
	}

	*actconfig = 0;

	ctrl->bmRequestType = (unsigned char)0x80;
	ctrl->bRequest = 0x08;
	ctrl->wValue = 0x00;
	ctrl->wIndex = 0x00;
	ctrl->wLength = 1;
	ctrl->timeout = CONFIG_ASK_DEVICE_TIMEOUT;
	ctrl->data = actconfig;

	errno = 0;
	if (0 > (ioctl(fd, USBDEVFS_CONTROL, ctrl))) {
		log( LOG_HOTPLUG_ERROR, "Could not get active configuration from device : %s", strerror(errno) );
		ret = -errno;
	} else {
		log( LOG_HOTPLUG_OTHER, "Active device configuration is %d", *actconfig );
		ret = *actconfig;
	}

CONFIG_ASK_DEVICE_END:
	if (ctrl) free(ctrl);
	if (actconfig) free(actconfig);

	return ret;
}
#endif /* CONFIG_SETTING_ASK_DEVICE */

#ifdef INTERFACE_SETTING_ASK_DEVICE
#define INTERFACE_ASK_DEVICE_TIMEOUT 500 /* ms */
static int interface_ask_device( JNIEnv *env, int fd, unsigned char interface )
{
	int ret = 0;

	struct javaxusb_usbdevfs_ctrltransfer *ctrl = NULL;
	unsigned char *actsetting = NULL;

	if (!(ctrl = malloc(sizeof(*ctrl))) || !(actsetting = malloc(1))) {
		log( LOG_CRITICAL, "Out of memory!" );
		ret = -ENOMEM;
		goto INTERFACE_ASK_DEVICE_END;
	}

	*actsetting = 0;

	ctrl->bmRequestType = (unsigned char)0x81;
	ctrl->bRequest = 0x0a;
	ctrl->wValue = 0x00;
	ctrl->wIndex = interface;
	ctrl->wLength = 1;
	ctrl->timeout = INTERFACE_ASK_DEVICE_TIMEOUT;
	ctrl->data = actsetting;

	errno = 0;
	if (0 > (ioctl(fd, USBDEVFS_CONTROL, ctrl))) {
		log( LOG_HOTPLUG_ERROR, "Could not get active interface %d setting from device : %s", interface, strerror(errno) );
		ret = -errno;
	} else {
		log( LOG_HOTPLUG_OTHER, "Active interface %d setting is %d", interface, *actsetting );
		ret = *actsetting;
	}

INTERFACE_ASK_DEVICE_END:
	if (ctrl) free(ctrl);
	if (actsetting) free(actsetting);

	return ret;
}
#endif /* INTERFACE_SETTING_ASK_DEVICE */

int getActiveConfig( JNIEnv *env, int fd, unsigned char bus, unsigned char dev )
{
	int ret = -1; /* -1 = failure */

#ifdef CONFIG_SETTING_ASK_DEVICE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Getting active config using GET_CONFIGURATION standard request." );
		ret = config_ask_device( env, fd );
		log( LOG_HOTPLUG_OTHER, "Device returned %d%s.", ret, 0>ret ? " (failure)" : "" );
	}
#endif
#ifdef CONFIG_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Getting active config using %s.", usbdevfs_devices_filename() );
		ret = config_use_devices_file( env, bus, dev );
		log( LOG_HOTPLUG_OTHER, "%s returned %d%s.", usbdevfs_devices_filename(), ret, 0>ret ? " (failure)" : "" );
	}
#endif
#ifdef CONFIG_SETTING_1_ALWAYS_ACTIVE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Returning config 1 as active; no checking." );
		ret = 1;
	}
#endif

	return ret;
}

int getActiveInterfaceSetting( JNIEnv *env, int fd, unsigned char bus, unsigned char dev, unsigned char interface )
{
	int ret = -1; /* -1 = failure  */

#ifdef INTERFACE_SETTING_ASK_DEVICE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Getting active interface %d setting using GET_INTERFACE standard request.", interface );
		ret = interface_ask_device( env, fd, interface );
		log( LOG_HOTPLUG_OTHER, "Device returned %d%s.", ret, 0>ret ? " (failure)" : "" );
	}
#endif
#ifdef INTERFACE_SETTING_USE_DEVICES_FILE
	if (0 > ret) {
		log( LOG_HOTPLUG_OTHER, "Getting active interface %d setting using %s.", interface, usbdevfs_devices_filename() );
		ret = interface_use_devices_file( env, bus, dev, interface );
		log( LOG_HOTPLUG_OTHER, "%s returned %d%s.", usbdevfs_devices_filename(), ret, 0>ret ? " (failure)" : "" );
	}
#endif

	return ret;
}

JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeGetActiveConfigurationNumber
(JNIEnv *env, jclass JavaxUsb, jobject linuxDeviceOsImp)
{
	int fd;
	unsigned char busnum, devnum;
	int configNumber;

	jclass LinuxDeviceOsImp = NULL, LinuxDeviceProxy = NULL;
	jobject linuxDeviceProxy = NULL;
	jfieldID linuxDeviceProxyID;
	jmethodID getKey;
	jstring jname = NULL;

	LinuxDeviceOsImp = CheckedGetObjectClass( env, linuxDeviceOsImp );
	linuxDeviceProxyID = CheckedGetFieldID( env, LinuxDeviceOsImp, "linuxDeviceProxy", "Lcom/ibm/jusb/os/linux/LinuxDeviceProxy;" );
	linuxDeviceProxy = (*env)->GetObjectField( env, linuxDeviceOsImp, linuxDeviceProxyID );
	LinuxDeviceProxy = CheckedGetObjectClass( env, linuxDeviceProxy );
	getKey = CheckedGetMethodID( env, LinuxDeviceProxy, "getKey", "()Ljava/lang/String;" );
	jname = (jstring)CheckedCallObjectMethod( env, linuxDeviceProxy, getKey );
	(*env)->DeleteLocalRef( env, LinuxDeviceProxy );
	(*env)->DeleteLocalRef( env, linuxDeviceProxy );
	(*env)->DeleteLocalRef( env, LinuxDeviceOsImp );

	if (0 > (fd = open_device( env, jname, O_RDWR ))) {
		(*env)->DeleteLocalRef( env, jname );
		return errno ? -errno : -1;
	}

	busnum = (unsigned char)get_busnum_from_jname( env, jname );
	devnum = (unsigned char)get_devnum_from_jname( env, jname );

	configNumber = getActiveConfig( env, fd, busnum, devnum );

	close(fd);
	(*env)->DeleteLocalRef( env, jname );

	return (jint)configNumber;
}

JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeGetActiveInterfaceSettingNumber
(JNIEnv *env, jclass JavaxUsb, jobject linuxDeviceOsImp, jint interfaceNumber)
{
	int fd;
	unsigned char busnum, devnum;
	int settingNumber;

	jclass LinuxDeviceOsImp = NULL, LinuxDeviceProxy = NULL;
	jobject linuxDeviceProxy = NULL;
	jfieldID linuxDeviceProxyID;
	jmethodID getKey;
	jstring jname = NULL;

	LinuxDeviceOsImp = CheckedGetObjectClass( env, linuxDeviceOsImp );
	linuxDeviceProxyID = CheckedGetFieldID( env, LinuxDeviceOsImp, "linuxDeviceProxy", "Lcom/ibm/jusb/os/linux/LinuxDeviceProxy;" );
	linuxDeviceProxy = (*env)->GetObjectField( env, linuxDeviceOsImp, linuxDeviceProxyID );
	LinuxDeviceProxy = CheckedGetObjectClass( env, linuxDeviceProxy );
	getKey = CheckedGetMethodID( env, LinuxDeviceProxy, "getKey", "()Ljava/lang/String;" );
	jname = (jstring)CheckedCallObjectMethod( env, linuxDeviceProxy, getKey );
	(*env)->DeleteLocalRef( env, LinuxDeviceProxy );
	(*env)->DeleteLocalRef( env, linuxDeviceProxy );
	(*env)->DeleteLocalRef( env, LinuxDeviceOsImp );

	if (0 > (fd = open_device( env, jname, O_RDWR ))) {
		(*env)->DeleteLocalRef( env, jname );
		return errno ? -errno : -1;
	}

	busnum = (unsigned char)get_busnum_from_jname( env, jname );
	devnum = (unsigned char)get_devnum_from_jname( env, jname );

	settingNumber = getActiveInterfaceSetting( env, fd, busnum, devnum, (unsigned char)interfaceNumber );

	close(fd);
	(*env)->DeleteLocalRef( env, jname );

	return (jint)settingNumber;	
}
