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
 * OpcUaMessages is considered to be a localization resource for
 * {@code OPC-UA Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code OPC-UA Driver} bundle.
 */
public interface OpcUaMessages {

	@En("Activating OPC-UA Driver.....")
	public String activating();

	@En("Activating OPC-UA Driver.....Done")
	public String activatingDone();

	@En("Unable to Connect...No desired Endpoints returned")
	public String connectionProblem();

	@En("Deactivating OPC-UA Driver.....")
	public String deactivating();

	@En("Deactivating OPC-UA Driver.....Done")
	public String deactivatingDone();

	@En("Unable to Disconnect...")
	public String disconnectionProblem();

	@En("Error while disconnecting....")
	public String errorDisconnecting();

	@En("Node Identifier")
	public String nodeId();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("OPC-UA Read Operation Failed")
	public String readFailed();

	@En("Updating OPC-UA Driver.....")
	public String updating();

	@En("Updating OPC-UA Driver.....Done")
	public String updatingDone();

	@En("Value cannot be null")
	public String valueNonNull();

	@En("OPC-UA Write Operation Failed")
	public String writeFailed();

}
