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
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.sierra.usb598;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.util.SerialUtil;
import org.eclipse.kura.net.modem.ModemCdmaServiceProvider;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.event.EventConstants;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SierraUsb598 implements EvdoCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(SierraUsb598.class);

    protected static final String MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG = "Modem not available for AT commands";

    private static String[] topicsOfInterest = null;
    private static final int HIP_PORT = 1;
    private final Object atLock = new Object();

    private ConnectionFactory connectionFactory = null;
    private String model = null;
    private String manufacturer = null;
    private String esn = null;
    private String revisionId = null;
    private ModemDevice device = null;
    private List<NetConfig> netConfigs = null;
    private CommConnection commHipConnection = null;
    private ScheduledExecutorService executor = null;

    /**
     * SierraUsb598 modem constructor
     *
     * @param usbDevice
     *            - modem USB device as {@link UsbModemDevice}
     * @param connectionFactory
     *            - connection factory as {@link ConnectionFactory}
     * @param technologyType
     *            - cellular technology type as
     *            {@link ModemTechnologyType}
     */
    public SierraUsb598(ModemDevice device, ConnectionFactory connectionFactory) {

        this.device = device;
        this.connectionFactory = connectionFactory;
        topicsOfInterest = generateSubscribeTopics();
        // subscribe on specific topics of interest
        Dictionary<String, String[]> d = new Hashtable();
        d.put(EventConstants.EVENT_TOPIC, topicsOfInterest);
        for (String element : topicsOfInterest) {
            logger.debug("Subscribing for {}", element);
        }

        // define notification thread
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void bind() {
        try {
            this.commHipConnection = openSerialPort(getHipPort());
            this.executor.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName("SierraUsb598");
                logger.debug("**** HIP Thread() run ");
            }, 0, 1, TimeUnit.SECONDS);
        } catch (KuraException e) {
            logger.error("bind() :: ", e);
        }
    }

    public void unbind() {

        if (this.commHipConnection != null) {
            try {
                this.executor.shutdown();
                closeSerialPort(this.commHipConnection);
            } catch (KuraException e) {
                logger.error("unbind() :: ", e);
            }
        }
    }

    @Override
    public String getModel() throws KuraException {
        synchronized (this.atLock) {
            if (this.model == null) {
                logger.debug("sendCommand getModelNumber :: {}", SierraUsb598AtCommands.getModelNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getModelNumber.getCommand().getBytes(),
                            500);
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
                logger.debug("sendCommand getManufacturer :: {}", SierraUsb598AtCommands.getManufacturer.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getManufacturer.getCommand().getBytes(),
                            500);
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
            if (this.esn != null) {
                logger.debug("sendCommand getSerialNumber :: {}", SierraUsb598AtCommands.getSerialNumber.getCommand());
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getSerialNumber.getCommand().getBytes(),
                            500);

                    if (reply != null) {
                        StringBuilder replySB = new StringBuilder();
                        replySB.append(new String(reply));
                        // if response is incomplete, try to get the rest
                        if (!replySB.toString().matches(".*OK\\s*$")) {
                            sleep(200);
                            reply = commAtConnection.flushSerialBuffer();

                            if (reply != null) {
                                replySB.append(new String(reply));
                            }
                        }

                        String serialNum = getResponseString(replySB.toString());
                        if (serialNum != null && !serialNum.isEmpty()) {
                            this.esn = serialNum;
                        }
                    }
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }
            }
        }
        return this.esn;
    }

    @Override
    public String getMobileSubscriberIdentity() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getRevisionID() throws KuraException {
        synchronized (this.atLock) {
            if (this.revisionId == null) {
                byte[] reply;
                CommConnection commAtConnection = null;
                try {
                    commAtConnection = openSerialPort(getAtPort());
                    if (!isAtReachable(commAtConnection)) {
                        throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                    }
                    reply = commAtConnection.sendCommand(SierraUsb598AtCommands.getRevision.getCommand().getBytes(),
                            500);
                    if (reply != null) {
                        this.revisionId = getResponseString(reply);
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
        // not implemented
        return false;
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
    public void reset() throws KuraException {
        logger.info("resetting modem ...");
        try {
            powerOff();
            sleep(15000);
            powerOn();
            sleep(3000);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public int getSignalStrength() throws KuraException {
        return 0;
    }

    @Override
    public ModemRegistrationStatus getRegistrationStatus() throws KuraException {
        return null;
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
        return null;
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

        String port = null;
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

        String port = null;
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
        // not implemented
        return null;
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        // not implemented
        return false;
    }

    @Override
    public void enableGps() throws KuraException {
        // not implemented
    }

    @Override
    public void disableGps() throws KuraException {
        // not implemented
    }

    @Override
    public boolean isProvisioned() throws KuraException {
        // not implemented
        return false;
    }

    @Override
    public void provision() throws KuraException {
        // not implemented
    }

    @Override
    public String getMobileDirectoryNumber() throws KuraException {
        // not implemented
        return null;
    }

    @Override
    public String getMobileIdentificationNumber() throws KuraException {
        // not implemented
        return null;
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

    private String getHipPort() throws KuraException {
        String port;

        if (this.device instanceof UsbModemDevice) {
            UsbModemDevice usbModemDevice = (UsbModemDevice) this.device;
            List<String> ports = usbModemDevice.getTtyDevs();
            if (ports != null && !ports.isEmpty()) {
                port = ports.get(HIP_PORT);
            } else {
                throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No HIP serial port available");
            }
        } else {
            throw new KuraException(KuraErrorCode.SERIAL_PORT_NOT_EXISTING, "No HIP serial port available");
        }

        return port;
    }

    @Override
    public ModemCdmaServiceProvider getServiceProvider() {
        // not implemented
        return null;
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
        return new ArrayList<>();
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
                status = connection.sendCommand(SierraUsb598AtCommands.at.getCommand().getBytes(), 500).length > 0;
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

    private void powerOff() {
        // do nothing
    }

    private void powerOn() {
        // do nothing
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /*
     * This method generates array of subscribe topics
     */
    private static String[] generateSubscribeTopics() {

        StringBuilder buf = new StringBuilder(SierraUsb598.class.getName().replace('.', '/'));
        buf.append('/');
        buf.append('*');
        String[] topics = new String[1];
        topics[0] = buf.toString();
        return topics;
    }

    public boolean hasDiversityAntenna() {
        return false;
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
