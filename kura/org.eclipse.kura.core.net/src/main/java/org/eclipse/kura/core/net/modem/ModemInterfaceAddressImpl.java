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
package org.eclipse.kura.core.net.modem;

import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;

public class ModemInterfaceAddressImpl extends NetInterfaceAddressImpl implements ModemInterfaceAddress {

    private int m_signalStrength;
    private boolean m_isRoaming;
    private ModemConnectionStatus m_connectionStatus;
    private long m_bytesTransmitted;
    private long m_bytesReceived;
    private ModemConnectionType m_connectionType;

    public ModemInterfaceAddressImpl() {
        super();
        this.m_connectionType = ModemConnectionType.PPP; // FIXME - hardcoded
    }

    public ModemInterfaceAddressImpl(ModemInterfaceAddress other) {
        super(other);
        this.m_signalStrength = other.getSignalStrength();
        this.m_isRoaming = other.isRoaming();
        this.m_connectionStatus = other.getConnectionStatus();
        this.m_bytesTransmitted = other.getBytesTransmitted();
        this.m_bytesReceived = other.getBytesReceived();
        this.m_connectionType = other.getConnectionType();
    }

    @Override
    public int getSignalStrength() {
        return this.m_signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.m_signalStrength = signalStrength;
    }

    @Override
    public boolean isRoaming() {
        return this.m_isRoaming;
    }

    public void setIsRoaming(boolean isRoaming) {
        this.m_isRoaming = isRoaming;
    }

    @Override
    public ModemConnectionStatus getConnectionStatus() {
        return this.m_connectionStatus;
    }

    public void setConnectionStatus(ModemConnectionStatus connectionStatus) {
        this.m_connectionStatus = connectionStatus;
    }

    @Override
    public long getBytesTransmitted() {
        return this.m_bytesTransmitted;
    }

    public void setBytesTransmitted(long bytesTransmitted) {
        this.m_bytesTransmitted = bytesTransmitted;
    }

    @Override
    public long getBytesReceived() {
        return this.m_bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.m_bytesReceived = bytesReceived;
    }

    @Override
    public ModemConnectionType getConnectionType() {
        return this.m_connectionType;
    }

    public void setConnectionType(ModemConnectionType connectionType) {
        this.m_connectionType = connectionType;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (m_bytesReceived ^ (m_bytesReceived >>> 32));
		result = prime * result + (int) (m_bytesTransmitted ^ (m_bytesTransmitted >>> 32));
		result = prime * result + ((m_connectionStatus == null) ? 0 : m_connectionStatus.hashCode());
		result = prime * result + ((m_connectionType == null) ? 0 : m_connectionType.hashCode());
		result = prime * result + (m_isRoaming ? 1231 : 1237);
		result = prime * result + m_signalStrength;
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
		if (m_bytesReceived != other.m_bytesReceived) {
			return false;
		}
		if (m_bytesTransmitted != other.m_bytesTransmitted) {
			return false;
		}
		if (m_connectionStatus != other.m_connectionStatus) {
			return false;
		}
		if (m_connectionType != other.m_connectionType) {
			return false;
		}
		if (m_isRoaming != other.m_isRoaming) {
			return false;
		}
		if (m_signalStrength != other.m_signalStrength) {
			return false;
		}
		return true;
	}

}
