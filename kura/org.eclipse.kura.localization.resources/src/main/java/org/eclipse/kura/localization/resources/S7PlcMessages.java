/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * S7PlcMessages is considered to be a localization resource for
 * {@code S7 PLC Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code S7 PLC Driver} bundle.
 */
public interface S7PlcMessages {

	@En("Activating S7 PLC Driver.....")
	public String activating();

	@En("Activating S7 PLC Driver.....Done")
	public String activatingDone();

	@En("Area No")
	public String areaNo();

	@En("Unable to Connect...")
	public String connectionProblem();

	@En("Deactivating S7 PLC Driver.....")
	public String deactivating();

	@En("Deactivating S7 PLC Driver.....Done")
	public String deactivatingDone();

	@En("Unable to Disconnect...")
	public String disconnectionProblem();

	@En("Error while disconnecting....")
	public String errorDisconnecting();

	@En("Channel Value Type must be an instance of Byte Array Value")
	public String instanceOfByteArray();

	@En("Channel Value Type must be an instance of Integer Value")
	public String instanceOfInteger();

	@En("Offset")
	public String offset();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("Updating S7 PLC Driver.....")
	public String updating();

	@En("Updating S7 PLC Driver.....Done")
	public String updatingDone();

}
