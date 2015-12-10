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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemCdmaServiceProvider;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemReadyService;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SimCardSlot;
import org.eclipse.kura.net.modem.SubscriberInfo;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitDe910 extends TelitModem implements EvdoCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(TelitDe910.class);
		
	private ScheduledExecutorService m_executorUtil;
	
	 /**
     * TelitDe910 modem constructor
     * 
     * @param usbDevice - modem USB device as {@link UsbModemDevice}
     * @param platform - hardware platform as {@link String}
     * @param connectionFactory - connection factory {@link ConnectionFactory}
     */
	public TelitDe910(ModemDevice device, String platform,
			ConnectionFactory connectionFactory) {
		
		super(device, platform, connectionFactory);
		m_executorUtil = Executors.newSingleThreadScheduledExecutor();
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
					m_rssi = getSignalStrength();
					s_logger.trace("TelitDe910() :: Serial Number={}", m_serialNumber);
					s_logger.trace("TelitDe910() :: Model={}", m_model);
					s_logger.trace("TelitDe910() :: Manufacturer={}", m_manufacturer);
					s_logger.trace("TelitDe910() :: Revision ID={}", m_revisionId);
					s_logger.trace("TelitDe910() :: GPS Supported={}", m_gpsSupported);
					s_logger.trace("TelitDe910() :: RSSI={}", m_rssi);
				}
			}
		} catch (KuraException e) {
			s_logger.error("failed to initialize Telit DE910 modem - {}", e);
		}
    }
	
	@Override
	public String getIntegratedCirquitCardId() throws KuraException {
		return "";
	}
	
	@Override
	public String getMobileSubscriberIdentity() throws KuraException {
		return getMobileSubscriberIdentity(0);
	}
	
	@Override
	public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
		ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getRegistrationStatus :: {}", TelitDe910AtCommands.getNetRegistrationStatus.getCommand());
    		CommConnection commAtConnection = null;
    		byte[] reply = null;
    		try {
	    		commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getNetRegistrationStatus.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
    		try {
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
    		} catch (Exception e) {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
    		}
    	}
        return modemRegistrationStatus;
	}

	@Override
	public long getCallTxCounter() throws KuraException {
		long txCnt = 0;
    	synchronized (s_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", TelitDe910AtCommands.getSessionDataVolume.getCommand());
	    	CommConnection commAtConnection = null;
	    	byte[] reply = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
		    try {
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
		    } catch (Exception e) {
		    	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		    }
    	}
        return txCnt;
	}

	@Override
	public long getCallRxCounter() throws KuraException {
		long rxCnt = 0;
    	synchronized (s_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", TelitDe910AtCommands.getSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
			try {
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
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    	}
        return rxCnt;
	}

	@Override
	public String getServiceType() throws KuraException {
		String serviceType = null;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getServiceType :: {}", TelitDe910AtCommands.getServiceType.getCommand());
    		CommConnection commAtConnection = null;
	    	byte[] reply = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getServiceType.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
			try {
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
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    	}
		return serviceType;
	}
	
	@Override
	public String getMobileDirectoryNumber() throws KuraException {
		
		String sMdn = null;
		synchronized (s_atLock) {
			s_logger.debug("sendCommand getMdn :: {}", TelitDe910AtCommands.getMdn.getCommand());
			CommConnection commAtConnection = null;
			byte[] reply = null;
			try {
				commAtConnection = openSerialPort(getAtPort());
				if (!isAtReachable(commAtConnection)) {
					throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
				}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMdn.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
			try {
				if (reply != null) {
					sMdn = getResponseString(reply);
					if (sMdn.startsWith("#MODEM:")) {
						sMdn = sMdn.substring("#MODEM:".length()).trim();
					}
				}
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    	}
		return sMdn;
	}
	
	@Override
	public String getMobileIdentificationNumber() throws KuraException {
		
		String sMsid = null;
		synchronized (s_atLock) {
			s_logger.debug("sendCommand getMsid :: {}", TelitDe910AtCommands.getMsid.getCommand());
			CommConnection commAtConnection = null;
			byte[] reply = null;
			try {
				commAtConnection = openSerialPort(getAtPort());
				if (!isAtReachable(commAtConnection)) {
					throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
				}
				reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMsid.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
			try {
				if (reply != null) {
					sMsid = getResponseString(reply);
					if (sMsid.startsWith("#MODEM:")) {
						sMsid = sMsid.substring("#MODEM:".length()).trim();
					}
				}
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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
				CommConnection commAtConnection = null;
				try {
					commAtConnection = openSerialPort(getAtPort());
					if (!isAtReachable(commAtConnection)) {
						closeSerialPort(commAtConnection);
						throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitDe910.class.getName());
					}
					commAtConnection.sendCommand(TelitDe910AtCommands.provisionVerizon.getCommand().getBytes(), 1000, 100);
				} catch (Exception e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				} finally {
					closeSerialPort(commAtConnection);
				}
			}
			
			s_logger.info("waiting for OTASP session to complete ...");
			sleep (180000);
		}
	}

	@Override
	public boolean isSimCardReady() throws KuraException {
		return true;
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
	public SubscriberInfo [] getSubscriberInfo(boolean refreshActiveSimInfo) throws KuraException {
		return m_subscriberInfo;
	}
	
	@Override
	public SubscriberInfo [] obtainSubscriberInfo(SimCardSlot cfgSimCardSlot, int execDelay, ModemReadyService callback) {
		final SimCardSlot simCardSlot = cfgSimCardSlot;
		final ModemReadyService modemReadyService = callback;
		final CellularModem modem = this;
		m_executorUtil.schedule(new Runnable() {
    		@Override
    		public void run() {
    			try {
					m_subscriberInfo = obtainSubscriberInfo(simCardSlot);
					modemReadyService.postModemReadyEvent(modem);
				} catch (KuraException e) {
					s_logger.error("failed to obtain subscriber info for Telit modem - {}", e);
				}
    		}
    	}, execDelay, TimeUnit.MILLISECONDS);
		return m_subscriberInfo;
	}
	
	@Override
	public SubscriberInfo [] obtainSubscriberInfo(SimCardSlot activeSimCardSlot) throws KuraException {
		SubscriberInfo [] ret = new SubscriberInfo [1]; ret[0] = null; 
		ret[0] = new SubscriberInfo(getMobileSubscriberIdentity(0), 
				getIntegratedCirquitCardId(0),
				getSubscriberNumber(0));
		return ret;
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
}
