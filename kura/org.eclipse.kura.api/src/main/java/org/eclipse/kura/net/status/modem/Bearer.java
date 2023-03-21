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

import java.util.Objects;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class describes the Bearer or Context associated to a modem connection.
 *
 */
@ProviderType
public class Bearer {

    private final String name;
    private final boolean connected;
    private final String apn;
    private final Set<BearerIpType> ipTypes;
    private final long bytesTransmitted;
    private final long bytesReceived;

    public Bearer(String name, boolean connected, String apn, Set<BearerIpType> ipTypes, long bytesTransmitted,
            long bytesReceived) {
        super();
        this.name = name;
        this.connected = connected;
        this.apn = apn;
        this.ipTypes = ipTypes;
        this.bytesTransmitted = bytesTransmitted;
        this.bytesReceived = bytesReceived;
    }

    public String getName() {
        return this.name;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public String getApn() {
        return this.apn;
    }

    public Set<BearerIpType> getIpTypes() {
        return this.ipTypes;
    }

    public long getBytesTransmitted() {
        return this.bytesTransmitted;
    }

    public long getBytesReceived() {
        return this.bytesReceived;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apn, this.bytesReceived, this.bytesTransmitted, this.connected, this.ipTypes,
                this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Bearer other = (Bearer) obj;
        return Objects.equals(this.apn, other.apn) && this.bytesReceived == other.bytesReceived
                && this.bytesTransmitted == other.bytesTransmitted && this.connected == other.connected
                && Objects.equals(this.ipTypes, other.ipTypes) && Objects.equals(this.name, other.name);
    }

}
