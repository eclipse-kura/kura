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
