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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemReadyService;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.net.modem.SimCardSlot;
import org.eclipse.kura.net.modem.SubscriberInfo;
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
	
	private ScheduledExecutorService m_executorUtil;
	
	private static final Object s_simLock = new Object();

    /**
     * TelitHe910 modem constructor
     * 
     * @param usbDevice - modem USB device as {@link UsbModemDevice}
     * @param platform - hardware platform as {@link String}
     * @param connectionFactory - connection factory {@link ConnectionFactory}
     */
	public TelitHe910(ModemDevice device, String platform,
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
					s_logger.trace("{} :: Serial Number={}", getClass().getName(), m_serialNumber);
					s_logger.trace("{} :: Model={}", getClass().getName(), m_model);
					s_logger.trace("{} :: Manufacturer={}", getClass().getName(), m_manufacturer);
					s_logger.trace("{} :: Revision ID={}", getClass().getName(), m_revisionId);
					s_logger.trace("{} :: GPS Supported={}", getClass().getName(), m_gpsSupported);
					s_logger.trace("{} :: RSSI={}", getClass().getName(), m_rssi);
				}
			}
		} catch (KuraException e) {
			s_logger.error("failed to initialize Telit HE910 modem - {}", e);
		}
    }
	
	@Override
	public String getMobileSubscriberIdentity() throws KuraException {
		return getMobileSubscriberIdentity(getSimCardSlot().getValue());
	}

	@Override
	public String getIntegratedCirquitCardId() throws KuraException {
		return getIntegratedCirquitCardId(getSimCardSlot().getValue());
	}
	
	@Override
	public SubscriberInfo [] getSubscriberInfo(boolean refreshActiveSimInfo) throws KuraException {
		if (refreshActiveSimInfo) {
			SimCardSlot simCardSlot = getSimCardSlot();
			if (simCardSlot == SimCardSlot.A) {
				m_subscriberInfo[0].setActive(true);
				m_subscriberInfo[1].setActive(false);
			} else if (simCardSlot == SimCardSlot.B) {
				m_subscriberInfo[0].setActive(false);
				m_subscriberInfo[1].setActive(true);
			}
		}
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
					m_subscriberInfo = obtainSubscriberInfoPrivate(simCardSlot);
					modemReadyService.postModemReadyEvent(modem);
				} catch (KuraException e) {
					s_logger.error("failed to obtain subscriber info for Telit modem - {}", e);
				}
    		}
    	}, execDelay, TimeUnit.MILLISECONDS);
		return m_subscriberInfo;
	}
	
	@Override
	public SubscriberInfo [] obtainSubscriberInfo(SimCardSlot cfgSimCardSlot) throws KuraException {
		m_subscriberInfo = obtainSubscriberInfoPrivate(cfgSimCardSlot);
		return m_subscriberInfo;
	}
	
	private SubscriberInfo [] obtainSubscriberInfoPrivate(SimCardSlot cfgSimCardSlot) throws KuraException {
		SubscriberInfo [] ret = new SubscriberInfo [2];
		synchronized (s_simLock) {
			ret[0] = new SubscriberInfo(); ret[1] = new SubscriberInfo();
			SimCardSlot simSlot = getSimCardSlot();
			s_logger.debug("obtainSubscriberInfo() :: original simSlot={}", simSlot);
			if (simSlot == SimCardSlot.A) {
				if (isSimCardReady()) {
					ret[0] = new SubscriberInfo(
								getMobileSubscriberIdentity(SimCardSlot.A.getValue()), 
								getIntegratedCirquitCardId(SimCardSlot.A.getValue()),
								getSubscriberNumber(SimCardSlot.A.getValue()));
				}
				s_logger.debug("obtainSubscriberInfo() :: switching to SIM Slot {}", SimCardSlot.B);
				if (setSimCardSlot(SimCardSlot.B)) {
					sleep(9000);
					SubscriberInfo subscriberInfo = new SubscriberInfo(
							getMobileSubscriberIdentity(SimCardSlot.B.getValue()), 
							getIntegratedCirquitCardId(SimCardSlot.B.getValue()),
							getSubscriberNumber(SimCardSlot.B.getValue()));	
					if (!subscriberInfo.equals(ret[0])) {
						ret[1] = subscriberInfo;
					}
				}
			} else if (simSlot == SimCardSlot.B) {
				if (isSimCardReady()) {
					ret[1] = new SubscriberInfo(
							getMobileSubscriberIdentity(SimCardSlot.B.getValue()), 
							getIntegratedCirquitCardId(SimCardSlot.B.getValue()),
							getSubscriberNumber(SimCardSlot.B.getValue()));
				}
				s_logger.debug("obtainSubscriberInfo() :: switching to SIM Slot {}", SimCardSlot.A);
				if (setSimCardSlot(SimCardSlot.A)) {
					sleep(9000);
					SubscriberInfo subscriberInfo = new SubscriberInfo(
							getMobileSubscriberIdentity(SimCardSlot.A.getValue()), 
							getIntegratedCirquitCardId(SimCardSlot.A.getValue()),
							getSubscriberNumber(SimCardSlot.A.getValue()));
					if (!subscriberInfo.equals(ret[1])) {
						ret[0] = subscriberInfo;
					}
				}
			}
			if (cfgSimCardSlot != null) {
				simSlot = getSimCardSlot();
				if (simSlot != cfgSimCardSlot) {
					s_logger.debug("obtainSubscriberInfo() :: switching to configured SIM Slot {}", cfgSimCardSlot);
					if(setSimCardSlot(cfgSimCardSlot)) {
						simSlot = cfgSimCardSlot;
					}
				}
			} else {
				s_logger.debug("obtainSubscriberInfo() :: switching to original SIM Slot {}",simSlot);
				setSimCardSlot(simSlot);
			}
			
			if (simSlot == SimCardSlot.A) {
				ret[0].setActive(true);
			} else if (simSlot == SimCardSlot.B) {
				ret[1].setActive(true);
			}
		}
		return ret;
	}
	
	@Override
	public SimCardSlot getSimCardSlot() throws KuraException {
		SimCardSlot simCardSlot = null;
		String port = null;
		
		if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
			port = getDataPort();
		} else {
			port = getAtPort();
		}
		synchronized (s_atLock) {
			s_logger.debug("sendCommand getCurrentSimSlot :: {} command to port {}", TelitHe910AtCommands.getCurrentSimSlot.getCommand(), port);
			byte[] reply = null;
	    	CommConnection commAtConnection = null;
	    	try {
	    		commAtConnection = openSerialPort(port);
	    		if (!isAtReachable(commAtConnection)) {	    		
	    			throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    		}   
	    		reply = commAtConnection.sendCommand(TelitHe910AtCommands.getCurrentSimSlot.getCommand().getBytes(), 500/*500, 100*/);
	    	} catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            } finally {	        
    	        closeSerialPort(commAtConnection);
        	}
	    	try {
		    	if (reply != null) {
		        	String sReply = getResponseString(reply);
		        	String [] asReply = sReply.split("\r\n");
		        	int ind = -1;
		        	if ((asReply.length >= 2) && ((ind=asReply[1].indexOf(",")) > 0)) {
		        		int simSlot = Integer.parseInt(asReply[1].substring(ind+1));
		        		simCardSlot = SimCardSlot.getSimCardSlot(simSlot, true);
		        	}
		        }
	    	} catch (Exception e) {
	    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
	    	}
		}
		return simCardSlot;
	}
	
	@Override
	public boolean setSimCardSlot(SimCardSlot simCardSlot) throws KuraException {
		
		boolean ret = false;
		String cmd = null;
		if (simCardSlot == SimCardSlot.A) {
			cmd = TelitHe910AtCommands.setSimSlotA.getCommand();
		} else if (simCardSlot == SimCardSlot.B) {
			cmd = TelitHe910AtCommands.setSimSlotB.getCommand();
		} else {
			s_logger.error("Invalid SIM card slot");
			return false;
		}
		String port = null;
		
		if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
			port = getDataPort();
		} else {
			port = getAtPort();
		}
		synchronized (s_atLock) {
			s_logger.debug("sendCommand setSimSlot{} command to port {}", simCardSlot, port);
	    	CommConnection commAtConnection = null;
	    	try {
	    		commAtConnection = openSerialPort(port);
	    		if (!isAtReachable(commAtConnection)) {	    		
	    			throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    		}
		    	commAtConnection.sendCommand(cmd.getBytes(), 500, 100);    	
	    	} catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            } finally {	        
    	        closeSerialPort(commAtConnection);
        	}
	    	if (simCardSlot == getSimCardSlot()) {
	    		ret = true;
	    		if (isSimCardReady()) {
		    		s_logger.info("setSimCardSlot() :: successfully switched to simCardSlot {}", simCardSlot);
					ret = true;
	    		}
			}
		}
		return ret;
	}
    
    @Override
	public boolean isSimCardReady() throws KuraException {
		
    	boolean simReady = false;
		String port = null;
		
		if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
			port = getDataPort();
		} else {
			port = getAtPort();
		}

    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getSimStatus :: {} command to port {}", TelitHe910AtCommands.getSimStatus.getCommand(), port);
	    	byte[] reply = null;
	    	CommConnection commAtConnection = null;
	    	try {
	    	    commAtConnection = openSerialPort(port);
	    	    if (!isAtReachable(commAtConnection)) {	    		
	    	        throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	    }
	    	    
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 500, 100/*1000, 100*/);
    	        if (reply != null) {
    	            String simStatus = getResponseString(reply);
    	            String[] simStatusSplit = simStatus.split(",");
    	            if((simStatusSplit.length > 1) && (Integer.valueOf(simStatusSplit[1]) > 0)) {
    	                simReady = true;
    	            } 
    	        }
    	        
    	        if (!simReady) {
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
	        	}
	    	} catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            } finally {	        
    	        closeSerialPort(commAtConnection);
        	}
    	}
    	return simReady;
	}
    
    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
    	
    	ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getRegistrationStatus :: {}", TelitHe910AtCommands.getRegistrationStatus.getCommand());
    		CommConnection commAtConnection = null;
	    	byte[] reply = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getRegistrationStatus.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
			try {
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
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
	    	CommConnection commAtConnection = null;
	    	byte[] reply = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
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
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
	    	CommConnection commAtConnection = null;
	    	byte[] reply = null;
	    	try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
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
    		s_logger.debug("sendCommand getMobileStationClass :: {}", TelitHe910AtCommands.getMobileStationClass.getCommand());
    		CommConnection commAtConnection = null;
    		byte[] reply = null;
    		try {
		    	commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
    		try {
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
    		} catch (Exception e) {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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
}
