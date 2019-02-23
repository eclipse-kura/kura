/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class SerialModemDevice implements ModemDevice {

    private final String productName;
    private final String manufacturerName;
    private final List<String> serialPorts;

    public SerialModemDevice(String product, String manufacturer, List<String> serialPorts) {
        this.productName = product;
        this.manufacturerName = manufacturer;
        this.serialPorts = serialPorts;
    }

    @Override
    public String getProductName() {
        return this.productName;
    }

    @Override
    public String getManufacturerName() {
        return this.manufacturerName;
    }

    @Override
    public List<String> getSerialPorts() {
        return this.serialPorts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((manufacturerName == null) ? 0 : manufacturerName.hashCode());
        result = prime * result + ((productName == null) ? 0 : productName.hashCode());
        result = prime * result + ((serialPorts == null) ? 0 : serialPorts.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SerialModemDevice other = (SerialModemDevice) obj;
        if (manufacturerName == null) {
            if (other.manufacturerName != null)
                return false;
        } else if (!manufacturerName.equals(other.manufacturerName))
            return false;
        if (productName == null) {
            if (other.productName != null)
                return false;
        } else if (!productName.equals(other.productName))
            return false;
        if (serialPorts == null) {
            if (other.serialPorts != null)
                return false;
        } else if (!serialPorts.equals(other.serialPorts))
            return false;
        return true;
    }
}
