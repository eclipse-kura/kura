#include "LinuxUdev.h"

JNIEXPORT jobject JNICALL Java_org_eclipse_kura_linux_util_LinuxUdevNative_getUsbDevices(JNIEnv *env, jclass LinuxUdevNative, jstring deviceClass) {
	struct udev *udev;
	struct udev_enumerate *enumerate;
	struct udev_list_entry *devices, *dev_list_entry;
	struct udev_device *dev;

	const char *nativeDeviceClass = (*env)->GetStringUTFChars(env, deviceClass, 0);
	jclass UsbDeviceClass;
	jmethodID UsbDeviceConstructor;
	jobject UsbDeviceObject;

	//specifics for BLOCK devices
	jstring blockDeviceNode;

	//specifics for NET devices
	jstring interfaceName;

	//specifics for TTY devices
	jstring ttyDeviceNode;

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
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	} else if(strcmp(nativeDeviceClass,"net")==0) {
		UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbNetDevice");
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	} else if(strcmp(nativeDeviceClass,"tty")==0) {
		UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbTtyDevice");
		UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
	}

	/* Create the udev object */
	udev = udev_new();
	if (!udev) {
		printf("Can't create udev\n");
		exit(1);
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
		//printf("Device Node Path: %s\n", udev_device_get_devnode(dev));
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
		dev = udev_device_get_parent_with_subsystem_devtype(
		       dev,
		       "usb",
		       "usb_device");
		if (dev) {
			if(strcmp(nativeDeviceClass,"block")==0) {
				UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										blockDeviceNode);
			} else if(strcmp(nativeDeviceClass,"net")==0) {
				UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										interfaceName);
			} else if(strcmp(nativeDeviceClass,"tty")==0) {
				UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										ttyDeviceNode);
			}

			//add it to the ArrayList
			jboolean jbool = (*env)->CallBooleanMethod(env, objArr, mid_add, UsbDeviceObject);
			if (jbool == NULL) return NULL;

			udev_device_unref(dev);
		}
	}
	/* Free the enumerator object */
	udev_enumerate_unref(enumerate);

	udev_unref(udev);

	//release the nativeDeviceClass
	(*env)->ReleaseStringUTFChars(env, deviceClass, nativeDeviceClass);

	return objArr;
}

JNIEXPORT void JNICALL Java_org_eclipse_kura_linux_util_LinuxUdevNative_nativeHotplugThread(JNIEnv *env, jclass LinuxUdevNative, jobject linuxUdevNative) {

	jclass UsbDeviceClass;
	jmethodID UsbDeviceConstructor;
	jobject UsbDeviceObject;

	//event type enum for return
	jstring eventType;

	//specifics for BLOCK devices
	jstring blockDeviceNode;

	//specifics for NET devices
	jstring interfaceName;

	//specifics for TTY devices
	jstring ttyDeviceNode;

	struct udev *udev;

	/* Create the udev object */
	udev = udev_new();
	if (!udev) {
		printf("Can't create udev\n");
		return;
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
			//printf("\nselect() says there should be data\n");

			/* Make the call to receive the device.
			   select() ensured that this will not block. */
			struct udev_device *dev = udev_monitor_receive_device(mon);
			if (dev) {
				char *subsystem = udev_device_get_subsystem(dev);

				if(strcmp(subsystem,"block")==0) {
					blockDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
				} else if(strcmp(subsystem,"net")==0) {
					interfaceName = (*env)->NewStringUTF(env, udev_device_get_sysname(dev));
				} else if(strcmp(subsystem,"tty")==0) {
					ttyDeviceNode = (*env)->NewStringUTF(env, udev_device_get_devnode(dev));
				}

/*
				printf("Got Device\n");
				printf("   Node: %s\n", udev_device_get_devnode(dev));
				printf("   Subsystem: %s\n", udev_device_get_subsystem(dev));
				printf("   Devtype: %s\n", udev_device_get_devtype(dev));

				printf("   Action: %s\n",udev_device_get_action(dev));
*/

				//get the event type
				char *action = udev_device_get_action(dev);
				if(strcmp("add",action)==0) {
					eventType = (*env)->NewStringUTF(env, "ATTACHED");
				} else if(strcmp("remove",action)==0) {
					eventType = (*env)->NewStringUTF(env, "DETACHED");
				}

				dev = udev_device_get_parent_with_subsystem_devtype(
						       dev,
						       "usb",
						       "usb_device");
				if (dev) {
					if(strcmp(subsystem,"block")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbBlockDevice");
						UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

						UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										blockDeviceNode);

						jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
						(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
					} else if(strcmp(subsystem,"net")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbNetDevice");
						UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

						UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										interfaceName);

						jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
						(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
					} else if(strcmp(subsystem,"tty")==0) {
						UsbDeviceClass = (*env)->FindClass(env, "org/eclipse/kura/usb/UsbTtyDevice");
						UsbDeviceConstructor = (*env)->GetMethodID(env, UsbDeviceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

						UsbDeviceObject = (*env)->NewObject(env, UsbDeviceClass, UsbDeviceConstructor,
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idVendor")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "idProduct")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "manufacturer")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "product")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "busnum")),
										(*env)->NewStringUTF(env, udev_device_get_sysattr_value(dev, "devpath")),
										ttyDeviceNode);

						jmethodID LinuxUdevNativeCallback = (*env)->GetMethodID(env, LinuxUdevNative, "callback", "(Ljava/lang/String;Lorg/eclipse/kura/usb/UsbDevice;)V");
						(*env)->CallVoidMethod(env, linuxUdevNative, LinuxUdevNativeCallback, eventType, UsbDeviceObject);
					}

					udev_device_unref(dev);
				}
			}
			else {
				printf("No Device from receive_device(). An error occured.\n");
			}
		}
		usleep(250*1000);
		//printf(".");
		fflush(stdout);
	}
}
