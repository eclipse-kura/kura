/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.modem.quectel.generic;

import java.io.IOException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuectelGeneric extends HspaModem implements HspaCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(QuectelGeneric.class);
    private static final String MODEM_NOT_AVAILABLE = "Modem not available for AT commands: ";

    public QuectelGeneric(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

        super(device, platform, connectionFactory);

        if (device != null) {
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
                logger.error("Failed to initialize " + QuectelGeneric.class.getName(), e);
            }
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
                    QuectelGenericAtCommands.GET_SIM_STATUS.getCommand(), port);
            byte[] reply = null;
            CommConnection commAtConnection = null;
            try {

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE + QuectelGeneric.class.getName());
                }

                reply = commAtConnection.sendCommand(QuectelGenericAtCommands.GET_SIM_STATUS.getCommand().getBytes(),
                        1000, 100);
                if (reply != null) {
                    String simStatus = getResponseString(reply);
                    String[] simStatusSplit = simStatus.split(",");
                    if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
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
                    QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE + QuectelGeneric.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(
                        QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(), 1000, 100);
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
                        getQueryNetworkInformation();
                        getRegisteredNetwork();
                        getExtendedRegistrationStatus();
                        break;
                    case 3:
                        modemRegistrationStatus = ModemRegistrationStatus.REGISTRATION_DENIED;
                        break;
                    case 4:
                        modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
                        break;
                    case 5:
                        modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_ROAMING;
                        getQueryNetworkInformation();
                        getRegisteredNetwork();
                        getExtendedRegistrationStatus();
                        break;
                    default:
                    }
                }
            }
        }
        return modemRegistrationStatus;
    }

    public void getQueryNetworkInformation() throws KuraException {
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getQueryNetworkInformation :: {}", QuectelGenericAtCommands.GET_QUERY_NETWORK_INFORMATION.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(QuectelGenericAtCommands.GET_QUERY_NETWORK_INFORMATION.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sQnwinfo = this.getResponseString(reply);
                getQueryNetworkInformationReply(sQnwinfo);
            }
        }
    }

    public String[] getQueryNetworkInformationReply(String sQnwinfo) throws KuraException {
        if (sQnwinfo.startsWith("+QNWINFO:")) {
            sQnwinfo = sQnwinfo.substring("+QNWINFO:".length()).trim();
            logger.trace("getQueryNetworkInformation() :: +QNWINFO={}", sQnwinfo);
            asQnwinfo = sQnwinfo.split(",");
            this.band = asQnwinfo[2].substring(1, asQnwinfo[2].length() - 1);
            logger.trace("getBand() :: Band={}", this.band);
            this.radio = asQnwinfo[0].substring(1, asQnwinfo[0].length() - 1);
            logger.info("getRadio() :: Radio={}", this.radio);
        }
        return this.asQnwinfo;
    }

    public void getExtendedRegistrationStatus() throws KuraException {
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getLac :: {}", QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                commAtConnection.sendCommand(QuectelGenericAtCommands.GET_EXTENDED_REGISTRATION_STATUS.getCommand().getBytes(),
                        1000, 100);
                reply = commAtConnection.sendCommand(QuectelGenericAtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
            if (reply != null) {
                String sCgreg = this.getResponseString(reply);
                getExtendedRegistrationStatusReply(sCgreg);
            }
        }
    }

    public String[] getExtendedRegistrationStatusReply(String sCgreg) throws KuraException {
        int sLac;
        int sCi;
        if (sCgreg.startsWith("+CGREG:")) {
            sCgreg = sCgreg.substring("+CGREG:".length()).trim();
            logger.trace("getLac() :: +CGREG={}", sCgreg);
            asCgreg = sCgreg.split(",");
            sLac = Integer.parseInt(asCgreg[2].substring(1, asCgreg[2].length() - 1), 16);
            sCi = Integer.parseInt(asCgreg[3].substring(1, asCgreg[3].length() - 1), 16);
            this.lac = String.valueOf(sLac);
            this.ci = String.valueOf(sCi);
        }
        return asCgreg;
    }

    @Override
    public void getRegisteredNetwork() throws KuraException {
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getRegisteredNetwork :: {}", QuectelGenericAtCommands.GET_REGISTERED_NETWORK.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection.sendCommand(QuectelGenericAtCommands.GET_REGISTERED_NETWORK.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sQspn = this.getResponseString(reply);
                getRegisteredNetworkReply(sQspn);
            }
        }
    }

    public String[] getRegisteredNetworkReply(String sQspn) throws KuraException {
        if (sQspn.startsWith("+QSPN:")) {
            sQspn = sQspn.substring("+QSPN:".length()).trim();
            logger.info("getRegisteredNetwork() :: +QSPN={}", sQspn);
            asQspn = sQspn.split(",");
            this.plmnid = asQspn[4];
            if (this.plmnid.length() == 7 || this.plmnid.length() == 8) {
                this.plmnid = this.plmnid.substring(1, this.plmnid.length() - 1);
                logger.trace("getPLMNID() :: PLMNID={}", this.plmnid);
            }
            this.networkName = asQspn[0].substring(1, asQspn[0].length() - 1);
            logger.info("getNetworkName() :: Network={}", this.networkName);
        }
        return this.asQspn;
    }

    @Override
    public long getCallTxCounter() throws KuraException {

        long txCnt = 0;
        synchronized (this.atLock) {
            logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    QuectelGenericAtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE + QuectelGeneric.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(
                        QuectelGenericAtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] sDataVolume = this.getResponseString(reply).split(" ");
                if (sDataVolume.length >= 2) {
                    txCnt = Integer.parseInt(sDataVolume[1].split(",")[0]);
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
                    QuectelGenericAtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE + QuectelGeneric.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(
                        QuectelGenericAtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] sDataVolume = this.getResponseString(reply).split(" ");
                if (sDataVolume.length >= 2) {
                    rxCnt = Integer.parseInt(sDataVolume[1].split(",")[1]);
                }
            }
        }
        return rxCnt;
    }

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getMobileStationClass :: {}",
                    QuectelGenericAtCommands.GET_MOBILESTATION_CLASS.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE + QuectelGeneric.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(
                        QuectelGenericAtCommands.GET_MOBILESTATION_CLASS.getCommand().getBytes(), 1000, 100);
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
    public boolean isGpsSupported() throws KuraException {
        return false; // Will be activated later
    }

    @Override
    public void enableGps() throws KuraException {
        throw new UnsupportedOperationException("Modem GPS not supported");
    }

    @Override
    public void disableGps() throws KuraException {
        throw new UnsupportedOperationException("Modem GPS not supported");
    }

    @Override
    public void reset() throws KuraException {
        while (true) {
            sleep(5000);
            try {
                turnOff();
                sleep(1000);
                turnOn();
                logger.info("reset() :: modem reset successful");
                break;
            } catch (Exception e) {
                logger.error("Failed to reset the modem", e);
            }
        }
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

    @Override
    public boolean hasDiversityAntenna() {
        return false; // To be activated later
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
