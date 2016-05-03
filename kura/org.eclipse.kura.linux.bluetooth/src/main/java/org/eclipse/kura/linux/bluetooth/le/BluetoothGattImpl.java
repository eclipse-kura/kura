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
package org.eclipse.kura.linux.bluetooth.le;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.bluetooth.BluetoothLeNotificationListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothGattImpl implements BluetoothGatt, BluetoothProcessListener {

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothGattImpl.class);

	private final long GATT_CONNECTION_TIMEOUT = 10000;
	private final long GATT_SERVICE_TIMEOUT    = 6000;
	private final long GATT_COMMAND_TIMEOUT    = 2000;

	private final String REGEX_NOT_CONNECTED   = "\\[\\s{3}\\].*>$";
	private final String REGEX_CONNECTED       = ".*\\[CON\\].*>$";
//	private final String REGEX_SERVICES        = "attr.handle\\:.*[0-9|a-f|A-F]{8}(-[0-9|a-f|A-F]{4}){3}-[0-9|a-f|A-F]{12}$";
//	private final String REGEX_CHARACTERISTICS = "handle.*properties.*value\\shandle.*uuid\\:\\s[0-9|a-f|A-F]{8}(-[0-9|a-f|A-F]{4}){3}-[0-9|a-f|A-F]{12}$";
	private final String REGEX_READ_CHAR       = "Characteristic\\svalue/descriptor\\:[\\s|0-9|a-f|A-F]*";
	private final String REGEX_READ_CHAR_UUID  = "handle\\:.*value\\:[\\s|0-9|a-f|A-F]*";
	private final String REGEX_NOTIFICATION    = ".*Notification\\shandle.*value\\:.*[\\n\\r]*";
	private final String REGEX_ERROR_HANDLE    = "Invalid\\shandle";
	private final String REGEX_ERROR_UUID      = "Invalid\\sUUID";

	private List<BluetoothGattService> m_bluetoothServices;
	private List<BluetoothGattCharacteristic> m_bluetoothGattCharacteristics;
	private BluetoothLeNotificationListener m_listener;
	private String m_charValue;
	private String m_charValueUuid;

	private BluetoothProcess m_proc;
	private BufferedWriter   m_bufferedWriter;
	private boolean          m_connected = false;
	private boolean          m_ready = false;
	private StringBuilder    m_stringBuilder = null;
	private String           m_address;

	public BluetoothGattImpl(String address) {
		m_address = address;
	}

	// --------------------------------------------------------------------
	//
	//  BluetoothGatt API
	//
	// --------------------------------------------------------------------
	@Override
	public boolean connect() throws KuraException {
		m_proc = BluetoothUtil.startSession(m_address, this);
		if (m_proc != null) {
			m_bufferedWriter = m_proc.getWriter();
			s_logger.info("Sending connect message...");
			m_ready = false;
			String command = "connect\n";
			sendCmd(command);

			// Wait for connection or timeout
			long startTime = System.currentTimeMillis();
			while (!m_ready && (System.currentTimeMillis() - startTime) < GATT_CONNECTION_TIMEOUT) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!m_ready) {
				throw new KuraTimeoutException("Gatttool connection timeout.");
			}
		}

		return m_connected;
	}

	@Override
	public void disconnect() {
		if (m_proc != null) {
			String command = "exit\n";
			sendCmd(command);
			m_proc.destroy();
			m_proc = null;
			s_logger.info("Disconnected");
		}
	}
	
	@Override
	public boolean checkConnection() throws KuraException {
		if (m_proc != null) {
			m_bufferedWriter = m_proc.getWriter();
			s_logger.info("Check for connection...");
			m_ready = false;
			String command = "\n";
			sendCmd(command);

			// Wait for connection or timeout
			long startTime = System.currentTimeMillis();
			while (!m_ready && (System.currentTimeMillis() - startTime) < GATT_CONNECTION_TIMEOUT) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!m_ready) {
				throw new KuraTimeoutException("Gatttool connection timeout.");
			}
		}

		return m_connected;
	}

	@Override
	public void setBluetoothLeNotificationListener(BluetoothLeNotificationListener listener) {
		m_listener = listener;
	}

	@Override
	public BluetoothGattService getService(UUID uuid) {
		return null;
	}

	@Override
	public List<BluetoothGattService> getServices() {
		if(m_proc != null) {
			m_bluetoothServices = new ArrayList<BluetoothGattService>();
			String command = "primary\n";
			sendCmd(command);
			try {
				Thread.sleep(GATT_SERVICE_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return m_bluetoothServices;
	}

	@Override
	public List<BluetoothGattCharacteristic> getCharacteristics(String startHandle, String endHandle) {
		s_logger.info("getCharacteristics "+startHandle+":"+endHandle);
		if(m_proc != null) {
			m_bluetoothGattCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
			String command = "characteristics " + startHandle + " " + endHandle + "\n";
			sendCmd(command);
			try {
				Thread.sleep(GATT_SERVICE_TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return m_bluetoothGattCharacteristics;
	}

	@Override
	public String readCharacteristicValue(String handle) throws KuraException {
		if(m_proc != null) {
			m_charValue = "";
			String command = "char-read-hnd " + handle + "\n";
			sendCmd(command);
			
			// Wait until read is complete, error is received or timeout
			long startTime = System.currentTimeMillis();
			while (m_charValue == "" && !m_charValue.startsWith("ERROR") && (System.currentTimeMillis() - startTime) < GATT_COMMAND_TIMEOUT) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (m_charValue == "") {
				throw new KuraTimeoutException("Gatttool read timeout."); 
			}
			if (m_charValue.startsWith("ERROR")) {
				throw KuraException.internalError("Gatttool read error.");
			}
				
		}
		
		return m_charValue;
	}

	@Override
	public String readCharacteristicValueByUuid(UUID uuid)  throws KuraException {
		if(m_proc != null) {
			m_charValueUuid = "";
			String l_uuid = uuid.toString();
			String command = "char-read-uuid " + l_uuid + "\n";
			s_logger.info("send command : "+command);
			sendCmd(command);
			
			// Wait until read is complete, error is received or timeout
			long startTime = System.currentTimeMillis();
			while (m_charValueUuid == "" && !m_charValueUuid.startsWith("ERROR") && (System.currentTimeMillis() - startTime) < GATT_COMMAND_TIMEOUT) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (m_charValue == "") {
				throw new KuraTimeoutException("Gatttool read timeout."); 
			}
			if (m_charValue.startsWith("ERROR")) {
				throw KuraException.internalError("Gatttool read error.");
			}
		}


		return m_charValueUuid;
	}

	@Override
	public void writeCharacteristicValue(String handle, String value) {
		if(m_proc != null) {
			m_charValueUuid = null;
//			String command = "char-write-req " + handle + " " + value + "\n";
			String command = "char-write-cmd " + handle + " " + value + "\n";
			sendCmd(command);
		}
	}

	@Override
	public void processInputStream(int ch) {
		if (m_stringBuilder == null) {
			m_stringBuilder = new StringBuilder();
		}

		// Process stream once newline, carriage return, or > char is received.
		// '>' indicates the gatttool prompt has returned.
		if (ch == 0xA || ch == 0xD || ch == 0x1B || (char) ch == '>') {
			m_stringBuilder.append((char) ch);
			processLine(m_stringBuilder.toString());
			m_stringBuilder.setLength(0);
		}
		else {
			m_stringBuilder.append((char) ch);
		}
	}

	@Override
	public void processInputStream(String string) {		
	}

	@Override
	public void processErrorStream(String string) {		
	}

	// --------------------------------------------------------------------
	//
	//  Private methods
	//
	// --------------------------------------------------------------------

	private void sendCmd(String command) {
		try {
			s_logger.debug("send command = {}", command);
			m_bufferedWriter.write(command);
			m_bufferedWriter.flush();
		} catch (IOException e) {
			s_logger.error("Error writing command: " + command, e);
		}
	}

	private void processLine(String line) {
		
		s_logger.debug("Processing line : "+line);
			
		// gatttool prompt indicates not connected, but session started
		if (line.matches(REGEX_NOT_CONNECTED)) {
			m_connected = false;
			m_ready = true;
		}
		// gatttool prompt indicates connected
		else if (line.matches(REGEX_CONNECTED)) {
			m_ready = true;
			m_connected = true;
		}
		// characteristic read by UUID returned
		else if (line.matches(REGEX_READ_CHAR_UUID)) {
			s_logger.debug("Characteristic value by UUID received: {}", line);
			// Parse the characteristic line, line is expected to be:
			// handle: 0xmmmm value: <value>
			String[] attr = line.split(":");
			m_charValueUuid = attr[2].trim();
			s_logger.info("m_charValueUuid: " + m_charValueUuid);
		}
		// services are being returned
		else if (line.startsWith("attr handle:")){
			s_logger.debug("Service : {}", line);
			// Parse the services line, line is expected to be:
			// attr handle: 0xnnnn, end grp handle: 0xmmmm uuid: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
			String[] attr = line.split("\\s");
			String startHandle = attr[2].substring(0,  attr[2].length() - 1);
			String endHandle = attr[6];
			String uuid = attr[8];

			if (m_bluetoothServices != null) {
				s_logger.debug("Adding new GATT service: " + uuid + ":" + startHandle + ":" + endHandle);
				m_bluetoothServices.add(new BluetoothGattServiceImpl(uuid, startHandle, endHandle));
			}
		}
		// characteristics are being returned
		else if (line.startsWith("handle:")){
			s_logger.debug("Characteristic : {}", line);
			// Parse the characteristic line, line is expected to be:
			// handle: 0xnnnn, char properties: 0xmm, char value handle: 0xpppp, uuid: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
			String[] attr = line.split(" ");
			String handle = attr[1].substring(0,  attr[1].length() - 1);
			String properties = attr[4].substring(0,  attr[4].length() - 1);
			String valueHandle = attr[8].substring(0,  attr[8].length() - 1);
			String uuid = attr[10].substring(0,  attr[10].length() - 1);
			if (m_bluetoothGattCharacteristics != null) {
				s_logger.debug("Adding new GATT characteristic: {}", uuid);
				s_logger.debug(handle+"  "+properties+"  "+valueHandle);
				m_bluetoothGattCharacteristics.add(new BluetoothGattCharacteristicImpl(uuid, handle, properties, valueHandle));
			}
		}
		// characteristic read by handle returned
		else if (line.matches(REGEX_READ_CHAR)) {
			s_logger.debug("Characteristic value by handle received: {}", line);
			// Parse the characteristic line, line is expected to be:
			// Characteristic value/descriptor: <value>
			String[] attr = line.split(":");
			m_charValue = attr[1].trim();

		}
		// receiving notifications, need to notify listener
		else if (line.matches(REGEX_NOTIFICATION)) {
			s_logger.debug("Receiving notification: " + line);
			// Parse the characteristic line, line is expected to be:
			// Notification handle = 0xmmmm value: <value>
			String x = "Notification hanlde = ";
			String sub = line.substring(x.length()).trim();
			String[] attr = sub.split(":");
			String handle = attr[0].split("\\s")[0];
			String value = attr[1].trim();
			m_listener.onDataReceived(handle, value);
		}
		// error reading handle
		else if (line.matches(REGEX_ERROR_HANDLE)) {
			s_logger.info("REGEX_ERROR_HANDLE");
			m_charValue = "ERROR: Invalid handle!";
		}
		// error reading UUID
		else if (line.matches(REGEX_ERROR_UUID)) {
			s_logger.info("REGEX_ERROR_UUID");
			m_charValueUuid = "ERROR: Invalid UUID!";
		}

	}
}
