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
package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001,2008 International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Class to display Version info.
 * @author Dan Streetman
 */
public class Version
{
	/** Main method to print out version information */
	public static void main(String[] argv)
	{
		System.out.println("javax.usb Linux implementation version " + LINUX_IMP_VERSION);
		System.out.println("javax.usb Required Platform-Implementation version " + LINUX_API_VERSION + " (or later)");
		System.out.println(LINUX_IMP_DESCRIPTION);
	}

	public static final String LINUX_API_VERSION = "1.0.0";
	public static final String LINUX_IMP_VERSION = "1.0.3";
	public static final String LINUX_IMP_DESCRIPTION =
		 "JSR80 : javax.usb"
		+"\n"
		+"\n"+"Implementation for the Linux kernel (2.4/2.6/3.0).\n"
		+"\n"
		+"\n"+"*"
		+"\n"+"* Copyright (c) 1999 - 2001,2008 International Business Machines Corporation."
		+"\n"+"* All Rights Reserved."
		+"\n"+"*"
		+"\n"+"* This software is provided and licensed under the terms and conditions"
		+"\n"+"* of the Common Public License:"
		+"\n"+"* http://oss.software.ibm.com/developerworks/opensource/license-cpl.html"
		+"\n"
		+"\n"+"http://javax-usb.org/"
		;

}
