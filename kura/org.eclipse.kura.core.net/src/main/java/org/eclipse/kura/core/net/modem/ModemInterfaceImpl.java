/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.core.net.modem;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemPowerMode;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemInterfaceImpl<T extends ModemInterfaceAddress> extends AbstractNetInterface<T>
        implements ModemInterface<T> {

    private static final Logger s_logger = LoggerFactory.getLogger(ModemInterfaceImpl.class);

    private String m_modemId;
    private int m_pppNum;
    private String m_model;
    private String m_manufacturer;
    private String m_serialNumber;
    private String[] m_revisionId;
    private List<ModemTechnologyType> m_technologyTypes;
    private boolean m_poweredOn;
    private ModemPowerMode m_powerMode;
    private ModemDevice m_modemDevice;
    private boolean m_gpsSupported;

    public ModemInterfaceImpl(String name) {
        super(name);
    }

    public ModemInterfaceImpl(Class<T> modemInterfaceAddressClass,
            ModemInterface<? extends ModemInterfaceAddress> other) {
        super(other);
        this.m_modemId = other.getModemIdentifier();
        this.m_pppNum = other.getPppNum();
        this.m_model = other.getModel();
        this.m_manufacturer = other.getManufacturer();
        this.m_serialNumber = other.getSerialNumber();
        this.m_revisionId = other.getRevisionId();
        this.m_technologyTypes = other.getTechnologyTypes();
        this.m_poweredOn = other.isPoweredOn();
        this.m_powerMode = other.getPowerMode();
        this.m_modemDevice = other.getModemDevice();

        // Copy the NetInterfaceAddresses
        List<? extends ModemInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<T> interfaceAddresses = new ArrayList<T>();

        if (otherNetInterfaceAddresses != null) {
            for (ModemInterfaceAddress modemInterfaceAddress : otherNetInterfaceAddresses) {
                try {
                    ModemInterfaceAddressImpl copiedInterfaceAddressImpl = new ModemInterfaceAddressImpl(
                            modemInterfaceAddress);
                    interfaceAddresses.add(modemInterfaceAddressClass.cast(copiedInterfaceAddressImpl));
                } catch (Exception e) {
                    s_logger.debug("Could not copy interface address: {}", modemInterfaceAddress);
                }
            }
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }

    @Override
    public NetInterfaceType getType() {
        return NetInterfaceType.MODEM;
    }

    @Override
    public int getPppNum() {
        return this.m_pppNum;
    }

    public void setPppNum(int pppNum) {
        this.m_pppNum = pppNum;
    }

    @Override
    public String getModemIdentifier() {
        return this.m_modemId;
    }

    public void setModemIdentifier(String modemId) {
        this.m_modemId = modemId;
    }

    @Override
    public String getModel() {
        return this.m_model;
    }

    public void setModel(String model) {
        this.m_model = model;
    }

    @Override
    public String getManufacturer() {
        return this.m_manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.m_manufacturer = manufacturer;
    }

    @Override
    public String getSerialNumber() {
        return this.m_serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.m_serialNumber = serialNumber;
    }

    @Override
    public String[] getRevisionId() {
        return this.m_revisionId;
    }

    public void setRevisionId(String[] revisionId) {
        this.m_revisionId = revisionId;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.m_technologyTypes;
    }

    public void setTechnologyTypes(List<ModemTechnologyType> technologyTypes) {
        this.m_technologyTypes = technologyTypes;
    }

    @Override
    public boolean isPoweredOn() {
        return this.m_poweredOn;
    }

    public void setPoweredOn(boolean poweredOn) {
        this.m_poweredOn = poweredOn;
    }

    @Override
    public ModemPowerMode getPowerMode() {
        return this.m_powerMode;
    }

    public void setPowerMode(ModemPowerMode powerMode) {
        this.m_powerMode = powerMode;
    }

    @Override
    public ModemDevice getModemDevice() {
        return this.m_modemDevice;
    }

    public void setModemDevice(ModemDevice modemDevice) {
        this.m_modemDevice = modemDevice;
    }

    @Override
    public boolean isGpsSupported() {
        return this.m_gpsSupported;
    }

    public void setGpsSupported(boolean m_gpsSupported) {
        this.m_gpsSupported = m_gpsSupported;
    }
}
