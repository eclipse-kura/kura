/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.hspa;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HspaModem implements HspaCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(HspaModem.class);

    protected static final String MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG = "Modem not available for AT commands";

    // FIXME PDP context should not be hard coded
    protected int pdpContext = 1;

    protected final Object atLock = new Object();

    protected String model;
    protected String manufacturer;
    protected String serialNumber;
    protected String revisionId;
    protected int rssi;
    protected Boolean gpsSupported;
    protected String imsi;
    protected String iccid;

    private ModemDevice device;
    private final String platform;
    private final ConnectionFactory connectionFactory;
    private List<NetConfig> netConfigs;

    public HspaModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        this.device = device;
        this.platform = platform;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void reset() throws KuraException {
        logger.warn("Modem reset not supported");
    }

    @Override
    public String getModel() throws KuraException {
        synchronized (this.atLock) {
            if (this.model == null) {
                logger.debug("sendCommand getModelNumber :: {}", HspaModemAtCommands.getModelNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getModelNumber.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    this.model = getResponseString(reply);
                }
            }
        }
        return this.model;
    }

    @Override
    public String getManufacturer() throws KuraException {
        synchronized (this.atLock) {
            if (this.manufacturer == null) {
                logger.debug("sendCommand getManufacturer :: {}", HspaModemAtCommands.getManufacturer.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getManufacturer.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
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
            if (this.serialNumber == null) {
                logger.debug("sendCommand getSerialNumber :: {}", HspaModemAtCommands.getSerialNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getSerialNumber.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
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

    @Override
    public String getRevisionID() throws KuraException {
        synchronized (this.atLock) {
            if (this.revisionId == null) {
                logger.debug("sendCommand getRevision :: {}", HspaModemAtCommands.getRevision.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getRevision.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
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
        boolean ret;
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            ret = isAtReachable(commAtConnection);
            closeSerialPort(commAtConnection);
        }
        return ret;
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
    public int getSignalStrength() throws KuraException {

        int signalStrength = -113;
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getSignalStrength :: {}", HspaModemAtCommands.getSignalStrength.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(HspaModemAtCommands.getSignalStrength.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
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
            }
        }
        this.rssi = signalStrength;
        return signalStrength;
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        return false;
    }

    @Override
    public void enableGps() throws KuraException {
        logger.warn("Modem GPS not supported");
    }

    @Override
    public void disableGps() throws KuraException {
        logger.warn("Modem GPS not supported");
    }

    @Override
    public String getMobileSubscriberIdentity() throws KuraException {
        synchronized (this.atLock) {
            if (this.imsi == null && isSimCardReady()) {
                logger.debug("sendCommand getIMSI :: {}", HspaModemAtCommands.getIMSI.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getIMSI.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String mobileSubscriberIdentity = getResponseString(reply);
                    if (mobileSubscriberIdentity != null && !mobileSubscriberIdentity.isEmpty()) {
                        if (mobileSubscriberIdentity.startsWith("+CIMI:")) {
                            mobileSubscriberIdentity = mobileSubscriberIdentity.substring("+CIMI:".length()).trim();
                        }
                        this.imsi = mobileSubscriberIdentity;
                    }
                }
            }
        }
        return this.imsi;
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        synchronized (this.atLock) {
            if (this.iccid == null && isSimCardReady()) {
                logger.debug("sendCommand getICCID :: {}", HspaModemAtCommands.getICCID.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    reply = commAtConnection.sendCommand(HspaModemAtCommands.getICCID.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
                if (reply != null) {
                    String cirquitCardId = getResponseString(reply);
                    if (cirquitCardId != null && !cirquitCardId.isEmpty()) {
                        if (cirquitCardId.startsWith("+CCID:")) {
                            cirquitCardId = cirquitCardId.substring("+CCID:".length()).trim();
                        }
                        this.iccid = cirquitCardId;
                    }
                }
            }
        }
        return this.iccid;
    }

    @Override
    public String getDataPort() throws KuraException {
        String port;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getDataPort());
                } else {
                    throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No PPP serial port available");
                }
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
        String port;
        List<String> ports = this.device.getSerialPorts();
        if (ports != null && !ports.isEmpty()) {
            if (this.device instanceof UsbModemDevice) {
                SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) this.device);
                if (usbModemInfo != null) {
                    port = ports.get(usbModemInfo.getAtPort());
                } else {
                    throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No AT serial port available");
                }
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
                    throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No GPS serial port available");
                }
            } else {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
            }
        } else {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No serial ports available");
        }
        return port;
    }

    @Override
    public ModemDevice getModemDevice() {
        return this.device;
    }

    public void setModemDevice(ModemDevice device) {
        this.device = device;
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

    protected CommConnection openSerialPort(String port) throws KuraException {

        CommConnection connection = null;
        if (this.connectionFactory != null) {
            String uri = new CommURI.Builder(port).withBaudRate(115200).withDataBits(8).withStopBits(1).withParity(0)
                    .withTimeout(2000).build().toString();

            try {
                connection = (CommConnection) this.connectionFactory.createConnection(uri, 1, false);
            } catch (Exception e) {
                logger.debug("Exception creating connection: " + e);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e, "Connection Failed");
            }
        }
        return connection;
    }

    @Override
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
        List<ModemPdpContext> pdpContextInfo = new ArrayList<>();
        synchronized (this.atLock) {
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(formGetPdpContextAtCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sreply = this.getResponseString(reply);
                Scanner scanner = new Scanner(sreply);
                while (scanner.hasNextLine()) {
                    String[] tokens = scanner.nextLine().split(",");
                    if (!tokens[0].startsWith("+CGDCONT:")) {
                        continue;
                    }
                    int contextNo = Integer.parseInt(tokens[0].substring("+CGDCONT:".length()).trim());
                    ModemPdpContextType pdpType = ModemPdpContextType
                            .getContextType(tokens[1].substring(1, tokens[1].length() - 1));
                    String apn = tokens[2].substring(1, tokens[2].length() - 1);
                    ModemPdpContext modemPdpContext = new ModemPdpContext(contextNo, pdpType, apn);
                    pdpContextInfo.add(modemPdpContext);
                }
                scanner.close();
            }
        }
        return pdpContextInfo;
    }

    protected void closeSerialPort(CommConnection connection) throws KuraException {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
        }
    }

    protected boolean isAtReachable(CommConnection connection) {

        boolean status = false;
        int attemptNo = 0;
        do {
            try {
                logger.trace("isAtReachable() :: sending AT commnd to modem on port {}", connection.getURI().getPort());
                byte[] reply = connection.sendCommand(HspaModemAtCommands.at.getCommand().getBytes(), 1000, 100);
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

    protected UsbModemDriver getModemDriver() {

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

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {

        ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
        synchronized (this.atLock) {
            logger.debug("sendCommand getRegistrationStatus :: {}",
                    HspaModemAtCommands.getRegistrationStatus.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(HspaModemAtCommands.getRegistrationStatus.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sRegStatus = getResponseString(reply);
                String[] regStatusSplit = sRegStatus.split(",");
                if (regStatusSplit.length >= 2) {
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
        synchronized (this.atLock) {
            logger.debug("sendCommand getMobileStationClass :: {}",
                    HspaModemAtCommands.getMobileStationClass.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(HspaModemAtCommands.getMobileStationClass.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sCgclass = this.getResponseString(reply);
                if (sCgclass.startsWith("+CGCLASS:")) {
                    sCgclass = sCgclass.substring("+CGCLASS:".length()).trim();
                    if ("\"A\"".equals(sCgclass)) {
                        serviceType = "UMTS";
                    } else if ("\"B\"".equals(sCgclass)) {
                        serviceType = "GSM/GPRS";
                    } else if ("\"CG\"".equals(sCgclass)) {
                        serviceType = "GPRS";
                    } else if ("\"CC\"".equals(sCgclass)) {
                        serviceType = "GSM";
                    }
                }
            }
        }

        return serviceType;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {

        List<ModemTechnologyType> modemTechnologyTypes;
        ModemDevice dev = getModemDevice();
        if (dev == null) {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No modem device");
        }
        if (dev instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) dev);
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

    @Override
    @Deprecated
    public ModemTechnologyType getTechnologyType() {
        ModemTechnologyType modemTechnologyType = null;
        try {
            List<ModemTechnologyType> modemTechnologyTypes = getTechnologyTypes();
            if (modemTechnologyTypes != null && !modemTechnologyTypes.isEmpty()) {
                modemTechnologyType = modemTechnologyTypes.get(0);
            }
        } catch (KuraException e) {
            logger.error("Failed to obtain modem technology - {}", e);
        }
        return modemTechnologyType;
    }

    @Override
    public boolean isSimCardReady() throws KuraException {
        boolean status = false;
        synchronized (this.atLock) {
            logger.debug("sendCommand isSimCardReady :: {}", HspaModemAtCommands.getSimPinStatus.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(HspaModemAtCommands.getSimPinStatus.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
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

    private String formGetPdpContextAtCommand() {
        StringBuilder sb = new StringBuilder(HspaModemAtCommands.pdpContext.getCommand());
        sb.append("?\r\n");
        return sb.toString();
    }
}
