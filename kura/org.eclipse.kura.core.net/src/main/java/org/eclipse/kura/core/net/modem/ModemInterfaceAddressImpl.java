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
package org.eclipse.kura.core.net.modem;

import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;

public class ModemInterfaceAddressImpl extends NetInterfaceAddressImpl implements ModemInterfaceAddress {

    private int signalStrength;
    private boolean isRoaming;
    private ModemConnectionStatus connectionStatus;
    private long bytesTransmitted;
    private long bytesReceived;
    private ModemConnectionType connectionType;

    public ModemInterfaceAddressImpl() {
        super();
        this.connectionType = ModemConnectionType.PPP; // FIXME - hardcoded
    }

    public ModemInterfaceAddressImpl(ModemInterfaceAddress other) {
        super(other);
        this.signalStrength = other.getSignalStrength();
        this.isRoaming = other.isRoaming();
        this.connectionStatus = other.getConnectionStatus();
        this.bytesTransmitted = other.getBytesTransmitted();
        this.bytesReceived = other.getBytesReceived();
        this.connectionType = other.getConnectionType();
    }

    @Override
    public int getSignalStrength() {
        return this.signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    @Override
    public boolean isRoaming() {
        return this.isRoaming;
    }

    public void setIsRoaming(boolean isRoaming) {
        this.isRoaming = isRoaming;
    }

    @Override
    public ModemConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    public void setConnectionStatus(ModemConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public long getBytesTransmitted() {
        return this.bytesTransmitted;
    }

    public void setBytesTransmitted(long bytesTransmitted) {
        this.bytesTransmitted = bytesTransmitted;
    }

    @Override
    public long getBytesReceived() {
        return this.bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    @Override
    public ModemConnectionType getConnectionType() {
        return this.connectionType;
    }

    public void setConnectionType(ModemConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.bytesReceived ^ this.bytesReceived >>> 32);
        result = prime * result + (int) (this.bytesTransmitted ^ this.bytesTransmitted >>> 32);
        result = prime * result + (this.connectionStatus == null ? 0 : this.connectionStatus.hashCode());
        result = prime * result + (this.connectionType == null ? 0 : this.connectionType.hashCode());
        result = prime * result + (this.isRoaming ? 1231 : 1237);
        result = prime * result + this.signalStrength;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModemInterfaceAddressImpl)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        ModemInterfaceAddressImpl other = (ModemInterfaceAddressImpl) obj;
        if (this.bytesReceived != other.bytesReceived) {
            return false;
        }
        if (this.bytesTransmitted != other.bytesTransmitted) {
            return false;
        }
        if (this.connectionStatus != other.connectionStatus) {
            return false;
        }
        if (this.connectionType != other.connectionType) {
            return false;
        }
        if (this.isRoaming != other.isRoaming) {
            return false;
        }
        if (this.signalStrength != other.signalStrength) {
            return false;
        }
        return true;
    }

}
