/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.wifi;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Background Scan container class
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class WifiBgscan {

    private static final int SCAN_LONG_INTERVAL_POSITION = 3;
    private static final int SCAN_RSSI_THRESHOLD_POSITION = 2;
    private static final int SCAN_SHORT_INTERVAL_POSITION = 1;
    private WifiBgscanModule module = null;
    private int shortInterval = 0;
    private int longInterval = 0;
    private int rssiThreshold = 0;

    public WifiBgscan(WifiBgscanModule module, int shortInterval, int rssiThreshold, int longInterval) {

        this.module = module;
        this.shortInterval = shortInterval;
        this.rssiThreshold = rssiThreshold;
        this.longInterval = longInterval;
    }

    public WifiBgscan(WifiBgscan bgscan) {

        this.module = bgscan.module;
        this.shortInterval = bgscan.shortInterval;
        this.rssiThreshold = bgscan.rssiThreshold;
        this.longInterval = bgscan.longInterval;
    }

    public WifiBgscan(String str) {

        if (str == null || str.length() == 0) {
            this.module = WifiBgscanModule.NONE;
        } else {
            String[] sa = str.split(":");
            if (sa[0].equals("simple")) {
                this.module = WifiBgscanModule.SIMPLE;
            } else if (sa[0].equals("learn")) {
                this.module = WifiBgscanModule.LEARN;
            }

            this.shortInterval = Integer.parseInt(sa[SCAN_SHORT_INTERVAL_POSITION]);
            this.rssiThreshold = Integer.parseInt(sa[SCAN_RSSI_THRESHOLD_POSITION]);
            this.longInterval = Integer.parseInt(sa[SCAN_LONG_INTERVAL_POSITION]);
        }
    }

    public WifiBgscanModule getModule() {
        return this.module;
    }

    public int getShortInterval() {
        return this.shortInterval;
    }

    public int getLongInterval() {
        return this.longInterval;
    }

    public int getRssiThreshold() {
        return this.rssiThreshold;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + longInterval;
        result = prime * result + ((module == null) ? 0 : module.hashCode());
        result = prime * result + rssiThreshold;
        result = prime * result + shortInterval;
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof WifiBgscan)) {
            return false;
        }

        WifiBgscan bgscan = (WifiBgscan) obj;

        if (this.module != bgscan.module) {
            return false;
        }

        if (this.rssiThreshold != bgscan.rssiThreshold) {
            return false;
        }

        if (this.shortInterval != bgscan.shortInterval) {
            return false;
        }

        if (this.longInterval != bgscan.longInterval) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();
        if (this.module == WifiBgscanModule.SIMPLE) {
            sb.append("simple:");
        } else if (this.module == WifiBgscanModule.LEARN) {
            sb.append("learn:");
        } else {
            sb.append("");
            return sb.toString();
        }

        sb.append(this.shortInterval);
        sb.append(':');
        sb.append(this.rssiThreshold);
        sb.append(':');
        sb.append(this.longInterval);

        return sb.toString();
    }
}
