/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.util.ArrayList;
import java.util.List;

public enum SerialModemComm {

    MiniGateway("/dev/ttyO5", "/dev/ttyO5", null, 115200, 8, 1, 0),
    Reliagate_10_11("/dev/ttyACM0", "/dev/ttyACM0", null, 115200, 8, 1, 0);

    private String atPort;
    private String dataPort;
    private String gpsPort;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;

    private SerialModemComm(String atPort, String dataPort, String gpsPort, int baudRate, int dataBits, int stopBits,
            int parity) {
        this.atPort = atPort;
        this.dataPort = dataPort;
        this.gpsPort = gpsPort;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public String getAtPort() {
        return this.atPort;
    }

    public String getDataPort() {
        return this.dataPort;
    }

    public String getGpsPort() {
        return this.gpsPort;
    }

    public int getBaudRate() {
        return this.baudRate;
    }

    public int getDataBits() {
        return this.dataBits;
    }

    public int getStopBits() {
        return this.stopBits;
    }

    public int getParity() {
        return this.parity;
    }

    public List<String> getSerialPorts() {
        List<String> ports = new ArrayList<>();
        ports.add(this.atPort);
        return ports;
    }
}
