/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
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

    private String modemId;
    private int pppNum;
    private String model;
    private String manufacturer;
    private String serialNumber;
    private String[] revisionId;
    private List<ModemTechnologyType> technologyTypes;
    private boolean poweredOn;
    private ModemPowerMode powerMode;
    private ModemDevice modemDevice;
    private boolean gpsSupported;

    public ModemInterfaceImpl(String name) {
        super(name);
    }

    public ModemInterfaceImpl(Class<T> modemInterfaceAddressClass,
            ModemInterface<? extends ModemInterfaceAddress> other) {
        super(other);
        this.modemId = other.getModemIdentifier();
        this.pppNum = other.getPppNum();
        this.model = other.getModel();
        this.manufacturer = other.getManufacturer();
        this.serialNumber = other.getSerialNumber();
        this.revisionId = other.getRevisionId();
        this.technologyTypes = other.getTechnologyTypes();
        this.poweredOn = other.isPoweredOn();
        this.powerMode = other.getPowerMode();
        this.modemDevice = other.getModemDevice();

        // Copy the NetInterfaceAddresses
        List<? extends ModemInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<T> interfaceAddresses = new ArrayList<>();

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
        return this.pppNum;
    }

    public void setPppNum(int pppNum) {
        this.pppNum = pppNum;
    }

    @Override
    public String getModemIdentifier() {
        return this.modemId;
    }

    public void setModemIdentifier(String modemId) {
        this.modemId = modemId;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public String[] getRevisionId() {
        return this.revisionId;
    }

    public void setRevisionId(String[] revisionId) {
        this.revisionId = revisionId;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() {
        return this.technologyTypes;
    }

    public void setTechnologyTypes(List<ModemTechnologyType> technologyTypes) {
        this.technologyTypes = technologyTypes;
    }

    @Override
    public boolean isPoweredOn() {
        return this.poweredOn;
    }

    public void setPoweredOn(boolean poweredOn) {
        this.poweredOn = poweredOn;
    }

    @Override
    public ModemPowerMode getPowerMode() {
        return this.powerMode;
    }

    public void setPowerMode(ModemPowerMode powerMode) {
        this.powerMode = powerMode;
    }

    @Override
    public ModemDevice getModemDevice() {
        return this.modemDevice;
    }

    public void setModemDevice(ModemDevice modemDevice) {
        this.modemDevice = modemDevice;
    }

    @Override
    public boolean isGpsSupported() {
        return this.gpsSupported;
    }

    public void setGpsSupported(boolean gpsSupported) {
        this.gpsSupported = gpsSupported;
    }
}
