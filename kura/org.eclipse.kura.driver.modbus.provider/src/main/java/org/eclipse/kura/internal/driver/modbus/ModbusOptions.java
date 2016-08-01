/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.modbus;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.ModbusDriverMessages;

import com.fazecast.jSerialComm.SerialPort;
import com.ghgande.j2mod.modbus.Modbus;

/**
 * The Class ModbusOptions is responsible to provide all the required
 * configurable options for the Modbus Driver
 */
final class ModbusOptions {

	/** Modbus Serial (RTU) access type Baudrate */
	private static final String BAUD_RATE = "modbus.rtu.baudrate";

	/** Modbus Serial (RTU) access type Databits */
	private static final String DATABITS = "modbus.rtu.databits";

	/** Modbus Serial (RTU) access type Encoding */
	private static final String ENCODING = "modbus.rtu.encoding";

	/** Modbus Serial (RTU) access type Flow Control In */
	private static final String FLOW_CONTROL_IN = "modbus.rtu.flowcontrolin";

	/** Modbus Serial (RTU) access type Flow Control Out */
	private static final String FLOW_CONTROL_OUT = "modbus.rtu.flowcontrolout";

	/** Modbus TCP or UDP access type configuration IP */
	private static final String IP = "modbus.tcp-udp.ip";

	/** Modbus Serial (RTU) access type Parity */
	private static final String PARITY = "modbus.rtu.parity";

	/** Modbus TCP or UDP access type configuration Port */
	private static final String PORT = "modbus.tcp-udp.port";

	/** Localization Resource. */
	private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

	/** Modbus Serial (RTU) access type Stopbits */
	private static final String STOPBITS = "modbus.rtu.stopbits";

	/** Modbus TCP or UDP or RTU access type */
	private static final String TYPE = "access.type";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new Modbus options.
	 *
	 * @param properties
	 *            the properties
	 */
	ModbusOptions(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.m_properties = properties;
	}

