
/** 
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

#include "JavaxUsb.h"

static inline int build_device( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, unsigned char bus, unsigned char dev,
	jobject parent, int parentport, jobject connectedDevices, jobject disconnectedDevices );

static inline int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device, unsigned char bus, unsigned char dev );

static inline jobject build_interface( JNIEnv *env, jclass JavaxUsb, int fd, jobject config, struct jusb_interface_descriptor *if_desc, unsigned char bus, unsigned char dev );

static inline void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc );

static inline void *get_descriptor( JNIEnv *env, int fd );

static int select_usbfs(const struct dirent *entry);

/**
 * Update topology tree
 * @author Dan Streetman
 */
JNIEXPORT jint JNICALL Java_com_ibm_jusb_os_linux_JavaxUsb_nativeTopologyUpdater
			( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, jobject connectedDevices, jobject disconnectedDevices )
{
	int busses, port, devices = 0;
	struct dirent **buslist = NULL;

	jclass LinuxUsbServices = CheckedGetObjectClass( env, linuxUsbServices );
	jmethodID getRootUsbHubImp = CheckedGetMethodID( env, LinuxUsbServices, "getRootUsbHubImp", "()Lcom/ibm/jusb/UsbHubImp;" );
	jobject rootHub = CheckedCallObjectMethod( env, linuxUsbServices, getRootUsbHubImp );
	CheckedDeleteLocalRef( env, LinuxUsbServices );

	if (0 > (*env)->EnsureLocalCapacity(env, 40)) {
		log( LOG_CRITICAL, "Could not reserve enough local references, Out of Memory!\n");
		return -ENOMEM;
	}

 	if (!rootHub) {
		log( LOG_ERROR, "Could not get rootHub!\n");
		return -EINVAL;
	}

	errno = 0;
	if (0 > (busses = scandir(usbdevfs_path(), &buslist, select_usbfs, alphasort)) ) {
		log( LOG_ERROR, "Could not access %s : %s", usbdevfs_path(), strerror(errno) );
		return -errno;
	}

	for (port=0; port<busses; port++) {
		struct dirent **devlist = NULL;
		int bus, hcAddress, devs;
		int busdir_len = strlen(usbdevfs_path()) + strlen(buslist[port]->d_name) + 2;
		char busdir[busdir_len];

		sprintf(busdir, "%s/%s", usbdevfs_path(), buslist[port]->d_name);
		bus = atoi( buslist[port]->d_name );

		errno = 0;
		devs = scandir(busdir, &devlist, select_usbfs, alphasort);

		if (0 > devs) {
			log( LOG_ERROR, "Could not access device nodes in %s : %s", busdir, strerror(errno) );
		} else if (!devs) {
			log( LOG_ERROR, "No device nodes found in %s : %s", busdir, strerror(errno) );
		} else {
			/* Hopefully, the host controller has the lowest numbered address on this bus! */
			hcAddress = atoi( devlist[0]->d_name );
			devices += build_device( env, JavaxUsb, linuxUsbServices, bus, hcAddress, rootHub, port, connectedDevices, disconnectedDevices );
		}

		while (0 < devs) free(devlist[--devs]);
		if (devlist) free(devlist);

		free(buslist[port]);
	}
	free(buslist);

	if (rootHub) CheckedDeleteLocalRef( env, rootHub );

	return 0;
}

