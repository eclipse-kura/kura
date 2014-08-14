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

public interface ModemManagerService {

	public CellularModem getModemService (String ifaceName);
	//public void setModemService (String usbPort, UsbCellularModem modem);
	//public void removeModemService(String usbPort);
}
