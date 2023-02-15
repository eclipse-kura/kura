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

public class Bearer {

    private final String name;
    private final boolean connected;
    private final String apn;
    private final BearerIpType ipType;
    private final long bytesTransmitted;
    private final long bytesReceived;

    public Bearer(String name, boolean connected, String apn, BearerIpType ipType, long bytesTransmitted,
            long bytesReceived) {
        super();
        this.name = name;
        this.connected = connected;
        this.apn = apn;
        this.ipType = ipType;
        this.bytesTransmitted = bytesTransmitted;
        this.bytesReceived = bytesReceived;
    }

    public String getName() {
        return name;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getApn() {
        return apn;
    }

    public BearerIpType getIpType() {
        return ipType;
    }

    public long getBytesTransmitted() {
        return bytesTransmitted;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

}
