/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech and/or its affiliates
 *     Red Hat Inc
 *******************************************************************************/
/*******************************************
 This code is meant to be a teaching
 resource. It can be used for anyone for
 any reason, including embedding into
 a commercial product.

 The document describing this file, and
 updated versions can be found at:
    http://www.signal11.us/oss/udev/

 Alan Ott
 Signal 11 Software
 *******************************************/

#include "LinuxUdev.h"

jobject get_interface_number(JNIEnv *env, struct udev_device *dev_interface) {
	jclass integerClass;
	jmethodID integerConstructorID;
	jobject interfaceNumber = NULL;
	int interfaceNumberInt;

	integerClass = (*env)->FindClass(env, "java/lang/Integer");
	if (integerClass == NULL) return NULL;
	integerConstructorID = (*env)->GetMethodID(env, integerClass, "<init>", "(I)V");
	if (integerConstructorID == NULL) return NULL;

	const char* interfaceNumberString = udev_device_get_sysattr_value(dev_interface, "bInterfaceNumber");
	if (interfaceNumberString) {
		interfaceNumberInt = (int) strtol(interfaceNumberString,NULL,16);
		interfaceNumber = (*env)->NewObject(env, integerClass, integerConstructorID, interfaceNumberInt);
	}

	return interfaceNumber;

}