static inline int build_device( JNIEnv *env, jclass JavaxUsb, jobject linuxUsbServices, unsigned char bus, unsigned char dev,
	jobject parent, int parentport, jobject connectedDevices, jobject disconnectedDevices )
{
	int fd = 0, port, ncfg;
	int devices = 0, speed = SPEED_UNKNOWN;
	struct usbdevfs_ioctl *usbioctl = NULL;
	struct usbdevfs_hub_portinfo *portinfo = NULL;
	struct usbdevfs_connectinfo *connectinfo = NULL;
	struct jusb_device_descriptor *dev_desc = NULL;
	int node_len = strlen(usbdevfs_path()) + 1 + 3 + 1 + 3 + 1;
	char node[node_len];

	jobject device = NULL, existingDevice = NULL;
	jstring keyString = NULL;

	jclass LinuxUsbServices = CheckedGetObjectClass( env, linuxUsbServices );
	jmethodID createUsbHubImp = CheckedGetStaticMethodID( env, JavaxUsb, "createUsbHubImp", "(Ljava/lang/String;I)Lcom/ibm/jusb/UsbHubImp;" );
	jmethodID createUsbDeviceImp = CheckedGetStaticMethodID( env, JavaxUsb, "createUsbDeviceImp", "(Ljava/lang/String;)Lcom/ibm/jusb/UsbDeviceImp;" );
	jmethodID configureUsbDeviceImp = CheckedGetStaticMethodID( env, JavaxUsb, "configureUsbDeviceImp", "(Lcom/ibm/jusb/UsbDeviceImp;BBBBBBBBBBSSSSI)V" );
	jmethodID checkUsbDeviceImp = CheckedGetMethodID( env, LinuxUsbServices, "checkUsbDeviceImp", "(Lcom/ibm/jusb/UsbHubImp;ILcom/ibm/jusb/UsbDeviceImp;Ljava/util/List;Ljava/util/List;)Lcom/ibm/jusb/UsbDeviceImp;" );
	CheckedDeleteLocalRef( env, LinuxUsbServices );

	sprintf(node, usbdevfs_sprintf_node(), (0xff&bus), (0xff&dev));

	keyString = CheckedNewStringUTF( env, node );

	log( LOG_HOTPLUG_DEVICE, "Building device %s", node );

	fd = open( node, O_RDWR );
	if ( 0 >= fd ) {
		log( LOG_ERROR, "Could not access %s", node );
		goto BUILD_DEVICE_EXIT;
	}

	if (!(dev_desc = get_descriptor( env, fd ))) {
		log( LOG_ERROR, "Short read on device descriptor" );
		goto BUILD_DEVICE_EXIT;
	}

	if (dev_desc->bDeviceClass == USB_CLASS_HUB) {
		usbioctl = malloc(sizeof(*usbioctl));
		portinfo = malloc(sizeof(*portinfo));
		usbioctl->ioctl_code = USBDEVFS_HUB_PORTINFO;
		usbioctl->ifno = 0;
		usbioctl->data = portinfo;
		errno = 0;
		if (0 >= ioctl( fd, USBDEVFS_IOCTL, usbioctl )) {
			log( LOG_ERROR, "Could not get portinfo from hub, error = %d", errno );
			goto BUILD_DEVICE_EXIT;
		} else {
		  log( LOG_HOTPLUG_DEVICE, "Device is hub with %d ports",portinfo->nports );
		}
		free(usbioctl);
		usbioctl = NULL;
		device = CheckedCallStaticObjectMethod( env, JavaxUsb, createUsbHubImp, keyString, portinfo->nports );
	} else {
	  device = CheckedCallStaticObjectMethod( env, JavaxUsb, createUsbDeviceImp, keyString );
	}

	connectinfo = malloc(sizeof(*connectinfo));
	errno = 0;
	if (0 > (ioctl( fd, USBDEVFS_CONNECTINFO, connectinfo ))) {
		log( LOG_ERROR, "Could not get connectinfo from device, error = %d", errno );
		goto BUILD_DEVICE_EXIT;
	} else {
	  log( LOG_HOTPLUG_OTHER, "Device speed is %s", (connectinfo->slow?"1.5 Mbps":"12 Mbps") );
	}
	speed = ( connectinfo->slow ? SPEED_LOW : SPEED_FULL );
	free(connectinfo);
	connectinfo = NULL;

	CheckedCallStaticVoidMethod( env, JavaxUsb, configureUsbDeviceImp, device, 
		dev_desc->bLength, dev_desc->bDescriptorType,
		dev_desc->bDeviceClass, dev_desc->bDeviceSubClass, dev_desc->bDeviceProtocol,
		dev_desc->bMaxPacketSize0, dev_desc->iManufacturer, dev_desc->iProduct, dev_desc->iSerialNumber,
		dev_desc->bNumConfigurations, dev_desc->idVendor, dev_desc->idProduct,
		dev_desc->bcdDevice, dev_desc->bcdUSB, speed );

	/* Build config descriptors */
	for (ncfg=0; ncfg<dev_desc->bNumConfigurations; ncfg++) {
		if (build_config( env, JavaxUsb, fd, device, bus, dev )) {
			log( LOG_ERROR, "Could not get config %d for device %d", ncfg, dev );
			goto BUILD_DEVICE_EXIT;
		}
	}

	existingDevice = CheckedCallObjectMethod( env, linuxUsbServices, checkUsbDeviceImp, parent, parentport+1, device, connectedDevices, disconnectedDevices );
	CheckedDeleteLocalRef( env, device );
	device = existingDevice;

	/* This device is set up and ready! */
	devices = 1;
	close( fd );
	fd = 0;

	if ((dev_desc->bDeviceClass == USB_CLASS_HUB) && portinfo)
		for (port=0; port<(portinfo->nports); port++)
			if (portinfo->port[port]) {
				log( LOG_HOTPLUG_OTHER, "Building device %d attached to port %d", portinfo->port[port], port);
				devices += build_device( env, JavaxUsb, linuxUsbServices, bus, portinfo->port[port], device, port, connectedDevices, disconnectedDevices );
			}

BUILD_DEVICE_EXIT:
	if (fd) close(fd);
	if (device) CheckedDeleteLocalRef( env, device );
	if (connectinfo) free(connectinfo);
	if (dev_desc) free(dev_desc);
	if (usbioctl) free(usbioctl);
	if (portinfo) free(portinfo);
	if (keyString) CheckedDeleteLocalRef( env, keyString );

	return devices;
}

