/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
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
        result = prime * result + (this.manufacturerName == null ? 0 : this.manufacturerName.hashCode());
        result = prime * result + (this.productName == null ? 0 : this.productName.hashCode());
        result = prime * result + (this.serialPorts == null ? 0 : this.serialPorts.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SerialModemDevice other = (SerialModemDevice) obj;
        if (this.manufacturerName == null) {
            if (other.manufacturerName != null) {
                return false;
            }
        } else if (!this.manufacturerName.equals(other.manufacturerName)) {
            return false;
        }
        if (this.productName == null) {
            if (other.productName != null) {
                return false;
            }
        } else if (!this.productName.equals(other.productName)) {
            return false;
        }
        if (this.serialPorts == null) {
            if (other.serialPorts != null) {
                return false;
            }
        } else if (!this.serialPorts.equals(other.serialPorts)) {
            return false;
        }
        return true;
    }
}
