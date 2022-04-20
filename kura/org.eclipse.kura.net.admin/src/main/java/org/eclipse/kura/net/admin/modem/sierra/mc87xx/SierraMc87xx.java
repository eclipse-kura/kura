/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.mc87xx;

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
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.util.SerialUtil;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SierraMc87xx implements HspaCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(SierraMc87xx.class);

    protected static final String MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG = "Modem not available for AT commands";

    private ConnectionFactory connectionFactory = null;

    private String model = null;
    private String manufacturer = null;
    private String serialNumber = null;
    private String revisionId = null;

    private final Object atLock = new Object();

    private ModemDevice device = null;
    private List<NetConfig> netConfigs = null;

    public SierraMc87xx(ModemDevice device, ConnectionFactory connectionFactory) {

        this.device = device;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public String getModel() throws KuraException {
        synchronized (this.atLock) {
            if (this.model == null) {
                logger.debug("sendCommand getModelNumber :: {}", SierraMc87xxAtCommands.getModelNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }

                    reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getModelNumber.getCommand().getBytes(),
                            1000, 100);
                    if (reply != null) {
                        this.model = getResponseString(reply);
                    }
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.model;
    }

    @Override
    public String getManufacturer() throws KuraException {
        synchronized (this.atLock) {
            if (this.manufacturer == null) {
                logger.debug("sendCommand getManufacturer :: {}", SierraMc87xxAtCommands.getManufacturer.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getManufacturer.getCommand().getBytes(),
                            1000, 100);

                    if (reply != null) {
                        this.manufacturer = getResponseString(reply);
                    }
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.manufacturer;
    }

    @Override
    public String getSerialNumber() throws KuraException {
        synchronized (this.atLock) {
            if (this.serialNumber == null) {
                logger.debug("sendCommand getSerialNumber :: {}", SierraMc87xxAtCommands.getSerialNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSerialNumber.getCommand().getBytes(),
                            1000, 100);
                    if (reply != null) {
                        this.serialNumber = getResponseString(reply);
                    }
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.serialNumber;
    }

    @Override
    public String getMobileSubscriberIdentity() throws KuraException {
        return null;
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        return null;
    }

    @Override
    public String getRevisionID() throws KuraException {
        synchronized (this.atLock) {
            if (this.revisionId == null) {
                logger.debug("sendCommand getRevision :: {}", SierraMc87xxAtCommands.getFirmwareVersion.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection
                            .sendCommand(SierraMc87xxAtCommands.getFirmwareVersion.getCommand().getBytes(), 1000, 100);
                    if (reply != null) {
                        String firmwareVersion = getResponseString(reply);
                        if (firmwareVersion.startsWith("!GVER:")) {
                            firmwareVersion = firmwareVersion.substring("!GVER:".length()).trim();
                            String[] aFirmwareVersion = firmwareVersion.split(" ");
                            this.revisionId = aFirmwareVersion[0];
                        }
                    }
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.revisionId;
    }

    @Override
    public boolean isReachable() throws KuraException {
        boolean ret;
        synchronized (this.atLock) {
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                ret = isAtReachable(commAtConnection);
            } finally {
                closeSerialPort(commAtConnection);
            }
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
                logger.warn("isPortReachable() :: The {} is not reachable", port);
            }
        }
        return ret;
    }

    @Override
    public void reset() throws KuraException {
        // not supported
    }

    @Override
    public int getSignalStrength() throws KuraException {

        int rssi = -113;
        synchronized (this.atLock) {
            logger.debug("sendCommand getSignalStrength :: {}", SierraMc87xxAtCommands.getSignalStrength.getCommand());
            byte[] reply;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSignalStrength.getCommand().getBytes(),
                        1000, 100);

                if (reply != null) {
                    String[] asCsq;
                    String sCsq = this.getResponseString(reply);
                    if (sCsq.startsWith("+CSQ:")) {
                        sCsq = sCsq.substring("+CSQ:".length()).trim();
                        asCsq = sCsq.split(",");
                        if (asCsq.length == 2) {
                            rssi = -113 + 2 * Integer.parseInt(asCsq[0]);

                        }
                    }
                }
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return rssi;
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {

        ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
        synchronized (this.atLock) {
            logger.debug("sendCommand getSystemInfo :: {}", SierraMc87xxAtCommands.getSystemInfo.getCommand());
            byte[] reply;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSystemInfo.getCommand().getBytes(), 1000,
                        100);

                if (reply != null) {
                    String sSysInfo = getResponseString(reply);
                    if (sSysInfo != null && sSysInfo.length() > 0) {
                        String[] aSysInfo = sSysInfo.split(",");
                        if (aSysInfo.length == 5) {
                            int srvStatus = Integer.parseInt(aSysInfo[0]);
                            int roamingStatus = Integer.parseInt(aSysInfo[2]);
                            if (srvStatus == 0) {
                                modemRegistrationStatus = ModemRegistrationStatus.NOT_REGISTERED;
                            } else if (srvStatus == 2 && roamingStatus == 0) {
                                modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_HOME;
                            } else if (srvStatus == 2 && roamingStatus == 1) {
                                modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_ROAMING;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return modemRegistrationStatus;
    }

    @Override
    public long getCallTxCounter() throws KuraException {
        // Not supported via AT interface need to use HIP/CnS
        return 0;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
        // Not supported via AT interface need to use HIP/CnS
        return 0;
    }

    @Override
    public String getServiceType() throws KuraException {

        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getMobileStationClass :: {}",
                    SierraMc87xxAtCommands.getMobileStationClass.getCommand());
            byte[] reply;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                reply = commAtConnection
                        .sendCommand(SierraMc87xxAtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);

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
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }

        return serviceType;
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
        return null;
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        return false;
    }

    @Override
    public void enableGps() throws KuraException {
        // TODO
    }

    @Override
    public void disableGps() throws KuraException {
        // TODO
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
        ModemDevice modemDevice = getModemDevice();
        if (modemDevice == null) {
            throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No modem device");
        }
        if (modemDevice instanceof UsbModemDevice) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) modemDevice);
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
    public boolean isSimCardReady() throws KuraException {

        boolean simReady = false;
        synchronized (this.atLock) {
            logger.debug("sendCommand getSystemInfo :: {}", SierraMc87xxAtCommands.getSystemInfo.getCommand());
            byte[] reply;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + TelitHe910.class.getName());
                }
                reply = commAtConnection.sendCommand(SierraMc87xxAtCommands.getSystemInfo.getCommand().getBytes(), 1000,
                        100);

                if (reply != null) {
                    String sSysInfo = getResponseString(reply);
                    if (sSysInfo != null && sSysInfo.length() > 0) {
                        String[] aSysInfo = sSysInfo.split(",");
                        if (aSysInfo.length == 5) {
                            int simStatus = Integer.parseInt(aSysInfo[4]);
                            if (simStatus == 1) {
                                simReady = true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return simReady;
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
        List<ModemPdpContext> pdpContextInfo = new ArrayList<>();
        synchronized (this.atLock) {
            byte[] reply;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + TelitHe910.class.getName());
                }

                reply = commAtConnection.sendCommand(formGetPdpContextAtCommand().getBytes(), 1000, 100);

                if (reply != null) {
                    String sreply = this.getResponseString(reply);
                    parseReply(pdpContextInfo, sreply);
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return pdpContextInfo;
    }

    private void parseReply(List<ModemPdpContext> pdpContextInfo, String sreply) {
        try (Scanner scanner = new Scanner(sreply)) {
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
        }
    }

    private CommConnection openSerialPort(String port) throws KuraException {

        if (this.connectionFactory != null) {
            return SerialUtil.openSerialPort(this.connectionFactory, port);
        }
        return null;
    }

    private void closeSerialPort(CommConnection connection) throws KuraException {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
        }
    }

    private boolean isAtReachable(CommConnection connection) {

        boolean status = false;
        int attemptNo = 0;
        do {
            try {
                status = connection.sendCommand(SierraMc87xxAtCommands.at.getCommand().getBytes(), 500).length > 0;
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private String formGetPdpContextAtCommand() {
        StringBuilder sb = new StringBuilder(SierraMc87xxAtCommands.pdpContext.getCommand());
        sb.append("?\r\n");
        return sb.toString();
    }

    @Override
    public boolean hasDiversityAntenna() {
        return false;
    }

    @Override
    public boolean isDiversityEnabled() {
        return false;
    }

    @Override
    public void enableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "enableDiversity");
    }

    @Override
    public void disableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, "disableDiversity");
    }

    @Override
    public String getFirmwareVersion() throws KuraException {
        return "N/A";
    }

    @Override
    public String getLAC() throws KuraException {
        return "N/A";
    }

    @Override
    public String getCI() throws KuraException {
        return "N/A";
    }

    @Override
    public String getPLMNID() throws KuraException {
        return "N/A";
    }

    @Override
    public String getBand() throws KuraException {
        return "N/A";
    }

    @Override
    public String getNetworkName() throws KuraException {
        return "N/A";
    }

    @Override
    public String getRadio() throws KuraException {
        return "N/A";
    }
}