static inline int build_config( JNIEnv *env, jclass JavaxUsb, int fd, jobject device, unsigned char bus, unsigned char dev )
{
	int result = -1;
	struct jusb_config_descriptor *cfg_desc = NULL;
	unsigned char *desc = NULL;
	unsigned short wTotalLength;
	unsigned int pos;
	jobject config = NULL, interface = NULL;
	jmethodID createUsbConfigurationImp;

	if (!(cfg_desc = get_descriptor( env, fd ))) {
		log( LOG_ERROR, "Short read on config desriptor." );
		goto BUILD_CONFIG_EXIT;
	}

	createUsbConfigurationImp = CheckedGetStaticMethodID( env, JavaxUsb, "createUsbConfigurationImp", "(Lcom/ibm/jusb/UsbDeviceImp;BBSBBBBB)Lcom/ibm/jusb/UsbConfigurationImp;" );

	log( LOG_HOTPLUG_OTHER, "Building config %d", cfg_desc->bConfigurationValue );

	wTotalLength = cfg_desc->wTotalLength;
	pos = cfg_desc->bLength;

	config = CheckedCallStaticObjectMethod( env, JavaxUsb, createUsbConfigurationImp, device,
		cfg_desc->bLength, cfg_desc->bDescriptorType, wTotalLength,
		cfg_desc->bNumInterfaces, cfg_desc->bConfigurationValue, cfg_desc->iConfiguration,
		cfg_desc->bmAttributes, cfg_desc->bMaxPower );

	while (pos < wTotalLength) {
		desc = get_descriptor( env, fd );
		if ((!desc) || (2 > desc[0])) {
			log( LOG_ERROR, "Short read on descriptor" );
			goto BUILD_CONFIG_EXIT;
		}
		pos += desc[0];
		switch( desc[1] ) {
			case USB_DT_DEVICE:
				log( LOG_ERROR, "Got device descriptor inside of config descriptor" );
				goto BUILD_CONFIG_EXIT;

			case USB_DT_CONFIG:
				log( LOG_ERROR, "Got config descriptor inside of config descriptor" );
				goto BUILD_CONFIG_EXIT;

			case USB_DT_INTERFACE:
				if (interface) CheckedDeleteLocalRef( env, interface );
				interface = build_interface( env, JavaxUsb, fd, config, (struct jusb_interface_descriptor*)desc, bus, dev );
				break;

			case USB_DT_ENDPOINT:
				build_endpoint( env, JavaxUsb, interface, (struct jusb_endpoint_descriptor*)desc );
				break;

			default:
				/* Ignore proprietary descriptor */
				break;
		}
		free(desc);
		desc = NULL;
	}

	result = 0;

BUILD_CONFIG_EXIT:
	if (config) CheckedDeleteLocalRef( env, config );
	if (interface) CheckedDeleteLocalRef( env, interface );
	if (cfg_desc) free(cfg_desc);
	if (desc) free(desc);

	return result;
}