JNIEXPORT jobject JNICALL Java_org_eclipse_kura_linux_usb_LinuxUdevNative_getUsbDevices(JNIEnv *env, jclass LinuxUdevNative, jstring deviceClass) {
	struct udev *udev;
	struct udev_enumerate *enumerate;
	struct udev_list_entry *devices, *dev_list_entry;
	struct udev_device *dev, *dev_parent;

	const char *nativeDeviceClass = (*env)->GetStringUTFChars(env, deviceClass, 0);
	jclass UsbDeviceClass = NULL;
	jmethodID UsbDeviceConstructor = NULL;
	jobject UsbDeviceObject = NULL;

	//specifics for BLOCK devices
	jstring blockDeviceNode = NULL;

	//specifics for NET devices
	jstring interfaceName = NULL;

	//specifics for TTY devices
	jstring ttyDeviceNode = NULL;

	//initialize the ArrayList for the return
	jclass arrayClass = (*env)->FindClass(env, "java/util/ArrayList");
	if (arrayClass == NULL) return NULL;
	jmethodID mid_init =  (*env)->GetMethodID(env, arrayClass, "<init>", "()V");
	if (mid_init == NULL) return NULL;
	jobject objArr = (*env)->NewObject(env, arrayClass, mid_init);
	if (objArr == NULL) return NULL;
	jmethodID mid_add = (*env)->GetMethodID(env, arrayClass, "add", "(Ljava/lang/Object;)Z");
	if (mid_add == NULL) return NULL;

	//set up our specific device type constructor
	if(strcmp(nativeDeviceClass,"block")==0) {
		UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbBlockDevice");
		if (UsbDeviceClass == NULL) return NULL;
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		if (UsbDeviceConstructor == NULL) return NULL;
	} else if(strcmp(nativeDeviceClass,"net")==0) {
		UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbNetDevice");
		if (UsbDeviceClass == NULL) return NULL;
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		if (UsbDeviceConstructor == NULL) return NULL;
	} else if(strcmp(nativeDeviceClass,"tty")==0) {
		UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbTtyDevice");
		if (UsbDeviceClass == NULL) return NULL;
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V");
		if (UsbDeviceConstructor == NULL) return NULL;
	}

	/* Create the udev object */
	udev = udev_new();
	if (!udev) {
		printf("Can't create udev\n");
		jclass Exception = (*env)->FindClass(env, "java/lang/IOException");
		(*env)->ThrowNew(env, Exception,"Can't create udev object.");
	}

	/* Create a list of the devices in the '/sys/class/' subsystem. */
	enumerate = udev_enumerate_new(udev);
	udev_enumerate_add_match_subsystem(enumerate, nativeDeviceClass);

	udev_enumerate_scan_devices(enumerate);
	devices = udev_enumerate_get_list_entry(enumerate);

	//loop through what we've found
	udev_list_entry_foreach(dev_list_entry, devices) {
		const char *path;

		/* Get the filename of the /sys entry for the device
		   and create a udev_device object (dev) representing it */
		path = udev_list_entry_get_name(dev_list_entry);
		dev = udev_device_new_from_syspath(udev, path);

		/* usb_device_get_devnode() returns the path to the device node
		   itself in /dev - save this for TTY devices */
		if(strcmp(nativeDeviceClass,"block")==0) {
			blockDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
		} else if(strcmp(nativeDeviceClass,"net")==0) {
			interfaceName = (*env)->NewStringUTF(env, udev_device_get_sysname(dev));
		} else if(strcmp(nativeDeviceClass,"tty")==0) {
			ttyDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
		}

		/* The device pointed to by dev contains information about
		   the /sys/class/ device. In order to get information about the
		   USB device, get the parent device with the
		   subsystem/devtype pair of "usb"/"usb_device". This will
		   be several levels up the tree, but the function will find
		   it.*/
		dev_parent = udev_device_get_parent_with_subsystem_devtype(
				dev,
				"usb",
				"usb_device");

		if (dev_parent) {
			if(strcmp(nativeDeviceClass,"block")==0) {
				UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
						blockDeviceNode);
			} else if(strcmp(nativeDeviceClass,"net")==0) {
				UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
						(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
						interfaceName);
			} else if(strcmp(nativeDeviceClass,"tty")==0) {

				/* Get the parent with usb_interface devtype to retrieve the tty interface number */
				struct udev_device *dev_interface = udev_device_get_parent_with_subsystem_devtype(
						dev,
						"usb",
						"usb_interface");

				if (dev_interface) {
					const jobject interfaceNumber = get_interface_number(env, dev_interface);
					if (interfaceNumber == NULL) {
						udev_device_unref(dev);
						udev_enumerate_unref(enumerate);
						udev_unref(udev);
						(*env)->ReleaseStringUTFChars(env, deviceClass, nativeDeviceClass);

						return NULL;
					}
					UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
							(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
							ttyDeviceNode,
							interfaceNumber);
				}
			}

			//add it to the ArrayList
			const jboolean jbool = (*env)->CallBooleanMethod(env, objArr, mid_add, UsbDeviceObject);

			if ( JNI_FALSE == jbool ) {
				/* early clean up */

				udev_device_unref(dev);
				udev_enumerate_unref(enumerate);
				udev_unref(udev);
				(*env)->ReleaseStringUTFChars(env, deviceClass, nativeDeviceClass);

				return objArr;
			}

			udev_device_unref(dev);
		}
	}

	/* Free the enumerator object */
	udev_enumerate_unref(enumerate);
	udev_unref(udev);

	/* release the nativeDeviceClass */
	(*env)->ReleaseStringUTFChars(env, deviceClass, nativeDeviceClass);

	return objArr;
}

