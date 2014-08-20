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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.net.admin.modem.telit.he910;

import java.io.IOException;
import java.util.List;

import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SerialModemComm;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;

/**
 * Defines Telit HE910 modem
 */
public class TelitHe910 implements HspaCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(TelitHe910.class);
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	private IVectorJ21GpioService m_vectorJ21GpioService = null;
	private ConnectionFactory m_connectionFactory = null;
 
	private ModemTechnologyType m_technologyType = null;
	private String m_model = null;
	private String m_manufacturer = null;
	private String m_serialNumber = null;
	private String m_revisionId = null;
	private int m_pdpContext = 1;
	private Boolean m_gpsSupported = null;
	
	private Object m_atLock = null; 
	
	private ModemDevice m_device = null;
	private List<NetConfig> m_netConfigs = null;

    /**
     * TelitHe910 modem constructor
     * 
     * @param usbDevice - modem USB device as {@link UsbModemDevice}
     * @param connectionFactory - connection factory {@link ConnectionFactory}
     * @param technologyType - cellular technology type as {@link ModemTechnologyType}
     */
	public TelitHe910(ModemDevice device, ConnectionFactory connectionFactory,
			ModemTechnologyType technologyType) {
        
        m_device = device;
        m_connectionFactory = connectionFactory;
        m_technologyType = technologyType;
        m_atLock = new Object();
         
        try {
			String atPort = getAtPort();
			String gpsPort = getGpsPort();
			if (atPort != null) {
				if (atPort.equals(getDataPort()) || atPort.equals(gpsPort)) {
					m_serialNumber = getSerialNumber();
					m_model = getModel();
					m_manufacturer = getManufacturer();		
					m_revisionId = getRevisionID();
					m_gpsSupported = isGpsSupported();
				}
			}
		} catch (KuraException e) {
			e.printStackTrace();
		}
    }
    
    public void bindVectorJ21GpioService(
			IVectorJ21GpioService vectorJ21GpioService) {
		
		s_logger.info("bindVectorJ21GpioService()");
		m_vectorJ21GpioService = vectorJ21GpioService;
	}
    
    @Override
    public String getModel() throws KuraException {
    	synchronized (m_atLock) {
	    	if (m_model == null) {
	    		s_logger.debug("sendCommand getModelNumber :: " + TelitHe910AtCommands.getModelNumber.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.getModelNumber.getCommand().getBytes(), 1000, 100);
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
		    	s_logger.debug("sendCommand getManufacturer :: " + TelitHe910AtCommands.getManufacturer.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
		    	try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.getManufacturer.getCommand().getBytes(), 1000, 100);
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
	    		s_logger.debug("sendCommand getSerialNumber :: " + TelitHe910AtCommands.getSerialNumber.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSerialNumber.getCommand().getBytes(), 1000, 100);
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
    	synchronized (m_atLock) {
	    	if (m_revisionId == null) {
	    		s_logger.debug("sendCommand getRevision :: " + TelitHe910AtCommands.getRevision.getCommand());
	    		byte [] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
	    		try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.getRevision.getCommand().getBytes(), 1000, 100);
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
    	synchronized (m_atLock) {
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		ret = isAtReachable(commAtConnection);
    		closeSerialPort(commAtConnection);
    	}
		return ret;
	}
    
    @Override
    public boolean isSimCardReady() throws KuraException {
    	boolean simReady = false;
    	synchronized (m_atLock) {
    		s_logger.debug("sendCommand getSimStatus :: " + TelitHe910AtCommands.getSimStatus.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			
	        if (reply != null) {
	            String simStatus = getResponseString(reply);
	            String[] simStatusSplit = simStatus.split(",");
	            if((simStatusSplit.length > 1) && (Integer.valueOf(simStatusSplit[1]) > 0)) {
	                simReady = true;
	            } 
	        }
	        
	        if (!simReady) {
	        	try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.simulateSimNotInserted.getCommand().getBytes(), 1000, 100);
					if (reply != null) {
						sleep(5000);
						reply = commAtConnection.sendCommand(TelitHe910AtCommands.simulateSimInserted.getCommand().getBytes(), 1000, 100);
						if (reply != null) {
							sleep(1000);
							reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 1000, 100);
	
							if (reply != null) {
								String simStatus = getResponseString(reply);
								String[] simStatusSplit = simStatus.split(",");
								if ((simStatusSplit.length > 1) && (Integer.valueOf(simStatusSplit[1]) > 0)) {
									simReady = true;
								}
							}
						}
					}
	        	} catch (IOException e) {
	        		 closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
	        }
	        
	        closeSerialPort(commAtConnection);
    	}
    	return simReady;
    }

    @Override
	public void reset() throws KuraException {
		s_logger.info("resetting modem ...");
		try {
			powerOff();
			sleep(15000);
			powerOn();
			sleep(3000);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

    @Override
    public int getSignalStrength() throws KuraException {
    	
    	int rssi = -113;
    	synchronized (m_atLock) {
	    	s_logger.debug("sendCommand getSignalStrength :: " + TelitHe910AtCommands.getSignalStrength.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSignalStrength.getCommand().getBytes(), 1000, 100);
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
    		s_logger.debug("sendCommand getRegistrationStatus :: " + TelitHe910AtCommands.getRegistrationStatus.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getRegistrationStatus.getCommand().getBytes(), 1000, 100);
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
    	
    	long txCnt = 0;
    	synchronized (m_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: " + TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String [] splitPdp = null;
				String [] splitData = null;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("#GDATAVOL:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 4) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == m_pdpContext) {
									txCnt = Integer.parseInt(splitData[2]);
								}
							}
						}
					}
				}
				reply = null;
			}
    	}
        return txCnt;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
    	long rxCnt = 0;
    	synchronized (m_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: " + TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String [] splitPdp = null;
				String [] splitData = null;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("#GDATAVOL:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 4) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == m_pdpContext) {
									rxCnt = Integer.parseInt(splitData[3]);
								}
							}
						}
					}
				}
				reply = null;
			}
    	}
        return rxCnt;
    }

    @Override
    public String getServiceType() throws KuraException {
    	String serviceType = null;
    	synchronized (m_atLock) {
    		s_logger.debug("sendCommand getMobileStationClass :: " + TelitHe910AtCommands.getMobileStationClass.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);
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
    
    public String getGpsPort() throws KuraException {
    	
    	String port = null;
    	if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
    			TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
    		port = SerialModemComm.MiniGateway.getAtPort();
    	} else {
    		port = getAtPort();
    	}
    	return port;
    }
    
    public boolean isGpsSupported() throws KuraException {
    	synchronized (m_atLock) {
    		if (m_gpsSupported == null) {
	    		s_logger.debug("sendCommand isGpsSupported :: " + TelitHe910AtCommands.isGpsPowered.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
	    		
				try {
					reply = commAtConnection.sendCommand(TelitHe910AtCommands.isGpsPowered.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
				    String sReply = getResponseString(reply);
				    if((sReply != null) && !sReply.isEmpty()) {
				    	if (sReply.startsWith("$GPSP:")) {
				    		m_gpsSupported = true;
				    	}
				    }
				}
    		}
    	}
    	boolean ret = false;
    	if (m_gpsSupported != null) {
    		ret = m_gpsSupported;
    	}
    	return ret;
    }
    
    public void enableGps() throws KuraException {
    	if ((m_gpsSupported == null) || (m_gpsSupported == false)) {
    		return;
    	}
    	synchronized (m_atLock) {
    		CommConnection commAtConnection = openSerialPort(getGpsPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
   
    		byte[] reply = null;
    		try {
    			if (!isGpsPowered(commAtConnection)) {
    				s_logger.debug("sendCommand gpsPowerUp :: " + TelitHe910AtCommands.gpsPowerUp.getCommand());
    				commAtConnection.sendCommand(TelitHe910AtCommands.gpsPowerUp.getCommand().getBytes(), 1000, 100);
    			}
    			
    			s_logger.debug("sendCommand gpsEnableNMEA :: " + TelitHe910AtCommands.gpsEnableNMEA.getCommand());
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.gpsEnableNMEA.getCommand().getBytes(), 3000, 100);
    		} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    		closeSerialPort(commAtConnection);
    		
    		if (reply != null) {
			    String sReply = getResponseString(reply);
			    if((sReply != null) && !sReply.isEmpty()) {
			    	if (sReply.startsWith("CONECT"))
			    	s_logger.debug("NMEA Enabled");
			    }
    		}
    	}
    }
    
    public void disableGps() throws KuraException {
    	if ((m_gpsSupported == null) || (m_gpsSupported == false)) {
    		return;
    	}
    	synchronized (m_atLock) {
    		CommConnection commAtConnection = openSerialPort(getGpsPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
   
    		try {
    			if (!isGpsPowered(commAtConnection)) {
    				s_logger.debug("sendCommand gpsDisableNMEA :: " + TelitHe910AtCommands.gpsDisableNMEA.getCommand());
    				commAtConnection.sendCommand(TelitHe910AtCommands.gpsDisableNMEA.getCommand().getBytes(), 1000, 100);
    				
    				s_logger.debug("sendCommand gpsPowerDown :: " + TelitHe910AtCommands.gpsPowerDown.getCommand());
    				commAtConnection.sendCommand(TelitHe910AtCommands.gpsPowerDown.getCommand().getBytes(), 1000, 100);
    			}
    		} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    		closeSerialPort(commAtConnection);
    	}
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
	public ModemTechnologyType getTechnologyType() {
		return m_technologyType;
	}
	
	private boolean isGpsPowered(CommConnection commAtConnection) throws KuraException {
    	
    	boolean gpsPowered = false;
    	if ((m_gpsSupported == null) || (m_gpsSupported == false)) {
    		return false;
    	}
    	
    	s_logger.debug("sendCommand isGpsPowered :: " + TelitHe910AtCommands.isGpsPowered.getCommand());
    	byte[] reply = null;	
		try {
			reply = commAtConnection.sendCommand(TelitHe910AtCommands.isGpsPowered.getCommand().getBytes(), 1000, 100);
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		if (reply != null) {
			String sReply = getResponseString(reply);
			if((sReply != null) && !sReply.isEmpty()) {
				if (sReply.startsWith("$GPSP:")) {
			    	sReply = sReply.substring("$GPSP:".length()).trim();
			    	gpsPowered = sReply.equals("1")? true : false;
			    }
			}
		}
    	
    	return gpsPowered;
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
						TelitHe910AtCommands.at.getCommand().getBytes(), 500).length > 0);
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
    
    /*
	 * This method turns modem power off
	 */
	private void powerOff() throws KuraException {
		
		if (this.m_vectorJ21GpioService != null) {
			
			try {
				this.m_vectorJ21GpioService
					.j21pinTurnOff(IVectorJ21GpioService.J21PIN_CELL_ON_OFF);
				sleep(1000);
				this.toggle(2);
				this.m_vectorJ21GpioService
					.j21pinTurnOff(IVectorJ21GpioService.J21PIN_CELL_PWR_EN);
			} catch (Exception e) {
				throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
			}
			
			// s_logger.info("HE910 has been powered OFF on USB port - " + m_usbDevice.getUsbPort());
		}
	}
	
	/*
	 * This method turns modem power on
	 */
	private void powerOn() throws KuraException {

		if (this.m_vectorJ21GpioService != null) {

			try {
				this.m_vectorJ21GpioService
						.j21pinTurnOff(IVectorJ21GpioService.J21PIN_CELL_ON_OFF);
				this.m_vectorJ21GpioService
						.j21pinTurnOn(IVectorJ21GpioService.J21PIN_CELL_PWR_EN);
				sleep(1000);
				this.toggle(5);
			} catch (Exception e) {
				throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
			}

			// s_logger.info("HE910 has been powered ON on USB port - " + m_usbDevice.getUsbPort());
		}
	}
	
	/*
	 * This method toggles J21 pin 8 (CELL_ON/OFF)
	 */
	private void toggle(int hold) throws KuraException {

		if (this.m_vectorJ21GpioService != null) {
			try {
				this.m_vectorJ21GpioService
					.j21pinTurnOn(IVectorJ21GpioService.J21PIN_CELL_ON_OFF);
				
				sleep(hold * 1000);
				
				this.m_vectorJ21GpioService
						.j21pinTurnOff(IVectorJ21GpioService.J21PIN_CELL_ON_OFF);
			} catch (Exception e) {
				throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}
    
    private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}
}
