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
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

import javax.usb.util.UsbUtil;

import com.ibm.jusb.*;
import com.ibm.jusb.util.*;

/**
 * Interface for configuration-changing Requests.
 * @author Dan Streetman
 */
public class LinuxSetConfigurationRequest extends LinuxControlRequest
{
	/** @return This request's type. */
	public int getType() { return LinuxRequest.LINUX_SET_CONFIGURATION_REQUEST; }

	/** @return The configuration number */
	public int getConfiguration() { return configuration; }

	/** @param irp The UsbControlIrpImp */
	public void setUsbIrpImp(UsbControlIrpImp irp)
	{
		super.setUsbIrpImp(irp);
		configuration = UsbUtil.unsignedInt((byte)irp.wValue());
	}

	private int configuration = 0;
}
