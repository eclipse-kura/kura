/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.util;

public abstract class LinkToolImpl {

    private String interfaceName = null;
    private boolean linkDetected = false;
    private int speed = 0; // in b/s
    private String duplex = null;
    private int signal = 0;

    public String getIfaceName() {
        return this.interfaceName;
    }

    public void setIfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public boolean isLinkDetected() {
        return this.linkDetected;
    }

    public void setLinkDetected(boolean linkDetected) {
        this.linkDetected = linkDetected;
    }

    public int getSpeed() {
        return this.speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getDuplex() {
        return this.duplex;
    }

    public void setDuplex(String duplex) {
        this.duplex = duplex;
    }

    public int getSignal() {
        return this.signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

}
