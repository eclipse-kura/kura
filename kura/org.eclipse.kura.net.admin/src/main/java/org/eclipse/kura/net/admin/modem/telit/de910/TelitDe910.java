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
package org.eclipse.kura.net.admin.modem.telit.de910;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.ModemCdmaServiceProvider;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitDe910 extends TelitModem implements EvdoCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(TelitDe910.class);

    /**
     * TelitDe910 modem constructor
     *
     * @param usbDevice
     *            - modem USB device as {@link UsbModemDevice}
     * @param platform
     *            - hardware platform as {@link String}
     * @param connectionFactory
     *            - connection factory {@link ConnectionFactory}
     */
    public TelitDe910(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

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

                logger.trace("TelitDe910() :: Serial Number={}", this.serialNumber);
                logger.trace("TelitDe910() :: IMSI={}", this.imsi);
                logger.trace("TelitDe910() :: ICCID={}", this.iccid);
                logger.trace("TelitDe910() :: Model={}", this.model);
                logger.trace("TelitDe910() :: Manufacturer={}", this.manufacturer);
                logger.trace("TelitDe910() :: Revision ID={}", this.revisionId);
                logger.trace("TelitDe910() :: GPS Supported={}", this.gpsSupported);
                logger.trace("TelitDe910() :: RSSI={}", this.rssi);
            }
        } catch (KuraException e) {
            logger.error("Failed to initialize TelitDe910", e);
        }
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        return "";
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
        ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
        synchronized (this.atLock) {
            logger.debug("sendCommand getRegistrationStatus :: {}",
                    TelitDe910AtCommands.getNetRegistrationStatus.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection
                        .sendCommand(TelitDe910AtCommands.getNetRegistrationStatus.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sRegStatus = getResponseString(reply);
                if (sRegStatus.startsWith("+CREG:")) {
                    sRegStatus = sRegStatus.substring("+CREG:".length()).trim();
                }
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
                    TelitDe910AtCommands.getSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(),
                        1000, 100);
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
                                txCnt = Integer.parseInt(splitData[2]);
                            }
                            break;
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
                    TelitDe910AtCommands.getSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitDe910AtCommands.getSessionDataVolume.getCommand().getBytes(),
                        1000, 100);
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
                                rxCnt = Integer.parseInt(splitData[3]);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return rxCnt;
    }

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getServiceType :: {}", TelitDe910AtCommands.getServiceType.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitDe910AtCommands.getServiceType.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sServiceType = this.getResponseString(reply);
                if (sServiceType.startsWith("+SERVICE:")) {
                    sServiceType = sServiceType.substring("+SERVICE:".length()).trim();
                    int servType = Integer.parseInt(sServiceType);
                    switch (servType) {
                    case 0:
                        serviceType = "No Service";
                        break;
                    case 1:
                        serviceType = "1xRTT";
                        break;
                    case 2:
                        serviceType = "EVDO Release 0";
                        break;
                    case 3:
                        serviceType = "EVDO Release A";
                        break;
                    case 4:
                        serviceType = "GPRS";
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        return serviceType;
    }

    @Override
    public String getMobileDirectoryNumber() throws KuraException {

        String sMdn = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getMdn :: {}", TelitDe910AtCommands.getMdn.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMdn.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                sMdn = getResponseString(reply);
                if (sMdn.startsWith("#MODEM:")) {
                    sMdn = sMdn.substring("#MODEM:".length()).trim();
                }
            }
        }
        return sMdn;
    }

    @Override
    public String getMobileIdentificationNumber() throws KuraException {

        String sMsid = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getMsid :: {}", TelitDe910AtCommands.getMsid.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(TelitDe910AtCommands.getMsid.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                sMsid = getResponseString(reply);
                if (sMsid.startsWith("#MODEM:")) {
                    sMsid = sMsid.substring("#MODEM:".length()).trim();
                }
            }
        }
        return sMsid;
    }

    @Override
    public ModemCdmaServiceProvider getServiceProvider() throws KuraException {

        ModemCdmaServiceProvider cdmaSerciceProvider = ModemCdmaServiceProvider.UNKNOWN;
        if (this.revisionId == null) {
            getRevisionID();
        }
        if (this.revisionId != null && this.revisionId.length() >= 9) {
            int provider = Integer.parseInt(this.revisionId.substring(7, 8));

            if (provider == TelitDe910ServiceProviders.SPRINT.getProvider()) {
                cdmaSerciceProvider = ModemCdmaServiceProvider.SPRINT;
            } else if (provider == TelitDe910ServiceProviders.AERIS.getProvider()) {
                cdmaSerciceProvider = ModemCdmaServiceProvider.AERIS;
            } else if (provider == TelitDe910ServiceProviders.VERIZON.getProvider()) {
                cdmaSerciceProvider = ModemCdmaServiceProvider.VERIZON;
            }
        }
        return cdmaSerciceProvider;
    }

    @Override
    public boolean isProvisioned() throws KuraException {
        boolean ret = false;
        String mdn = getMobileDirectoryNumber();
        if (mdn != null && mdn.length() > 4 && !mdn.startsWith("0000")) {
            ret = true;
        }
        return ret;
    }

    @Override
    public void provision() throws KuraException {

        if (getServiceProvider() == ModemCdmaServiceProvider.VERIZON) {

            logger.info("will make an attempt to provision DE910-DUAL modem on VERIZON network");

            boolean startOTASPsession = false;
            ModemRegistrationStatus regStatus = getRegistrationStatus();
            if (regStatus == ModemRegistrationStatus.REGISTERED_ROAMING) {
                logger.warn("The DE910-DUAL cannot typically be fully provisioned while roaming");
                startOTASPsession = true;
            } else if (regStatus == ModemRegistrationStatus.REGISTERED_HOME) {
                logger.info("The DE910-DUAL is registered on the network");
                startOTASPsession = true;
            } else if (regStatus == ModemRegistrationStatus.NOT_REGISTERED) {
                logger.warn("The DE910-DUAL is not registered on the network, provision session aborted");
            } else {
                logger.error("Unsupported network registration status, provision session aborted");
            }

            if (startOTASPsession) {
                logger.info("Starting 'OTASP' provision session");
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }
                try {
                    commAtConnection.sendCommand(TelitDe910AtCommands.provisionVerizon.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                }
                closeSerialPort(commAtConnection);
            }

            logger.info("waiting for OTASP session to complete ...");
            sleep(180000);
        }
    }

    @Override
    public boolean isTelitSimCardReady() throws KuraException {
        return true;
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

    @Override
    public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
        return new ArrayList<>();
    }
}
