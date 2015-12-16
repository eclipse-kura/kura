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
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.SimCardSlot;

public interface HspaCellularModem extends CellularModem {

	public SimCardSlot getSimCardSlot() throws KuraException;
	public SimCardSlot getSimCardSlot(String port) throws KuraException;
	public boolean setSimCardSlot(SimCardSlot simCardSlot) throws KuraException;
	public boolean setSimCardSlot(SimCardSlot simCardSlot, String port) throws KuraException;
	public boolean isSimCardReady() throws KuraException;
	public boolean isSimCardReady(String port) throws KuraException;
}
