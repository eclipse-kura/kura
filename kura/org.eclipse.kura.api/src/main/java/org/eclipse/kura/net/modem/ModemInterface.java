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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetInterface;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Network interface for modems.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ModemInterface<T extends ModemInterfaceAddress> extends NetInterface<T> {

    /**
     * Reports ppp interface number for this modem
     *
     * @return ppp interface number as {@link int}
     */
    public int getPppNum();

    /**
     * Reports identifier string for this modem
     *
     * @return modem identifier as {@link String}
     */
    public String getModemIdentifier();

    /**
     * Reports modem's model
     *
     * @return model, null if not known
     */
    public String getModel();

    /**
     * Returns modem's manufacturer identification
     *
     * @return manufacturer, null if not known
     */
    public String getManufacturer();

    /**
     * Answers modem's serial number
     *
     * @return ESN, null if not known
     */
    public String getSerialNumber();

    /**
     * Reports modem's revision identification
     *
     * @return array of revision ID's, null if not known
     */
    public String[] getRevisionId();

    /**
     * Reports network technology (e.g. EVDO, HSDPA, etc)
     *
     * @return - network technology as <code>ModemTechnologyType</code>
     */
    public List<ModemTechnologyType> getTechnologyTypes();

    /**
     * Reports if modem is powered on
     *
     * @return
     *         true - modem is on <br>
     *         false - modem is off
     */
    public boolean isPoweredOn();

    /**
     * Reports modem's power mode. (e.g. ONLINE, OFFLINE, LOW_POWER)
     *
     * @return modem power mode
     */
    public ModemPowerMode getPowerMode();

    /**
     * Return's the associated ModemDevice for this modem
     *
     * @return <code>ModemDevice</code>
     */
    public ModemDevice getModemDevice();

    /**
     * Reports if GPS is supported
     *
     * @return * @return
     *         true - GPS is supported <br>
     *         false - GPS is not supported
     */
    public boolean isGpsSupported();
}
