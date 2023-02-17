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
package org.eclipse.kura.net.status.wifi;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;

public class WifiInterfaceStatus extends NetworkInterfaceStatus {

    private final Set<WifiCapability> capabilities;
    private final List<Long> supportedBitrates;
    private final Set<WifiRadioMode> supportedRadioModes;
    private final List<Integer> supportedChannels;
    private final List<Long> supportedFrequencies;
    private final String countryCode;
    private final WifiMode mode;
    private final Optional<WifiAccessPoint> activeWifiAccessPoint;
    private final List<WifiAccessPoint> availableWifiAccessPoints;

    private WifiInterfaceStatus(WifiInterfaceStatusBuilder builder) {
        super(builder);
        this.capabilities = builder.capabilities;
        this.supportedBitrates = builder.supportedBitrates;
        this.supportedRadioModes = builder.supportedRadioModes;
        this.supportedChannels = builder.supportedChannels;
        this.supportedFrequencies = builder.supportedFrequencies;
        this.countryCode = builder.countryCode;
        this.mode = builder.mode;
        this.activeWifiAccessPoint = builder.currentWifiAccessPoint;
        this.availableWifiAccessPoints = builder.availableWifiAccessPoints;
    }

    public Set<WifiCapability> getCapabilities() {
        return this.capabilities;
    }

    public List<Long> getSupportedBitrates() {
        return this.supportedBitrates;
    }

    public Set<WifiRadioMode> getSupportedRadioModes() {
        return this.supportedRadioModes;
    }

    public List<Integer> getSupportedChannels() {
        return this.supportedChannels;
    }

    public List<Long> getSupportedFrequencies() {
        return this.supportedFrequencies;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public WifiMode getMode() {
        return this.mode;
    }

    public Optional<WifiAccessPoint> getActiveWifiAccessPoint() {
        return this.activeWifiAccessPoint;
    }

    public List<WifiAccessPoint> getAvailableWifiAccessPoints() {
        return this.availableWifiAccessPoints;
    }

    public static WifiInterfaceStatusBuilder builder() {
        return new WifiInterfaceStatusBuilder();
    }

    public static class WifiInterfaceStatusBuilder extends NetworkInterfaceStatusBuilder<WifiInterfaceStatusBuilder> {

        private Set<WifiCapability> capabilities = EnumSet.of(WifiCapability.NONE);
        private List<Long> supportedBitrates = Arrays.asList(0L);
        private Set<WifiRadioMode> supportedRadioModes = EnumSet.of(WifiRadioMode.UNKNOWN);
        private List<Integer> supportedChannels = Arrays.asList(0);
        private List<Long> supportedFrequencies = Arrays.asList(0L);
        private String countryCode = "00";
        private WifiMode mode = WifiMode.UNKNOWN;
        private Optional<WifiAccessPoint> currentWifiAccessPoint = Optional.empty();
        private List<WifiAccessPoint> availableWifiAccessPoints = Collections.emptyList();

        public WifiInterfaceStatusBuilder withCapabilities(Set<WifiCapability> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public WifiInterfaceStatusBuilder withSupportedBitrates(List<Long> supportedBitrates) {
            this.supportedBitrates = supportedBitrates;
            return this;
        }

        public WifiInterfaceStatusBuilder withSupportedRadioModes(EnumSet<WifiRadioMode> supportedRadioModes) {
            this.supportedRadioModes = supportedRadioModes;
            return this;
        }

        public WifiInterfaceStatusBuilder withSupportedChannels(List<Integer> supportedChannels) {
            this.supportedChannels = supportedChannels;
            return this;
        }

        public WifiInterfaceStatusBuilder withSupportedFrequencies(List<Long> supportedFrequencies) {
            this.supportedFrequencies = supportedFrequencies;
            return this;
        }

        public WifiInterfaceStatusBuilder withCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public WifiInterfaceStatusBuilder withMode(WifiMode mode) {
            this.mode = mode;
            return this;
        }

        public WifiInterfaceStatusBuilder withActiveWifiAccessPoint(Optional<WifiAccessPoint> currentWifiAccessPoint) {
            this.currentWifiAccessPoint = currentWifiAccessPoint;
            return this;
        }

        public WifiInterfaceStatusBuilder withAvailableWifiAccessPoints(
                List<WifiAccessPoint> availableWifiAccessPoints) {
            this.availableWifiAccessPoints = availableWifiAccessPoints;
            return this;
        }

        @Override
        public WifiInterfaceStatus build() {
            this.withType(NetworkInterfaceType.WIFI);
            return new WifiInterfaceStatus(this);
        }

        @Override
        public WifiInterfaceStatusBuilder getThis() {
            return this;
        }
    }

}
