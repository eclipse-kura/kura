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

package org.eclipse.kura.net.admin.modem.hspa;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.ModemDriver;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HspaModem implements HspaCellularModem{
	
	private static final Logger s_logger = LoggerFactory.getLogger(HspaModem.class);
	
	protected int m_pdpContext = 1;
	
	protected static Object s_atLock = new Object(); 
	
	protected String m_model;
	protected String m_manufacturer;
	protected String m_serialNumber;
	protected String m_revisionId;
	protected int m_rssi;
	protected Boolean m_gpsSupported;
	protected String m_imsi;
	protected String m_iccid;
	
	private ModemDevice m_device;
	private String m_platform;
	private ConnectionFactory m_connectionFactory;
	private List<NetConfig> m_netConfigs;
	
	public HspaModem(ModemDevice device, String platform,
			ConnectionFactory connectionFactory) {
		
		m_device = device;
		m_platform = platform;
		m_connectionFactory = connectionFactory;
	}
	
	@Override
	public void reset() throws KuraException {
		s_logger.warn("Modem reset not supported");
	}
	
	@Override
	public String getModel() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_model == null) {
	    		s_logger.debug("sendCommand getModelNumber :: {}", HspaModemAtCommands.getModelNumber.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
		    	}
				try {
					reply = commAtConnection.sendCommand(HspaModemAtCommands.getModelNumber.getCommand().getBytes(), 1000, 100);
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
    	synchronized (s_atLock) {
	    	if (m_manufacturer == null) {
		    	s_logger.debug("sendCommand getManufacturer :: {}", HspaModemAtCommands.getManufacturer.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
		    	}
		    	try {
					reply = commAtConnection.sendCommand(HspaModemAtCommands.getManufacturer.getCommand().getBytes(), 1000, 100);
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
    	synchronized (s_atLock) {
	    	if (m_serialNumber == null) {
	    		s_logger.debug("sendCommand getSerialNumber :: {}", HspaModemAtCommands.getSerialNumber.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
		    	}
				try {
					reply = commAtConnection.sendCommand(HspaModemAtCommands.getSerialNumber.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
				    String serialNum = getResponseString(reply);
				    if(serialNum != null && !serialNum.isEmpty()) {
				    	if (serialNum.startsWith("#CGSN:")) {
				    		serialNum = serialNum.substring("#CGSN:".length()).trim();
				    	}
				    	m_serialNumber = serialNum;        
				    }
				}
	    	}
    	}
        return m_serialNumber;
    }
	
	@Override
	public String getRevisionID() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_revisionId == null) {
	    		s_logger.debug("sendCommand getRevision :: {}", HspaModemAtCommands.getRevision.getCommand());
	    		byte [] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
		    	}
	    		try {
					reply = commAtConnection.sendCommand(HspaModemAtCommands.getRevision.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
	    		closeSerialPort(commAtConnection);
				if (reply != null) {
					m_revisionId = getResponseString(reply);
				}
	    	}
    	}
        return m_revisionId;
    }
    
	@Override
	public boolean isReachable() throws KuraException {
    	boolean ret = false;
    	synchronized (s_atLock) {
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		ret = isAtReachable(commAtConnection);
    		closeSerialPort(commAtConnection);
    	}
		return ret;
	}
	
	@Override
	public boolean isPortReachable(String port) {
		boolean ret = false;
		synchronized (s_atLock) {
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
	public int getSignalStrength() throws KuraException {
    	
    	int signalStrength = -113;
    	synchronized (s_atLock) {
    		String atPort = getAtPort();
				
	    	s_logger.debug("sendCommand getSignalStrength :: {}", HspaModemAtCommands.getSignalStrength.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(atPort);
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
	    	}
			try {
				reply = commAtConnection.sendCommand(HspaModemAtCommands.getSignalStrength.getCommand().getBytes(), 1000, 100);
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
					s_logger.trace("getSignalStrength() :: +CSQ={}", sCsq);
					asCsq = sCsq.split(",");
					if (asCsq.length == 2) {
						int rssi = Integer.parseInt(asCsq[0]);
						if (rssi < 99) {
							signalStrength = -113 + 2 * rssi;
						}
						s_logger.trace("getSignalStrength() :: signalStrength={}", signalStrength);
					}
				}
				reply = null;
			}
    	}
    	m_rssi = signalStrength;
        return signalStrength;
    }
	
	@Override
    public boolean isGpsSupported() throws KuraException {    	
    	return false;
    }
    
	@Override
    public void enableGps() throws KuraException {
    	s_logger.warn("Modem GPS not supported");
    }
    
	@Override
    public void disableGps() throws KuraException {
		s_logger.warn("Modem GPS not supported");
    }
    
	@Override
    public String getMobileSubscriberIdentity() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_imsi == null) {
	    		if (isSimCardReady()) {
		    		s_logger.debug("sendCommand getIMSI :: {}", HspaModemAtCommands.getIMSI.getCommand());
		    		byte[] reply = null;
		    		CommConnection commAtConnection = openSerialPort(getAtPort());
		    		if (!isAtReachable(commAtConnection)) {
		    			closeSerialPort(commAtConnection);
			    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
			    	}
					try {
						reply = commAtConnection.sendCommand(HspaModemAtCommands.getIMSI.getCommand().getBytes(), 1000, 100);
					} catch (IOException e) {
						closeSerialPort(commAtConnection);
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
					}
					closeSerialPort(commAtConnection);
					if (reply != null) {
					    String imsi = getResponseString(reply);
					    if(imsi != null && !imsi.isEmpty()) {
					    	if (imsi.startsWith("+CIMI:")) {
					    		imsi = imsi.substring("+CIMI:".length()).trim();
					    	}
					    	m_imsi = imsi;
					    }
					}
	    		}
	    	}
    	}
        return m_imsi;
    }
    
	@Override
    public String getIntegratedCirquitCardId() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_iccid == null) {
	    		if (isSimCardReady()) {
		    		s_logger.debug("sendCommand getICCID :: {}", HspaModemAtCommands.getICCID.getCommand());
		    		byte[] reply = null;
		    		CommConnection commAtConnection = openSerialPort(getAtPort());
		    		if (!isAtReachable(commAtConnection)) {
		    			closeSerialPort(commAtConnection);
			    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
			    	}
					try {
						reply = commAtConnection.sendCommand(HspaModemAtCommands.getICCID.getCommand().getBytes(), 1000, 100);
					} catch (IOException e) {
						closeSerialPort(commAtConnection);
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
					}
					closeSerialPort(commAtConnection);
					if (reply != null) {
					    String iccid = getResponseString(reply);
					    if(iccid != null && !iccid.isEmpty()) {
					    	if (iccid.startsWith("+CCID:")) {
					    		iccid = iccid.substring("+CCID:".length()).trim();
					    	}
					    	m_iccid = iccid;        
					    }
					}
	    		}
	    	}
    	}
        return m_iccid;
    }
        
	@Override
    public String getDataPort() throws KuraException {
    	String port = null;
    	List <String> ports = m_device.getSerialPorts();
    	if ((ports != null) && (ports.size() > 0)) {
	    	if (m_device instanceof UsbModemDevice) {
	    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)m_device);
	    		if (usbModemInfo != null)  {
	    			port = ports.get(usbModemInfo.getDataPort());
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
	    		}
	    	} else if (m_device instanceof SerialModemDevice) {
	    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	    		if (serialModemInfo != null) {
	    			port = serialModemInfo.getDriver().getComm().getDataPort();
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
	    	} else if (m_device instanceof SerialModemDevice) {
	    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	    		if (serialModemInfo != null) {
	    			port = serialModemInfo.getDriver().getComm().getAtPort();
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
    	String port = null;
    	List <String> ports = m_device.getSerialPorts();
    	if ((ports != null) && (ports.size() > 0)) {
	    	if (m_device instanceof UsbModemDevice) {
	    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)m_device);
	    		if (usbModemInfo != null) {
	    			int gpsPort = usbModemInfo.getGpsPort();
	    			if (gpsPort >= 0) {
	    				port = ports.get(gpsPort);
	    			}
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No GPS serial port available");
	    		}
	    	} else if (m_device instanceof SerialModemDevice) {
	    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	    		if (serialModemInfo != null) {
	    			port = serialModemInfo.getDriver().getComm().getGpsPort();
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No GPS serial port available");
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
    public ModemDevice getModemDevice() {
        return m_device;
    }
    
    public void setModemDevice(ModemDevice device) {
    	m_device = device;
    }
    
    @Override
    public List<NetConfig> getConfiguration() {
		return m_netConfigs;
	}

	public void setConfiguration(List<NetConfig> netConfigs) {
		m_netConfigs = netConfigs;
	}
	
	@Override
	public CommURI getSerialConnectionProperties(SerialPortType portType) throws KuraException {
		CommURI commURI = null;
		try {
			String port;
			if (portType == SerialPortType.ATPORT) {
				port = getAtPort();
			} else if (portType == SerialPortType.DATAPORT) {
				port = getDataPort();
			} else if (portType == SerialPortType.GPSPORT) {
				port = getGpsPort();
			} else {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Invalid Port Type");
			}
			if (port != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("comm:").append(port).append(";baudrate=115200;databits=8;stopbits=1;parity=0");
				commURI = CommURI.parseString(sb.toString());
			}
		} catch (URISyntaxException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "URI Syntax Exception");
		}
		return commURI;
	}
	
	@Override
	public boolean isGpsEnabled() {
		return false;
	}
	
	//public abstract boolean isSimCardReady() throws KuraException;
	
	protected CommConnection openSerialPort (String port) throws KuraException {
    	
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
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "Connection Failed");
			}
		}
		return connection;
    }
	
    protected void closeSerialPort (CommConnection connection) throws KuraException {
		try {
		    if (connection != null) {
		        connection.close();
		    }
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
    }
    
    protected boolean isAtReachable(CommConnection connection) {
    	
    	boolean status = false;
    	int attemptNo = 0;
    	do {
	    	try {
	    		s_logger.trace ("isAtReachable() :: sending AT commnd to modem on port {}", connection.getURI().getPort());
				byte [] reply = connection.sendCommand(HspaModemAtCommands.at.getCommand().getBytes(), 1000, 100);
				if (reply.length > 0) {
					String sReply = new String(reply);
					if (sReply.contains("OK")) {
						status = true;
					}
				}
			} catch (Exception e) {
				sleep(2000);
			} finally {
				attemptNo++;
			}
    	} while((status == false) && (attemptNo < 3));
    	
    	s_logger.trace("isAtReachable() :: port={}, status={}", connection.getURI().getPort(), status);
    	return status;
    }
    
    // Parse the AT command response for the relevant info
 	protected String getResponseString(String resp) {
 	    if(resp == null) {
 	        return "";
 	    }
 	    
 	    // remove the command and space at the beginning, and the 'OK' and spaces at the end
 	    return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
 	}
 	
	protected String getResponseString(byte[] resp) {
		if (resp == null) {
			return "";
		}

		return getResponseString(new String(resp));
	}
	
	protected void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
	
	protected ModemDriver getModemDriver() {
		
		if (m_device == null) {
			return null;
		}
		ModemDriver modemDriver = null;
		if (m_device instanceof UsbModemDevice) {
    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)m_device);
    		if (usbModemInfo != null) {
    			List<? extends UsbModemDriver> usbDeviceDrivers = usbModemInfo.getDeviceDrivers();
    			if ((usbDeviceDrivers != null) && (usbDeviceDrivers.size() > 0)) {
    				modemDriver = usbDeviceDrivers.get(0);
    			}
    		}
		} else if (m_device instanceof SerialModemDevice) {
    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
    		if (serialModemInfo != null) {
    			modemDriver = serialModemInfo.getDriver();
    		} 
		}
		return modemDriver;
	}

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
    	
    	ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getRegistrationStatus :: {}", HspaModemAtCommands.getRegistrationStatus.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
	    	}
			try {
				reply = commAtConnection.sendCommand(HspaModemAtCommands.getRegistrationStatus.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
	        if (reply != null) {
	            String sRegStatus = getResponseString(reply);
	            String[] regStatusSplit = sRegStatus.split(",");
	            if(regStatusSplit.length >= 2) {
	                int status = Integer.parseInt(regStatusSplit[1]);
	                switch (status) {
	                case 0:
	                	modemRegistrationStatus = ModemRegistrationStatus.NOT_REGISTERED;
	                	break;
	                case 1:
	                	modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_HOME;
	                	break;
	                case 3:
	                	modemRegistrationStatus = ModemRegistrationStatus.REGISTRATION_DENIED;
	                	break;
	                case 5:
	                	modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_ROAMING;
	                	break;
	                }
	            } 
	        }
    	}
        return modemRegistrationStatus;
    }
    
    @Override
    public long getCallTxCounter() throws KuraException {
    	return 0;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
    	return 0;
    }
    
    @Override
    public String getServiceType() throws KuraException {
    	String serviceType = null;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getMobileStationClass :: {}", HspaModemAtCommands.getMobileStationClass.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
	    	}
			try {
				reply = commAtConnection.sendCommand(HspaModemAtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);
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
    	} else if (device instanceof SerialModemDevice) {
    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
    		if (serialModemInfo != null) {
    			modemTechnologyTypes = serialModemInfo.getTechnologyTypes();
    		} else {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No serialModemInfo available");
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
		boolean status = false;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand isSimCardReady :: {}", HspaModemAtCommands.getSimPinStatus.getCommand());
    		byte[] reply = null;
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
    			throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
    		}
    		try {
    			reply = commAtConnection.sendCommand(HspaModemAtCommands.getSimPinStatus.getCommand().getBytes(), 1000, 100);
    		} catch (IOException e) {
    			closeSerialPort(commAtConnection);
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
    		}
    		closeSerialPort(commAtConnection);
    		if (reply != null) {
				String sReply = new String(reply);
				if (sReply.contains("OK")) {
					status = true;
				}
    		}
    	}
    	return status;
	}
}
