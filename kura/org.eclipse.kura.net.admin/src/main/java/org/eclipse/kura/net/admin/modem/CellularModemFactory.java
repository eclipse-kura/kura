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
package org.eclipse.kura.net.admin.modem;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;

public interface CellularModemFactory {

    /**
     * Obtains generic modem service
     *
     * @param modemDevice
     *            - modem device as <code>ModemDevice</code>
     * @param platform
     *            - hardware platform as {@link String}
     * @return generic modem service as <code>CellularModemService</code>
     * @throws Exception
     */
    public CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform) throws Exception;

    /**
     * Releases a modem service created by this factory instance
     *
     * @param usbPortAddress
     */
    public void releaseModemService(CellularModem modem);

    /**
     * Reports modem services available
     *
     * @return list of modem services as <code>Hashtable<String, CellularModemService></code>
     *         with the usb port as the key
     */
    public Map<ModemDevice, CellularModem> getAllModemServices();

    /**
     * Reports type of modem service
     *
     * @return type of modem service as <code>ModemTechnologyType</code>
     */
    @Deprecated
    public ModemTechnologyType getType();

    /**
     * Reports modem services available
     *
     * @return list of modem services as <code>Hashtable<String, CellularModemService></code>
     *         with the usb port as the key
     */
    @Deprecated
    public Hashtable<String, ? extends CellularModem> getModemServices();

    /**
     * Releases modem service specified by its USB address
     *
     * @param usbPortAddress
     */
    @Deprecated
    public void releaseModemService(String usbPortAddress);
}
