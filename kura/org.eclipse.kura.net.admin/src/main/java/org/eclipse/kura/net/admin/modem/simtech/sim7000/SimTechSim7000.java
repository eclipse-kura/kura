/*******************************************************************************
 * Copyright (c) 2020 3 PORT d.o.o. and others
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

package org.eclipse.kura.net.admin.modem.simtech.sim7000;

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
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimTechSim7000 extends HspaModem implements HspaCellularModem {

    private static final String MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS = "Modem not available for AT commands: ";
    private static final Logger logger = LoggerFactory.getLogger(SimTechSim7000.class);

    public SimTechSim7000(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

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
            logger.error("Failed to initialize " + SimTechSim7000.class.getName(), e);
        }
    }

    @Override
    public boolean isSimCardReady() throws KuraException {
        boolean status = false;
        synchronized (this.atLock) {
            logger.debug("sendCommand isSimCardReady :: {}", SimTechSim7000AtCommands.getSimPinStatus.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS + SimTechSim7000.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(SimTechSim7000AtCommands.getSimPinStatus.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String sReply = new String(reply);
                if (sReply.contains("READY")) {
                    status = true;
                }
            }
        }
        return status;
    }

    @Override
    public String getServiceType() throws KuraException {
        String serviceType = null;
        synchronized (this.atLock) {
            logger.debug("sendCommand getModuleStatus :: {}", SimTechSim7000AtCommands.getModuleStatus.getCommand());
            byte[] reply = null;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_COMMANDS + SimTechSim7000.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(SimTechSim7000AtCommands.getModuleStatus.getCommand().getBytes(),
                        1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String zpas = this.getResponseString(reply);
                if (zpas.startsWith("+CNSMOD:")) {
                    String[] zPasNetwork = zpas.substring("+CNSMOD:".length()).trim().split(",");
                    if (zPasNetwork.length > 0) {
                        int type = Integer.parseInt(zPasNetwork[1]);
                        if (type == 0) {
                            serviceType = "No Service";
                        } else if (type == 1) {
                            serviceType = "GSM";
                        } else if (type == 3) {
                            serviceType = "EGPRS";
                        } else if (type == 7) {
                            serviceType = "LTE M1";
                        } else if (type == 9) {
                            serviceType = "LTE NB";
                        }

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
                    MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + SimTechSim7000.class.getName());
        }
        try {
            reply = comm.sendCommand(
                    (SimTechSim7000AtCommands.pdpContext.getCommand() + "?\r\n").getBytes(StandardCharsets.US_ASCII),
                    1000, 100);
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
    public int getSignalStrength() throws KuraException {
        int signalStrength = -115;
        synchronized (this.atLock) {
            String atPort = getAtPort();

            logger.debug("sendCommand getSignalStrength :: {}",
                    SimTechSim7000AtCommands.getSignalStrength.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(atPort);
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + SimTechSim7000.class.getName());
            }
            try {
                reply = commAtConnection.sendCommand(SimTechSim7000AtCommands.getSignalStrength.getCommand().getBytes(),
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
                        if (rssiVal == 0) {
                            signalStrength = -115;
                        } else if (rssiVal == 1) {
                            signalStrength = -111;
                        } else if (rssiVal < 99) {
                            signalStrength = -110 + (2 * (rssiVal - 2));
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

}
