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
package org.eclipse.kura.net.admin.modem.telit.de910;

import java.io.IOException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SerialModemComm;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.modem.ModemCdmaServiceProvider;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitDe910 implements EvdoCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(TelitDe910.class);
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.name");
	
	private IVectorJ21GpioService m_vectorJ21GpioService = null;
	private ConnectionFactory m_connectionFactory = null;
	private ModemTechnologyType m_technologyType = null;
	private String m_model = null;
	private String m_manufacturer = null;
	private String m_esn = null;
	private String m_revisionId = null;
	
	private Object m_atLock = null; 
	private ModemDevice m_device = null;
	private Boolean m_gpsSupported = null;
	
	private List<NetConfig> m_netConfigs = null;
	
	public TelitDe910(ModemDevice device, ConnectionFactory connectionFactory,
			ModemTechnologyType technologyType) {
		
        m_device = device;
        m_connectionFactory = connectionFactory;
        m_technologyType = technologyType;
        m_atLock = new Object();
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
	    		s_logger.debug("sendCommand getModelNumber :: " + TelitDe910AtCommands.getModelNumber.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitDe910AtCommands.getModelNumber.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
					m_model = getResponseString(reply);
					reply = null;
				}
				closeSerialPort(commAtConnection);
	    	}
    	}
        return m_model;
	}
	
	@Override
	public String getManufacturer() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_manufacturer == null) {
		    	s_logger.debug("sendCommand getManufacturer :: " + TelitDe910AtCommands.getManufacturer.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
		    	try {
					reply = commAtConnection.sendCommand(TelitDe910AtCommands.getManufacturer.getCommand().getBytes(), 1000, 100);
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
	    	if (m_esn == null) {
	    		s_logger.debug("sendCommand getSerialNumber :: " + TelitDe910AtCommands.getSerialNumber.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSerialNumber.getCommand().getBytes(), 1000, 100);
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
				    	m_esn = serialNum;        
				    }
				}
	    	}
    	}
        return m_esn;
	}

	@Override
	public String getRevisionID() throws KuraException {
		synchronized (m_atLock) {
	    	if (m_revisionId == null) {
	    		s_logger.debug("sendCommand getRevision :: " + TelitDe910AtCommands.getRevision.getCommand());
	    		byte [] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
	    		try {
					reply = commAtConnection.sendCommand(TelitDe910AtCommands.getRevision.getCommand().getBytes(), 1000, 100);
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
	    	s_logger.debug("sendCommand getSignalStrength :: " + TelitDe910AtCommands.getSignalStrength.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSignalStrength.getCommand().getBytes(), 1000, 100);
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
    		s_logger.debug("sendCommand getRegistrationStatus :: " + TelitDe910AtCommands.getNetRegistrationStatus.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getNetRegistrationStatus.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
	        if (reply != null) {
	            String sRegStatus = getResponseString(reply);
	            if (sRegStatus.startsWith("+CREG:")) {
	            	sRegStatus = sRegStatus.substring("+CREG:".length()).trim();
	            }
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
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: " + TelitDe910AtCommands.getSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(), 1000, 100);
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
								txCnt = Integer.parseInt(splitData[2]);
							}
							break;
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
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: " + TelitDe910AtCommands.getSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(), 1000, 100);
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
								rxCnt = Integer.parseInt(splitData[3]);
							}
							break;
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
    		s_logger.debug("sendCommand getServiceType :: " + TelitDe910AtCommands.getServiceType.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getServiceType.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String sServiceType = this.getResponseString(reply);
				if (sServiceType.startsWith("+SERVICE:")) {
					sServiceType = sServiceType.substring("+SERVICE:".length()).trim();
					int servType = Integer.parseInt(sServiceType);
					switch (servType) {
					case 0:
						serviceType = "No Service";
						break;
					case 1:
						serviceType = "1xRTT";
						break;
					case 2:
						serviceType = "EVDO Release 0";
						break;
					case 3:
						serviceType = "EVDO Release A";
						break;
					case 4:
						serviceType = "GPRS";
						break;
					}
				}
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
	    		if (usbModemInfo != null) {
	    			port = ports.get(usbModemInfo.getDataPort());
	    		} else {
	    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
	    		}
	    	} else if (m_device instanceof SerialModemDevice) {
	    		// TODO
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
	    		// TODO
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
    	if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
    			TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
    		port = SerialModemComm.MiniGateway.getAtPort();
    	} else {
    		port = getAtPort();
    	}
    	return port;
	}

	@Override
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

	@Override
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

	@Override
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
	public String getMobileDirectoryNumber() throws KuraException {
		
		String sMdn = null;
		synchronized (m_atLock) {
	    
			s_logger.debug("sendCommand getMdn :: " + TelitDe910AtCommands.getMdn.getCommand());
			byte[] reply = null;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
			}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMdn.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				sMdn = getResponseString(reply);
				if (sMdn.startsWith("#MODEM:")) {
					sMdn = sMdn.substring("#MODEM:".length()).trim();
				}
			}
    	}
        return sMdn;
	}
	
	@Override
	public String getMobileIdentificationNumber() throws KuraException {
		
		String sMsid = null;
		synchronized (m_atLock) {
	    
			s_logger.debug("sendCommand getMdn :: " + TelitDe910AtCommands.getMsid.getCommand());
			byte[] reply = null;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
			}
			try {
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMsid.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				sMsid = getResponseString(reply);
				if (sMsid.startsWith("#MODEM:")) {
					sMsid = sMsid.substring("#MODEM:".length()).trim();
				}
			}
    	}
        return sMsid;
	}
	
	@Override
	public ModemCdmaServiceProvider getServiceProvider() throws KuraException {
	
		ModemCdmaServiceProvider cdmaSerciceProvider = ModemCdmaServiceProvider.UNKNOWN;
		if (m_revisionId == null) {
			getRevisionID();
		}
		if ((m_revisionId != null) && (m_revisionId.length() >= 9)) {
			int provider = Integer.parseInt(m_revisionId.substring(7, 8));
			
			if (provider == TelitDe910ServiceProviders.SPRINT.getProvider()) {
				cdmaSerciceProvider = ModemCdmaServiceProvider.SPRINT;
			} else if (provider == TelitDe910ServiceProviders.AERIS.getProvider()) {
				cdmaSerciceProvider = ModemCdmaServiceProvider.AERIS;
			} else if (provider == TelitDe910ServiceProviders.VERIZON.getProvider()) {
				cdmaSerciceProvider = ModemCdmaServiceProvider.VERIZON;
			}
		}
		return cdmaSerciceProvider;
	}

	@Override
	public boolean isProvisioned() throws KuraException {
		boolean ret = false;
		String mdn = this.getMobileDirectoryNumber();
		if ((mdn != null) && (mdn.length() > 4)) {
			if (!mdn.startsWith("0000")) {
				ret = true;
			}
		}
		return ret;
	}

	@Override
	public void provision() throws KuraException {
		
		if (this.getServiceProvider() == ModemCdmaServiceProvider.VERIZON) {
			
			s_logger.info("will make an attempt to provision DE910-DUAL modem on VERIZON network");
		
			boolean startOTASPsession = false;
			ModemRegistrationStatus regStatus = getRegistrationStatus();
			if (regStatus == ModemRegistrationStatus.REGISTERED_ROAMING) {
				s_logger.warn("The DE910-DUAL cannot typically be fully provisioned while roaming");
				startOTASPsession = true;
			} else if (regStatus == ModemRegistrationStatus.REGISTERED_HOME) {
				s_logger.info("The DE910-DUAL is registered on the network");
				startOTASPsession = true;
			} else if (regStatus == ModemRegistrationStatus.NOT_REGISTERED) {
				s_logger.warn("The DE910-DUAL is not registered on the network, provision session aborted");
			} else {
				s_logger.error("Unsupported network registration status, provision session aborted");
			}
			
			if (startOTASPsession) {
				s_logger.info("Starting 'OTASP' provision session");
				CommConnection commAtConnection = openSerialPort(getAtPort());
				if (!isAtReachable(commAtConnection)) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
				}
				try {
					commAtConnection.sendCommand(TelitDe910AtCommands.provisionVerizon.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
			}
			
			s_logger.info("waiting for OTASP session to complete ...");
			sleep (180000);
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
				connection = (CommConnection) m_connectionFactory.createConnection(uri, 1, false);
			} catch (Exception e) {
				s_logger.debug("Exception creating connection: " + e);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		return connection;
    }
    
	private void closeSerialPort(CommConnection connection) throws KuraException {
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
						TelitDe910AtCommands.at.getCommand().getBytes(), 500).length > 0);
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
			
			// s_logger.info("DE910 has been powered OFF on USB port - " + m_usbDevice.getUsbPort());
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

			// s_logger.info("DE910 has been powered ON on USB port - " + m_usbDevice.getUsbPort());
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
