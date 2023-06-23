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
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import java.util.Set;

import org.eclipse.kura.net.status.modem.Bearer;
import org.eclipse.kura.net.status.modem.BearerIpType;

@SuppressWarnings("unused")
public class BearerDTO {

    private final String name;
    private final boolean connected;
    private final String apn;
    private final Set<BearerIpType> ipTypes;
    private final long bytesTransmitted;
    private final long bytesReceived;

    public BearerDTO(final Bearer bearer) {
        this.name = bearer.getName();
        this.connected = bearer.isConnected();
        this.apn = bearer.getApn();
        this.ipTypes = bearer.getIpTypes();
        this.bytesTransmitted = bearer.getBytesTransmitted();
        this.bytesReceived = bearer.getBytesReceived();
    }
}
