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
    private final Set<WifiSecurity> wpaSecurity;
    private final Set<WifiSecurity> rsnSecurity;

    private WifiAccessPoint(WifiAccessPointBuilder builder) {
        this.ssid = builder.ssid;
        this.hardwareAddress = builder.hardwareAddress;
        this.channel = builder.channel;
        this.mode = builder.mode;
        this.maxBitrate = builder.maxBitrate;
        this.signalQuality = builder.signalQuality;
        this.wpaSecurity = builder.wpaSecurity;
        this.rsnSecurity = builder.rsnSecurity;
    }

    public String getSsid() {
        return this.ssid;
    }

    public byte[] getHardwareAddress() {
        return this.hardwareAddress;
    }

    public WifiChannel getChannel() {
        return this.channel;
    }

    public WifiMode getMode() {
        return this.mode;
    }

    public long getMaxBitrate() {
        return this.maxBitrate;
    }

    public int getSignalQuality() {
        return this.signalQuality;
    }

    public Set<WifiSecurity> getWpaSecurity() {
        return this.wpaSecurity;
    }

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
                this.signalQuality, this.ssid, this.wpaSecurity);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        WifiAccessPoint other = (WifiAccessPoint) obj;
        return Objects.equals(channel, other.channel) && Arrays.equals(this.hardwareAddress, other.hardwareAddress)
                && this.maxBitrate == other.maxBitrate && this.mode == other.mode
                && Objects.equals(this.rsnSecurity, other.rsnSecurity) && this.signalQuality == other.signalQuality
                && Objects.equals(this.ssid, other.ssid) && Objects.equals(this.wpaSecurity, other.wpaSecurity);
    }

}
