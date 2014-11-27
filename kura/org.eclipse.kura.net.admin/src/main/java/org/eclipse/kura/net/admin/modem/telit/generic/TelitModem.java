package org.eclipse.kura.net.admin.modem.telit.generic;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SerialModemComm;
import org.eclipse.kura.linux.net.modem.SerialModemDriver;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModems;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitModem {
	
	private static final Logger s_logger = LoggerFactory.getLogger(TelitModem.class);
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	protected static Object s_atLock = new Object(); 
	
	protected String m_model;
	protected String m_manufacturer;
	protected String m_serialNumber;
	protected String m_revisionId;
	protected int m_rssi;
	protected Boolean m_gpsSupported;
	protected String m_imsi;
	protected String m_iccid;
	
	private boolean m_gpsEnabled;
	private ModemDevice m_device;
	private String m_platform;
	private ConnectionFactory m_connectionFactory;
	private ModemTechnologyType m_technologyType;
	private List<NetConfig> m_netConfigs = null;
	
	public TelitModem(ModemDevice device, String platform,
			ConnectionFactory connectionFactory,
			ModemTechnologyType technologyType) {
		
		m_device = device;
		m_platform = platform;
		m_connectionFactory = connectionFactory;
		m_technologyType = technologyType;
		m_gpsEnabled = false;
	}
	
	public void reset() throws KuraException {

		boolean status = false;
		int resetAttempts = 5;

		sleep(5000);
		while (resetAttempts > 0) {
			try {
				status = turnOff();
				if (status) {
					status = turnOn();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (status) {
				s_logger.info("reset() :: modem reset successful");
				break;
			} else {
				resetAttempts--;
				s_logger.info("reset() :: modem reset failed, attempts left: {}", resetAttempts);
				sleep(1000);
			}
		}
	}
	
	public String getModel() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_model == null) {
	    		s_logger.debug("sendCommand getModelNumber :: " + TelitModemAtCommands.getModelNumber.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getModelNumber.getCommand().getBytes(), 1000, 100);
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
	
	public String getManufacturer() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_manufacturer == null) {
		    	s_logger.debug("sendCommand getManufacturer :: " + TelitModemAtCommands.getManufacturer.getCommand());
		    	byte[] reply = null;
		    	CommConnection commAtConnection = openSerialPort(getAtPort());
		    	if (!isAtReachable(commAtConnection)) {
		    		closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
		    	try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getManufacturer.getCommand().getBytes(), 1000, 100);
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
	
	public String getSerialNumber() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_serialNumber == null) {
	    		s_logger.debug("sendCommand getSerialNumber :: " + TelitModemAtCommands.getSerialNumber.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getSerialNumber.getCommand().getBytes(), 1000, 100);
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
	
	public String getRevisionID() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_revisionId == null) {
	    		s_logger.debug("sendCommand getRevision :: " + TelitModemAtCommands.getRevision.getCommand());
	    		byte [] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
	    		try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getRevision.getCommand().getBytes(), 1000, 100);
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
    
	public boolean isReachable() throws KuraException {
    	boolean ret = false;
    	synchronized (s_atLock) {
    		CommConnection commAtConnection = openSerialPort(getAtPort());
    		ret = isAtReachable(commAtConnection);
    		closeSerialPort(commAtConnection);
    	}
		return ret;
	}
	
	public int getSignalStrength() throws KuraException {
    	
    	int signalStrength = -113;
    	synchronized (s_atLock) {
    		String atPort = getAtPort();
    		String gpsPort = getGpsPort();
			if ((atPort.equals(getDataPort()) || (atPort.equals(gpsPort)
					&& m_gpsEnabled)) && (m_rssi < 0)) {
				return m_rssi;
			}
	    	s_logger.debug("sendCommand getSignalStrength :: " + TelitModemAtCommands.getSignalStrength.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(atPort);
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
			try {
				reply = commAtConnection.sendCommand(TelitModemAtCommands.getSignalStrength.getCommand().getBytes(), 1000, 100);
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
	
    public boolean isGpsSupported() throws KuraException {
    	synchronized (s_atLock) {
    		if (m_gpsSupported == null) {
	    		s_logger.debug("sendCommand isGpsSupported :: " + TelitModemAtCommands.isGpsPowered.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
	    		
				try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.isGpsPowered.getCommand().getBytes(), 1000, 100);
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
    		m_gpsEnabled = false;
    		return;
    	}
    	synchronized (s_atLock) {
    		CommConnection commAtConnection = openSerialPort(getGpsPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
   
    		byte[] reply = null;
    		try {
    			if (!isGpsPowered(commAtConnection)) {
    				s_logger.debug("sendCommand gpsPowerUp :: " + TelitModemAtCommands.gpsPowerUp.getCommand());
    				commAtConnection.sendCommand(TelitModemAtCommands.gpsPowerUp.getCommand().getBytes(), 1000, 100);
    			}
    			
    			s_logger.debug("sendCommand gpsEnableNMEA :: " + TelitModemAtCommands.gpsEnableNMEA.getCommand());
				reply = commAtConnection.sendCommand(TelitModemAtCommands.gpsEnableNMEA.getCommand().getBytes(), 3000, 100);
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
    	
    	m_gpsEnabled = true;
    }
    
    public void disableGps() throws KuraException {
    	if ((m_gpsSupported == null) || (m_gpsSupported == false)) {
    		m_gpsEnabled = false;
    		return;
    	}
    	synchronized (s_atLock) {
    		CommConnection commAtConnection = openSerialPort(getGpsPort());
    		if (!isAtReachable(commAtConnection)) {
    			closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	}
   
    		try {
    			if (!isGpsPowered(commAtConnection)) {
    				s_logger.debug("sendCommand gpsDisableNMEA :: " + TelitModemAtCommands.gpsDisableNMEA.getCommand());
    				commAtConnection.sendCommand(TelitModemAtCommands.gpsDisableNMEA.getCommand().getBytes(), 1000, 100);
    				
    				s_logger.debug("sendCommand gpsPowerDown :: " + TelitModemAtCommands.gpsPowerDown.getCommand());
    				commAtConnection.sendCommand(TelitModemAtCommands.gpsPowerDown.getCommand().getBytes(), 1000, 100);
    			}
    		} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
    		closeSerialPort(commAtConnection);
    	}
    	
    	m_gpsEnabled = false;
    }
    
    public String getMobileSubscriberIdentity() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_imsi == null) {
	    		s_logger.debug("sendCommand getIMSI :: " + TelitModemAtCommands.getIMSI.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getIMSI.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
				    String imsi = getResponseString(reply);
				    if(imsi != null && !imsi.isEmpty()) {
				    	if (imsi.startsWith("#CIMI:")) {
				    		imsi = imsi.substring("#CIMI:".length()).trim();
				    	}
				    	m_imsi = imsi;        
				    }
				}
	    	}
    	}
        return m_imsi;
    }
    
    public String getIntegratedCirquitCardId() throws KuraException {
    	synchronized (s_atLock) {
	    	if (m_iccid == null) {
	    		s_logger.debug("sendCommand getICCID :: " + TelitModemAtCommands.getICCID.getCommand());
	    		byte[] reply = null;
	    		CommConnection commAtConnection = openSerialPort(getAtPort());
	    		if (!isAtReachable(commAtConnection)) {
	    			closeSerialPort(commAtConnection);
		    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
		    	}
				try {
					reply = commAtConnection.sendCommand(TelitModemAtCommands.getICCID.getCommand().getBytes(), 1000, 100);
				} catch (IOException e) {
					closeSerialPort(commAtConnection);
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				closeSerialPort(commAtConnection);
				if (reply != null) {
				    String iccid = getResponseString(reply);
				    if(iccid != null && !iccid.isEmpty()) {
				    	if (iccid.startsWith("#CCID:")) {
				    		iccid = iccid.substring("#CCID:".length()).trim();
				    	}
				    	m_iccid = iccid;        
				    }
				}
	    	}
    	}
        return m_iccid;
    }
    
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
	
    public ModemDevice getModemDevice() {
        return m_device;
    }
    
    public List<NetConfig> getConfiguration() {
		return m_netConfigs;
	}

	public void setConfiguration(List<NetConfig> netConfigs) {
		m_netConfigs = netConfigs;
	}
	
	public ModemTechnologyType getTechnologyType() {
		return m_technologyType;
	}
	
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
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
		}
		return connection;
    }
    
    protected void closeSerialPort (CommConnection connection) throws KuraException {
		try {
			connection.close();
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
    }
    
    
    protected boolean isAtReachable(CommConnection connection) {
        
    	boolean status = false;
    	int attemptNo = 0;
    	do {
			try {
				status = (connection.sendCommand(TelitModemAtCommands.at.getCommand().getBytes(), 1000, 100).length > 0);
			} catch (Exception e) {
				attemptNo++;
				sleep(2000);
			}
    	} while((status == false) && (attemptNo < 3));
    	
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
    
	private boolean isGpsPowered(CommConnection commAtConnection) throws KuraException {
    	
    	boolean gpsPowered = false;
    	if ((m_gpsSupported == null) || (m_gpsSupported == false)) {
    		return false;
    	}
    	
    	s_logger.debug("sendCommand isGpsPowered :: " + TelitModemAtCommands.isGpsPowered.getCommand());
    	byte[] reply = null;	
		try {
			reply = commAtConnection.sendCommand(TelitModemAtCommands.isGpsPowered.getCommand().getBytes(), 1000, 100);
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
	
	private boolean isOn() throws Exception {

		boolean isModemOn = false;
		if (m_device instanceof UsbModemDevice) {
			isModemOn = SupportedUsbModems.isAttached(
					((UsbModemDevice) m_device).getVendorId(),
					((UsbModemDevice) m_device).getProductId());
			s_logger.info("isOn() :: USB modem attached? {}", isModemOn);
		} else if (m_device instanceof SerialModemDevice) {
			isModemOn = isReachable();
			s_logger.info("isOn() :: Serial modem reachable? {}", isModemOn);
		} else {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
					"Unsupported modem device");
		}
		return isModemOn;
	}
	
	private boolean turnOff() throws Exception {

		boolean retVal = true;
		int remainingAttempts = 5;
		do {
			if (remainingAttempts <= 0) {
				retVal = false;
				break;
			}
			s_logger.info("turnOff() :: turning modem OFF ... attempts left: {}", remainingAttempts);
			if (m_platform.equals("Mini-Gateway")) {
				SerialModemDriver.toggleGpio65();
			} else if (m_platform.equals("reliagate-10-20")) {
				FileWriter fw = new FileWriter("/sys/class/gpio/usb-rear-pwr/value");
				fw.write("0");
				fw.close();
			} else if (m_platform.equals("reliagate-50-21")) {
				Runtime rt = Runtime.getRuntime();
				Process pr = rt.exec("/usr/sbin/vector-j21-gpio 11 0");
				int status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 0' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				pr = rt.exec("/usr/sbin/vector-j21-gpio 11 1");
				status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 1' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(3000);

				pr = rt.exec("/usr/sbin/vector-j21-gpio 11 0");
				status = pr.waitFor();
				s_logger.info("turnOff() :: '/usr/sbin/vector-j21-gpio 11 0' returned {}", status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnOff() :: modem turnOff operation is not supported for the {} platform", m_platform);
			}
			remainingAttempts--;
			sleep(5000);
		} while (isOn());

		s_logger.info("turnOff() :: Modem is OFF? - {}", retVal);
		return retVal;
	}
	
	private boolean turnOn() throws Exception {

		boolean retVal = true;
		int remainingAttempts = 5;

		do {
			if (remainingAttempts <= 0) {
				retVal = false;
				break;
			}
			s_logger.info("turnOn() :: turning modem ON ... attempts left: {}", remainingAttempts);
			if (m_platform.equals("Mini-Gateway")) {
				SerialModemDriver.toggleGpio65();
			} else if (m_platform.equals("reliagate-10-20")) {
				FileWriter fw = new FileWriter("/sys/class/gpio/usb-rear-pwr/value");
				fw.write("1");
				fw.close();
			} else if (m_platform.equals("reliagate-50-21")) {
				Runtime rt = Runtime.getRuntime();
				Process pr = rt.exec("/usr/sbin/vector-j21-gpio 11 1");
				int status = pr.waitFor();
				s_logger.info("turnOn() :: '/usr/sbin/vector-j21-gpio 11 1' returned {}", status);
				if (status != 0) {
					continue;
				}
				sleep(1000);

				pr = rt.exec("/usr/sbin/vector-j21-gpio 6");
				status = pr.waitFor();
				s_logger.info("turnOn() :: '/usr/sbin/vector-j21-gpio 6' returned {}", status);
				retVal = (status == 0) ? true : false;
			} else {
				s_logger.warn("turnOn() :: modem turnOn operation is not supported for the {} platform", m_platform);
			}
			remainingAttempts--;
			sleep(5000);
		} while (!isOn());

		s_logger.info("turnOn() :: Modem is ON? - {}", retVal);
		return retVal;
	}
}