static inline jobject build_interface( JNIEnv *env, jclass JavaxUsb, int fd, jobject config, struct jusb_interface_descriptor *if_desc, unsigned char bus, unsigned char dev )
{
	jobject interface;

	jmethodID createUsbInterfaceImp = CheckedGetStaticMethodID( env, JavaxUsb, "createUsbInterfaceImp", "(Lcom/ibm/jusb/UsbConfigurationImp;BBBBBBBBB)Lcom/ibm/jusb/UsbInterfaceImp;" );

	log( LOG_HOTPLUG_OTHER, "Building interface %d", if_desc->bInterfaceNumber );

	interface = CheckedCallStaticObjectMethod( env, JavaxUsb, createUsbInterfaceImp, config,
		if_desc->bLength, if_desc->bDescriptorType,
		if_desc->bInterfaceNumber, if_desc->bAlternateSetting, if_desc->bNumEndpoints, if_desc->bInterfaceClass,
		if_desc->bInterfaceSubClass, if_desc->bInterfaceProtocol, if_desc->iInterface );

	return interface;
}

static inline void build_endpoint( JNIEnv *env, jclass JavaxUsb, jobject interface, struct jusb_endpoint_descriptor *ep_desc )
{
	jmethodID createUsbEndpointImp = CheckedGetStaticMethodID( env, JavaxUsb, "createUsbEndpointImp", "(Lcom/ibm/jusb/UsbInterfaceImp;BBBBBS)Lcom/ibm/jusb/UsbEndpointImp;" );

	log( LOG_HOTPLUG_OTHER, "Building endpoint 0x%2.02x", ep_desc->bEndpointAddress );

	if (!interface) {
		log( LOG_ERROR, "Interface is NULL");
		return;
	}

	CheckedCallStaticObjectMethod( env, JavaxUsb, createUsbEndpointImp, interface,
		ep_desc->bLength, ep_desc->bDescriptorType,
		ep_desc->bEndpointAddress, ep_desc->bmAttributes, ep_desc->bInterval, ep_desc->wMaxPacketSize );
}

static inline void *get_descriptor( JNIEnv *env, int fd )
{
	unsigned char *buffer = NULL, *len = NULL;
	int nread;

	len = malloc(1);
	if (1 > read( fd, len, 1 )) {
		log( LOG_ERROR, "Cannot read from file!" );
		goto GET_DESCRIPTOR_EXIT;
	}

	if (*len == 0) {
		log( LOG_ERROR, "Zero-length descriptor?" );
		goto GET_DESCRIPTOR_EXIT;
	}

	buffer = malloc(*len);
	buffer[0] = *len;
	free(len);
	len = NULL;

	nread = read( fd, buffer+1, buffer[0]-1 );
	if (buffer[0]-1 != nread) {
		if (buffer[0]-1 > nread) log( LOG_ERROR, "Short read on file" );
		else log( LOG_ERROR, "Long read on file" );
		free(buffer);
		buffer = NULL;
	}

GET_DESCRIPTOR_EXIT:
	if (len) free(len);

	return buffer;
}

static int select_usbfs(const struct dirent *entry)
{
	/* This originally used entry->d_type, however Linux 2.4 doesn't implement d_type,
	 * and POSIX apparently doesn't define it either.  So let's just do name matching.
	 * Hope that doesn't change.
	 */
	int n = atoi(entry->d_name);
	/* If the number conversion of the name is 1-999, it's (hopefully) ok.
	 */
	return (0 < n) && (n < 1000);
}