	/**
	 * Returns the Modbus RTU Baudrate
	 *
	 * @return the Modbus RTU Baudrate
	 */
	int getBaudrate() {
		int baudRate = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(BAUD_RATE)
				&& (this.m_properties.get(BAUD_RATE) != null) && (this.m_properties.get(BAUD_RATE) instanceof String)) {
			baudRate = Integer.valueOf(this.m_properties.get(BAUD_RATE).toString());
		}
		return baudRate;
	}

	/**
	 * Returns the Modbus RTU Databits
	 *
	 * @return the Modbus RTU Databits
	 */
	int getDatabits() {
		int databits = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(DATABITS)
				&& (this.m_properties.get(DATABITS) != null) && (this.m_properties.get(DATABITS) instanceof String)) {
			databits = Integer.valueOf(this.m_properties.get(DATABITS).toString());
		}
		return databits;
	}

	/**
	 * Returns the Modbus RTU Encoding
	 *
	 * @return the Modbus RTU Encoding
	 */
	String getEncoding() {
		String encoding = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(ENCODING)
				&& (this.m_properties.get(ENCODING) != null) && (this.m_properties.get(ENCODING) instanceof String)) {
			encoding = this.m_properties.get(ENCODING).toString();
		}
		if (encoding != null) {
			if ("SERIAL_ENCODING_ASCII".equals(encoding)) {
				return Modbus.SERIAL_ENCODING_ASCII;
			}
			if ("SERIAL_ENCODING_RTU".equals(encoding)) {
				return Modbus.SERIAL_ENCODING_RTU;
			}
			if ("SERIAL_ENCODING_BIN".equals(encoding)) {
				return Modbus.SERIAL_ENCODING_BIN;
			}
		}
		return null;
	}

	/**
	 * Returns the Modbus RTU Flow Control In
	 *
	 * @return the Modbus RTU Flow Control In
	 */
	int getFlowControlIn() {
		String flowControlIn = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(FLOW_CONTROL_IN)
				&& (this.m_properties.get(FLOW_CONTROL_IN) != null)
				&& (this.m_properties.get(FLOW_CONTROL_IN) instanceof String)) {
			flowControlIn = this.m_properties.get(FLOW_CONTROL_IN).toString();
		}
		if (flowControlIn != null) {
			if ("FLOW_CONTROL_DISABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_DISABLED;
			}
			if ("FLOW_CONTROL_RTS_ENABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_RTS_ENABLED;
			}
			if ("FLOW_CONTROL_CTS_ENABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_CTS_ENABLED;
			}
			if ("FLOW_CONTROL_DSR_ENABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_DSR_ENABLED;
			}
			if ("FLOW_CONTROL_DTR_ENABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_DTR_ENABLED;
			}
			if ("FLOW_CONTROL_XONXOFF_IN_ENABLED".equals(flowControlIn)) {
				return SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED;
			}
		}
		return 0;
	}

	/**
	 * Returns the Modbus RTU Flow Control Out
	 *
	 * @return the Modbus RTU Flow Control Out
	 */
	int getFlowControlOut() {
		String flowControlOut = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(FLOW_CONTROL_OUT)
				&& (this.m_properties.get(FLOW_CONTROL_OUT) != null)
				&& (this.m_properties.get(FLOW_CONTROL_OUT) instanceof String)) {
			flowControlOut = this.m_properties.get(FLOW_CONTROL_OUT).toString();
		}
		if (flowControlOut != null) {
			if ("FLOW_CONTROL_DISABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_DISABLED;
			}
			if ("FLOW_CONTROL_RTS_ENABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_RTS_ENABLED;
			}
			if ("FLOW_CONTROL_CTS_ENABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_CTS_ENABLED;
			}
			if ("FLOW_CONTROL_DSR_ENABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_DSR_ENABLED;
			}
			if ("FLOW_CONTROL_DTR_ENABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_DTR_ENABLED;
			}
			if ("FLOW_CONTROL_XONXOFF_OUT_ENABLED".equals(flowControlOut)) {
				return SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
			}
		}
		return 0;
	}

	/**
	 * Returns the Modbus TCP or UDP IP
	 *
	 * @return the Modbus TCP or UDP IP
	 */
	String getIp() {
		String ipAddress = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(IP) && (this.m_properties.get(IP) != null)
				&& (this.m_properties.get(IP) instanceof String)) {
			ipAddress = this.m_properties.get(IP).toString();
		}
		return ipAddress;
	}

	/**
	 * Returns the Modbus RTU Parity
	 *
	 * @return the Modbus RTU Parity
	 */
	int getParity() {
		String parity = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(PARITY)
				&& (this.m_properties.get(PARITY) != null) && (this.m_properties.get(PARITY) instanceof String)) {
			parity = this.m_properties.get(PARITY).toString();
		}
		if (parity != null) {
			if ("NO_PARITY".equals(parity)) {
				return SerialPort.NO_PARITY;
			}
			if ("ODD_PARITY".equals(parity)) {
				return SerialPort.ODD_PARITY;
			}
			if ("EVEN_PARITY".equals(parity)) {
				return SerialPort.EVEN_PARITY;
			}
			if ("MARK_PARITY".equals(parity)) {
				return SerialPort.MARK_PARITY;
			}
			if ("SPACE_PARITY".equals(parity)) {
				return SerialPort.SPACE_PARITY;
			}
		}
		return 0;
	}

	/**
	 * Returns Modbus TCP or UDP Port
	 *
	 * @return the Modbus TCP or UDP Port
	 */
	int getPort() {
		int port = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(PORT) && (this.m_properties.get(PORT) != null)
				&& (this.m_properties.get(PORT) instanceof String)) {
			port = Integer.valueOf(this.m_properties.get(PORT).toString());
		}
		return port;
	}

	/**
	 * Returns the Modbus RTU Stopbits
	 *
	 * @return the Modbus RTU Stopbits
	 */
	int getStopbits() {
		String stopbits = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(STOPBITS)
				&& (this.m_properties.get(STOPBITS) != null) && (this.m_properties.get(STOPBITS) instanceof String)) {
			stopbits = this.m_properties.get(STOPBITS).toString();
		}
		if (stopbits != null) {
			if ("ONE_STOP_BIT".equals(stopbits)) {
				return SerialPort.ONE_STOP_BIT;
			}
			if ("ONE_POINT_FIVE_STOP_BITS".equals(stopbits)) {
				return SerialPort.ONE_POINT_FIVE_STOP_BITS;
			}
			if ("TWO_STOP_BITS".equals(stopbits)) {
				return SerialPort.TWO_STOP_BITS;
			}
		}
		return 0;
	}

	/**
	 * Returns the type of the Modbus Access
	 *
	 * @return the Modbus Access Type
	 */
	ModbusType getType() {
		String messageType = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(TYPE) && (this.m_properties.get(TYPE) != null)
				&& (this.m_properties.get(TYPE) instanceof String)) {
			messageType = (String) this.m_properties.get(TYPE);
		}
		if (messageType != null) {
			if ("TCP".equals(messageType)) {
				return ModbusType.TCP;
			}
			if ("UDP".equals(messageType)) {
				return ModbusType.UDP;
			}
			if ("RTU".equals(messageType)) {
				return ModbusType.RTU;
			}
		}
		return null;
	}

}
