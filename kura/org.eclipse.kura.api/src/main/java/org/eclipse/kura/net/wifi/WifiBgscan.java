/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

            this.shortInterval = Integer.parseInt(sa[1]);
            this.rssiThreshold = Integer.parseInt(sa[2]);
            this.longInterval = Integer.parseInt(sa[3]);
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
