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
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.event.EventConstants;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SierraUsb598 implements EvdoCellularModem {

    private static final Logger s_logger = LoggerFactory.getLogger(SierraUsb598.class);

    private static String[] s_topicsOfInterest = null;
    // private static final int AT_PORT = 0;
    private static final int HIP_PORT = 1;

    private ConnectionFactory m_connectionFactory = null;
    private String m_model = null;
    private String m_manufacturer = null;
    private String m_esn = null;
    private String m_revisionId = null;

    private Object m_atLock = null;

    private int m_callStatus = -1; // current call status

    // private UsbModemDevice m_usbDevice = null;
    private ModemDevice m_device = null;

    // private NetInterfaceConfig<? extends NetInterfaceAddressConfig> m_netInterfaceConfig = null;
    private List<NetConfig> m_netConfigs = null;

    private CommConnection m_commHipConnection = null;

    private ScheduledExecutorService m_executor = null;

    private List<CnS> m_notifications = null;

    private String m_mdn = null; // mobile directory number
    private String m_min = null; // mobile identification number

    private String m_firmwareVersion = "";
    private String m_firmwareDate = "";
    private int m_prlVersion = 0; // PRL version

    /*
     * 'Received Signal Strength' (in dBm) and its lock
     */
    private int m_rssi = 0;
    private final Object m_rssiLock = new Object();

    /*
     * 'System ID' and its lock
     */
    private int m_sid = -1;
    private final Object m_sidLock = new Object();

    /*
     * 'Network ID' and its lock
     */
    private int m_nid = -1;
    private final Object m_nidLock = new Object();

    /*
     * 'Channel Number' and its lock
     */
    private int m_channelNo = -1;
    private final Object m_channelNoLock = new Object();

    /*
     * 'Channel State' and its lock
     */
    private int m_channelState = -1;
    private final Object m_channelStateLock = new Object();

    /*
     * 'Current Band Class' and its lock
     */
    private int m_bandClass = -1;
    private final Object m_bandClassLock = new Object();

    private int m_activationStatus = -1; // activation status

    private GregorianCalendar m_activationDate = null; // activation date

    /*
     * 'Roaming Status' and its lock
     */
    private int m_roamingStatus = -1;
    private final Object m_roamingStatusLock = new Object();

    /*
     * 'Service Type' and its lock
     */
    private int m_serviceType = -1;
    private final Object m_serviceTypeLock = new Object();

    private long m_txCount = -1L; // number of bytes transmitted during a call
    private long m_rxCount = -1L; // number of bytes received during a call
    private final Object m_byteCountLock = new Object(); // byte count lock

    /*
     * 'Power Mode' and its lock
     */
    private int m_powerMode = -1;
    private final Object m_powerModeLock = new Object();

    /**
     * SierraUsb598 modem constructor
     *
     * @param usbDevice
     *            - modem USB device as {@link UsbModemDevice}
     * @param connectionFactory
     *            - connection factory as {@link ConnectionFactory}
     * @param technologyType
     *            - cellular technology type as {@link ModemTechnologyType}
     */
    public SierraUsb598(ModemDevice device, ConnectionFactory connectionFactory) {

        this.m_device = device;
        this.m_connectionFactory = connectionFactory;
        this.m_atLock = new Object();
        s_topicsOfInterest = generateSubscribeTopics();
        // subscribe on specific topics of interest
        Dictionary d = new Hashtable();
        d.put(EventConstants.EVENT_TOPIC, s_topicsOfInterest);
        for (String element : s_topicsOfInterest) {
            s_logger.debug("Subscribing for {}", element);
        }
        // bundleContext.registerService(EventHandler.class.getName(), this, d);

        this.m_notifications = new ArrayList();

        // define notification thread
        this.m_executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void bind() {
        try {
            this.m_commHipConnection = openSerialPort(getHipPort());

            this.m_executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("SierraUsb598");
                    s_logger.debug("**** HIP Thread() run ");
                    // if(notificationThread() == false) {
                    // s_logger.debug("**** notificationThread() shut down");
                    // m_executor.shutdown();
                    // }
                }
            }, 0, 1, TimeUnit.SECONDS);
        } catch (KuraException e) {
            e.printStackTrace();
        }
    }

    public void unbind() {

        if (this.m_commHipConnection != null) {
            try {
                this.m_executor.shutdown();
                closeSerialPort(this.m_commHipConnection);
            } catch (KuraException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getModel() throws KuraException {
        synchronized (this.m_atLock) {
            if (this.m_model == null) {
                s_logger.debug("sendCommand getModelNumber :: {}", SierraUsb598AtCommands.getModelNumber.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                            "Modem not available for AT commands: " + SierraUsb598.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getModelNumber.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_model = getResponseString(reply);
                    reply = null;
                }
                closeSerialPort(commAtConnection);
            }
        }
        return this.m_model;
    }

    @Override
    public String getManufacturer() throws KuraException {
        synchronized (this.m_atLock) {
            if (this.m_manufacturer == null) {
                s_logger.debug("sendCommand getManufacturer :: {}",
                        SierraUsb598AtCommands.getManufacturer.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                            "Modem not available for AT commands: " + SierraUsb598.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getManufacturer.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_manufacturer = getResponseString(reply);
                    reply = null;
                }
            }
        }
        return this.m_manufacturer;
    }

    @Override
    public String getSerialNumber() throws KuraException {
        synchronized (this.m_atLock) {
            if (this.m_esn != null) {
                s_logger.debug("sendCommand getSerialNumber :: {}",
                        SierraUsb598AtCommands.getSerialNumber.getCommand());
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                            "Modem not available for AT commands: " + SierraUsb598.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getSerialNumber.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    StringBuffer replySB = new StringBuffer();
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
                        this.m_esn = serialNum;
                    }
                }
            }
        }
        return this.m_esn;
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
        synchronized (this.m_atLock) {
            if (this.m_revisionId == null) {
                byte[] reply = null;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                            "Modem not available for AT commands: " + SierraUsb598.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getRevision.getCommand().getBytes(),
                            500);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.m_revisionId = getResponseString(reply);
                }
            }
        }
        return this.m_revisionId;
    }

    @Override
    public boolean isReachable() throws KuraException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPortReachable(String port) {
        boolean ret = false;
        synchronized (this.m_atLock) {
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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getCallTxCounter() throws KuraException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getServiceType() throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ModemDevice getModemDevice() {
        return this.m_device;
    }

    protected void setModemDevice(ModemDevice device) {
        this.m_device = device;
    }

    @Override
    public String getDataPort() throws KuraException {

        String port = null;
        List<String> ports = this.m_device.getSerialPorts();
        if (ports != null && ports.size() > 0) {
            if (this.m_device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getDataPort());
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No PPP serial port available");
                }
            } else if (this.m_device instanceof SerialModemDevice) {
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
        List<String> ports = this.m_device.getSerialPorts();
        if (ports != null && ports.size() > 0) {
            if (this.m_device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.m_device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getAtPort());
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No AT serial port available");
                }
            } else if (this.m_device instanceof SerialModemDevice) {
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
    public boolean isProvisioned() throws KuraException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void provision() throws KuraException {
        // TODO Auto-generated method stub
    }

    @Override
    public String getMobileDirectoryNumber() throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMobileIdentificationNumber() throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetConfig> getConfiguration() {
        return this.m_netConfigs;
    }

    @Override
    public void setConfiguration(List<NetConfig> netConfigs) {
        this.m_netConfigs = netConfigs;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {

        List<ModemTechnologyType> modemTechnologyTypes = null;
        ModemDevice device = getModemDevice();
        if (device == null) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No modem device");
        }
        if (device instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) device);
            if (usbModemInfo != null) {
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
            if (modemTechnologyTypes != null && modemTechnologyTypes.size() > 0) {
                modemTechnologyType = modemTechnologyTypes.get(0);
            }
        } catch (KuraException e) {
            s_logger.error("Failed to obtain modem technology - {}", e);
        }
        return modemTechnologyType;
    }

    private String getHipPort() throws KuraException {
        String port = null;

        if (this.m_device instanceof UsbModemDevice) {
            UsbModemDevice usbModemDevice = (UsbModemDevice) this.m_device;
            List<String> ports = usbModemDevice.getTtyDevs();
            if (ports != null && ports.size() > 0) {
                port = ports.get(HIP_PORT);
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No HIP serial port available");
            }
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No HIP serial port available");
        }

        return port;
    }

    @Override
    public ModemCdmaServiceProvider getServiceProvider() {
        // TODO
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

    private CommConnection openSerialPort(String port) throws KuraException {

        CommConnection connection = null;
        if (this.m_connectionFactory != null) {
            String uri = new CommURI.Builder(port).withBaudRate(115200).withDataBits(8).withStopBits(1).withParity(0)
                    .withTimeout(2000).build().toString();

            try {
                connection = (CommConnection) this.m_connectionFactory.createConnection(uri, 1, false);
            } catch (Exception e) {
                s_logger.debug("Exception creating connection: {}", e);
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
                status = connection.sendCommand(SierraUsb598AtCommands.at.getCommand().getBytes(), 500).length > 0;
            } catch (Exception e) {
                attemptNo++;
                sleep(2000);
            }
        } while (status == false && attemptNo < 3);

        return status;
    }

    // Parse the AT command response for the relevant info
    private String getResponseString(String resp) {
        if (resp == null) {
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

        StringBuffer buf = new StringBuffer(SierraUsb598.class.getName().replace('.', '/'));
        buf.append('/');
        buf.append('*');
        String[] topics = new String[1];
        topics[0] = new String(buf.toString());

        return topics;
    }

    /*
     * private boolean notificationThread() {
     *
     * s_logger.debug("notificationThread() - start");
     * CnS notification = null;
     * // check for notifications (unsolicited messages)
     * s_logger.debug("notificationThread() - alNotifications.size(): " + m_notifications.size());
     * while (m_notifications.size() > 0) {
     * notification = m_notifications.remove(0);
     * s_logger.debug("notificationThread() - first notification: " + notification);
     * if (notification != null) {
     *
     * s_logger.debug("Notification ObjID: 0x" + Integer.toHexString(notification.getObjectId()));
     * int objID = notification.getObjectId();
     *
     * switch (objID) {
     * case CnsObjectIDs.OBJID_RSSI.getObjectID():
     * synchronized (m_rssiLock) {
     * this.setSignalStrengthInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_SID_VALUE.getObjectID():
     * synchronized (m_sidLock) {
     * this.setSystemIdInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_NID_VALUE.getObjectID():
     * synchronized (m_nidLock) {
     * this.setNetworkIdInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_CHANNEL_NUMBER.getObjectID():
     * synchronized (m_channelNoLock) {
     * this.setChannelNumberInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_CHANNEL_STATE.getObjectID():
     * synchronized (m_channelStateLock) {
     * this.setChannelStateInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_CURRENT_BAND_CLASS.getObjectID():
     * synchronized (m_bandClassLock) {
     * this.setBandClassInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_ROAMING_STATUS.getObjectID():
     * synchronized (m_roamingStatusLock) {
     * this.setRoamingStatusInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_CALL_BYTE_CNT.getObjectID():
     * synchronized (m_byteCountLock) {
     * this.setCallTxCounterInfo(notification);
     * this.setCallRxCounterInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_SRVC_INDICATION.getObjectID():
     * synchronized (m_serviceTypeLock) {
     * this.setServiceTypeInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_RADIO_PWR.getObjectID():
     * synchronized (m_powerModeLock) {
     * this.setPowerModeInfo(notification);
     * }
     * break;
     * case CnsObjectIDs.OBJID_CALL_DISCONNECTED.getObjectID():
     * // notification-only - no need to synchronize
     * this.setCallStatusInfo(SierraUsb598StatusCodes.CALLSTAT_DISCONNECTED.getStatusCode());
     * break;
     * case CnsObjectIDs.OBJID_CALL_CONNECTING.getObjectID():
     * // notification-only - no need to synchronize
     * this.setCallStatusInfo(SierraUsb598StatusCodes.CALLSTAT_CONNECTING.getStatusCode());
     * break;
     * case CnsObjectIDs.OBJID_CALL_CONNECTED.getObjectID():
     * // notification-only - no need to synchronize
     * this.setCallStatusInfo(SierraUsb598StatusCodes.CALLSTAT_CONNECTED.getStatusCode());
     * break;
     * case CnsObjectIDs.OBJID_CALL_DORMANT.getObjectID():
     * // notification-only - no need to synchronize
     * this.setCallStatusInfo(SierraUsb598StatusCodes.CALLSTAT_DORMANT.getStatusCode());
     * break;
     *
     *
     * // Have not seen this happen case MC572xCnS.OBJID_CALL_ERROR:
     * //
     * // this.kuraLoggerService.logDebug (LABEL + "!!!!! --------> GOT CALL ERROR NOTIFICATION <------- !!!!!"); byte
     * [] pl = notification.getPayload(); for (int i = 0; i <
     * // notification.getPayloadLength(); i++) { System.out.print("0x" + Integer.toHexString(pl[i]) + " "); }
     * System.out.println(); break;
     *
     * }
     * notification = null; // release notification
     * }
     * }
     * return true;
     * }
     */

    /*
     * This method enables notification on specified CnS object
     */
    private void enableNotification(int objID) throws Exception {

        CnS cnsReply = null;
        CnS cnsCommand = new CnS(objID, CnsOpTypes.OPTYPE_NOTIF_ENB.getOpType(),
                CnsAppIDs.USB598_APPLICATION_ID.getID());

        cnsReply = this.cnsExchange(cnsCommand, 500);
        if (cnsReply != null && cnsReply.getOperationType() == CnsOpTypes.OPTYPE_NOTIF_ENB_REP.getOpType()) {
            s_logger.debug("Notification on objID=0x" + Integer.toHexString(objID) + " is enabled");
        }
    }

    /*
     * This method sets receive signal strength information.
     */
    private void setSignalStrengthInfo(CnS cnsReply) {
        byte[] cnsPayload = null;

        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_rssi = 0 - (cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff);
                s_logger.debug("!!! RSS !!! : {} dBm", this.m_rssi);
            }
        }
    }

    private void setSystemIdInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_sid = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                s_logger.debug("!!! SID !!! : {}", this.m_sid);
            }
        }
    }

    private void setNetworkIdInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_nid = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                s_logger.debug("!!! NID !!! : {}", this.m_nid);
            }
        }
    }

    private void setChannelNumberInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_channelNo = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;
                s_logger.debug("!!! Channel Number !!! : {}", this.m_channelNo);
            }
        }
    }

    private void setChannelStateInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_channelState = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! Channel State !!! : {}", this.m_channelState);
            }
        }
    }

    private void setBandClassInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_bandClass = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! Current Band Class !!! : {}", this.m_bandClass);
            }
        }
    }

    /*
     * This method sets firmware version
     */
    private void setFirmwareVersion(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_firmwareVersion = new String(cnsPayload);
            }
        }
    }

    /*
     * This method sets firmware date
     */
    private void setFirmwareDate(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_firmwareDate = new String(cnsPayload);
            }
        }
    }

    /*
     * This method sets PRL version information
     */
    private void setPrlVersionInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_prlVersion = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! PRL Version !!! : {}", this.m_prlVersion);
            }
        }
    }

    /*
     * This method sets 'Activation Status' information
     */
    private void setActivationStatusInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_activationStatus = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! Activation Status !!! : " + Integer.toHexString(this.m_activationStatus));
            }
        }
    }

    /*
     * This method sets 'Activation Date' information
     */
    private void setActivationDateInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                String sdate = new String(cnsPayload, 2, 8);
                s_logger.debug("!!! Activation Date !!! : " + sdate + " length=" + sdate.length());

                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                try {
                    this.m_activationDate.setTime(df.parse(sdate));
                } catch (ParseException e) {
                    this.m_activationDate = null;
                }
            }
        }
    }

    /*
     * This method sets 'Roaming Status' information
     */
    private void setRoamingStatusInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_roamingStatus = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! Roaming Status !!! : {}", this.m_roamingStatus);
            }
        }
    }

    private void setMobileDirectoryNumberInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                int len = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;
                StringBuffer mdn = new StringBuffer();
                int offset = 4;
                for (int i = 0; i < len; i++) {
                    mdn.append(cnsPayload[offset + i]);
                }
                this.m_mdn = convertDecimalNumeralToString(mdn);
                s_logger.debug("!!! MDN !!! : {}", this.m_mdn);
            }
        }
    }

    private void setMobileIdentificationNumberInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                int len = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;
                StringBuffer min = new StringBuffer();
                int offset = 4;
                for (int i = 0; i < len; i++) {
                    min.append(cnsPayload[offset + i]);
                }
                this.m_min = convertDecimalNumeralToString(min);
                s_logger.debug("!!! MIN !!! : {}", this.m_min);
            }
        }
    }

    private void setServiceTypeInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_serviceType = cnsPayload[0] << 8 & 0x0ffff | cnsPayload[1] & 0x0ff;

                s_logger.debug("!!! Service Type !!! : " + SierraUsb598Status.getServiceIndication(this.m_serviceType));
            }
        }
    }

    private void setPowerModeInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_powerMode = cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff;

                s_logger.debug("!!! Power Mode !!! : " + SierraUsb598Status.getPowerMode(this.m_powerMode));

            }
        }
    }

    /*
     * This method sets call TX byte counter information
     */
    private void setCallTxCounterInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_txCount = (cnsPayload[0] << 24 & 0x0ffffffff | cnsPayload[1] << 16 & 0x0ffffff
                        | cnsPayload[2] << 8 & 0x0ffff | cnsPayload[3] & 0x0ff) & 0x0ffffffffL;
                s_logger.debug("!!! TX Count !!! :{}", this.m_txCount);
            }
        }
    }

    /*
     * This method sets call RX byte counter information
     */
    private void setCallRxCounterInfo(CnS cnsReply) {

        byte[] cnsPayload = null;
        if (cnsReply != null) {
            cnsPayload = cnsReply.getPayload();
            if (cnsPayload != null) {
                this.m_rxCount = (cnsPayload[4] << 24 & 0x0ffffffff | cnsPayload[5] << 16 & 0x0ffffff
                        | cnsPayload[6] << 8 & 0x0ffff | cnsPayload[7] & 0x0ff) & 0x0ffffffffL;
                s_logger.debug("!!! RX Count !!! :{}", this.m_rxCount);
            }
        }
    }

    private void setCallStatusInfo(int callStatus) {
        this.m_callStatus = callStatus;
        /*
         * if ((this.callStatus == Usb598Status.CALLSTAT_CONNECTED) ||
         * (this.callStatus == Usb598Status.CALLSTAT_DORMANT)) {
         * this.connected = true;
         * } else {
         * this.connected = false;
         * }
         */
    }

    /*
     * This method converts supplied string buffer of decimal numerals to string
     */
    private String convertDecimalNumeralToString(StringBuffer buf) {

        StringBuffer ret = new StringBuffer();
        int num = 0;

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
        // ModemCommand modemCommand = new ModemCommand(cnsCommand);
        s_logger.debug("cnsExchange() start");
        try {
            cnsReply = new CnS(this.m_commHipConnection.sendCommand(cnsCommand.getRequest(), tout));
        } catch (Exception e) {
            s_logger.debug("Failed to send command: {}", e);
        }
        // ModemReply modemReply =
        // this.modemChannelService.sendCommand(this.getHIPport(), modemCommand,
        // tout);
        // if (modemReply != null) {
        // cnsReply = modemReply.getCnsReply();
        // }
        return cnsReply;
    }

    /*
     * This method sends CnS command to the modem and obtains reply. Default
     * timeout is set to 500 msec
     */
    private CnS cnsExchange(CnS cnsCommand) throws Exception {

        return this.cnsExchange(cnsCommand, 500);
    }
}
