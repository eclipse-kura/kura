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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines Telit HE910 modem
 */
public class TelitHe910 extends TelitModem implements HspaCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(TelitHe910.class);
	
	private int m_pdpContext = 1;

    /**
     * TelitHe910 modem constructor
     * 
     * @param usbDevice - modem USB device as {@link UsbModemDevice}
     * @param platform - hardware platform as {@link String}
     * @param connectionFactory - connection factory {@link ConnectionFactory}
     * @param technologyType - cellular technology type as {@link ModemTechnologyType}
     */
	public TelitHe910(ModemDevice device, String platform,
			ConnectionFactory connectionFactory,
			ModemTechnologyType technologyType) {
        
		super(device, platform, connectionFactory, technologyType);
        
        try {
			String atPort = getAtPort();
			String gpsPort = getGpsPort();
			if (atPort != null) {
				if (atPort.equals(getDataPort()) || atPort.equals(gpsPort)) {
					m_serialNumber = getSerialNumber();
					m_imsi = getMobileSubscriberIdentity();
					m_iccid = getIntegratedCirquitCardId();
					m_model = getModel();
					m_manufacturer = getManufacturer();		
					m_revisionId = getRevisionID();
					m_gpsSupported = isGpsSupported();
					m_rssi = getSignalStrength();
				}
			}
		} catch (KuraException e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public boolean isSimCardReady() throws KuraException {
    	boolean simReady = false;
    	synchronized (s_atLock) {
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
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
    	
    	ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
    	synchronized (s_atLock) {
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
    	synchronized (s_atLock) {
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
    	synchronized (s_atLock) {
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
    	synchronized (s_atLock) {
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
}
