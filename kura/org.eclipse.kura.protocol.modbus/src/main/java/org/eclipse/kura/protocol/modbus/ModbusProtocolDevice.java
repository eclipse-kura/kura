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
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Modbus protocol implements a subset of the Modbus standard command set.
 * It also provides for the extension of some data typing to allow register
 * pairings to hold 32 bit data (see the configureDataMap for more detail).
 * <p>
 * The protocol supports RTU and ASCII mode operation.
 * 
 */
public class ModbusProtocolDevice implements ModbusProtocolDeviceService {
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolDevice.class);

	private static final String GPIO_BASE_PATH = "/sys/class/gpio/";
	private static final String GPIO_EXPORT_PATH = "export";

	private ConnectionFactory 	   m_connectionFactory;
	private UsbService			   m_usbService;

	static final String PROTOCOL_NAME = "modbus";
	public static final String PROTOCOL_CONNECTION_TYPE_SERIAL = "SERIAL";
	public static final String SERIAL_232 = "RS232";
	public static final String SERIAL_485 = "RS485";
	public static final String PROTOCOL_CONNECTION_TYPE_ETHER_TCP = "ETHERTCP";
	private int 				m_respTout;
	private int 				m_txMode;
	private boolean				m_serial485 = false;
	private boolean 			m_connConfigd = false;
	private boolean 			m_protConfigd = false;
	private String 				m_connType = null;
	private Communicate 		m_comm;
	private Properties 	m_modbusProperties=null;

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = connectionFactory;
	}

	public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
		this.m_connectionFactory = null;
	}

	public void setUsbService(UsbService usbService) {
		this.m_usbService = usbService;
	}

	public void unsetUsbService(UsbService usbService) {
		this.m_usbService = null;
	}

	private boolean serialPortExists(){
		if(m_modbusProperties==null)
			return false;

		String portName = m_modbusProperties.getProperty("port");
		if(portName != null) {
			if(portName.contains("/dev/")) {
				File f = new File(portName);
				if(f.exists()) {
					return true;
				}
			} else {
				List<UsbTtyDevice> utd=m_usbService.getUsbTtyDevices();	
				if(utd!=null){
					for (UsbTtyDevice u : utd) {
						if(portName.equals(u.getUsbPort())) {
							// replace device number with tty 
							portName = u.getDeviceNode();
							m_modbusProperties.setProperty("port", portName);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------	
	protected void activate(ComponentContext componentContext) 
	{
		s_logger.info("activate...");
	}

	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");
		try {
			disconnect();
		} catch (ModbusProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * two connection types are available, PROTOCOL_CONNECTION_TYPE_SERIAL (SERIAL MODE 232 or 485)
	 * and
	 * PROTOCOL_CONNECTION_TYPE_ETHER_TCP. The required properties for the
	 * connection will vary with each connection.
	 * <p>
	 * <h4>PROTOCOL_CONNECTION_TYPE_SERIAL</h4>
	 * see {@link org.eclipse.kura.comm.CommConnection CommConnection} package for more
	 * detail.
	 * <table border="1">
	 * <tr>
	 * <th>Key</th>
	 * <th>Description</th>
	 * </tr>
	 * <tr>
	 * <td>connectionType</td>
	 * <td>"SERIAL" (from PROTOCOL_CONNECTION_TYPE_SERIAL). This parameter
	 * indicates the connection type for the configuration. See
	 * {@link org.eclipse.kura.comm.CommConnection CommConnection} for more details on serial
	 * port configuration.
	 * </tr>
	 * <tr>
	 * <td>port</td>
	 * <td>the actual device port, such as "/dev/ttyUSB0" in linux</td>
	 * </tr>
	 * <tr>
	 * <td>serialMode</td>
	 * <td>SERIAL_232 or SERIAL_485</td>
	 * </tr>
	 * <tr>
	 * <td>baudRate</td>
	 * <td>baud rate to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>stopBits</td>
	 * <td>number of stop bits to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>parity</td>
	 * <td>parity mode to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>bitsPerWord</td>
	 * <td>only RTU mode supported, bitsPerWord must be 8</td>
	 * </tr>
	 * </table>
	 * <p>
	 * <h4>PROTOCOL_CONNECTION_TYPE_ETHER_TCP</h4>
	 * The Ethernet mode merely opens a socket and sends the full RTU mode
	 * Modbus packet over that socket connection and expects to receive a full
	 * RTU mode Modbus response, including the CRC bytes.
	 * <table border="1">
	 * <tr>
	 * <th>Key</th>
	 * <th>Description</th>
	 * </tr>
	 * <tr>
	 * <td>connectionType</td>
	 * <td>"ETHERTCP" (from PROTOCOL_CONNECTION_TYPE_ETHER_TCP). This parameter
	 * indicates the connection type for the configurator.
	 * </tr>
	 * <tr>
	 * <td>ipAddress</td>
	 * <td>the 4 octet IP address of the field device (xxx.xxx.xxx.xxx)</td>
	 * </tr>
	 * <tr>
	 * <td>port</td>
	 * <td>port on the field device to connect to</td>
	 * </tr>
	 * </table>
	 */
	public void configureConnection(Properties connectionConfig)
			throws ModbusProtocolException {
		if ((m_connType = connectionConfig.getProperty("connectionType")) == null)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

		m_modbusProperties=connectionConfig;

		String txMode;
		String respTimeout;
		if ((m_protConfigd)
				|| ((txMode = connectionConfig.getProperty("transmissionMode")) == null)
				|| ((respTimeout = connectionConfig.getProperty("respTimeout")) == null))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
		if(txMode.equals(ModbusTransmissionMode.RTU)) m_txMode = ModbusTransmissionMode.RTU_MODE;
		else if(txMode.equals(ModbusTransmissionMode.ASCII)) m_txMode = ModbusTransmissionMode.ASCII_MODE;
		else throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION); 
		m_respTout = Integer.parseInt(respTimeout);
		if (m_respTout < 0)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
		m_protConfigd = true;

		if (m_connConfigd) {
			m_comm.disconnect();
			m_comm = null;
			m_connConfigd = false;
		}

		if (m_connType.equals(PROTOCOL_CONNECTION_TYPE_SERIAL)){
			if(!serialPortExists())
				throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_AVAILABLE);
			m_comm = new SerialCommunicate(m_connectionFactory, connectionConfig);
		}
		else if (m_connType.equals(PROTOCOL_CONNECTION_TYPE_ETHER_TCP))
			m_comm = new EthernetCommunicate(m_connectionFactory, connectionConfig);
		else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

		m_connConfigd = true;
	}

	/**
	 * get the name "modbus" for this protocol
	 * 
	 * @return "modbus"
	 */
	public String getProtocolName() {
		return "modbus";
	}

	public void connect() throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
		m_comm.connect();
	}

	public void disconnect() throws ModbusProtocolException {
		if (m_connConfigd){
			m_comm.disconnect();
			m_comm = null;
			m_connConfigd = false;
			s_logger.info("Serial comm disconnected");
		}
		m_protConfigd=false;
	}

	public int getConnectStatus() {
		if (!m_connConfigd)
			return KuraConnectionStatus.NEVERCONNECTED;
		return m_comm.getConnectStatus();
	}


	/**
	 * The only constructor must be the configuration mechanism
	 */
	abstract private class Communicate {
		abstract public void connect();

		abstract public void disconnect() throws ModbusProtocolException;

		abstract public int getConnectStatus();

		abstract public byte[] msgTransaction(byte[] msg)
				throws ModbusProtocolException;
	}

	/**
	 * Installation of a serial connection to communicate, using javax.comm.SerialPort 
	 * <p>
	 * <table border="1">
	 * <tr>
	 * <th>Key</th>
	 * <th>Description</th>
	 * </tr>
	 * <tr>
	 * <td>port</td>
	 * <td>the actual device port, such as "/dev/ttyUSB0" in linux</td>
	 * </tr>
	 * <tr>
	 * <td>serialMode</td>
	 * <td>SERIAL_232 or SERIAL_485</td>
	 * </tr>
	 * <tr>
	 * <td>baudRate</td>
	 * <td>baud rate to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>stopBits</td>
	 * <td>number of stop bits to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>parity</td>
	 * <td>parity mode to be configured for the port</td>
	 * </tr>
	 * <tr>
	 * <td>bitsPerWord</td>
	 * <td>only RTU mode supported, bitsPerWord must be 8</td>
	 * </tr>
	 * </table>
	 * see {@link org.eclipse.kura.comm.CommConnection CommConnection} package for more
	 * detail.
	 */
	private final class SerialCommunicate extends Communicate {

		InputStream in;
		OutputStream out;
		CommConnection conn=null;

		FileWriter gpioModeSwitch;

		public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig)
				throws ModbusProtocolException {
			s_logger.debug("Configure serial connection");


			String sPort;
			String sBaud;
			String sStop;
			String sParity;
			String sBits;				
			String gpioSwitchPin = null;
			String gpioRsModePin = null;

			if (((sPort = connectionConfig.getProperty("port")) == null)
					|| ((sBaud = connectionConfig.getProperty("baudRate")) == null)
					|| ((sStop = connectionConfig.getProperty("stopBits")) == null)
					|| ((sParity = connectionConfig.getProperty("parity")) == null)
					|| ((sBits = connectionConfig.getProperty("bitsPerWord")) == null))
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);

			int baud = Integer.valueOf(sBaud).intValue();
			int stop = Integer.valueOf(sStop).intValue();
			int parity = Integer.valueOf(sParity).intValue();
			int bits = Integer.valueOf(sBits).intValue();
			if(connectionConfig.getProperty("serialMode")!=null){
				m_serial485=(connectionConfig.getProperty("serialMode")==SERIAL_485);
				if(m_serial485){
					if (((gpioSwitchPin = connectionConfig.getProperty("serialGPIOswitch"))==null)
							|| ((gpioRsModePin = connectionConfig.getProperty("serialGPIOrsmode"))==null))
						throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
				}
			}

			String uri = new CommURI.Builder(sPort)
			.withBaudRate(baud)
			.withDataBits(bits)
			.withStopBits(stop)
			.withParity(parity)
			.withTimeout(2000)
			.build().toString();

			try {
				conn = (CommConnection) connFactory.createConnection(uri, 1, false);
			} catch (IOException e1) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,e1);
			}
			if(m_serial485){
				setupSerialGpio(gpioRsModePin, gpioSwitchPin);
			}

			// get the streams
			try {
				in = conn.openInputStream();
				out = conn.openOutputStream();
				if(m_serial485)
					gpioModeSwitch  = new FileWriter(new File("/sys/class/gpio/" + gpioSwitchPin + "/value"));
			} catch (Exception e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,e);
			}
		}

		/**
		 * Initializes the GPIO Serial Ports to RS485 mode with output direction.
		 * Communication can then be switched to RX or TX.
		 * @param gpioRsMode
		 * @param gpioSwitch
		 * @throws ModbusProtocolException 
		 */
		private void setupSerialGpio(String gpioRsMode, String gpioSwitch) throws ModbusProtocolException {
			final String[] requiredGpio = {
					gpioRsMode, /* port 3 RS mode */
					gpioSwitch /* port TX<->RX switch */
			};

			for (String gpio : requiredGpio) {
				File gpioFile = new File(GPIO_BASE_PATH + gpio);
				if (!gpioFile.exists()) {
					// # Pin is not exported, so do it now
					FileWriter fwExport = null;
					try {
						fwExport = new FileWriter(new File(GPIO_BASE_PATH + GPIO_EXPORT_PATH));
						// write only the PIN number
						fwExport.write(gpio.replace("gpio", ""));
						fwExport.flush();
						s_logger.debug("Exported GPIO {}", gpio);

					} catch (IOException e) {
						s_logger.error("Exporting Error", e);
					} finally {
						if (fwExport != null) {
							try {
								fwExport.close();
							} catch (IOException e) {
								s_logger.error("Error closing export file", e);
							}
						}
					}
					// wait a little after exporting
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}


				// After exporting the pin, set the direction to "out"
				FileWriter fwDirection = null;
				try {
					fwDirection = new FileWriter(new File(GPIO_BASE_PATH + gpio + "/direction"));
					fwDirection.write("out");
					fwDirection.flush();
					s_logger.debug("Direction GPIO {}", gpio);

				} catch (IOException e) {
					s_logger.error("Direction Error", e);
				} finally {
					if (fwDirection != null) {
						try {
							fwDirection.close();
						} catch (IOException e) {
							s_logger.error("Error closing export file", e);
						}
					}
				}

				//Switch to RS485 mode
				FileWriter fwValue = null;
				try {
					fwValue = new FileWriter(new File(GPIO_BASE_PATH + gpio + "/value"));
					fwValue.write("1");
					fwValue.flush();
					s_logger.debug("Value GPIO {}", gpio);

				} catch (IOException e) {
					s_logger.error("Value Error", e);
				} finally {
					if (fwValue != null) {
						try {
							fwValue.close();
						} catch (IOException e) {
							s_logger.error("Error closing value file", e);
						}
					}
				}

			}

		}

		/**
		 * Sets the GPIO Serial port to Transmit mode. Data can be written to OutputStream.
		 * @throws InterruptedException 
		 */
		private void switchTX() throws ModbusProtocolException {
			try {
				gpioModeSwitch.write("1");
				gpioModeSwitch.flush();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchTX interrupted");
				}
			} catch (IOException e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchTX IOException "+e.getMessage());
			}
		}

		/**
		 * Sets the GPIO Serial port to Receive mode. Data can be read from InputStream.
		 * @throws InterruptedException 
		 */
		private void switchRX() throws ModbusProtocolException {
			try {
				gpioModeSwitch.write("0");
				gpioModeSwitch.flush();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchRX interrupted");
				}
			} catch (IOException e) {
				throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE,"switchRX IOException "+e.getMessage());
			}
		}

		public void connect() {
			/*
			 * always connected
			 */
		}

		public void disconnect() throws ModbusProtocolException {
			if (conn!=null) {
				try {
					conn.close();
					s_logger.debug("Serial connection closed");
				} catch (IOException e) {
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
				}
				conn = null;
			}
			if(m_serial485){
				if (gpioModeSwitch != null) {
					try {
						gpioModeSwitch.close();
					} catch (IOException e) {
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
					}
				}
			}
		}

		public int getConnectStatus() {
			return KuraConnectionStatus.CONNECTED;
		}

		private byte asciiLrcCalc(byte[] msg, int len){
			char[] ac=new char[2];
			ac[0]=(char) msg[len-4]; ac[1]=(char) msg[len - 3];
			String s=new String(ac);
			byte lrc=(byte) Integer.parseInt(s,16);
			return lrc;
		}

		private int binLrcCalc(byte[] msg){
			int llrc=0;
			for(int i=0; i<msg.length; i++) 
				llrc+=(int)msg[i] & 0xff;
			llrc = (llrc ^ 0xff) + 1;
			//byte lrc=(byte)(llrc & 0x0ff);
			return llrc;
		}

		/**
		 * convertCommandToAscii: convert a binary command into a standard Modbus
		 * ASCII frame 
		 */
		private byte[] convertCommandToAscii(byte[] msg){
			int lrc=binLrcCalc(msg);

			char[] hexArray = "0123456789ABCDEF".toCharArray();
			byte[] ab=new byte[msg.length*2 + 5];
			ab[0]=':';
			int v;
			for(int i=0; i<msg.length; i++){
				v=msg[i] & 0xff;
				ab[i*2 + 1] = (byte) hexArray[v >>> 4];
				ab[i*2 + 2] = (byte) hexArray[v & 0x0f];
			}
			v=lrc & 0x0ff;
			ab[ab.length-4]=(byte) hexArray[v >>> 4];
			ab[ab.length-3]=(byte) hexArray[v & 0x0f];
			ab[ab.length-2]=13;
			ab[ab.length-1]=10;
			return ab;
		}

		/**
		 * convertAsciiResponseToBin: convert a standard Modbus frame to
		 * byte array 
		 */
		private byte[] convertAsciiResponseToBin(byte[] msg, int len){
			int l = (len-5)/2;
			byte[] ab=new byte[l];
			char[] ac=new char[2];
			//String s=new String(msg);
			for(int i=0; i<l; i++){
				ac[0]=(char) msg[i*2 + 1]; ac[1]=(char) msg[i*2 + 2];
				//String s=new String(ac);
				ab[i]=(byte) Integer.parseInt(new String(ac),16);
			}
			return ab;
		}

		/**
		 * msgTransaction must be called with a byte array having two extra
		 * bytes for the CRC. It will return a byte array of the response to the
		 * message. Validation will include checking the CRC and verifying the
		 * command matches.
		 */
		public byte[] msgTransaction(byte[] msg)
				throws ModbusProtocolException {

			byte[] cmd = null;

			if(m_txMode == ModbusTransmissionMode.RTU_MODE){
				cmd = new byte[msg.length+2];
				for(int i=0; i<msg.length; i++)
					cmd[i]=msg[i];
				// Add crc calculation to end of message
				int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
				cmd[msg.length] = (byte) crc;
				cmd[msg.length + 1] = (byte) (crc >> 8);
			}
			else if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
				cmd=convertCommandToAscii(msg);
			}

			// Send the message
			try {
				synchronized (out) {
					synchronized (in) {
						// flush input
						if(m_serial485)
							switchRX();
						while (in.available() > 0)
							in.read();
						// send all data
						if(m_serial485)
							switchTX();
						out.write(cmd, 0, cmd.length);
						out.flush();
						//outputStream.waitAllSent(respTout);

						// wait for and process response
						if(m_serial485)
							switchRX();
						byte[] response = new byte[262]; // response buffer
						int respIndex = 0;
						int minimumLength = 5; // default minimum message length
						if(m_txMode == ModbusTransmissionMode.ASCII_MODE)
							minimumLength = 11;
						int timeOut = m_respTout;
						for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
							boolean endFrame=false;
							//while (respIndex < minimumLength) {
							while (!endFrame) {
								long start = System.currentTimeMillis();
								while(in.available()==0) {								
									try {
										Thread.sleep(5);	// avoid a high cpu load
									} catch (InterruptedException e) {
										throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Thread interrupted");
									}

									long elapsed = System.currentTimeMillis()-start;
									if( elapsed > timeOut) {
										String failMsg = "Recv timeout";
										s_logger.warn(failMsg+" : "+elapsed+" minimumLength="+minimumLength+" respIndex="+respIndex);
										throw new ModbusProtocolException(ModbusProtocolErrorCode.RESPONSE_TIMEOUT, failMsg);
									}
								}
								// address byte must match first
								if (respIndex == 0) {
									if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
										if ((response[0] = (byte) in.read()) == ':')
											respIndex++;
									}
									else{
										if ((response[0] = (byte) in.read()) == msg[0])
											respIndex++;
									}
								} else
									response[respIndex++] = (byte) in.read();

								if(m_txMode == ModbusTransmissionMode.RTU_MODE){
									timeOut = 100; // move to character timeout
									if(respIndex >= minimumLength)
										endFrame=true;
								}
								else{
									if((response[respIndex-1]==10)&&(response[respIndex-2]==13))
										endFrame=true;
								}
							}
							// if ASCII mode convert response
							if(m_txMode == ModbusTransmissionMode.ASCII_MODE){
								byte lrcRec = asciiLrcCalc(response,respIndex);
								response=convertAsciiResponseToBin(response,respIndex);
								byte lrcCalc = (byte)binLrcCalc(response);
								if(lrcRec!=lrcCalc)
									throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Bad LRC");
							}

							// Check first for an Exception response
							if ((response[1] & 0x80) == 0x80) {
								if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||(Crc16.getCrc16(response, 5, 0xffff) == 0))
									throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
											"Exception response = "+ Byte.toString(response[2]));
							} else {
								// then check for a valid message
								switch (response[1]) {
								case ModbusFunctionCodes.FORCE_SINGLE_COIL:
								case ModbusFunctionCodes.PRESET_SINGLE_REG:
								case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
								case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
									if (respIndex < 8)
										// wait for more data
										minimumLength = 8;
									else if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||
											(Crc16.getCrc16(response, 8, 0xffff) == 0)) {
										byte[] ret = new byte[6];
										for (int i = 0; i < 6; i++)
											ret[i] = response[i];
										return ret;
									}
									break;
								case ModbusFunctionCodes.READ_COIL_STATUS:
								case ModbusFunctionCodes.READ_INPUT_STATUS:
								case ModbusFunctionCodes.READ_INPUT_REGS:
								case ModbusFunctionCodes.READ_HOLDING_REGS:									
									int byteCnt;
									if ((m_txMode == ModbusTransmissionMode.ASCII_MODE))
										byteCnt= (response[2] & 0xff) + 3;
									else
										byteCnt= (response[2] & 0xff) + 5;
									if (respIndex < byteCnt)
										// wait for more data
										minimumLength = byteCnt;
									else if ((m_txMode == ModbusTransmissionMode.ASCII_MODE)||
											(Crc16.getCrc16(response, byteCnt,0xffff) == 0)) {
										byte[] ret = new byte[byteCnt];
										for (int i = 0; i < byteCnt; i++)
											ret[i] = response[i];
										return ret;
									}
								}
							}

							/*
							 * if required length then must have failed, drop
							 * first byte and try again
							 */
							if (respIndex >= minimumLength) {
								respIndex--;
								for (int i = 0; i < respIndex; i++)
									response[i] = response[i + 1];
								minimumLength = 5; // reset minimum length
							}
						}
					}
				}
			} catch (IOException e) {
				//e.printStackTrace();
				throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,e.getMessage());
			}
			throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
					"Too much activity on recv line");
		}
	}

	/**
	 * Installation of an ethernet connection to communicate 
	 */
	private final class EthernetCommunicate extends Communicate {
		InputStream inputStream;
		OutputStream outputStream;
		Socket socket;
		int port;
		String ipAddress;
		boolean connected = false;

		public EthernetCommunicate(ConnectionFactory connFactory, Properties connectionConfig)
				throws ModbusProtocolException {
			s_logger.debug("Configure TCP connection");
			String sPort;

			if (((sPort = connectionConfig.getProperty("port")) == null)
					|| ((ipAddress = connectionConfig.getProperty("ipAddress")) == null))
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
			port = Integer.valueOf(sPort).intValue();
			m_connConfigd = true;
			socket = new Socket();
		}

		public void connect() {
			if (!m_connConfigd) {
				s_logger.error("Can't connect, port not configured");
			} else {
				if (!connected) {
					try {
						socket = new Socket(ipAddress, port);
						try {
							inputStream = socket.getInputStream();
							outputStream = socket.getOutputStream();
							connected = true;
							s_logger.info("TCP connected");
						} catch (IOException e) {
							disconnect();
							s_logger.error("Failed to get socket streams: " + e);
						}
					} catch (IOException e) {
						s_logger.error("Failed to connect to remote: " + e);
					}
				}
			}
		}

		public void disconnect() {
			if (m_connConfigd) {
				if (connected) {
					try {
						if (!socket.isInputShutdown())
							socket.shutdownInput();
						if (!socket.isOutputShutdown())
							socket.shutdownOutput();
						socket.close();
					} catch (IOException eClose) {
						s_logger.error("Error closing TCP: "
								+ eClose);
					}
					inputStream = null;
					outputStream = null;
					connected = false;
					socket = null;
				}
			}
		}

		public int getConnectStatus() {
			if (connected)
				return KuraConnectionStatus.CONNECTED;
			else if (m_connConfigd)
				return KuraConnectionStatus.DISCONNECTED;
			else
				return KuraConnectionStatus.NEVERCONNECTED;
		}

		public byte[] msgTransaction(byte[] msg)
				throws ModbusProtocolException {
			byte[] cmd = null;

			if(m_txMode == ModbusTransmissionMode.RTU_MODE){
				cmd = new byte[msg.length+2];
				for(int i=0; i<msg.length; i++)
					cmd[i]=msg[i];
				// Add crc calculation to end of message
				int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
				cmd[msg.length] = (byte) crc;
				cmd[msg.length + 1] = (byte) (crc >> 8);
			}
			else 				
				throw new ModbusProtocolException(ModbusProtocolErrorCode.METHOD_NOT_SUPPORTED,"Only RTU over TCP/IP supported");


			// Check connection status and connect
			connect();
			if (!connected)
				throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
						"Cannot transact on closed socket");

			// Send the message
			try {
				// flush input
				while (inputStream.available() > 0)
					inputStream.read();
				// send all data
				outputStream.write(cmd, 0, cmd.length);
				outputStream.flush();
			} catch (IOException e) {
				// Assume this means the socket is closed...make sure it is
				s_logger.error("Socket disconnect in send: " + e);
				disconnect();
				throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Send failure: "
						+ e.getMessage());
			}

			// wait for and process response
			byte[] response = new byte[262]; // response buffer
			int respIndex = 0;
			int minimumLength = 5; // default minimum message length
			while (true) {
				while (respIndex < minimumLength) {
					try {
						socket.setSoTimeout(m_respTout);
						int resp = inputStream.read(response, respIndex,
								minimumLength - respIndex);
						if (resp > 0) {
							respIndex += resp;
						} else {
							s_logger.error("Socket disconnect in recv");
							disconnect();
							throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,"Recv failure");
						}
					} catch (SocketTimeoutException e) {
						String failMsg = "Recv timeout";
						s_logger.warn(failMsg);
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,failMsg);
					} catch (IOException e) {
						s_logger.error("Socket disconnect in recv: " + e);
						disconnect();
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
								"Recv failure");
					}
				}

				// Check first for an Exception response
				if ((response[1] & 0x80) == 0x80) {
					if (Crc16.getCrc16(response, 5, 0xffff) == 0)
						throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
								"Resp exception = "
										+ Byte.toString(response[2]));
				} else {
					// then check for a valid message
					switch (response[1]) {
					case ModbusFunctionCodes.FORCE_SINGLE_COIL:
					case ModbusFunctionCodes.PRESET_SINGLE_REG:
					case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
					case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
						if (respIndex < 8)
							// wait for more data
							minimumLength = 8;
						else if (Crc16.getCrc16(response, 8, 0xffff) == 0) {
							byte[] ret = new byte[8];
							for (int i = 0; i < 8; i++)
								ret[i] = response[i];
							return ret;
						}
						break;
					case ModbusFunctionCodes.READ_COIL_STATUS:
					case ModbusFunctionCodes.READ_INPUT_STATUS:
					case ModbusFunctionCodes.READ_INPUT_REGS:
					case ModbusFunctionCodes.READ_HOLDING_REGS:
						int byteCnt = (response[2] & 0xff) + 5;
						if (respIndex < byteCnt)
							// wait for more data
							minimumLength = byteCnt;
						else if (Crc16.getCrc16(response, byteCnt, 0xffff) == 0) {
							byte[] ret = new byte[byteCnt];
							for (int i = 0; i < byteCnt; i++)
								ret[i] = response[i];
							return ret;
						}
					}
				}

				/*
				 * if required length then must have failed, drop first byte and
				 * try again
				 */
				if (respIndex >= minimumLength)
					throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
							"Error in recv");
			}
		}
	}

	public boolean[] readCoils(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_COIL_STATUS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (count / 256);
		cmd[5] = (byte) (count % 256);

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == ((count + 7) / 8)) {
			byte mask = 1;
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				// get this point's value
				if ((resp[byteOffset] & mask) == mask)
					ret[index] = true;
				else
					ret[index] = false;
				// advance the mask and offset index
				if ((mask <<= 1) == 0) {
					mask = 1;
					byteOffset++;
				}
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);

		return ret;
	}

	public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_STATUS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (count / 256);
		cmd[5] = (byte) (count % 256);

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == ((count + 7) / 8)) {
			byte mask = 1;
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				// get this point's value
				if ((resp[byteOffset] & mask) == mask)
					ret[index] = true;
				else
					ret[index] = false;
				// advance the mask and offset index
				if ((mask <<= 1) == 0) {
					mask = 1;
					byteOffset++;
				}
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);

		return ret;
	}

	public void writeSingleCoil(int unitAddr, int dataAddress, boolean data)
			throws ModbusProtocolException{
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		byte[] resp;

		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.FORCE_SINGLE_COIL;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (data == true) ? (byte) 0xff : (byte) 0;
		cmd[5] = 0;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int i = 0; i < 6; i++)
			if (cmd[i] != resp[i])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);

	}

	public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data)
			throws ModbusProtocolException{
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * write multiple boolean values
		 */
		int localCnt = data.length;
		int index = 0;
		byte[] resp;
		/*
		 * construct the command, issue and verify response
		 */
		int dataLength = (localCnt + 7) / 8;
		byte[] cmd = new byte[dataLength + 7];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.FORCE_MULTIPLE_COILS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (localCnt / 256);
		cmd[5] = (byte) (localCnt % 256);
		cmd[6] = (byte) dataLength;

		// put the data on the command
		byte mask = 1;
		int byteOffset = 7;
		cmd[byteOffset] = 0;
		for (int j = 0; j < localCnt; j++, index++) {
			// get this point's value
			if (data[index])
				cmd[byteOffset] += mask;
			// advance the mask and offset index
			if ((mask <<= 1) == 0) {
				mask = 1;
				byteOffset++;
				cmd[byteOffset] = 0;
			}
		}

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int j = 0; j < 6; j++)
			if (cmd[j] != resp[j])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	public int[] readHoldingRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int[] ret = new int[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results, putting the results
		 * away at index and then incrementing index for the next command
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = 0;
		cmd[5] = (byte) count;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == (count * 2)) {
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				int val = resp[byteOffset
				               + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] & 0xff;
				val <<= 8;
				val += resp[byteOffset
				            + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] & 0xff;

				ret[index] = val;

				byteOffset += 2;
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
		return ret;
	}

	public int[] readInputRegisters(int unitAddr, int dataAddress, int count)
			throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int[] ret = new int[count];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results, putting the results
		 * away at index and then incrementing index for the next command
		 */
		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = 0;
		cmd[5] = (byte) count;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < 3) || (resp.length < ((resp[2] & 0xff) + 3)))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		if ((resp[2] & 0xff) == (count * 2)) {
			int byteOffset = 3;
			for (int j = 0; j < count; j++, index++) {
				int val = resp[byteOffset
				               + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] & 0xff;
				val <<= 8;
				val += resp[byteOffset
				            + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] & 0xff;

				ret[index] = val;

				byteOffset += 2;
			}
		} else
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
		return ret;
	}

	public void writeSingleRegister(int unitAddr, int dataAddress, int data)
			throws ModbusProtocolException{
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		byte[] cmd = new byte[6];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.PRESET_SINGLE_REG;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (data >> 8);
		cmd[5] = (byte) data;

		/*
		 * send the message and get the response
		 */
		byte[] resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int i = 0; i < 6; i++)
			if (cmd[i] != resp[i])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	public void writeMultipleRegister(int unitAddr, int dataAddress, int[] data)
			throws ModbusProtocolException{
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		int localCnt = data.length;
		/*
		 * construct the command, issue and verify response
		 */
		int dataLength = localCnt * 2;
		byte[] cmd = new byte[dataLength + 7];
		cmd[0] = (byte) unitAddr;
		cmd[1] = ModbusFunctionCodes.PRESET_MULTIPLE_REGS;
		cmd[2] = (byte) (dataAddress / 256);
		cmd[3] = (byte) (dataAddress % 256);
		cmd[4] = (byte) (localCnt / 256);
		cmd[5] = (byte) (localCnt % 256);
		cmd[6] = (byte) dataLength;

		// put the data on the command
		int byteOffset = 7;
		int index = 0;
		for (int j = 0; j < localCnt; j++, index++) {
			cmd[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1')] = (byte) (data[index] >> 8);
			cmd[byteOffset + (ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1')] = (byte) data[index];

			byteOffset += 2;
		}

		/*
		 * send the message and get the response
		 */
		byte[] resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		for (int j = 0; j < 6; j++)
			if (cmd[j] != resp[j])
				throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
	}

	public boolean[] readExceptionStatus(int unitAddr)
			throws ModbusProtocolException {
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		boolean[] ret = new boolean[8];
		int index = 0;

		byte[] resp;
		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.READ_EXCEPTION_STATUS;

		/*
		 * send the message and get the response
		 */
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if (resp.length < 3)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		byte mask = 1;
		for (int j = 0; j < 8; j++, index++) {
			// get this point's value
			if ((resp[2] & mask) == mask)
				ret[index] = true;
			else
				ret[index] = false;
			// advance the mask and offset index
			if ((mask <<= 1) == 0) {
				mask = 1;
			}
		}

		return ret;
	}

	public ModbusCommEvent getCommEventCounter(int unitAddr)
			throws ModbusProtocolException {
		ModbusCommEvent mce = new ModbusCommEvent();
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_COUNTER;

		/*
		 * send the message and get the response
		 */
		byte[] resp;
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if (resp.length < 6)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		int val = resp[2] & 0xff;
		val <<= 8;
		val += resp[3] & 0xff;
		mce.setStatus(val);
		val = resp[4] & 0xff;
		val <<= 8;
		val += resp[5] & 0xff;
		mce.setEventCount(val);

		return mce;
	}

	public ModbusCommEvent getCommEventLog(int unitAddr)
			throws ModbusProtocolException {
		ModbusCommEvent mce = new ModbusCommEvent();
		if (!m_connConfigd)
			throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);

		/*
		 * construct the command issue and get results
		 */
		byte[] cmd = new byte[2];
		cmd[0] = (byte) unitAddr;
		cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_LOG;

		/*
		 * send the message and get the response
		 */
		byte[] resp;
		resp = m_comm.msgTransaction(cmd);

		/*
		 * process the response (address & CRC already confirmed)
		 */
		if ((resp.length < ((resp[2] & 0xff) + 3))||((resp[2] & 0xff)>64+7))
			throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
		int val = resp[3] & 0xff;
		val <<= 8;
		val += resp[4] & 0xff;
		mce.setStatus(val);

		val = resp[5] & 0xff;
		val <<= 8;
		val += resp[6] & 0xff;
		mce.setEventCount(val);

		val = resp[7] & 0xff;
		val <<= 8;
		val += resp[8] & 0xff;
		mce.setMessageCount(val);

		int count=(resp[2] & 0xff)-4;
		int[] events=new int[count];
		for (int j = 0; j < count; j++) {
			int bval = resp[9+j] & 0xff;
			events[j] = bval;
		}
		mce.setEvents(events);

		return mce;
	}
}
