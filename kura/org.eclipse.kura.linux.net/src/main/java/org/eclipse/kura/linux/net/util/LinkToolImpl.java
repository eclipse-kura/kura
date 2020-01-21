/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
