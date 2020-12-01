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
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ModemDevice {

    /**
     * The list of serial ports available on the device
     *
     * @return a list of serial ports names
     */
    public List<String> getSerialPorts();

    /**
     * The manufacturer name of the device
     *
     * @return The manufacturer name of the device
     */
    public String getManufacturerName();

    /**
     * The product name of the device
     *
     * @return The product name of the device
     */
    public String getProductName();

}
