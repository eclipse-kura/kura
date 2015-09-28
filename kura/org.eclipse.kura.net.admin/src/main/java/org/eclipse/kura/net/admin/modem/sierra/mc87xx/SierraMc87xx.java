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
package org.eclipse.kura.net.admin.modem.sierra.mc87xx;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SierraMc87xx implements HspaCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(SierraMc87xx.class);
	
	private ConnectionFactory m_connectionFactory = null;
	
	private String m_model = null;
	private String m_manufacturer = null;
	private String m_serialNumber = null;
	private String m_revisionId = null;
	
	private Object m_atLock = null; 
	
	private ModemDevice m_device = null;
	private List<NetConfig> m_netConfigs = null;
	
	public SierraMc87xx(ModemDevice device, ConnectionFactory connectionFactory) {
        
        m_device = device;
        m_connectionFactory = connectionFactory;
        m_atLock = new Object();
	}
	
	@Override
	public String getModel() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_model == null) {
	    		s_logger.debug("sendCommand getModelNumber :: " + SierraMc87xxAtCommands.getModelNumber.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getModelNumber.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
					m_model = getResponseString(reply);
					reply = null;
				}
	    	}
    	}
        return m_model;
	}

	@Override
	public String getManufacturer() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_manufacturer == null) {
		    	s_logger.debug("sendCommand getManufacturer :: " + SierraMc87xxAtCommands.getManufacturer.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
		    	try {
					reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getManufacturer.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
		    	closeSerialPort(commAtConnection);
				if (reply != null) {
				    m_manufacturer = getResponseString(reply); 
					reply = null;
				}
	    	}
    	}
        return m_manufacturer;
	}

	@Override
	public String getSerialNumber() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_serialNumber == null) {
	    		s_logger.debug("sendCommand getSerialNumber :: " + SierraMc87xxAtCommands.getSerialNumber.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSerialNumber.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
					m_serialNumber = getResponseString(reply);
				}
	    	}
    	}
        return m_serialNumber;
	}
	
	@Override
	public String getMobileSubscriberIdentity() throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIntegratedCirquitCardId() throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRevisionID() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_revisionId == null) {
	    		s_logger.debug("sendCommand getRevision :: " + SierraMc87xxAtCommands.getFirmwareVersion.getCommand());
	    		byte [] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
	    		try {
					reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getFirmwareVersion.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
	    		closeSerialPort(commAtConnection);
				if (reply != null) {
					String firmwareVersion = getResponseString(reply);
					if (firmwareVersion.startsWith("!GVER:")) {
						firmwareVersion = firmwareVersion.substring("!GVER:".length()).trim();
						String [] aFirmwareVersion = firmwareVersion.split(" ");
						m_revisionId = aFirmwareVersion[0];
					}
				}
	    	}
    	}
        return m_revisionId;
	}

	@Override
	public boolean isReachable() throws KuraException {
		boolean ret = false;
    	synchronized (m_atLock) {
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		ret = isAtReachable(commAtConnection);
    		closeSerialPort(commAtConnection);
    	}
		return ret;
	}
	
	@Override
	public boolean isPortReachable(String port) {
		boolean ret = false;
		synchronized (m_atLock) {
			try {
				CommConnection commAtConnection = openSerialPort(port);
				closeSerialPort(commAtConnection);
				ret = true;
			} catch (KuraException e) {
				s_logger.warn("isPortReachable() :: The {} is not reachable", port);
			}
		}
		return ret;
	}

	@Override
	public void reset() throws KuraException {
		// TODO
		// not supported
	}

	@Override
	public int getSignalStrength() throws KuraException {
		
		int rssi = -113;
    	synchronized (m_atLock) {
	    	s_logger.debug("sendCommand getSignalStrength :: " + SierraMc87xxAtCommands.getSignalStrength.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSignalStrength.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String [] asCsq = null;
				String sCsq = this.getResponseString(reply);
				if (sCsq.startsWith("+CSQ:")) {
					sCsq = sCsq.substring("+CSQ:".length()).trim();
					asCsq = sCsq.split(",");
					if (asCsq.length == 2) {
						rssi = -113 + 2 * Integer.parseInt(asCsq[0]);
						
					}
				}
				reply = null;
			}
    	}
        return rssi;
	}

	@Override
	public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
		
		ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
		synchronized (m_atLock) {
			s_logger.debug("sendCommand getSystemInfo :: " + SierraMc87xxAtCommands.getSystemInfo.getCommand());
    		byte[] reply = null;
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSystemInfo.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String sSysInfo = getResponseString(reply);
				if ((sSysInfo != null) && (sSysInfo.length() > 0)) { 
					String [] aSysInfo = sSysInfo.split(",");
					if (aSysInfo.length == 5) {
						int srvStatus = Integer.parseInt(aSysInfo[0]);
						int roamingStatus = Integer.parseInt(aSysInfo[2]);
						switch (srvStatus) {
						case 0:
							modemRegistrationStatus = ModemRegistrationStatus.NOT_REGISTERED;
							break;
						case 2:
							switch (roamingStatus) {
							case 0:
								modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_HOME;
								break;
							case 1:
								modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_ROAMING;
								break;
							}
							break;
						}
					}
				}
			}
		}
		return modemRegistrationStatus;
	}

	@Override
	public long getCallTxCounter() throws KuraException {
		// TODO 
		// Not supported via AT interface need to use HIP/CnS
		return 0;
	}

	@Override
	public long getCallRxCounter() throws KuraException {
		// TODO
		// Not supported via AT interface need to use HIP/CnS
		return 0;
	}

	@Override
	public String getServiceType() throws KuraException {
		
		String serviceType = null;
    	synchronized (m_atLock) {
    		s_logger.debug("sendCommand getMobileStationClass :: " + SierraMc87xxAtCommands.getMobileStationClass.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String sCgclass = this.getResponseString(reply);
				if (sCgclass.startsWith("+CGCLASS:")) {
					sCgclass = sCgclass.substring("+CGCLASS:".length()).trim();
					if (sCgclass.equals("\"A\"")) {
						serviceType = "UMTS";
					} else if (sCgclass.equals("\"B\"")) {
						serviceType = "GSM/GPRS";
					} else if (sCgclass.equals("\"CG\"")) {
						serviceType = "GPRS";
					} else if (sCgclass.equals("\"CC\"")) {
						serviceType = "GSM";
					}
				}
				reply = null;
			}
    	}
		
		return serviceType;
	}

	@Override
	public ModemDevice getModemDevice() {
		return m_device;
	}
	
	protected void setModemDevice(ModemDevice device) {
		m_device = device;
	}

	@Override
	public String getDataPort() throws KuraException {
		String port = null;
		List <String> ports = m_device.getSerialPorts();
		if ((ports != null) && (ports.size() > 0)) {
	    	if (m_device instanceof UsbModemDevice) {
	    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)m_device);
	    		if (usbModemInfo != null) {
	    			port = ports.get(usbModemInfo.getDataPort());
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
	    		}
	    	} else {
	    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
	    	}
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
		}
		
    	return port;
	}

	@Override
	public String getAtPort() throws KuraException {
		String port = null;
		List <String> ports = m_device.getSerialPorts();
			if ((ports != null) && (ports.size() > 0)) {
	    	if (m_device instanceof UsbModemDevice) {
	    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)m_device);
	    		if (usbModemInfo != null) {
	    			port = ports.get(usbModemInfo.getAtPort());
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
	    		}
	    	} else {
	    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
	    	}
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serial ports available");
		}
    	
    	return port;
	}
	
	@Override
	public String getGpsPort() throws KuraException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isGpsSupported() throws KuraException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void enableGps() throws KuraException {
		// TODO Auto-generated method stub	
	}

	@Override
	public void disableGps() throws KuraException {
		// TODO Auto-generated method stub
	}

	@Override
	public List<NetConfig> getConfiguration() {
		return m_netConfigs;
	}

	@Override
	public void setConfiguration(List<NetConfig> netConfigs) {
		m_netConfigs = netConfigs;
	}
	
	@Override
	public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {
		
		List<ModemTechnologyType>modemTechnologyTypes = null;
		ModemDevice device = getModemDevice();
		if (device == null) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No modem device");
		}
		if (device instanceof UsbModemDevice) {
    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)device);
    		if (usbModemInfo != null)  {
    			modemTechnologyTypes = usbModemInfo.getTechnologyTypes();
    		} else {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No usbModemInfo available");
    		}
    	} else {
    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
    	}
		return modemTechnologyTypes;
	}

	@Override
	@Deprecated
	public ModemTechnologyType getTechnologyType() {
    	ModemTechnologyType modemTechnologyType = null;
    	try {
			List<ModemTechnologyType> modemTechnologyTypes = getTechnologyTypes();
			if((modemTechnologyTypes != null) && (modemTechnologyTypes.size() > 0)) {
				modemTechnologyType = modemTechnologyTypes.get(0);
			}
		} catch (KuraException e) {
			s_logger.error("Failed to obtain modem technology - {}", e);
		}
		return modemTechnologyType;
	}

	@Override
	public boolean isSimCardReady() throws KuraException {
		
		boolean simReady = false;
		synchronized (m_atLock) {
			s_logger.debug("sendCommand getSystemInfo :: " + SierraMc87xxAtCommands.getSystemInfo.getCommand());
    		byte[] reply = null;
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSystemInfo.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String sSysInfo = getResponseString(reply);
				if ((sSysInfo != null) && (sSysInfo.length() > 0)) { 
					String [] aSysInfo = sSysInfo.split(",");
					if (aSysInfo.length == 5) {
						int simStatus = Integer.parseInt(aSysInfo[4]);
						if (simStatus == 1) {
							simReady = true;
						}
					}
				}
			}
		}
		return simReady;
	}
	
	public CommURI getSerialConnectionProperties(SerialPortType portType) throws KuraException {
		try {
			String port;
			if (portType == SerialPortType.ATPORT) {
				port = getAtPort();
			} else if (portType == SerialPortType.DATAPORT) {
				port = this.getDataPort();
			} else if (portType == SerialPortType.GPSPORT) {
				port = getGpsPort();
			} else {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid Port Type");
			}
			StringBuffer sb = new StringBuffer();
			sb.append("comm:").append(port).append(";baudrate=115200;databits=8;stopbits=1;parity=0");
			return CommURI.parseString(sb.toString());
			
		} catch (URISyntaxException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "URI Syntax Exception");
		}
	}
	
	public boolean isGpsEnabled() {
		return false;
	}
	
	private CommConnection openSerialPort (String port) throws KuraException {
    	
    	CommConnection connection = null;
		if(m_connectionFactory != null) {
			String uri = new CommURI.Builder(port)
							.withBaudRate(115200)
							.withDataBits(8)
							.withStopBits(1)
							.withParity(0)
							.withTimeout(2000)
							.build().toString();
				
			try {
				connection = (CommConnection) m_connectionFactory
						.createConnection(uri, 1, false);
			} catch (Exception e) {
				s_logger.debug("Exception creating connection: " + e);
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
		}
		return connection;
    }
    
    private void closeSerialPort (CommConnection connection) throws KuraException {
		try {
			connection.close();
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
    }
    
    private boolean isAtReachable(CommConnection connection) {
    
    	boolean status = false;
    	int attemptNo = 0;
    	do {
			try {
				status = (connection.sendCommand(
						SierraMc87xxAtCommands.at.getCommand().getBytes(), 500).length > 0);
			} catch (Exception e) {
				attemptNo++;
				sleep(2000);
			}
    	} while((status == false) && (attemptNo < 3));
    	
    	return status;	
    }
    
    // Parse the AT command response for the relevant info
 	private String getResponseString(String resp) {
 	    if(resp == null) {
 	        return "";
 	    }
 	    
 	    // remove the command and space at the beginning, and the 'OK' and spaces at the end
 	    return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
 	}
 	
	private String getResponseString(byte[] resp) {
		if (resp == null) {
			return "";
		}

		return getResponseString(new String(resp));
	}
	
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
}
