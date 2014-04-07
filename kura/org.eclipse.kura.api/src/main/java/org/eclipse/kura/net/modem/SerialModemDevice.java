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
package org.eclipse.kura.net.modem;

import java.util.List;

public class SerialModemDevice implements ModemDevice {

	private String m_productName;
	private String m_manufacturerName;
	private List<String> m_serialPorts;
	
	public SerialModemDevice (String product, String manufacturer, List<String> serialPorts) {
		m_productName = product;
		m_manufacturerName = manufacturer;
		m_serialPorts = serialPorts;
	}

	public String getProductName() {
		return m_productName;
	}
	
	public String getManufacturerName() {
		return m_manufacturerName;
	}

	@Override
	public List<String> getSerialPorts() {
		return m_serialPorts;
	}
}
