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
package org.eclipse.kura.linux.net.modem;

import java.util.ArrayList;
import java.util.List;

public enum SerialModemComm {
	
	MiniGateway("/dev/ttyO5", "/dev/ttyO5", null, 115200, 8, 1, 0);
	
	private String m_atPort;
	private String m_dataPort;
	private String m_gpsPort;
	private int m_baudRate;
	private int m_dataBits;
	private int m_stopBits;
	private int m_parity;
	
	private SerialModemComm(String atPort, String dataPort, String gpsPort,
			int baudRate, int dataBits, int stopBits, int parity) {
		m_atPort = atPort;
		m_dataPort = dataPort;
		m_gpsPort = gpsPort;
		m_baudRate = baudRate;
		m_dataBits = dataBits;
		m_stopBits = stopBits;
		m_parity = parity;
	}
	
	public String getAtPort() {
		return m_atPort;
	}
	
	public String getDataPort() {
		return m_dataPort;
	}
	
	public String getGpsPort() {
		return m_gpsPort;
	}
	
	public int getBaudRate() {
		return m_baudRate;
	}
	
	public int getDataBits() {
		return m_dataBits;
	}
	
	public int getStopBits() {
		return m_stopBits;
	}
	
	public int getParity() {
		return m_parity;
	}
	
	public List<String> getSerialPorts() {
		List<String> ports = new ArrayList<String>();
		ports.add(m_atPort);
		return ports;
	}

}
