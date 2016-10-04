/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.modem;

import java.util.List;

public class SerialModemDevice implements ModemDevice {

    private final String m_productName;
    private final String m_manufacturerName;
    private final List<String> m_serialPorts;

    public SerialModemDevice(String product, String manufacturer, List<String> serialPorts) {
        this.m_productName = product;
        this.m_manufacturerName = manufacturer;
        this.m_serialPorts = serialPorts;
    }

    @Override
    public String getProductName() {
        return this.m_productName;
    }

    @Override
    public String getManufacturerName() {
        return this.m_manufacturerName;
    }

    @Override
    public List<String> getSerialPorts() {
        return this.m_serialPorts;
    }
}
