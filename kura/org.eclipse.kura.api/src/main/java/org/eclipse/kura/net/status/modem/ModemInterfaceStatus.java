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
 ******************************************************************************/
package org.eclipse.kura.net.status.modem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Class that contains specific properties to describe the status of a
 * Modem interface.
 *
 */
@ProviderType
public class ModemInterfaceStatus extends NetworkInterfaceStatus {

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
    private final Set<ModemModePair> supportedModes;
    private final ModemModePair currentModes;
    private final Set<ModemBand> supportedBands;
    private final Set<ModemBand> currentBands;
    private final boolean gpsSupported;
    private final List<Sim> availableSims;
    private final boolean simLocked;
    private final List<Bearer> bearers;
    private final ModemConnectionType connectionType;
    private final ModemConnectionStatus connectionStatus;
    private final Set<AccessTechnology> accessTechnologies;
    private final int signalQuality;
    private final int signalStrength;
    private final RegistrationStatus registrationStatus;
    private final String operatorName;

    private ModemInterfaceStatus(ModemInterfaceStatusBuilder builder) {
        super(builder);
        this.model = builder.model;
        this.manufacturer = builder.manufacturer;
        this.serialNumber = builder.serialNumber;
        this.softwareRevision = builder.softwareRevision;
        this.hardwareRevision = builder.hardwareRevision;
        this.primaryPort = builder.primaryPort;
        this.ports = builder.ports;
        this.supportedModemCapabilities = builder.supportedModemCapabilities;
        this.currentModemCapabilities = builder.currentModemCapabilities;
        this.powerState = builder.powerState;
        this.supportedModes = builder.supportedModes;
        this.currentModes = builder.currentModes;
        this.supportedBands = builder.supportedBands;
        this.currentBands = builder.currentBands;
        this.gpsSupported = builder.gpsSupported;
        this.availableSims = builder.availableSims;
        this.simLocked = builder.simLocked;
        this.bearers = builder.bearers;
        this.connectionType = builder.connectionType;
        this.connectionStatus = builder.connectionStatus;
        this.accessTechnologies = builder.accessTechnologies;
        this.signalQuality = builder.signalQuality;
        this.registrationStatus = builder.registrationStatus;
        this.operatorName = builder.operatorName;
        this.signalStrength = builder.signalStrength;
    }

