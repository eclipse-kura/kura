/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemConnectionStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.eclipse.kura.net.status.modem.RegistrationStatus;

@SuppressWarnings("unused")
public class ModemInterfaceStatusDTO extends NetworkInterfaceStatusDTO {

    private final String model;
    private final String manufacturer;
    private final String serialNumber;
    private final String softwareRevision;
    private final String hardwareRevision;
    private final String primaryPort;
    private final Map<String, ModemPortType> ports;
    private final Set<ModemCapability> supportedModemCapabilities;
    private final Set<ModemCapability> currentModemCapabilities;
    private final ModemPowerState powerState;
    private final Set<ModemModePairDTO> supportedModes;
    private final ModemModePairDTO currentModes;
    private final Set<ModemBand> supportedBands;
    private final Set<ModemBand> currentBands;
    private final boolean gpsSupported;
    private final List<SimDTO> availableSims;
    private final boolean simLocked;
    private final List<BearerDTO> bearers;
    private final ModemConnectionType connectionType;
    private final ModemConnectionStatus connectionStatus;
    private final Set<AccessTechnology> accessTechnologies;
    private final int signalQuality;
    private final int signalStrength;
    private final RegistrationStatus registrationStatus;
    private final String operatorName;

    public ModemInterfaceStatusDTO(final ModemInterfaceStatus status) {
        super(status);

        this.model = status.getModel();
        this.manufacturer = status.getManufacturer();
        this.serialNumber = status.getSerialNumber();
        this.softwareRevision = status.getSoftwareRevision();
        this.hardwareRevision = status.getHardwareRevision();
        this.primaryPort = status.getPrimaryPort();
        this.ports = status.getPorts();
        this.supportedModemCapabilities = status.getSupportedModemCapabilities();
        this.currentModemCapabilities = status.getCurrentModemCapabilities();
        this.powerState = status.getPowerState();
        this.supportedModes = status.getSupportedModes().stream().map(ModemModePairDTO::new)
                .collect(Collectors.toSet());
        this.currentModes = new ModemModePairDTO(status.getCurrentModes());
        this.supportedBands = status.getSupportedBands();
        this.currentBands = status.getCurrentBands();
        this.gpsSupported = status.isGpsSupported();
        this.availableSims = status.getAvailableSims().stream().map(SimDTO::new).collect(Collectors.toList());
        this.simLocked = status.isSimLocked();
        this.bearers = status.getBearers().stream().map(BearerDTO::new).collect(Collectors.toList());
        this.connectionType = status.getConnectionType();
        this.connectionStatus = status.getConnectionStatus();
        this.accessTechnologies = status.getAccessTechnologies();
        this.signalQuality = status.getSignalQuality();
        this.signalStrength = status.getSignalStrength();
        this.registrationStatus = status.getRegistrationStatus();
        this.operatorName = status.getOperatorName();
    }

}