JNIEXPORT void JNICALL Java_org_eclipse_kura_linux_usb_LinuxUdevNative_nativeHotplugThread(JNIEnv *env, jclass LinuxUdevNative, jobject linuxUdevNative) {

	jclass UsbDeviceClass = NULL;
	jmethodID UsbDeviceConstructor = NULL;
	jobject UsbDeviceObject = NULL;

	//event type enum for return
	jstring eventType = NULL;

	//specifics for BLOCK devices
	jstring blockDeviceNode = NULL;

	//specifics for NET devices
	jstring interfaceName = NULL;

	//specifics for TTY devices
	jstring ttyDeviceNode = NULL;

	struct udev *udev;

	/* Create the udev object */
	udev = udev_new();
	if (!udev) {
		printf("Can't create udev\n");
		jclass Exception = (*env)->FindClass(env, "java/lang/IOException");
		(*env)->ThrowNew(env, Exception,"Can't create udev object.");
	}

	/* Set up a monitor to monitor selected USB devices */
	struct udev_monitor *mon = udev_monitor_new_from_netlink(udev, "udev");
	udev_monitor_filter_add_match_subsystem_devtype(mon, "block", NULL);
	udev_monitor_filter_add_match_subsystem_devtype(mon, "net", NULL);
	udev_monitor_filter_add_match_subsystem_devtype(mon, "tty", NULL);
	udev_monitor_enable_receiving(mon);
	/* Get the file descriptor (fd) for the monitor.
	   This fd will get passed to select() */
	int fd = udev_monitor_get_fd(mon);

	/* This section will run continuously, calling usleep() at
	   the end of each pass. This is to demonstrate how to use
	   a udev_monitor in a non-blocking way. */
	while (1) {
		/* Set up the call to select(). In this case, select() will
		   only operate on a single file descriptor, the one
		   associated with our udev_monitor. Note that the timeval
		   object is set to 0, which will cause select() to not
		   block. */
		fd_set fds;
		struct timeval tv;
		int ret;

		FD_ZERO(&fds);
		FD_SET(fd, &fds);
		tv.tv_sec = 0;
		tv.tv_usec = 0;

		ret = select(fd+1, &fds, NULL, NULL, &tv);

		/* Check if our file descriptor has received data. */
		if (ret > 0 && FD_ISSET(fd, &fds)) {

			/* Make the call to receive the device.
			   select() ensured that this will not block. */
			struct udev_device *dev = udev_monitor_receive_device(mon);
			if (dev) {
				const char *subsystem = udev_device_get_subsystem(dev);

				if(strcmp(subsystem,"block")==0) {
					blockDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
				} else if(strcmp(subsystem,"net")==0) {
					interfaceName = (*env)->NewStringUTF(env, udev_device_get_sysname(dev));
				} else if(strcmp(subsystem,"tty")==0) {
					ttyDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
				}

				//get the event type
				const char *action = udev_device_get_action(dev);
				if(strcmp("add",action)==0) {
					eventType = (*env)->NewStringUTF(env, "ATTACHED");
				} else if(strcmp("remove",action)==0) {
					eventType = (*env)->NewStringUTF(env, "DETACHED");
				}

				struct udev_device *dev_parent = udev_device_get_parent_with_subsystem_devtype(
						dev,
						"usb",
						"usb_device");

				if (dev_parent) {
					if(strcmp(subsystem,"block")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbBlockDevice");
						if (UsbDeviceClass == NULL) {
							udev_device_unref(dev);
							break;
						}
						UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
						if (UsbDeviceConstructor == NULL) {
							udev_device_unref(dev);
							break;
						}

						UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
								blockDeviceNode);

						jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
						(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
					} else if(strcmp(subsystem,"net")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbNetDevice");
						if (UsbDeviceClass == NULL) {
							udev_device_unref(dev);
							break;
						}
						UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
						if (UsbDeviceConstructor == NULL) {
							udev_device_unref(dev);
							break;
						}

						UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
								(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
								interfaceName);

						jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
						(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
					} else if(strcmp(subsystem,"tty")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbTtyDevice");
						if (UsbDeviceClass == NULL) {
							udev_device_unref(dev);
							break;
						}

						/* Get the parent with usb_interface devtype to retrieve the tty interface number */
						struct udev_device *dev_interface = udev_device_get_parent_with_subsystem_devtype(
								dev,
								"usb",
								"usb_interface");

						if (dev_interface) {
							UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V");
							if (UsbDeviceConstructor == NULL) {
								udev_device_unref(dev);
								break;
							}

							const jobject interfaceNumber = get_interface_number(env, dev_interface);
							if (interfaceNumber == NULL) {
								udev_device_unref(dev);
								break;
							}

							UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
									ttyDeviceNode,
									interfaceNumber);

							jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
							(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);

						} else {
							/* During detaching the usb_interface dev does not exists...*/
							UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
							if (UsbDeviceConstructor == NULL) {
								udev_device_unref(dev);
								break;
							}

							UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idVendor")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "idProduct")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "manufacturer")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "product")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "busnum")),
									(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev_parent, "devpath")),
									ttyDeviceNode);

							jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
							(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
						}
					}
					udev_device_unref(dev);
				}
			}
			else {
				printf("No Device from receive_device(). An error occured.\n");
			}
		}
		usleep(250*1000);
	}

}
