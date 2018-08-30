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
package org.eclipse.kura.net.admin.modem.telit.he910;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
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

    private static final Logger logger = LoggerFactory.getLogger(TelitHe910.class);

    // FIXME PDP context should not be hard coded
    private static final int PDP_CONTEXT = 1;

    /**
     * TelitHe910 modem constructor
     *
     * @param usbDevice
     *            - modem USB device as {@link UsbModemDevice}
     * @param platform
     *            - hardware platform as {@link String}
     * @param connectionFactory
     *            - connection factory {@link ConnectionFactory}
     */
    public TelitHe910(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

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
            logger.error("Failed to initialize TelitHe910", e);
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
            logger.debug("sendCommand getSimStatus :: {} command to port {}",
                    TelitHe910AtCommands.getSimStatus.getCommand(), port);
            byte[] reply = null;
            CommConnection commAtConnection = null;
            try {

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }

                reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 1000,
                        100);
                if (reply != null) {
                    String simStatus = getResponseString(reply);
                    String[] simStatusSplit = simStatus.split(",");
                    if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
                        simReady = true;
                    }
                }

                if (!simReady) {
                    reply = commAtConnection.sendCommand(
                            TelitHe910AtCommands.simulateSimNotInserted.getCommand().getBytes(), 1000, 100);
                    if (reply != null) {
                        sleep(5000);
                        reply = commAtConnection.sendCommand(
                                TelitHe910AtCommands.simulateSimInserted.getCommand().getBytes(), 1000, 100);
                        if (reply != null) {
                            sleep(1000);
                            reply = commAtConnection
                                    .sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 1000, 100);

                            if (reply != null) {
                                String simStatus = getResponseString(reply);
                                String[] simStatusSplit = simStatus.split(",");
                                if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
                                    simReady = true;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
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
                    TelitHe910AtCommands.getRegistrationStatus.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitHe910AtCommands.getRegistrationStatus.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
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
        synchronized (this.atLock) {
            logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection
                        .sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp;
                String[] splitData;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("#GDATAVOL:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 4) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.PDP_CONTEXT) {
                                    txCnt = Integer.parseInt(splitData[2]);
                                }
                            }
                        }
                    }
                }
            }
        }
        return txCnt;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
        long rxCnt = 0;
        synchronized (this.atLock) {
            logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    TelitHe910AtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection
                        .sendCommand(TelitHe910AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp;
                String[] splitData;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("#GDATAVOL:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 4) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.PDP_CONTEXT) {
                                    rxCnt = Integer.parseInt(splitData[3]);
                                }
                            }
                        }
                    }
                }
            }
        }
        return rxCnt;
    }

    @Override
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
        List<ModemPdpContext> pdpContextInfo = new ArrayList<>();
        synchronized (this.atLock) {
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
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

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getMobileStationClass :: {}",
                    TelitHe910AtCommands.getMobileStationClass.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitHe910AtCommands.getMobileStationClass.getCommand().getBytes(),
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

    private String formGetPdpContextAtCommand() {
        StringBuilder sb = new StringBuilder(TelitHe910AtCommands.pdpContext.getCommand());
        sb.append("?\r\n");
        return sb.toString();
    }
}
