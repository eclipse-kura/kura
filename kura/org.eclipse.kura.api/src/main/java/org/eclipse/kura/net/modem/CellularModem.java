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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.net.NetConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CellularModem {

    public enum SerialPortType {
        DATAPORT,
        ATPORT,
        GPSPORT
    }

    /**
     * Reports modem's model
     *
     * @return model, null if not known
     */
    public String getModel() throws KuraException;

    /**
     * Returns modem's manufacturer identification
     *
     * @return manufacturer, null if not known
     */
    public String getManufacturer() throws KuraException;

    /**
     * Answers modem's serial number (IMEI/MEID/ESN)
     *
     * @return serial number, null if not known
     * @throws KuraException
     */
    public String getSerialNumber() throws KuraException;

    /**
     * Answers International Mobile Subscribe Identity (IMSI)
     *
     * @return IMSI number, null if not known
     * @throws KuraException
     */
    public String getMobileSubscriberIdentity() throws KuraException;

    /**
     * Answers Integrated Circuit Card Identification (ICCID)
     *
     * @return ICCID, "N/A" if not applicable
     * @throws KuraException
     */
    public String getIntegratedCirquitCardId() throws KuraException;

    /**
     * Reports modem's revision identification
     *
     * @return revision ID, null if not known
     */
    public String getRevisionID() throws KuraException;

    /**
     * Reports if Modem replies to the 'AT' command
     *
     * @return 'true' if AT reachable, 'false' otherwise
     * @throws KuraException
     */
    public boolean isReachable() throws KuraException;

    /**
     * Reports if specified port can be opened
     *
     * @param port
     *            - modem's serial port
     * @return 'true' if port can be opened, 'false' otherwise
     */
    public boolean isPortReachable(String port);

    /**
     * resets the modem and tries to restore the state
     * of the modem driver. (e.g. PPP connection, status thread)
     *
     * @throws KuraException
     */
    public void reset() throws KuraException;

    /**
     * Reports signal strength in dBm
     *
     * @throws KuraException
     * @return signal strength
     */
    public int getSignalStrength() throws KuraException;

    /**
     * Reports modem registration status
     *
     * @throws KuraException
     * @return modem registration status as {@link ModemRegistrationStatus}
     */
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException;

    /**
     * Reports number of bytes tarnsmitted during a call
     *
     * @return number of bytes transmitted
     * @throws KuraException
     */
    public long getCallTxCounter() throws KuraException;

    /**
     * Reports number of bytes received during a call
     *
     * @return number of bytes received
     * @throws KuraException
     */
    public long getCallRxCounter() throws KuraException;

    /**
     * Reports Service Type
     *
     * @throws KuraException
     * @return service indication
     */
    public String getServiceType() throws KuraException;

    /**
     * Returns the associated UsbModemDevice
     *
     * @return <code>UsbModemDevice</code>
     */
    public ModemDevice getModemDevice();

    public String getDataPort() throws KuraException;

    public String getAtPort() throws KuraException;

    public String getGpsPort() throws KuraException;

    public CommURI getSerialConnectionProperties(SerialPortType portType) throws KuraException;

    public boolean isGpsSupported() throws KuraException;

    public boolean isGpsEnabled();

    public void enableGps() throws KuraException;

    public void disableGps() throws KuraException;

    public List<NetConfig> getConfiguration();

    public void setConfiguration(List<NetConfig> netConfigs);

    public List<ModemTechnologyType> getTechnologyTypes() throws KuraException;

    /**
     * @since 1.4
     */
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException;
}
