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

import java.util.Collections;
import java.util.Set;

public class WifiAccessPoint {

    private final String ssid;
    private final byte[] hardwareAddress;
    private final long frequency;
    private final int channel;
    private final WifiMode mode;
    private final long maxBitrate;
    private final int signalQuality;
    private final Set<WifiSecurity> wpaSecurity;
    private final Set<WifiSecurity> rsnSecurity;

    private WifiAccessPoint(WifiAccessPointBuilder builder) {
        this.ssid = builder.ssid;
        this.hardwareAddress = builder.hardwareAddress;
        this.frequency = builder.frequency;
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

    public long getFrequency() {
        return this.frequency;
    }

    public int getChannel() {
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
        private long frequency;
        private int channel;
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

        public WifiAccessPointBuilder withFrequency(long frequency) {
            this.frequency = frequency;
            return this;
        }

        public WifiAccessPointBuilder withChannel(int channel) {
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

}
