/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.linux.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxSerialDev {

	private static final Logger s_logger = LoggerFactory.getLogger(LinuxSerialDev.class);
	
	public static final String USB_DEVICES_DIR = "/sys/bus/usb/devices/";


	/**
	 * Returns a Set of tty ports associated with the specified usb address.
	 * 
	 * @param usbAddress
	 * @return Set<String> of dev ports
	 */
	public static Set<String> getUsbPorts(String usbAddress) {
		HashSet<String> ports = new HashSet<String>();

		File usbDevDir = new File(USB_DEVICES_DIR);

		// Search the directory for files that start with the specified usb address
		for(File file : usbDevDir.listFiles()) {
			if(file.getName().matches(usbAddress + "(:.*)?")) {
				try {
					// Check to see if it has a 'tty' subdirectory, and if so get the list of filenames in that directory
					File ttyDir = new File(file.getCanonicalPath() + "/tty");
					if(ttyDir.isDirectory()) {
						for(File tty : ttyDir.listFiles()) {
							ports.add("/dev/" + tty.getName());
						}
					}
				} catch (Exception e) {
					continue;
				}
			}
		}		
		
		return ports;
	}
}
