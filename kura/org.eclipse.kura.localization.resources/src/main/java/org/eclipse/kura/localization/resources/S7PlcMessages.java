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

	@En("ANALOGINPUTS200")
	public String analogInput();

	@En("ANALOGOUTPUTS200")
	public String analogOutput();

	@En("Area No")
	public String areaNo();

	@En("Unable to Connect...")
	public String connectionProblem();

	@En("COUNTER")
	public String counter();

	@En("COUNTER200")
	public String counter200();

	@En("Dave Area")
	public String daveArea();

	@En("DB")
	public String db();

	@En("Deactivating S7 PLC Driver.....")
	public String deactivating();

	@En("Deactivating S7 PLC Driver.....Done")
	public String deactivatingDone();

	@En("DI")
	public String di();

	@En("Unable to Disconnect...")
	public String disconnectionProblem();

	@En("Error while disconnecting....")
	public String errorDisconnecting();

	@En("FLAGS")
	public String flags();

	@En("INPUTS")
	public String inputs();

	@En("LOCAL")
	public String local();

	@En("Offset")
	public String offset();

	@En("OUTPUTS")
	public String outputs();

	@En("P")
	public String p();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("SYSTEMFLAGS")
	public String sysFlags();

	@En("SYSINFO")
	public String sysInfo();

	@En("TIMER")
	public String timer();

	@En("TIMER200")
	public String timer200();

	@En("Updating S7 PLC Driver.....")
	public String updating();

	@En("Updating S7 PLC Driver.....Done")
	public String updatingDone();

	@En("V")
	public String v();

}
