/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.telit.generic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.util.SerialUtil;
import org.eclipse.kura.net.modem.CellularModem.SerialPortType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TelitModem {

    private static final Logger logger = LoggerFactory.getLogger(TelitModem.class);

    protected static final String MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG = "Modem not available for AT commands";

    protected final Object atLock = new Object();

    protected String model;
    protected String manufacturer;
    protected String serialNumber;
    protected String revisionId;
    protected int rssi;
    protected Boolean gpsSupported;
    protected String imsi;
    protected String iccid;
    protected ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
    protected String firmwareVersion;

    private boolean gpsEnabled;
    private ModemDevice device;
    private final String platform;
    private final ConnectionFactory connectionFactory;
    private List<NetConfig> netConfigs = null;

    protected TelitModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

        this.device = device;
        this.platform = platform;
        this.connectionFactory = connectionFactory;
        this.gpsEnabled = false;
    }

    public void initModemParameters() {
        if (device != null) {
            try {
                String atPort = getAtPort();
                if (atPort != null) {
                    this.serialNumber = getSerialNumber();
                    this.imsi = getMobileSubscriberIdentity(true);
                    this.iccid = getIntegratedCirquitCardId(true);
                    this.model = getModel();
                    this.manufacturer = getManufacturer();
                    this.revisionId = getRevisionID();
                    this.gpsSupported = isGpsSupported();
                    this.rssi = getSignalStrength(true);

                    logger.debug("{} :: Serial Number={}", getClass().getName(), this.serialNumber);
                    logger.debug("{} :: IMSI={}", getClass().getName(), this.imsi);
                    logger.debug("{} :: ICCID={}", getClass().getName(), this.iccid);
                    logger.debug("{} :: Model={}", getClass().getName(), this.model);
                    logger.debug("{} :: Manufacturer={}", getClass().getName(), this.manufacturer);
                    logger.debug("{} :: Revision ID={}", getClass().getName(), this.revisionId);
                    logger.debug("{} :: GPS Supported={}", getClass().getName(), this.gpsSupported);
                    logger.debug("{} :: RSSI={}", getClass().getName(), this.rssi);
                }
            } catch (KuraException e) {
                logger.error("Failed to initialize modem", e);
            }
        }
    }

    public void reset() {
        int offOnDelay = 1000;

        while (true) {
            sleep(5000);
            try {
                turnOff();

                this.gpsEnabled = false;
                sleep(offOnDelay);
                turnOn();
                logger.info("reset() :: modem reset successful");
                break;
            } catch (Exception e) {
                logger.error("Failed to reset the modem", e);
            }
        }
    }

    public String getModel() throws KuraException {
        synchronized (this.atLock) {
            if (this.model == null) {
                logger.debug("sendCommand getModelNumber :: {}", TelitModemAtCommands.GET_MODEL_NUMBER.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_MODEL_NUMBER.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.model = getResponseString(reply);
                }
            }
        }
        return this.model;
    }

    public String getManufacturer() throws KuraException {
        synchronized (this.atLock) {
            if (this.manufacturer == null) {
                logger.debug("sendCommand getManufacturer :: {}", TelitModemAtCommands.GET_MANUFACTURER.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_MANUFACTURER.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.manufacturer = getResponseString(reply);
                }
            }
        }
        return this.manufacturer;
    }

    public String getSerialNumber() throws KuraException {
        synchronized (this.atLock) {
            if (this.serialNumber == null) {
                logger.debug("sendCommand getSerialNumber :: {}", TelitModemAtCommands.GET_SERIAL_NUMBER.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_SERIAL_NUMBER.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String serialNum = getResponseString(reply);
                    if (serialNum != null && !serialNum.isEmpty()) {
                        if (serialNum.startsWith("#CGSN:")) {
                            serialNum = serialNum.substring("#CGSN:".length()).trim();
                        }
                        this.serialNumber = serialNum;
                    }
                }
            }
        }
        return this.serialNumber;
    }

    public String getRevisionID() throws KuraException {
        synchronized (this.atLock) {
            if (this.revisionId == null) {
                logger.debug("sendCommand getRevision :: {}", TelitModemAtCommands.GET_REVISION.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_REVISION.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.revisionId = getResponseString(reply);
                }
            }
        }
        return this.revisionId;
    }

    public boolean isReachable() throws KuraException {
        boolean ret;
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            ret = isAtReachable(commAtConnection);
            closeSerialPort(commAtConnection);
        }
        return ret;
    }

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

    public int getSignalStrength(boolean recompute) throws KuraException {

        if (recompute) {
            int signalStrength = -113;
            synchronized (this.atLock) {
                String atPort = getAtPort();
                String gpsPort = getGpsPort();
                if ((atPort.equals(getDataPort()) || atPort.equals(gpsPort) && this.gpsEnabled) && this.rssi < 0) {
                    logger.trace("getSignalStrength() :: returning previously obtained RSSI={} :: m_gpsEnabled={}",
                            this.rssi, this.gpsEnabled);
                    return this.rssi;
                }

                logger.debug("sendCommand getSignalStrength :: {}",
                        TelitModemAtCommands.GET_SIGNAL_STRENGTH.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(atPort);
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }
                try {
                    reply = commAtConnection
                            .sendCommand(TelitModemAtCommands.GET_SIGNAL_STRENGTH.getCommand().getBytes(), 1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    signalStrength = parseRssi(reply);
                }
            }
            this.rssi = signalStrength;
        }
        return this.rssi;
    }

    private int parseRssi(byte[] reply) {
        int signalStrength = -113;
        String[] asCsq;
        String sCsq = this.getResponseString(reply);
        if (sCsq.startsWith("+CSQ:")) {
            sCsq = sCsq.substring("+CSQ:".length()).trim();
            logger.trace("getSignalStrength() :: +CSQ={}", sCsq);
            asCsq = sCsq.split(",");
            if (asCsq.length == 2) {
                int rssiVal = Integer.parseInt(asCsq[0]);
                if (rssiVal < 99) {
                    signalStrength = -113 + 2 * rssiVal;
                }
                logger.trace("getSignalStrength() :: signalStrength={}", signalStrength);
            }
        }
        return signalStrength;
    }

    public int getSignalStrength() throws KuraException {
        return getSignalStrength(true);
    }

    public boolean isGpsSupported() throws KuraException {
        if (this.gpsSupported != null) {
            return this.gpsSupported;
        }
        if (getGpsPort() == null) {
            this.gpsSupported = false;
            return this.gpsSupported;
        }
        synchronized (this.atLock) {
            if (this.gpsSupported == null) {
                logger.debug("sendCommand isGpsSupported :: {}", TelitModemAtCommands.IS_GPS_POWERED.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }

                try {
                    reply = commAtConnection.sendCommand(TelitModemAtCommands.IS_GPS_POWERED.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String sReply = getResponseString(reply);
                    if (sReply != null && !sReply.isEmpty()) {
                        if (sReply.startsWith("$GPSP:")) {
                            this.gpsSupported = true;
                        } else {
                            this.gpsSupported = false;
                        }
                    }
                }
            }
        }
        return this.gpsSupported;
    }

    public void enableGps() throws KuraException {
        enableGps(TelitModemAtCommands.GPS_POWER_UP.getCommand());
    }

    protected void enableGps(String gpsPowerupCommand) throws KuraException {
        if (this.gpsSupported == null || !this.gpsSupported) {
            logger.warn("enableGps() :: GPS NOT SUPPORTED");
            this.gpsEnabled = false;
            return;
        }
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
            }

            byte[] reply;
            int numAttempts = 3;
            while (numAttempts > 0) {
                String atPort = getAtPort();
                String gpsPort = getGpsPort();
                String gpsEnableNMEAcommand = formGpsEnableNMEACommand(atPort, gpsPort);
                try {
                    if (!isGpsPowered(commAtConnection)) {
                        logger.debug("enableGps() :: sendCommand gpsPowerUp :: {}", gpsPowerupCommand);
                        commAtConnection.sendCommand(gpsPowerupCommand.getBytes(), 1000, 100);
                    }
                    logger.debug("enableGps() :: sendCommand gpsEnableNMEA :: {}", gpsEnableNMEAcommand);
                    reply = commAtConnection.sendCommand(gpsEnableNMEAcommand.getBytes(), 3000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }

                if (reply != null && reply.length > 0) {
                    String sReply = getResponseString(reply);
                    if (sReply != null) {
                        if (atPort.equals(gpsPort)) {
                            logger.trace("enableGps() :: gpsEnableNMEA reply={}", sReply);
                            if (!sReply.isEmpty() && sReply.startsWith("CONNECT")) {
                                logger.info("enableGps() :: Modem replied to the {} command with 'CONNECT'",
                                        gpsEnableNMEAcommand);
                                logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                                this.gpsEnabled = true;
                                break;
                            }
                        } else {
                            if (sReply.isEmpty()) {
                                logger.info("enableGps() :: Modem replied to the {} command with 'OK'",
                                        gpsEnableNMEAcommand);
                                logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                                this.gpsEnabled = true;
                                break;
                            }
                        }
                    }
                }
                numAttempts--;
                sleep(2000);
            }

            closeSerialPort(commAtConnection);
        }
    }

    public void disableGps() throws KuraException {
        if (this.gpsSupported == null || !this.gpsSupported) {
            logger.warn("disableGps() :: GPS NOT SUPPORTED");
            this.gpsEnabled = false;
            return;
        }
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                String atPort = getAtPort();
                String gpsPort = getGpsPort();
                if (atPort.equals(gpsPort) && !isAtReachable(commAtConnection)) {
                    int numAttempts = 3;
                    while (numAttempts > 0) {
                        logger.debug("disableGps() :: sendCommand escapeSequence {}",
                                TelitModemAtCommands.ESCAPE_SEQUENCE.getCommand());

                        sleep(1000); // do not send anything for 1 second before the escape sequence
                        byte[] reply = commAtConnection
                                .sendCommand(TelitModemAtCommands.ESCAPE_SEQUENCE.getCommand().getBytes(), 1000, 1100);

                        if (reply != null && reply.length > 0) {
                            logger.trace("disableGps() :: reply={}", new String(reply));
                            String sReply = new String(reply);
                            if (sReply.contains("NO CARRIER")) {
                                logger.info(
                                        "disableGps() :: Modem replied with 'NO CARRIER' to the +++ escape sequence");
                                sleep(2000);
                                if (isAtReachable(commAtConnection)) {
                                    logger.info("disableGps() :: !!! Modem GPS disabled !!!, OK");
                                    this.gpsEnabled = false;
                                    break;
                                } else {
                                    logger.error("disableGps() :: [1] Failed to disable modem GPS");
                                    numAttempts--;
                                }
                            } else {
                                if (isAtReachable(commAtConnection)) {
                                    logger.warn("disableGps() :: Modem didn't reply with 'NO CARRIER' "
                                            + "to the +++ escape sequence but port is AT reachable");
                                    logger.info("disableGps() :: Will assume that GPS is disabled");
                                    this.gpsEnabled = false;
                                    break;
                                } else {
                                    logger.error("disableGps() :: [2] Failed to disable modem GPS");
                                    numAttempts--;
                                }
                            }
                        } else {
                            logger.error("disableGps() :: [3] Failed to disable modem GPS");
                            numAttempts--;
                        }
                        sleep(2000);
                    }
                } else {
                    logger.warn("disableGps() :: Modem GPS has already been disabled");
                    this.gpsEnabled = false;
                }

                logger.debug("disableGps() :: sendCommand gpsPowerDown :: {}",
                        TelitModemAtCommands.GPS_POWER_DOWN.getCommand());
                commAtConnection.sendCommand(TelitModemAtCommands.GPS_POWER_DOWN.getCommand().getBytes(), 1000, 100);

            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
            closeSerialPort(commAtConnection);
        }
    }

    public String getMobileSubscriberIdentity(boolean recompute) throws KuraException {
        if (recompute) {
            synchronized (this.atLock) {
                if (this.imsi == null || this.imsi.equals("ERROR") && isTelitSimCardReady()) {
                    logger.debug("sendCommand getIMSI :: {}", TelitModemAtCommands.GET_IMSI.getCommand());
                    byte[] reply;
                    CommConnection commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                                MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                    }
                    try {
                        reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_IMSI.getCommand().getBytes(),
                                1000, 100);
                    } catch (IOException e) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                    }
                    closeSerialPort(commAtConnection);
                    parseIMSI(reply);
                }
            }
        }
        return this.imsi;
    }

    private void parseIMSI(byte[] reply) {
        if (reply != null) {
            String mobileSubscriberIdentity = getResponseString(reply);
            if (mobileSubscriberIdentity != null && !mobileSubscriberIdentity.isEmpty()) {
                if (mobileSubscriberIdentity.startsWith("#CIMI:")) {
                    mobileSubscriberIdentity = mobileSubscriberIdentity.substring("#CIMI:".length()).trim();
                }
                this.imsi = mobileSubscriberIdentity;
            }
        }
    }

    public String getMobileSubscriberIdentity() throws KuraException {
        return getMobileSubscriberIdentity(true);
    }

    public String getIntegratedCirquitCardId(boolean recompute) throws KuraException {
        if (recompute) {
            synchronized (this.atLock) {
                if (this.iccid == null && isTelitSimCardReady()) {
                    logger.debug("sendCommand getICCID :: {}", TelitModemAtCommands.GET_ICCID.getCommand());
                    byte[] reply;
                    CommConnection commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                                MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                    }
                    try {
                        reply = commAtConnection.sendCommand(TelitModemAtCommands.GET_ICCID.getCommand().getBytes(),
                                1000, 100);
                    } catch (IOException e) {
                        closeSerialPort(commAtConnection);
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                    }
                    closeSerialPort(commAtConnection);
                    parseICCID(reply);
                }
            }
        }
        return this.iccid;
    }

    private void parseICCID(byte[] reply) {
        if (reply != null) {
            String integratedCirquitCardId = getResponseString(reply);
            if (integratedCirquitCardId != null && !integratedCirquitCardId.isEmpty()) {
                if (integratedCirquitCardId.startsWith("#CCID:")) {
                    integratedCirquitCardId = integratedCirquitCardId.substring("#CCID:".length()).trim();
                }
                this.iccid = integratedCirquitCardId;
            }
        }
    }

    public String getIntegratedCirquitCardId() throws KuraException {
        return getIntegratedCirquitCardId(true);
    }

    public String getDataPort() throws KuraException {
        String port;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
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

    public String getAtPort() throws KuraException {
        String port;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
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

    public String getGpsPort() throws KuraException {
        String port = null;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
                if (usbModemInfo != null) {
                    int gpsPort = usbModemInfo.getGpsPort();
                    if (gpsPort >= 0) {
                        port = ports.get(gpsPort);
                    }
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

    public ModemDevice getModemDevice() {
        return this.device;
    }

    public void setModemDevice(ModemDevice device) {
        this.device = device;
    }

    public List<NetConfig> getConfiguration() {
        return this.netConfigs;
    }

    public void setConfiguration(List<NetConfig> netConfigs) {
        this.netConfigs = netConfigs;
    }

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
                StringBuilder sb = new StringBuilder();
                sb.append("comm:").append(port).append(";baudrate=115200;databits=8;stopbits=1;parity=0");
                commURI = CommURI.parseString(sb.toString());
            }
        } catch (URISyntaxException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "URI Syntax Exception");
        }
        return commURI;
    }

    public boolean isGpsEnabled() {
        return this.gpsEnabled;
    }

    public abstract boolean isTelitSimCardReady() throws KuraException;

    protected CommConnection openSerialPort(String port) throws KuraException {

        if (this.connectionFactory != null) {
            return SerialUtil.openSerialPort(this.connectionFactory, port);
        }
        return null;
    }

    protected void closeSerialPort(CommConnection connection) throws KuraException {
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
                logger.trace("isAtReachable() :: sending AT commnd to modem on port {}", connection.getURI().getPort());
                byte[] reply = connection.sendCommand(TelitModemAtCommands.AT.getCommand().getBytes(), 1000, 100);
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
        } while (!status && attemptNo < 3);

        logger.trace("isAtReachable() :: port={}, status={}", connection.getURI().getPort(), status);
        return status;
    }

    // Parse the AT command response for the relevant info
    protected String getResponseString(String resp) {
        if (resp == null) {
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
        if (this.gpsSupported == null || !this.gpsSupported) {
            return false;
        }

        logger.debug("sendCommand isGpsPowered :: {}", TelitModemAtCommands.IS_GPS_POWERED.getCommand());
        byte[] reply = null;
        try {
            reply = commAtConnection.sendCommand(TelitModemAtCommands.IS_GPS_POWERED.getCommand().getBytes(), 1000,
                    100);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        if (reply != null) {
            String sReply = getResponseString(reply);
            if (sReply != null && !sReply.isEmpty() && sReply.startsWith("$GPSP:")) {
                sReply = sReply.substring("$GPSP:".length()).trim();
                gpsPowered = "1".equals(sReply) ? true : false;
            }
        }

        return gpsPowered;
    }

    private void turnOff() throws KuraException {

        UsbModemDriver modemDriver = getModemDriver();
        if (modemDriver != null) {
            modemDriver.disable((UsbModemDevice) getModemDevice());
        } else {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE);
        }
    }

    private void turnOn() throws KuraException {

        UsbModemDriver modemDriver = getModemDriver();
        if (modemDriver != null) {
            modemDriver.enable((UsbModemDevice) getModemDevice());
        } else {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE);
        }
    }

    private UsbModemDriver getModemDriver() {

        if (this.device == null) {
            return null;
        }
        UsbModemDriver modemDriver = null;
        if (this.device instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
            if (usbModemInfo != null) {
                List<? extends UsbModemDriver> usbDeviceDrivers = usbModemInfo.getDeviceDrivers();
                if (usbDeviceDrivers != null && !usbDeviceDrivers.isEmpty()) {
                    modemDriver = usbDeviceDrivers.get(0);
                }
            }
        }
        return modemDriver;
    }

    private String formGpsEnableNMEACommand(String atPort, String gpsPort) throws KuraException {

        StringBuilder sbCommand = new StringBuilder(TelitModemAtCommands.GPS_ENABLE_NMEA.getCommand());
        if (atPort.equals(gpsPort)) {
            sbCommand.append("3");
        } else {
            sbCommand.append("2");
        }
        sbCommand.append(",1,1,1,1,1,1\r\n");
        return sbCommand.toString();
    }

    protected String getTelitFirmwareVersion() throws KuraException {
        if (StringUtils.isBlank(this.firmwareVersion)) {
            synchronized (this.atLock) {
                CommConnection commAtConnection = null;
                try {
                    String port = getUnusedAtPort();
                    commAtConnection = openSerialPort(port);
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }

                    readTelitFirmwareVersion(commAtConnection);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.firmwareVersion;
    }

    private void readTelitFirmwareVersion(CommConnection comm) throws IOException, KuraException {
        this.firmwareVersion = "N/A";
        byte[] reply = comm.sendCommand(
                TelitModemAtCommands.GET_REVISION.getCommand().getBytes(StandardCharsets.US_ASCII), 1000, 100);
        if (reply != null) {
            this.firmwareVersion = getResponseString(reply);
        }
    }

    protected String getUnusedAtPort() throws KuraException {
        String port;
        if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
            port = getDataPort();
        } else {
            port = getAtPort();
        }
        return port;
    }
}
