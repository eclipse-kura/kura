/*******************************************************************************
 * Copyright (c) 2019, 2021 3 PORT d.o.o. and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  3 PORT d.o.o.
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.zte.me3630;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZteMe3630 extends HspaModem implements HspaCellularModem {

    private static final String MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS = "Modem not available for AT commands: ";
    private static final Logger logger = LoggerFactory.getLogger(ZteMe3630.class);

    public ZteMe3630(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

        super(device, platform, connectionFactory);

        try {
            String atPort = getAtPort();
            String gpsPort = getGpsPort();
            if (atPort != null && (atPort.equals(getDataPort()) || atPort.equals(gpsPort))) {
                this.serialNumber = getSerialNumber();
                this.imsi = getMobileSubscriberIdentity();
                this.iccid = getIntegratedCirquitCardId();
                this.model = getModel();
                this.manufacturer = getManufacturer();
                this.revisionId = getRevisionID();
                this.gpsSupported = isGpsSupported();
                this.rssi = getSignalStrength();

                logger.trace("{} :: Serial Number={}", getClass().getName(), this.serialNumber);
                logger.trace("{} :: IMSI={}", getClass().getName(), this.imsi);
                logger.trace("{} :: ICCID={}", getClass().getName(), this.iccid);
                logger.trace("{} :: Model={}", getClass().getName(), this.model);
                logger.trace("{} :: Manufacturer={}", getClass().getName(), this.manufacturer);
                logger.trace("{} :: Revision ID={}", getClass().getName(), this.revisionId);
                logger.trace("{} :: GPS Supported={}", getClass().getName(), this.gpsSupported);
                logger.trace("{} :: RSSI={}", getClass().getName(), this.rssi);
            }
        } catch (KuraException e) {
            logger.error("Failed to initialize " + ZteMe3630.class.getName(), e);
        }
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

        synchronized (this.atLock) {
            logger.debug("sendCommand getSysInfo :: {} command to port {}", ZteMe3630AtCommands.getSysInfo.getCommand(),
                    port);
            byte[] reply = null;
            CommConnection commAtConnection = null;
            try {

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS + ZteMe3630.class.getName());
                }

                reply = commAtConnection.sendCommand(ZteMe3630AtCommands.getSysInfo.getCommand().getBytes(), 1000, 100);
                if (reply != null) {
                    String sysStatus = getResponseString(reply);
                    String[] sysStatusSplit = sysStatus.split(",");
                    // 0 = invalid card status
                    // 1 = valid card status
                    // 255 = card not existed or PIN required
                    if (sysStatusSplit.length > 4 && Integer.valueOf(sysStatusSplit[4]) == 1) {
                        simReady = true;
                    }
                }

            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, e);
            } catch (KuraException e) {
                throw e;
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return simReady;
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {

        ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
        synchronized (this.atLock) {
            logger.debug("sendCommand getRegistrationStatus :: {}",
                    ZteMe3630AtCommands.getRegistrationStatus.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS + ZteMe3630.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(ZteMe3630AtCommands.getRegistrationStatus.getCommand().getBytes(),
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
                    default:
                        modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
                    }
                }
            }
        }
        return modemRegistrationStatus;
    }

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getModuleStatus :: {}", ZteMe3630AtCommands.getModuleStatus.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS + ZteMe3630.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(ZteMe3630AtCommands.getModuleStatus.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String zpas = this.getResponseString(reply);
                if (zpas.startsWith("+ZPAS:")) {
                    String[] zPasNetwork = zpas.substring("+ZPAS:".length()).trim().split(",");
                    // No Service, Limited Service, GSM, GPRS, CDMA, EVDO, EHRPD, UMTS, HSDPA, HSUPA, HSPA, HSPA+, LTE,
                    // TD-SCDMA
                    if (zPasNetwork.length > 0) {
                        serviceType = zPasNetwork[0].replaceAll("\"", "");
                    }
                }
            }
        }

        return serviceType;
    }

    @Override
    public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {

        List<ModemTechnologyType> modemTechnologyTypes = null;
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

    @Override
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                return getPdpContextInfo(commAtConnection);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
    }

    protected List<ModemPdpContext> getPdpContextInfo(CommConnection comm) throws KuraException {
        List<ModemPdpContext> pdpContextInfo = new ArrayList<>();
        byte[] reply;
        if (!isAtReachable(comm)) {
            closeSerialPort(comm);
            throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                    MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + ZteMe3630.class.getName());
        }
        try {
            reply = comm.sendCommand(
                    (ZteMe3630AtCommands.pdpContext.getCommand() + "?\r\n").getBytes(StandardCharsets.US_ASCII), 1000,
                    100);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
        }
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
        return pdpContextInfo;
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        synchronized (this.atLock) {
            if (this.iccid == null && isSimCardReady()) {
                logger.debug("sendCommand getICCID :: {}", ZteMe3630AtCommands.getICCID.getCommand());
                byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + ZteMe3630.class.getName());
                }
                try {
                    reply = commAtConnection.sendCommand(ZteMe3630AtCommands.getICCID.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
                if (reply != null) {
                    String cirquitCardId = getResponseString(reply);
                    if (cirquitCardId != null && !cirquitCardId.isEmpty()) {
                        if (cirquitCardId.startsWith("+ZGETICCID:")) {
                            cirquitCardId = cirquitCardId.substring("+ZGETICCID:".length()).trim();
                        }
                        this.iccid = cirquitCardId;
                    }
                }
            }
        }
        return this.iccid;
    }

    @Override
    public int getSignalStrength() throws KuraException {
        int signalStrength = -113;
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getSignalStrength :: {}", ZteMe3630AtCommands.getSignalStrength.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + ZteMe3630.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(ZteMe3630AtCommands.getSignalStrength.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] asCsq;
                String sCsq = this.getResponseString(reply).toUpperCase();
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
        return false; // Will be activated later
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
    public String getFirmwareVersion() throws KuraException {
        return "N/A";
    }

}