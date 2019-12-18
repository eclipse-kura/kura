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
package org.eclipse.kura.net.admin.modem.quectel.bg96;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines Quectel BG96 modem
 */
public class QuectelBG96 extends HspaModem implements HspaCellularModem {

    private static final Logger s_logger = LoggerFactory.getLogger(QuectelBG96.class);

    private final int m_pdpContext = 1;

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
    public QuectelBG96(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

        super(device, platform, connectionFactory);

        try {
            String atPort = getAtPort();
            String gpsPort = getGpsPort();
            if (atPort != null) {
                if (atPort.equals(getDataPort()) || atPort.equals(gpsPort)) {
                    this.serialNumber = getSerialNumber();
                    this.imsi = getMobileSubscriberIdentity();
                    this.iccid = getIntegratedCirquitCardId();
                    this.model = getModel();
                    this.manufacturer = getManufacturer();
                    this.revisionId = getRevisionID();
                    this.gpsSupported = isGpsSupported();
                    this.rssi = getSignalStrength();

                    s_logger.trace("{} :: Serial Number={}", getClass().getName(), this.serialNumber);
                    s_logger.trace("{} :: IMSI={}", getClass().getName(), this.imsi);
                    s_logger.trace("{} :: ICCID={}", getClass().getName(), this.iccid);
                    s_logger.trace("{} :: Model={}", getClass().getName(), this.model);
                    s_logger.trace("{} :: Manufacturer={}", getClass().getName(), this.manufacturer);
                    s_logger.trace("{} :: Revision ID={}", getClass().getName(), this.revisionId);
                    s_logger.trace("{} :: GPS Supported={}", getClass().getName(), this.gpsSupported);
                    s_logger.trace("{} :: RSSI={}", getClass().getName(), this.rssi);
                }
            }
        } catch (KuraException e) {
            e.printStackTrace();
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
            s_logger.debug("sendCommand getSimStatus :: {} command to port {}",
                    QuectelBG96AtCommands.getSimStatus.getCommand(), port);
            byte[] reply = null;
            CommConnection commAtConnection = null;
            try {

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            "Modem not available for AT commands: " + QuectelBG96.class.getName());
                }

                reply = commAtConnection.sendCommand(QuectelBG96AtCommands.getSimStatus.getCommand().getBytes(), 1000,
                        100);
                if (reply != null) {
                    String simStatus = getResponseString(reply);
                    String[] simStatusSplit = simStatus.split(",");
                    if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
                        simReady = true;
                    }
                }

                if (!simReady) {
                    // reply = commAtConnection.sendCommand(
                    // QuectelBG96AtCommands.simulateSimNotInserted.getCommand().getBytes(), 1000, 100);
                    // if (reply != null) {
                    // sleep(5000);
                    // reply = commAtConnection.sendCommand(
                    // QuectelBG96AtCommands.simulateSimInserted.getCommand().getBytes(), 1000, 100);
                    // if (reply != null) {
                    // sleep(1000);
                    // reply = commAtConnection
                    // .sendCommand(QuectelBG96AtCommands.getSimStatus.getCommand().getBytes(), 1000, 100);
                    //
                    // if (reply != null) {
                    // String simStatus = getResponseString(reply);
                    // String[] simStatusSplit = simStatus.split(",");
                    // if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
                    // simReady = true;
                    // }
                    // }
                    // }
                    // }
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
            s_logger.debug("sendCommand getRegistrationStatus :: {}",
                    QuectelBG96AtCommands.getRegistrationStatus.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + QuectelBG96.class.getName());
            }
            try {
                reply = commAtConnection
                        .sendCommand(QuectelBG96AtCommands.getRegistrationStatus.getCommand().getBytes(), 1000, 100);
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

        long txCnt = 0;
        synchronized (this.atLock) {
            s_logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    QuectelBG96AtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + QuectelBG96.class.getName());
            }
            try {
                reply = commAtConnection
                        .sendCommand(QuectelBG96AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp = null;
                String[] splitData = null;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("#GDATAVOL:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 4) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.m_pdpContext) {
                                    txCnt = Integer.parseInt(splitData[2]);
                                }
                            }
                        }
                    }
                }
                reply = null;
            }
        }
        return txCnt;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
        long rxCnt = 0;
        synchronized (this.atLock) {
            s_logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    QuectelBG96AtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + QuectelBG96.class.getName());
            }
            try {
                reply = commAtConnection
                        .sendCommand(QuectelBG96AtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp = null;
                String[] splitData = null;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("#GDATAVOL:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 4) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.m_pdpContext) {
                                    rxCnt = Integer.parseInt(splitData[3]);
                                }
                            }
                        }
                    }
                }
                reply = null;
            }
        }
        return rxCnt;
    }

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            s_logger.debug("sendCommand getMobileStationClass :: {}",
                    QuectelBG96AtCommands.getMobileStationClass.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        "Modem not available for AT commands: " + QuectelBG96.class.getName());
            }
            try {
                reply = commAtConnection
                        .sendCommand(QuectelBG96AtCommands.getMobileStationClass.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
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
        return new ArrayList<>();
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        return false; // Will be activated later
    }

    @Override
    public void enableGps() throws KuraException {
        s_logger.warn("Modem GPS not supported");
    }

    @Override
    public void disableGps() throws KuraException {
        s_logger.warn("Modem GPS not supported");
    }

}
