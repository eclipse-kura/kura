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
import java.util.Objects;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class describes a Wifi Access Point.
 * It can be used both for describing a detected AP after a WiFi scan when in
 * Station mode and the provided AP when in Master (or Access Point) mode.
 *
 */
@ProviderType
public class WifiAccessPoint {

    private final String ssid;
    private final byte[] hardwareAddress;
    private final WifiChannel channel;
    private final WifiMode mode;
    private final long maxBitrate;
    private final int signalQuality;
    private final int signalStrength;
    private final Set<WifiSecurity> wpaSecurity;
    private final Set<WifiSecurity> rsnSecurity;

    private WifiAccessPoint(WifiAccessPointBuilder builder) {
        this.ssid = builder.ssid;
        this.hardwareAddress = builder.hardwareAddress;
        this.channel = builder.channel;
        this.mode = builder.mode;
        this.maxBitrate = builder.maxBitrate;
        this.signalQuality = builder.signalQuality;
        this.signalStrength = builder.signalStrength;
        this.wpaSecurity = builder.wpaSecurity;
        this.rsnSecurity = builder.rsnSecurity;
    }

    /**
     * Return the Service Set IDentifier of the WiFi network.
     * 
     * @return a string representing the ssid
     */
    public String getSsid() {
        return this.ssid;
    }

    /**
     * Return the Basic Service Set IDentifier of the WiFi access point.
     * 
     * @return a string representing the the bssid
     */
    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    /**
     * Return the {@link WifiChannel} used by the WiFi access point.
     * 
     * @return a {@link WifiChannel} object
     */
    public WifiChannel getChannel() {
        return this.channel;
    }

    /**
     * Return the {@link WifiMode} of the wireless interface.
     * 
     * @return a {@link WifiMode} entry
     */
    public WifiMode getMode() {
        return this.mode;
    }

    /**
     * Return the maximum bitrate this access point is capable of.
     * 
     * @return a long value representing the bitrate
     */
    public long getMaxBitrate() {
        return this.maxBitrate;
    }

    /**
     * Return the current signal quality of the access point in percentage.
     * 
     * @return an integer value between 0 and 100
     */
    public int getSignalQuality() {
        return this.signalQuality;
    }

    /**
     * Return the current signal strength of the access point in dBm.
     * 
     * @return an integer value representing the rssi
     */
    public int getSignalStrength() {
        return this.signalStrength;
    }

    /**
     * Return the WPA capabilities of the access point.
     * 
     * @return a set of {@link WifiSecurity} representing the capabilities
     */
    public Set<WifiSecurity> getWpaSecurity() {
        return this.wpaSecurity;
    }

    /**
     * Return the RSN capabilities of the access point.
     * 
     * @return a set of {@link WifiSecurity} representing the capabilities
     */
    public Set<WifiSecurity> getRsnSecurity() {
        return this.rsnSecurity;
    }

    public static WifiAccessPointBuilder builder() {
        return new WifiAccessPointBuilder();
    }

    public static final class WifiAccessPointBuilder {

        private String ssid;
        private byte[] hardwareAddress;
        private WifiChannel channel;
        private WifiMode mode;
        private long maxBitrate;
        private int signalQuality;
        private int signalStrength;
        private Set<WifiSecurity> wpaSecurity = Collections.emptySet();
        private Set<WifiSecurity> rsnSecurity = Collections.emptySet();

        private WifiAccessPointBuilder() {
        }

        public WifiAccessPointBuilder withSsid(String ssid) {
            this.ssid = ssid;
            return this;
        }

        public WifiAccessPointBuilder withHardwareAddress(byte[] hardwareAddress) {
            this.hardwareAddress = hardwareAddress;
            return this;
        }

        public WifiAccessPointBuilder withChannel(WifiChannel channel) {
            this.channel = channel;
            return this;
        }

        public WifiAccessPointBuilder withMode(WifiMode mode) {
            this.mode = mode;
            return this;
        }

        public WifiAccessPointBuilder withMaxBitrate(long maxBitrate) {
            this.maxBitrate = maxBitrate;
            return this;
        }

        public WifiAccessPointBuilder withSignalQuality(int signalQuality) {
            this.signalQuality = signalQuality;
            return this;
        }

        public WifiAccessPointBuilder withSignalStrength(int signalStrength) {
            this.signalStrength = signalStrength;
            return this;
        }

        public WifiAccessPointBuilder withWpaSecurity(Set<WifiSecurity> wpaSecurity) {
            this.wpaSecurity = wpaSecurity;
            return this;
        }

        public WifiAccessPointBuilder withRsnSecurity(Set<WifiSecurity> rsnSecurity) {
            this.rsnSecurity = rsnSecurity;
            return this;
        }

        public WifiAccessPoint build() {
            return new WifiAccessPoint(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.hardwareAddress);
        result = prime * result + Objects.hash(this.channel, this.maxBitrate, this.mode, this.rsnSecurity,
                this.signalQuality, this.signalStrength, this.ssid, this.wpaSecurity);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        WifiAccessPoint other = (WifiAccessPoint) obj;
        return Objects.equals(this.channel, other.channel) && Arrays.equals(this.hardwareAddress, other.hardwareAddress)
                && this.maxBitrate == other.maxBitrate && this.mode == other.mode
                && Objects.equals(this.rsnSecurity, other.rsnSecurity) && this.signalQuality == other.signalQuality
                && Objects.equals(this.ssid, other.ssid) && Objects.equals(this.signalStrength, other.signalStrength)
                && Objects.equals(this.wpaSecurity, other.wpaSecurity);
    }

}
