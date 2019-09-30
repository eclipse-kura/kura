/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.sierra.CnS;
import org.eclipse.kura.net.admin.modem.sierra.CnsAppIDs;
import org.eclipse.kura.net.admin.modem.sierra.CnsOpTypes;
import org.eclipse.kura.net.modem.ModemCdmaServiceProvider;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.event.EventConstants;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SierraUsb598 implements EvdoCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(SierraUsb598.class);

    protected static final String MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG = "Modem not available for AT commands";

    private static String[] topicsOfInterest = null;
    private static final int HIP_PORT = 1;
    private final Object atLock = new Object();

    private ConnectionFactory connectionFactory = null;
    private String model = null;
    private String manufacturer = null;
    private String esn = null;
    private String revisionId = null;
    private int callStatus = -1; // current call status
    private ModemDevice device = null;
    private List<NetConfig> netConfigs = null;
    private CommConnection commHipConnection = null;
    private ScheduledExecutorService executor = null;
    private List<CnS> notifications = null;
    private String mdn = null; // mobile directory number
    private String min = null; // mobile identification number
    private String firmwareVersion = "";
    private String firmwareDate = "";
    private int prlVersion = 0; // PRL version

    /*
     * 'Received Signal Strength' (in dBm) and its lock
     */
    private int rssi = 0;

    /*
     * 'System ID' and its lock
     */
    private int sid = -1;

    /*
     * 'Network ID' and its lock
     */
    private int nid = -1;

    /*
     * 'Channel Number' and its lock
     */
    private int channelNo = -1;
    private static Object channelNoLock = new Object();

    /*
     * 'Channel State' and its lock
     */
    private int channelState = -1;

    /*
     * 'Current Band Class' and its lock
     */
    private int bandClass = -1;
    private static Object bandClassLock = new Object();

    private int activationStatus = -1; // activation status

    private GregorianCalendar activationDate = null; // activation date

    /*
     * 'Roaming Status' and its lock
     */
    private int roamingStatus = -1;

    /*
     * 'Service Type' and its lock
     */
    private int serviceType = -1;

    private long txCount = -1L; // number of bytes transmitted during a call
    private long rxCount = -1L; // number of bytes received during a call

    /*
     * 'Power Mode' and its lock
     */
    private int powerMode = -1;

    /**
     * SierraUsb598 modem constructor
     *
     * @param usbDevice
     *            - modem USB device as {@link UsbModemDevice}
     * @param connectionFactory
     *            - connection factory as {@link ConnectionFactory}
     * @param technologyType
     *            - cellular technology type as
     *            {@link ModemTechnologyType}
     */
    public SierraUsb598(ModemDevice device, ConnectionFactory connectionFactory) {

        this.device = device;
        this.connectionFactory = connectionFactory;
        topicsOfInterest = generateSubscribeTopics();
        // subscribe on specific topics of interest
        Dictionary d = new Hashtable();
        d.put(EventConstants.EVENT_TOPIC, topicsOfInterest);
        for (String element : topicsOfInterest) {
            logger.debug("Subscribing for {}", element);
        }
        // bundleContext.registerService(EventHandler.class.getName(), this, d);

        this.notifications = new ArrayList();

        // define notification thread
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void bind() {
        try {
            this.commHipConnection = openSerialPort(getHipPort());
            this.executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("SierraUsb598");
                    logger.debug("**** HIP Thread() run ");
                }
            }, 0, 1, TimeUnit.SECONDS);
        } catch (KuraException e) {
            logger.error("bind() :: ", e);
        }
    }

    public void unbind() {

        if (this.commHipConnection != null) {
            try {
                this.executor.shutdown();
                closeSerialPort(this.commHipConnection);
            } catch (KuraException e) {
                logger.error("unbind() :: ", e);
            }
        }
    }

    @Override
    public String getModel() throws KuraException {
        synchronized (this.atLock) {
            if (this.model == null) {
                logger.debug("sendCommand getModelNumber :: {}", SierraUsb598AtCommands.getModelNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getModelNumber.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.model = getResponseString(reply);
                }
                closeSerialPort(commAtConnection);
            }
        }
        return this.model;
    }

    @Override
    public String getManufacturer() throws KuraException {
        synchronized (this.atLock) {
            if (this.manufacturer == null) {
                logger.debug("sendCommand getManufacturer :: {}", SierraUsb598AtCommands.getManufacturer.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getManufacturer.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.manufacturer = getResponseString(reply);
                }
            }
        }
        return this.manufacturer;
    }

    @Override
    public String getSerialNumber() throws KuraException {
        synchronized (this.atLock) {
            if (this.esn != null) {
                logger.debug("sendCommand getSerialNumber :: {}", SierraUsb598AtCommands.getSerialNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getSerialNumber.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    StringBuilder replySB = new StringBuilder();
                    replySB.append(new String(reply));
                    // if response is incomplete, try to get the rest
                    if (!replySB.toString().matches(".*OK\\s*$")) {
                        sleep(200);
                        try {
                            reply = commAtConnection.flushSerialBuffer();
                        } catch (IOException e) {
                            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                        }
                        if (reply != null) {
                            replySB.append(new String(reply));
                        }
                    }

                    String serialNum = getResponseString(replySB.toString());
                    if (serialNum != null && !serialNum.isEmpty()) {
                        this.esn = serialNum;
                    }
                }
            }
        }
        return this.esn;
    }

    @Override
    public String getMobileSubscriberIdentity() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getRevisionID() throws KuraException {
        synchronized (this.atLock) {
            if (this.revisionId == null) {
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getRevision.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.revisionId = getResponseString(reply);
                }
            }
        }
        return this.revisionId;
    }

    @Override
    public boolean isReachable() throws KuraException {
        // not implemented
        return false;
    }

    @Override
    public boolean isPortReachable(String port) {
        boolean ret = false;
        synchronized (this.atLock) {
            try {
                CommConnection commAtConnection = openSerialPort(port);
                closeSerialPort(commAtConnection);
                ret = true;
            } catch (KuraException e) {
                logger.warn("isPortReachable() :: The {} is not reachable", port, e);
            }
        }
        return ret;
    }

    @Override
    public void reset() throws KuraException {
        logger.info("resetting modem ...");
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
        return 0;
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
        return null;
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
        return null;
    }

    @Override
    public ModemDevice getModemDevice() {
        return this.device;
    }

    protected void setModemDevice(ModemDevice device) {
        this.device = device;
    }

    @Override
    public String getDataPort() throws KuraException {

        String port = null;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getDataPort());
                } else {
                    throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No PPP serial port available");
                }
            } else if (this.device instanceof SerialModemDevice) {
                // TODO
            } else {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No serial ports available");
        }
        return port;
    }

    @Override
    public String getAtPort() throws KuraException {

        String port = null;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getAtPort());
                } else {
                    throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No AT serial port available");
                }
            } else if (this.device instanceof SerialModemDevice) {
                // TODO
            } else {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No serial ports available");
        }
        return port;
    }

    @Override
    public String getGpsPort() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        // not implemented
        return false;
    }

    @Override
    public void enableGps() throws KuraException {
        // not implemented
    }

    @Override
    public void disableGps() throws KuraException {
        // not implemented
    }

    @Override
    public boolean isProvisioned() throws KuraException {
        // not implemented
        return false;
    }

    @Override
    public void provision() throws KuraException {
        // not implemented
    }

    @Override
    public String getMobileDirectoryNumber() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getMobileIdentificationNumber() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public List<NetConfig> getConfiguration() {
        return this.netConfigs;
    }

    @Override
    public void setConfiguration(List<NetConfig> netConfigs) {
        this.netConfigs = netConfigs;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {

        List<ModemTechnologyType> modemTechnologyTypes;
        ModemDevice device = getModemDevice();
        if (device == null) {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No modem device");
        }
        if (device instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) device);
            if (usbModemInfo != null) {
                modemTechnologyTypes = usbModemInfo.getTechnologyTypes();
            } else {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No usbModemInfo available");
            }
        } else {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
        }
        return modemTechnologyTypes;
    }

    private String getHipPort() throws KuraException {
        String port;

        if (this.device instanceof UsbModemDevice) {
            UsbModemDevice usbModemDevice = (UsbModemDevice) this.device;
            List<String> ports = usbModemDevice.getTtyDevs();
            if (ports != null && !ports.isEmpty()) {
                port = ports.get(HIP_PORT);
            } else {
                throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No HIP serial port available");
            }
        } else {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No HIP serial port available");
        }

        return port;
    }

    @Override
    public ModemCdmaServiceProvider getServiceProvider() {
        // not implemented
        return null;
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
                throw new KuraException(KuraErrorCode.SERIAL_PORT_INVALID_CONFIGURATION, "Invalid Port Type");
            }
            if (port != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("comm:").append(port).append(";baudrate=115200;databits=8;stopbits=1;parity=0");
                commURI = CommURI.parseString(sb.toString());
            }
        } catch (URISyntaxException e) {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_INVALID_CONFIGURATION, "URI Syntax Exception");
        }
        return commURI;
    }

    @Override
    public boolean isGpsEnabled() {
        return false;
    }

    @Override
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
        return new ArrayList<>();
    }

    private CommConnection openSerialPort(String port) throws KuraException {

        CommConnection connection = null;
        if (this.connectionFactory != null) {
            String uri = new CommURI.Builder(port).withBaudRate(115200).withDataBits(8).withStopBits(1).withParity(0)
                    .withTimeout(2000).build().toString();

            try {
                connection = (CommConnection) this.connectionFactory.createConnection(uri, 1, false);
            } catch (Exception e) {
                logger.debug("Exception creating connection: {}", e);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
        }
        return connection;
    }

    private void closeSerialPort(CommConnection connection) throws KuraException {
        try {
            connection.close();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
        }
    }

    private boolean isAtReachable(CommConnection connection) {

        boolean status = false;
        int attemptNo = 0;
        do {
            try {
                status = connection.sendCommand(SierraUsb598AtCommands.at.getCommand().getBytes(), 500).length > 0;
            } catch (Exception e) {
                attemptNo++;
                sleep(2000);
            }
        } while (!status && attemptNo < 3);

        return status;
    }

    // Parse the AT command response for the relevant info
    private String getResponseString(String resp) {
        if (resp == null) {
            return "";
        }

        // remove the command and space at the beginning, and the 'OK' and spaces at the
        // end
        return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
    }

    private String getResponseString(byte[] resp) {
        if (resp == null) {
            return "";
        }

        return getResponseString(new String(resp));
    }

    private void powerOff() {
        // do nothing
    }

    private void powerOn() {
        // do nothing
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /*
     * This method generates array of subscribe topics
     */
    private static String[] generateSubscribeTopics() {

        StringBuilder buf = new StringBuilder(SierraUsb598.class.getName().replace('.', '/'));
        buf.append('/');
        buf.append('*');
        String[] topics = new String[1];
        topics[0] = buf.toString();
        return topics;
    }

    /*
     * This method enables notification on specified CnS object
     */
    private void enableNotification(int objID) throws Exception {

        CnS cnsReply;
        CnS cnsCommand = new CnS(objID, CnsOpTypes.OPTYPE_NOTIF_ENB.getOpType(),
                CnsAppIDs.USB598_APPLICATION_ID.getID());

        cnsReply = this.cnsExchange(cnsCommand, 500);
        if (cnsReply != null && cnsReply.getOperationType() == CnsOpTypes.OPTYPE_NOTIF_ENB_REP.getOpType()) {
            logger.debug("Notification on objID=0x" + Integer.toHexString(objID) + " is enabled");
        }
    }

    /*
     * This method sets receive signal strength information.
     */
    private void setSignalStrengthInfo(CnS cnsReply) {
        byte[] cnsPayload;

        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.rssi = 0 - (cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff);
                logger.debug("!!! RSS !!! : {} dBm", this.rssi);
            }
        }
    }

    private void setSystemIdInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.sid = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                logger.debug("!!! SID !!! : {}", this.sid);
            }
        }
    }

    private void setNetworkIdInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.nid = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                logger.debug("!!! NID !!! : {}", this.nid);
            }
        }
    }

    private void setChannelNumberInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.channelNo = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                logger.debug("!!! Channel Number !!! : {}", this.channelNo);
            }
        }
    }

    private void setChannelStateInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.channelState = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                logger.debug("!!! Channel State !!! : {}", this.channelState);
            }
        }
    }

    private void setBandClassInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.bandClass = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                logger.debug("!!! Current Band Class !!! : {}", this.bandClass);
            }
        }
    }

    /*
     * This method sets firmware version
     */
    private void setFirmwareVersion(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.firmwareVersion = new String(cnsPayload);
            }
        }
    }

    /*
     * This method sets firmware date
     */
    private void setFirmwareDate(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.firmwareDate = new String(cnsPayload);
            }
        }
    }

    /*
     * This method sets PRL version information
     */
    private void setPrlVersionInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.prlVersion = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                logger.debug("!!! PRL Version !!! : {}", this.prlVersion);
            }
        }
    }

    /*
     * This method sets 'Activation Status' information
     */
    private void setActivationStatusInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.activationStatus = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                logger.debug("!!! Activation Status !!! : " + Integer.toHexString(this.activationStatus));
            }
        }
    }

    /*
     * This method sets 'Activation Date' information
     */
    private void setActivationDateInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                String sdate = new String(cnsPayload, 2, 8);
                logger.debug("!!! Activation Date !!! : " + sdate + " length=" + sdate.length());

                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                try {
                    this.activationDate.setTime(df.parse(sdate));
                } catch (ParseException e) {
                    this.activationDate = null;
                }
            }
        }
    }

    /*
     * This method sets 'Roaming Status' information
     */
    private void setRoamingStatusInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.roamingStatus = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                logger.debug("!!! Roaming Status !!! : {}", this.roamingStatus);
            }
        }
    }

    private void setMobileDirectoryNumberInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                int len = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;
                StringBuilder sbMdn = new StringBuilder();
                int offset = 4;
                for (int i = 0; i < len; i++) {
                    sbMdn.append(cnsPayload[offset + i]);
                }
                this.mdn = convertDecimalNumeralToString(sbMdn);
                logger.debug("!!! MDN !!! : {}", this.mdn);
            }
        }
    }

    private void setMobileIdentificationNumberInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                int len = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;
                StringBuilder sbMin = new StringBuilder();
                int offset = 4;
                for (int i = 0; i < len; i++) {
                    sbMin.append(cnsPayload[offset + i]);
                }
                this.min = convertDecimalNumeralToString(sbMin);
                logger.debug("!!! MIN !!! : {}", this.min);
            }
        }
    }

    private void setServiceTypeInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.serviceType = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                logger.debug("!!! Service Type !!! : " + SierraUsb598Status.getServiceIndication(this.serviceType));
            }
        }
    }

    private void setPowerModeInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.powerMode = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;

                logger.debug("!!! Power Mode !!! : " + SierraUsb598Status.getPowerMode(this.powerMode));
            }
        }
    }

    /*
     * This method sets call TX byte counter information
     */
    private void setCallTxCounterInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.txCount = (cnsPayload[0] << 24 & 0x0ffffffff | cnsPayload[1] << 16 & 0x0ffffff
                        | cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff) & 0x0ffffffffL;
                logger.debug("!!! TX Count !!! :{}", this.txCount);
            }
        }
    }

    /*
     * This method sets call RX byte counter information
     */
    private void setCallRxCounterInfo(CnS cnsReply) {

        byte[] cnsPayload;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.rxCount = (cnsPayload[4] << 24 & 0x0ffffffff | cnsPayload[5] << 16 & 0x0ffffff
                        | cnsPayload[6] << 8 & 0x0ffff | cnsPayload[7] & 0x0ff) & 0x0ffffffffL;
                logger.debug("!!! RX Count !!! :{}", this.rxCount);
            }
        }
    }

    private void setCallStatusInfo(int callStatus) {
        this.callStatus = callStatus;
    }

    /*
     * This method converts supplied string buffer of decimal numerals to string
     */
    private String convertDecimalNumeralToString(StringBuilder buf) {

        StringBuilder ret = new StringBuilder();
        int num;
        for (int i = 0; i < buf.length(); i = i + 2) {
            num = Character.digit(buf.charAt(i), 10) * 10 + Character.digit(buf.charAt(i + 1), 10) - 0x30;
            ret.append(num);
        }
        return ret.toString();
    }

    /*
     * This method sends CnS command to the modem and obtains reply.
     */
    private CnS cnsExchange(CnS cnsCommand, int tout) throws Exception {
        CnS cnsReply = null;
        logger.debug("cnsExchange() start");
        try {
            cnsReply = new CnS(this.commHipConnection.sendCommand(cnsCommand.getRequest(), tout));
        } catch (Exception e) {
            logger.debug("Failed to send command: {}", e);
        }
        return cnsReply;
    }

    /*
     * This method sends CnS command to the modem and obtains reply. Default timeout
     * is set to 500 msec
     */
    private CnS cnsExchange(CnS cnsCommand) throws Exception {
        return this.cnsExchange(cnsCommand, 500);
    }

    public boolean hasDiversityAntenna() {
        return false;
    }

    @Override
    public boolean isDiversityEnabled() {
        return false;
    }

    @Override
    public void enableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void disableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }
}