    public String getModel() {
        return this.model;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public String getSerialNumber() {
        return this.serialNumber;
    }

    public String getSoftwareRevision() {
        return this.softwareRevision;
    }

    public String getHardwareRevision() {
        return this.hardwareRevision;
    }

    public String getPrimaryPort() {
        return this.primaryPort;
    }

    public Map<String, ModemPortType> getPorts() {
        return this.ports;
    }

    public Set<ModemCapability> getSupportedModemCapabilities() {
        return this.supportedModemCapabilities;
    }

    public Set<ModemCapability> getCurrentModemCapabilities() {
        return this.currentModemCapabilities;
    }

    public ModemPowerState getPowerState() {
        return this.powerState;
    }

    public Set<ModemModePair> getSupportedModes() {
        return this.supportedModes;
    }

    public ModemModePair getCurrentModes() {
        return this.currentModes;
    }

    public Set<ModemBand> getSupportedBands() {
        return this.supportedBands;
    }

    public Set<ModemBand> getCurrentBands() {
        return this.currentBands;
    }

    public Boolean isGpsSupported() {
        return this.gpsSupported;
    }

    public List<Sim> getAvailableSims() {
        return this.availableSims;
    }

    public boolean isSimLocked() {
        return this.simLocked;
    }

    public List<Bearer> getBearers() {
        return this.bearers;
    }

    public ModemConnectionType getConnectionType() {
        return this.connectionType;
    }

    public ModemConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    public Set<AccessTechnology> getAccessTechnologies() {
        return this.accessTechnologies;
    }

    public int getSignalQuality() {
        return this.signalQuality;
    }

    public RegistrationStatus getRegistrationStatus() {
        return this.registrationStatus;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public int getSignalStrength() {
        return this.signalStrength;
    }

    public static ModemInterfaceStatusBuilder builder() {
        return new ModemInterfaceStatusBuilder();
    }

    public static class ModemInterfaceStatusBuilder extends NetworkInterfaceStatusBuilder<ModemInterfaceStatusBuilder> {

        private static final String NA = "N/A";
        private String model = NA;
        private String manufacturer = NA;
        private String serialNumber = NA;
        private String softwareRevision = NA;
        private String hardwareRevision = NA;
        private String primaryPort = NA;
        private Map<String, ModemPortType> ports = Collections.emptyMap();
        private Set<ModemCapability> supportedModemCapabilities = EnumSet.of(ModemCapability.NONE);
        private Set<ModemCapability> currentModemCapabilities = EnumSet.of(ModemCapability.NONE);
        private ModemPowerState powerState = ModemPowerState.UNKNOWN;
        private Set<ModemModePair> supportedModes = Collections.emptySet();
        private ModemModePair currentModes = new ModemModePair(Collections.emptySet(), ModemMode.NONE);
        private Set<ModemBand> supportedBands = EnumSet.of(ModemBand.UNKNOWN);
        private Set<ModemBand> currentBands = EnumSet.of(ModemBand.UNKNOWN);
        private boolean gpsSupported = false;
        private List<Sim> availableSims = Collections.emptyList();
        private boolean simLocked = false;
        private List<Bearer> bearers = Collections.emptyList();
        private ModemConnectionType connectionType = ModemConnectionType.DirectIP;
        private ModemConnectionStatus connectionStatus = ModemConnectionStatus.UNKNOWN;
        private Set<AccessTechnology> accessTechnologies = EnumSet.of(AccessTechnology.UNKNOWN);
        private int signalQuality = 0;
        private int signalStrength = -113;
        private RegistrationStatus registrationStatus = RegistrationStatus.UNKNOWN;
        private String operatorName = NA;

        public ModemInterfaceStatusBuilder withModel(String model) {
            this.model = model;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSoftwareRevision(String softwareRevision) {
            this.softwareRevision = softwareRevision;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withHardwareRevision(String hardwareRevision) {
            this.hardwareRevision = hardwareRevision;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withPrimaryPort(String primaryPort) {
            this.primaryPort = primaryPort;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withPorts(Map<String, ModemPortType> ports) {
            this.ports = ports;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSupportedModemCapabilities(
                Set<ModemCapability> supportedModemCapabilities) {
            this.supportedModemCapabilities = supportedModemCapabilities;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withCurrentModemCapabilities(Set<ModemCapability> currentModemCapabilities) {
            this.currentModemCapabilities = currentModemCapabilities;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withPowerState(ModemPowerState powerState) {
            this.powerState = powerState;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSupportedModes(Set<ModemModePair> supportedModes) {
            this.supportedModes = supportedModes;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withCurrentModes(ModemModePair currentModes) {
            this.currentModes = currentModes;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSupportedBands(Set<ModemBand> supportedBands) {
            this.supportedBands = supportedBands;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withCurrentBands(Set<ModemBand> currentBands) {
            this.currentBands = currentBands;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withGpsSupported(Boolean gpsSupported) {
            this.gpsSupported = gpsSupported;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withAvailableSims(List<Sim> availableSims) {
            this.availableSims = availableSims;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSimLocked(boolean simLocked) {
            this.simLocked = simLocked;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withBearers(List<Bearer> bearers) {
            this.bearers = bearers;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withConnectionType(ModemConnectionType connectionType) {
            this.connectionType = connectionType;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withConnectionStatus(ModemConnectionStatus connectionStatus) {
            this.connectionStatus = connectionStatus;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withAccessTechnologies(Set<AccessTechnology> accessTechnologies) {
            this.accessTechnologies = accessTechnologies;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSignalQuality(int signalQuality) {
            this.signalQuality = signalQuality;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withSignalStrength(int signalStrength) {
            this.signalStrength = signalStrength;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withRegistrationStatus(RegistrationStatus registrationStatus) {
            this.registrationStatus = registrationStatus;
            return getThis();
        }

        public ModemInterfaceStatusBuilder withOperatorName(String operatorName) {
            this.operatorName = operatorName;
            return getThis();
        }

        @Override
        public ModemInterfaceStatus build() {
            withType(NetworkInterfaceType.MODEM);
            return new ModemInterfaceStatus(this);
        }

        @Override
        public ModemInterfaceStatusBuilder getThis() {
            return this;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.accessTechnologies, this.availableSims, this.bearers,
                this.connectionStatus, this.connectionType, this.currentBands, this.currentModemCapabilities,
                this.currentModes, this.gpsSupported, this.hardwareRevision, this.manufacturer, this.model,
                this.operatorName, this.ports, this.powerState, this.primaryPort, this.registrationStatus,
                this.signalStrength, this.serialNumber, this.signalQuality, this.simLocked, this.softwareRevision,
                this.supportedBands, this.supportedModemCapabilities, this.supportedModes);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || getClass() != obj.getClass()) {
            return false;
        }
        ModemInterfaceStatus other = (ModemInterfaceStatus) obj;
        return Objects.equals(this.accessTechnologies, other.accessTechnologies)
                && Objects.equals(this.availableSims, other.availableSims)
                && Objects.equals(this.bearers, other.bearers) && this.connectionStatus == other.connectionStatus
                && this.connectionType == other.connectionType && Objects.equals(this.currentBands, other.currentBands)
                && Objects.equals(this.currentModemCapabilities, other.currentModemCapabilities)
                && Objects.equals(this.currentModes, other.currentModes) && this.gpsSupported == other.gpsSupported
                && Objects.equals(this.hardwareRevision, other.hardwareRevision)
                && Objects.equals(this.manufacturer, other.manufacturer) && Objects.equals(this.model, other.model)
                && Objects.equals(this.operatorName, other.operatorName) && Objects.equals(this.ports, other.ports)
                && this.powerState == other.powerState && Objects.equals(this.primaryPort, other.primaryPort)
                && this.registrationStatus == other.registrationStatus && this.signalStrength == other.signalStrength
                && Objects.equals(this.serialNumber, other.serialNumber) && this.signalQuality == other.signalQuality
                && this.simLocked == other.simLocked && Objects.equals(this.softwareRevision, other.softwareRevision)
                && Objects.equals(this.supportedBands, other.supportedBands)
                && Objects.equals(this.supportedModemCapabilities, other.supportedModemCapabilities)
                && Objects.equals(this.supportedModes, other.supportedModes);
    }

}
