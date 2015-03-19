/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.net.modem;

import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;

public class ModemInterfaceAddressImpl extends NetInterfaceAddressImpl implements ModemInterfaceAddress 
{
	private int							m_signalStrength;
	private boolean						m_isRoaming;
	private ModemConnectionStatus		m_connectionStatus;
	private long						m_bytesTransmitted;
	private long						m_bytesReceived;
	private ModemConnectionType			m_connectionType;	
	
	public ModemInterfaceAddressImpl() {
		super();
		m_connectionType = ModemConnectionType.PPP; // FIXME - hardcoded
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
		return m_signalStrength;
	}
	
	public void setSignalStrength(int signalStrength) {
		this.m_signalStrength = signalStrength;
	}

	@Override
	public boolean isRoaming() {
		return m_isRoaming;
	}
	
	public void setIsRoaming(boolean isRoaming) {
		this.m_isRoaming = isRoaming;
	}

	@Override
	public ModemConnectionStatus getConnectionStatus() {
		return m_connectionStatus;
	}
	
	public void setConnectionStatus(ModemConnectionStatus connectionStatus) {
		this.m_connectionStatus = connectionStatus;
	}

	@Override
	public long getBytesTransmitted() {
		return m_bytesTransmitted;
	}
	
	public void setBytesTransmitted(long bytesTransmitted) {
		this.m_bytesTransmitted = bytesTransmitted;
	}

	@Override
	public long getBytesReceived() {
		return m_bytesReceived;
	}
	
	public void setBytesReceived(long bytesReceived) {
		this.m_bytesReceived = bytesReceived;
	}

	@Override
	public ModemConnectionType getConnectionType() {
		return m_connectionType;
	}
	
	public void setConnectionType(ModemConnectionType connectionType) {
		this.m_connectionType = connectionType;
	}

}
