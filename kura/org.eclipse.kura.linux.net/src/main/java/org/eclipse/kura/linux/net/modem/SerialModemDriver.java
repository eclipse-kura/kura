/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.io.ConnectionFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialModemDriver extends ModemDriver {

    private static final Logger logger = LoggerFactory.getLogger(SerialModemDriver.class);

    private static final String OS_VERSION = System.getProperty("kura.os.version");
    private static final String TARGET_NAME = System.getProperty("target.device");

    private final SerialModemComm serialModemComm;
    private final String getModelAtCommand;
    private final String modemName;
    private String modemModel;

    private final ServiceTracker<ConnectionFactory, ConnectionFactory> m_serviceTracker;

    public SerialModemDriver(String modemName, SerialModemComm serialModemComm, String getModelAtCommand) {

        this.modemName = modemName;
        this.serialModemComm = serialModemComm;
        this.getModelAtCommand = getModelAtCommand;
        BundleContext bundleContext = FrameworkUtil.getBundle(SerialModemDriver.class).getBundleContext();

        this.m_serviceTracker = new ServiceTracker<>(bundleContext, ConnectionFactory.class, null);
        this.m_serviceTracker.open(true);
    }

    public int install() throws KuraException {
        int status = -1;
        boolean modemReachable = false;

        try {
            modemReachable = isAtReachable(3, 1000);
        } catch (KuraException kuraEx) {
            logger.warn("Exception reaching serial modem ... ", kuraEx);
            try {
                unlockSerialPort();
                sleep(2000);
                modemReachable = isAtReachable(3, 1000);
            } catch (Exception e) {
                logger.error("Error unlocking the {} device ", this.serialModemComm.getDataPort(), e);
            }
        }

        if (!modemReachable) {
            logger.info("{} modem is not reachable, installing driver ...", this.modemName);
            int retries = 3;
            if (OS_VERSION != null && TARGET_NAME != null
                    && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_"
                            + KuraConstants.Mini_Gateway.getImageVersion())
                    && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())
                    || OS_VERSION
                            .equals(KuraConstants.Reliagate_10_11.getImageName() + "_"
                                    + KuraConstants.Reliagate_10_11.getImageVersion())
                            && TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
                try {
                    turnModemOn();
                    retries = 15;
                } catch (Exception e) {
                    logger.error("Failed to turn modem on ", e);
                }
            }

            modemReachable = isAtReachable(retries, 1000);
        }

        if (modemReachable) {
            if (OS_VERSION != null && TARGET_NAME != null
                    && OS_VERSION.equals(KuraConstants.Reliagate_10_11.getImageName() + "_"
                            + KuraConstants.Reliagate_10_11.getImageVersion())
                    && TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
                enableSIM();
            }
            status = 0;
            logger.info("{} modem is reachable !!!", this.modemName);
        } else {
            logger.warn("{} modem is not reachable, failed to install modem driver", this.modemName);
        }

        return status;
    }

    public int remove() throws Exception {

        int status = -1;
        boolean modemReachable = true;

        try {
            modemReachable = isAtReachable(3, 1000);
        } catch (KuraException e) {
            logger.warn("Exception reaching serial modem ... ", e);
        }
        if (modemReachable) {
            int retries = 3;
            if (OS_VERSION != null && TARGET_NAME != null
                    && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_"
                            + KuraConstants.Mini_Gateway.getImageVersion())
                    && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())
                    || OS_VERSION
                            .equals(KuraConstants.Reliagate_10_11.getImageName() + "_"
                                    + KuraConstants.Reliagate_10_11.getImageVersion())
                            && TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
                turnModemOff();
                sleep(2000);
                retries = 15;
            }

            modemReachable = isAtReachable(retries, 1000);
        }

        if (!modemReachable) {
            status = 0;
            logger.info("{} modem is not reachable !!!", this.modemName);
        } else {
            logger.info("{} modem is still reachable, failed to remove modem driver", this.modemName);
        }
        return status;
    }

    public boolean isReachable() throws KuraException {
        return isAtReachable(3, 1000);
    }

    public String getModemName() {
        return this.modemName;
    }

    public String getModemModel() {
        return this.modemModel;
    }

    public SerialModemComm getComm() {
        return this.serialModemComm;
    }

    private CommConnection openSerialPort(int tout) throws KuraException {

        ConnectionFactory connectionFactory;
        connectionFactory = this.m_serviceTracker.getService();
        CommConnection connection = null;
        if (connectionFactory != null) {
            String uri = new CommURI.Builder(this.serialModemComm.getAtPort())
                    .withBaudRate(this.serialModemComm.getBaudRate()).withDataBits(this.serialModemComm.getDataBits())
                    .withStopBits(this.serialModemComm.getStopBits()).withParity(this.serialModemComm.getParity())
                    .withTimeout(tout).build().toString();

            try {
                connection = (CommConnection) connectionFactory.createConnection(uri, 1, false);
            } catch (Exception e) {
                logger.warn("Exception creating connection: ", e);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }

        return connection;
    }

    private static void closeSerialPort(CommConnection connection) throws KuraException {
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
    }

    private boolean isAtReachable(int attempts, int retryInMsec) throws KuraException {
        boolean status = false;
        CommConnection connection = openSerialPort(2000);
        int numAttempts = attempts;
        do {
            numAttempts--;
            try {
                status = connection.sendCommand("at\r\n".getBytes(), 500).length > 0;
                if (status) {
                    byte[] reply = connection.sendCommand(this.getModelAtCommand.getBytes(), 1000, 100);
                    if (reply != null) {
                        this.modemModel = getResponseString(reply);
                    }
                } else {
                    if (numAttempts > 0) {
                        sleep(retryInMsec);
                    }
                }
            } catch (Exception e) {
                sleep(retryInMsec);
            }
        } while (!status && numAttempts > 0);

        closeSerialPort(connection);
        return status;
    }

    // Parse the AT command response for the relevant info
    private static String getResponseString(String resp) {
        if (resp == null) {
            return "";
        }

        // remove the command and space at the beginning, and the 'OK' and spaces at the end
        return resp.replaceFirst("^\\S*\\s*", "").replaceFirst("\\s*(OK)?\\s*$", "");
    }

    private static String getResponseString(byte[] resp) {
        if (resp == null) {
            return "";
        }

        return getResponseString(new String(resp));
    }

    private void unlockSerialPort() throws Exception {
        String dataPort = this.serialModemComm.getDataPort();
        dataPort = dataPort.substring(dataPort.lastIndexOf('/') + 1);
        File fLockFile = new File("/var/lock/LCK.." + dataPort);
        if (fLockFile.exists()) {
            logger.warn("lock exists for the {} device", dataPort);
            BufferedReader br = new BufferedReader(new FileReader(fLockFile));
            int lockedPid = Integer.parseInt(br.readLine().trim());
            br.close();

            ProcessStats processStats = LinuxProcessUtil.startWithStats("pgrep pppd");
            br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
            String spid;
            int pidToKill = -1;
            while ((spid = br.readLine()) != null) {
                int pid = Integer.parseInt(spid);
                if (pid == lockedPid) {
                    pidToKill = pid;
                    break;
                }
            }
            br.close();
            if (pidToKill > 0) {
                logger.info("killing pppd that locks the {} device", dataPort);
                int stat = LinuxProcessUtil.start("kill " + pidToKill, true);
                if (stat == 0) {
                    logger.info("deleting {}", fLockFile.getName());
                    if (!fLockFile.delete()) {
                        logger.error("failed to delete lock file {}", fLockFile.getName());
                    }
                }
            }
        }
    }

    private void enableSIM() {

        CommConnection connection = null;
        try {
            connection = openSerialPort(10000);

            connection.sendCommand("AT#SIMDET=0\r\n".getBytes(), 500);
            Thread.sleep(5000);
            connection.sendCommand("AT#SIMDET=1\r\n".getBytes(), 500);
            Thread.sleep(1000);
            connection.sendCommand("AT#QSS?\r\n".getBytes(), 500);
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.error("Error in enabling SIM.", e);
        } finally {
            if (connection != null) {
                try {
                    closeSerialPort(connection);
                } catch (KuraException e) {
                    logger.error("Error in closing serial port.", e);
                }
            }
        }
    }

}
