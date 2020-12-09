/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Modbus protocol implements a subset of the Modbus standard command set.
 * It also provides for the extension of some data typing to allow register
 * pairings to hold 32 bit data (see the configureDataMap for more detail).
 * <p>
 * The protocol supports RTU and ASCII mode operation.
 *
 */
public class ModbusProtocolDevice implements ModbusProtocolDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(ModbusProtocolDevice.class);

    private ConnectionFactory connectionFactory;
    private UsbService usbService;

    static final String PROTOCOL_NAME = "modbus";
    public static final String PROTOCOL_CONNECTION_TYPE_SERIAL = "RS232";
    public static final String PROTOCOL_CONNECTION_TYPE_ETHER_RTU = "TCP-RTU";
    public static final String PROTOCOL_CONNECTION_TYPE_ETHER_TCP = "TCP/IP";
    private int respTout;
    private int txMode;
    private boolean connConfigd = false;
    private boolean protConfigd = false;
    private String connType = null;
    private Communicate comm;
    private Properties modbusProperties = null;
    private static int transactionIndex = 0;

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = null;
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        this.usbService = null;
    }

    private boolean serialPortExists() {
        if (this.modbusProperties == null) {
            return false;
        }

        String portName = this.modbusProperties.getProperty("port");
        if (portName != null) {
            if (portName.contains("/dev/")) {
                File f = new File(portName);
                if (f.exists()) {
                    return true;
                }
            } else {
                List<UsbTtyDevice> utd = this.usbService.getUsbTtyDevices();
                if (utd != null) {
                    for (UsbTtyDevice u : utd) {
                        if (portName.equals(u.getUsbPort())) {
                            // replace device number with tty
                            portName = u.getDeviceNode();
                            this.modbusProperties.setProperty("port", portName);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------
    protected void activate(ComponentContext componentContext) {
        logger.info("activate...");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
        try {
            disconnect();
        } catch (ModbusProtocolException e) {
            logger.error("ModbusProtocolException :  {}", e.getCode());
        }
    }

    /**
     * two connection types are available:
     * <ul>
     * <li>serial mode (PROTOCOL_CONNECTION_TYPE_SERIAL)
     *
     * <li>Ethernet with 2 possible modes : RTU over TCP/IP (PROTOCOL_CONNECTION_TYPE_ETHER_RTU) or real MODBUS-TCP/IP
     * (PROTOCOL_CONNECTION_TYPE_ETHER_TCP).
     * <ul>
     * <p>
     * <h4>PROTOCOL_CONNECTION_TYPE_SERIAL</h4>
     * see {@link org.eclipse.kura.comm.CommConnection CommConnection} package for more detail.
     * <table border="1">
     * <tr>
     * <th>Key</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>connectionType</td>
     * <td>"RS232" (from PROTOCOL_CONNECTION_TYPE_SERIAL). This parameter indicates the connection type for the
     * configuration. See {@link org.eclipse.kura.comm.CommConnection CommConnection} for more details on serial port
     * configuration.
     * </tr>
     * <tr>
     * <td>port</td>
     * <td>the actual device port, such as "/dev/ttyUSB0" in linux</td>
     * </tr>
     * <tr>
     * <td>baudRate</td>
     * <td>baud rate to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>stopBits</td>
     * <td>number of stop bits to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>parity</td>
     * <td>parity mode to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>bitsPerWord</td>
     * <td>only RTU mode supported, bitsPerWord must be 8</td>
     * </tr>
     * </table>
     * <p>
     * <h4>PROTOCOL_CONNECTION_TYPE_ETHER_TCP</h4>
     * The Ethernet mode merely opens a socket and sends the full RTU mode Modbus packet over that socket connection and
     * expects to receive a full RTU mode Modbus response, including the CRC bytes.
     * <table border="1">
     * <tr>
     * <th>Key</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>connectionType</td>
     * <td>"ETHERTCP" (from PROTOCOL_CONNECTION_TYPE_ETHER_TCP). This parameter indicates the connection type for the
     * configurator.
     * </tr>
     * <tr>
     * <td>ipAddress</td>
     * <td>the 4 octet IP address of the field device (xxx.xxx.xxx.xxx)</td>
     * </tr>
     * <tr>
     * <td>port</td>
     * <td>port on the field device to connect to</td>
     * </tr>
     * </table>
     */
    @Override
    public void configureConnection(Properties connectionConfig) throws ModbusProtocolException {
        if ((this.connType = connectionConfig.getProperty("connectionType")) == null) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }

        this.modbusProperties = connectionConfig;

        String txMode;
        String respTimeout;
        if (this.protConfigd || (txMode = connectionConfig.getProperty("transmissionMode")) == null
                || (respTimeout = connectionConfig.getProperty("respTimeout")) == null) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }
        if (txMode.equals(ModbusTransmissionMode.RTU)) {
            this.txMode = ModbusTransmissionMode.RTU_MODE;
        } else if (txMode.equals(ModbusTransmissionMode.ASCII)) {
            this.txMode = ModbusTransmissionMode.ASCII_MODE;
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }
        this.respTout = Integer.parseInt(respTimeout);
        if (this.respTout < 0) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }
        this.protConfigd = true;

        if (this.connConfigd) {
            this.comm.disconnect();
            this.comm = null;
            this.connConfigd = false;
        }

        if (PROTOCOL_CONNECTION_TYPE_SERIAL.equals(this.connType)) {
            if (!serialPortExists()) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_AVAILABLE);
            }
            this.comm = new SerialCommunicate(this.connectionFactory, connectionConfig);
        } else if (PROTOCOL_CONNECTION_TYPE_ETHER_TCP.equals(this.connType)
                || PROTOCOL_CONNECTION_TYPE_ETHER_RTU.equals(this.connType)) {
            this.comm = new EthernetCommunicate(this.connectionFactory, connectionConfig);
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }

        this.connConfigd = true;
    }

    /**
     * get the name "modbus" for this protocol
     *
     * @return "modbus"
     */
    @Override
    public String getProtocolName() {
        return "modbus";
    }

    @Override
    public void connect() throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }
        this.comm.connect();
    }

    @Override
    public void disconnect() throws ModbusProtocolException {
        if (this.connConfigd) {
            this.comm.disconnect();
            this.comm = null;
            this.connConfigd = false;
            logger.info("Serial comm disconnected");
        }
        this.protConfigd = false;
    }

    @Override
    public int getConnectStatus() {
        if (!this.connConfigd) {
            return KuraConnectionStatus.NEVERCONNECTED;
        }
        return this.comm.getConnectStatus();
    }

    /**
     * The only constructor must be the configuration mechanism
     */
    abstract private class Communicate {

        abstract public void connect();

        abstract public void disconnect() throws ModbusProtocolException;

        abstract public int getConnectStatus();

        abstract public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException;
    }

    /**
     * Installation of a serial connection to communicate, using javax.comm.SerialPort
     * <p>
     * <table border="1">
     * <tr>
     * <th>Key</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>port</td>
     * <td>the actual device port, such as "/dev/ttyUSB0" in linux</td>
     * </tr>
     * <tr>
     * <td>serialMode</td>
     * <td>SERIAL_232 or SERIAL_485</td>
     * </tr>
     * <tr>
     * <td>baudRate</td>
     * <td>baud rate to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>stopBits</td>
     * <td>number of stop bits to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>parity</td>
     * <td>parity mode to be configured for the port</td>
     * </tr>
     * <tr>
     * <td>bitsPerWord</td>
     * <td>only RTU mode supported, bitsPerWord must be 8</td>
     * </tr>
     * </table>
     * see {@link org.eclipse.kura.comm.CommConnection CommConnection} package for more detail.
     */
    private final class SerialCommunicate extends Communicate {

        InputStream in;
        OutputStream out;
        CommConnection conn = null;

        public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig)
                throws ModbusProtocolException {
            logger.info("Configure serial connection");

            String sPort;
            String sBaud;
            String sStop;
            String sParity;
            String sBits;

            if ((sPort = connectionConfig.getProperty("port")) == null
                    || (sBaud = connectionConfig.getProperty("baudRate")) == null
                    || (sStop = connectionConfig.getProperty("stopBits")) == null
                    || (sParity = connectionConfig.getProperty("parity")) == null
                    || (sBits = connectionConfig.getProperty("bitsPerWord")) == null) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
            }

            int baud = Integer.valueOf(sBaud).intValue();
            int stop = Integer.valueOf(sStop).intValue();
            int parity = Integer.valueOf(sParity).intValue();
            int bits = Integer.valueOf(sBits).intValue();

            String uri = new CommURI.Builder(sPort).withBaudRate(baud).withDataBits(bits).withStopBits(stop)
                    .withParity(parity).withTimeout(2000).build().toString();

            try {
                this.conn = (CommConnection) connFactory.createConnection(uri, 1, false);
            } catch (IOException e1) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE, e1.getMessage());
            }

            // get the streams
            try {
                this.in = this.conn.openInputStream();
                this.out = this.conn.openOutputStream();
            } catch (Exception e) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.CONNECTION_FAILURE, e);
            }
            logger.info("Serial connection connected");
        }

        @Override
        public void connect() {
            /*
             * always connected
             */
        }

        @Override
        public void disconnect() throws ModbusProtocolException {
            if (this.conn != null) {
                try {
                    this.conn.close();
                    logger.debug("Serial connection closed");
                } catch (IOException e) {
                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, e.getMessage());
                }
                this.conn = null;
            }
        }

        @Override
        public int getConnectStatus() {
            return KuraConnectionStatus.CONNECTED;
        }

        private byte asciiLrcCalc(byte[] msg, int len) {
            char[] ac = new char[2];
            ac[0] = (char) msg[len - 4];
            ac[1] = (char) msg[len - 3];
            String s = new String(ac);
            byte lrc = (byte) Integer.parseInt(s, 16);
            return lrc;
        }

        private int binLrcCalc(byte[] msg) {
            int llrc = 0;
            for (byte element : msg) {
                llrc += element & 0xff;
            }
            llrc = (llrc ^ 0xff) + 1;
            // byte lrc=(byte)(llrc & 0x0ff);
            return llrc;
        }

        /**
         * convertCommandToAscii: convert a binary command into a standard Modbus
         * ASCII frame
         */
        private byte[] convertCommandToAscii(byte[] msg) {
            int lrc = binLrcCalc(msg);

            char[] hexArray = "0123456789ABCDEF".toCharArray();
            byte[] ab = new byte[msg.length * 2 + 5];
            ab[0] = ':';
            int v;
            for (int i = 0; i < msg.length; i++) {
                v = msg[i] & 0xff;
                ab[i * 2 + 1] = (byte) hexArray[v >>> 4];
                ab[i * 2 + 2] = (byte) hexArray[v & 0x0f];
            }
            v = lrc & 0x0ff;
            ab[ab.length - 4] = (byte) hexArray[v >>> 4];
            ab[ab.length - 3] = (byte) hexArray[v & 0x0f];
            ab[ab.length - 2] = 13;
            ab[ab.length - 1] = 10;
            return ab;
        }

        /**
         * convertAsciiResponseToBin: convert a standard Modbus frame to
         * byte array
         */
        private byte[] convertAsciiResponseToBin(byte[] msg, int len) {
            int l = (len - 5) / 2;
            byte[] ab = new byte[l];
            char[] ac = new char[2];
            // String s=new String(msg);
            for (int i = 0; i < l; i++) {
                ac[0] = (char) msg[i * 2 + 1];
                ac[1] = (char) msg[i * 2 + 2];
                // String s=new String(ac);
                ab[i] = (byte) Integer.parseInt(new String(ac), 16);
            }
            return ab;
        }

        /**
         * msgTransaction must be called with a byte array having two extra
         * bytes for the CRC. It will return a byte array of the response to the
         * message. Validation will include checking the CRC and verifying the
         * command matches.
         */
        @Override
        public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {

            byte[] cmd = null;

            if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.RTU_MODE) {
                cmd = new byte[msg.length + 2];
                for (int i = 0; i < msg.length; i++) {
                    cmd[i] = msg[i];
                }
                // Add crc calculation to end of message
                int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
                cmd[msg.length] = (byte) crc;
                cmd[msg.length + 1] = (byte) (crc >> 8);
            } else if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE) {
                cmd = convertCommandToAscii(msg);
            }

            // Send the message
            try {
                synchronized (this.out) {
                    synchronized (this.in) {
                        // flush input
                        while (this.in.available() > 0) {
                            this.in.read();
                        }
                        // send all data
                        this.out.write(cmd, 0, cmd.length);
                        this.out.flush();
                        // outputStream.waitAllSent(respTout);

                        // wait for and process response
                        byte[] response = new byte[262]; // response buffer
                        int respIndex = 0;
                        int minimumLength = 5; // default minimum message length
                        if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE) {
                            minimumLength = 11;
                        }
                        int timeOut = ModbusProtocolDevice.this.respTout;
                        for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
                            boolean endFrame = false;
                            // while (respIndex < minimumLength) {
                            while (!endFrame) {
                                long start = System.currentTimeMillis();
                                while (this.in.available() == 0) {
                                    try {
                                        Thread.sleep(5);	// avoid a high cpu load
                                    } catch (InterruptedException e) {
                                        throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                                "Thread interrupted");
                                    }

                                    long elapsed = System.currentTimeMillis() - start;
                                    if (elapsed > timeOut) {
                                        String failMsg = "Recv timeout";
                                        logger.warn(failMsg + " : " + elapsed + " minimumLength=" + minimumLength
                                                + " respIndex=" + respIndex);
                                        throw new ModbusProtocolException(ModbusProtocolErrorCode.RESPONSE_TIMEOUT,
                                                failMsg);
                                    }
                                }
                                // address byte must match first
                                if (respIndex == 0) {
                                    if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE) {
                                        if ((response[0] = (byte) this.in.read()) == ':') {
                                            respIndex++;
                                        }
                                    } else {
                                        if ((response[0] = (byte) this.in.read()) == msg[0]) {
                                            respIndex++;
                                        }
                                    }
                                } else {
                                    response[respIndex++] = (byte) this.in.read();
                                }

                                if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.RTU_MODE) {
                                    timeOut = 100; // move to character timeout
                                    if (respIndex >= minimumLength) {
                                        endFrame = true;
                                    }
                                } else {
                                    if (response[respIndex - 1] == 10 && response[respIndex - 2] == 13) {
                                        endFrame = true;
                                    }
                                }
                            }
                            // if ASCII mode convert response
                            if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE) {
                                byte lrcRec = asciiLrcCalc(response, respIndex);
                                response = convertAsciiResponseToBin(response, respIndex);
                                byte lrcCalc = (byte) binLrcCalc(response);
                                if (lrcRec != lrcCalc) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "Bad LRC");
                                }
                            }

                            // Check first for an Exception response
                            if ((response[1] & 0x80) == 0x80) {
                                if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE
                                        || Crc16.getCrc16(response, 5, 0xffff) == 0) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "Exception response = " + Byte.toString(response[2]));
                                }
                            } else {
                                // then check for a valid message
                                switch (response[1]) {
                                case ModbusFunctionCodes.FORCE_SINGLE_COIL:
                                case ModbusFunctionCodes.PRESET_SINGLE_REG:
                                case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
                                case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
                                    if (respIndex < 8) {
                                        // wait for more data
                                        minimumLength = 8;
                                    } else if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE
                                            || Crc16.getCrc16(response, 8, 0xffff) == 0) {
                                        byte[] ret = new byte[6];
                                        for (int i = 0; i < 6; i++) {
                                            ret[i] = response[i];
                                        }
                                        return ret;
                                    }
                                    break;
                                case ModbusFunctionCodes.READ_COIL_STATUS:
                                case ModbusFunctionCodes.READ_INPUT_STATUS:
                                case ModbusFunctionCodes.READ_INPUT_REGS:
                                case ModbusFunctionCodes.READ_HOLDING_REGS:
                                    int byteCnt;
                                    if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE) {
                                        byteCnt = (response[2] & 0xff) + 3;
                                    } else {
                                        byteCnt = (response[2] & 0xff) + 5;
                                    }
                                    if (respIndex < byteCnt) {
                                        // wait for more data
                                        minimumLength = byteCnt;
                                    } else if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.ASCII_MODE
                                            || Crc16.getCrc16(response, byteCnt, 0xffff) == 0) {
                                        byte[] ret = new byte[byteCnt];
                                        for (int i = 0; i < byteCnt; i++) {
                                            ret[i] = response[i];
                                        }
                                        return ret;
                                    }
                                }
                            }

                            /*
                             * if required length then must have failed, drop
                             * first byte and try again
                             */
                            if (respIndex >= minimumLength) {
                                respIndex--;
                                for (int i = 0; i < respIndex; i++) {
                                    response[i] = response[i + 1];
                                }
                                minimumLength = 5; // reset minimum length
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // e.printStackTrace();
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, e.getMessage());
            }
            throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                    "Too much activity on recv line");
        }
    }

    /**
     * Installation of an ethernet connection to communicate
     */
    private final class EthernetCommunicate extends Communicate {

        InputStream inputStream;
        OutputStream outputStream;
        Socket socket;
        int port;
        String ipAddress;
        String connType;
        boolean connected = false;

        public EthernetCommunicate(ConnectionFactory connFactory, Properties connectionConfig)
                throws ModbusProtocolException {
            logger.debug("Configure TCP connection");
            String sPort;
            this.connType = connectionConfig.getProperty("connectionType");

            if ((sPort = connectionConfig.getProperty("ethport")) == null
                    || (this.ipAddress = connectionConfig.getProperty("ipAddress")) == null) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
            }
            this.port = Integer.valueOf(sPort).intValue();
            ModbusProtocolDevice.this.connConfigd = true;
            this.socket = new Socket();
        }

        @Override
        public void connect() {
            if (!ModbusProtocolDevice.this.connConfigd) {
                logger.error("Can't connect, port not configured");
            } else {
                if (!this.connected) {
                    try {
                        this.socket = new Socket();
                        this.socket.connect(new InetSocketAddress(this.ipAddress, this.port),
                                ModbusProtocolDevice.this.respTout);
                        try {
                            this.inputStream = this.socket.getInputStream();
                            this.outputStream = this.socket.getOutputStream();
                            this.connected = true;
                            logger.info("TCP connected");
                        } catch (IOException e) {
                            disconnect();
                            logger.error("Failed to get socket streams: " + e);
                        }
                    } catch (IOException e) {
                        this.socket = null;
                        logger.error("Failed to connect to remote: " + e);
                    }
                }
            }
        }

        @Override
        public void disconnect() {
            if (this.socket == null) {
                return;
            }
            if (ModbusProtocolDevice.this.connConfigd) {
                if (this.connected) {
                    try {
                        if (!this.socket.isInputShutdown()) {
                            this.socket.shutdownInput();
                        }
                        if (!this.socket.isOutputShutdown()) {
                            this.socket.shutdownOutput();
                        }
                        this.socket.close();
                    } catch (IOException eClose) {
                        logger.error("Error closing TCP: " + eClose);
                    }
                    this.inputStream = null;
                    this.outputStream = null;
                    this.connected = false;
                    this.socket = null;
                }
            }
        }

        @Override
        public int getConnectStatus() {
            if (this.connected) {
                return KuraConnectionStatus.CONNECTED;
            } else if (ModbusProtocolDevice.this.connConfigd) {
                return KuraConnectionStatus.DISCONNECTED;
            } else {
                return KuraConnectionStatus.NEVERCONNECTED;
            }
        }

        @Override
        public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {
            byte[] cmd = null;

            // ---------------------------------------------- Send Message
            // ---------------------------------------------------
            if (ModbusProtocolDevice.this.txMode == ModbusTransmissionMode.RTU_MODE) {
                if (PROTOCOL_CONNECTION_TYPE_ETHER_TCP.equals(this.connType)) {
                    cmd = new byte[msg.length + 6];
                    // build MBAP header
                    int index = getNextTransactionIndex();
                    cmd[0] = (byte) (index >> 8);
                    cmd[1] = (byte) index;
                    cmd[2] = 0;
                    cmd[3] = 0;
                    // length
                    int len = msg.length;
                    cmd[4] = (byte) (len >> 8);
                    cmd[5] = (byte) len;
                    for (int i = 0; i < msg.length; i++) {
                        cmd[i + 6] = msg[i];
                    }
                    // No crc in Modbus TCP
                } else {
                    cmd = new byte[msg.length + 2];
                    for (int i = 0; i < msg.length; i++) {
                        cmd[i] = msg[i];
                    }
                    // Add crc calculation to end of message
                    int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
                    cmd[msg.length] = (byte) crc;
                    cmd[msg.length + 1] = (byte) (crc >> 8);
                }
            } else {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.METHOD_NOT_SUPPORTED,
                        "Only RTU over TCP/IP supported");
            }

            // Check connection status and connect
            connect();
            if (!this.connected) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                        "Cannot transact on closed socket");
            }

            // Send the message
            try {
                // flush input
                while (this.inputStream.available() > 0) {
                    this.inputStream.read();
                }
                // send all data
                this.outputStream.write(cmd, 0, cmd.length);
                this.outputStream.flush();
            } catch (IOException e) {
                // Assume this means the socket is closed...make sure it is
                logger.error("Socket disconnect in send: " + e);
                disconnect();
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                        "Send failure: " + e.getMessage());
            }

            // ---------------------------------------------- Receive response
            // ---------------------------------------------------
            // wait for and process response

            boolean endFrame = false;
            byte[] response = new byte[262]; // response buffer
            int respIndex = 0;
            int minimumLength = 5; // default minimum message length
            if (PROTOCOL_CONNECTION_TYPE_ETHER_TCP.equals(this.connType)) {
                minimumLength += 6;
            }
            while (!endFrame) {
                try {
                    this.socket.setSoTimeout(ModbusProtocolDevice.this.respTout);
                    int resp = this.inputStream.read(response, respIndex, 1);
                    if (resp > 0) {
                        respIndex += resp;
                        if (PROTOCOL_CONNECTION_TYPE_ETHER_TCP.equals(this.connType)) {
                            if (respIndex == 7) {
                                // test modbus id
                                if (response[6] != msg[0]) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "incorrect modbus id " + String.format("%02X", response[6]));
                                }
                            } else if (respIndex == 8) {
                                // test function number
                                if ((response[7] & 0x7f) != msg[1]) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "incorrect function number " + String.format("%02X", response[7]));
                                }
                            } else if (respIndex == 9) {
                                // Check first for an Exception response
                                if ((response[7] & 0x80) == 0x80) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "Modbus responds an error = " + String.format("%02X", response[8]));
                                } else {
                                    if (response[7] == ModbusFunctionCodes.FORCE_SINGLE_COIL
                                            || response[7] == ModbusFunctionCodes.PRESET_SINGLE_REG
                                            || response[7] == ModbusFunctionCodes.FORCE_MULTIPLE_COILS
                                            || response[7] == ModbusFunctionCodes.PRESET_MULTIPLE_REGS) {
                                        minimumLength = 12;
                                    } else {
                                        // bytes count
                                        minimumLength = (response[8] & 0xff) + 9;
                                    }
                                }
                            } else if (respIndex == minimumLength) {
                                endFrame = true;
                            }
                        } else {

                        }
                    } else {
                        logger.error("Socket disconnect in recv");
                        throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Recv failure");
                    }
                } catch (SocketTimeoutException e) {
                    String failMsg = "Recv timeout";
                    logger.warn(failMsg);
                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, failMsg);
                } catch (IOException e) {
                    logger.error("Socket disconnect in recv: " + e);
                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Recv failure");
                }

            }

            // then check for a valid message
            switch (response[7]) {
            case ModbusFunctionCodes.FORCE_SINGLE_COIL:
            case ModbusFunctionCodes.PRESET_SINGLE_REG:
            case ModbusFunctionCodes.FORCE_MULTIPLE_COILS:
            case ModbusFunctionCodes.PRESET_MULTIPLE_REGS:
                byte[] ret = new byte[8];
                for (int i = 6; i < 12; i++) {
                    ret[i - 6] = response[i];
                }
                return ret;
            case ModbusFunctionCodes.READ_COIL_STATUS:
            case ModbusFunctionCodes.READ_INPUT_STATUS:
            case ModbusFunctionCodes.READ_INPUT_REGS:
            case ModbusFunctionCodes.READ_HOLDING_REGS:
                int byteCnt = (response[8] & 0xff) + 3 + 6;
                ret = new byte[byteCnt - 6];
                for (int i = 6; i < byteCnt; i++) {
                    ret[i - 6] = response[i];
                }
                return ret;
            }
            return null;
        }
    }

    @Override
    public boolean[] readCoils(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        boolean[] ret = new boolean[count];
        int index = 0;

        byte[] resp;
        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_COIL_STATUS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (count / 256);
        cmd[5] = (byte) (count % 256);

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == (count + 7) / 8) {
            byte mask = 1;
            int byteOffset = 3;
            for (int j = 0; j < count; j++, index++) {
                // get this point's value
                if ((resp[byteOffset] & mask) == mask) {
                    ret[index] = true;
                } else {
                    ret[index] = false;
                }
                // advance the mask and offset index
                if ((mask <<= 1) == 0) {
                    mask = 1;
                    byteOffset++;
                }
            }
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }

        return ret;
    }

    @Override
    public boolean[] readDiscreteInputs(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        boolean[] ret = new boolean[count];
        int index = 0;

        byte[] resp;
        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_STATUS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (count / 256);
        cmd[5] = (byte) (count % 256);

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == (count + 7) / 8) {
            byte mask = 1;
            int byteOffset = 3;
            for (int j = 0; j < count; j++, index++) {
                // get this point's value
                if ((resp[byteOffset] & mask) == mask) {
                    ret[index] = true;
                } else {
                    ret[index] = false;
                }
                // advance the mask and offset index
                if ((mask <<= 1) == 0) {
                    mask = 1;
                    byteOffset++;
                }
            }
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }

        return ret;
    }

    @Override
    public void writeSingleCoil(int unitAddr, int dataAddress, boolean data) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] resp;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.FORCE_SINGLE_COIL;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = data == true ? (byte) 0xff : (byte) 0;
        cmd[5] = 0;

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int i = 0; i < 6; i++) {
            if (cmd[i] != resp[i]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }

    }

    @Override
    public void writeMultipleCoils(int unitAddr, int dataAddress, boolean[] data) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * write multiple boolean values
         */
        int localCnt = data.length;
        int index = 0;
        byte[] resp;
        /*
         * construct the command, issue and verify response
         */
        int dataLength = (localCnt + 7) / 8;
        byte[] cmd = new byte[dataLength + 7];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.FORCE_MULTIPLE_COILS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (localCnt / 256);
        cmd[5] = (byte) (localCnt % 256);
        cmd[6] = (byte) dataLength;

        // put the data on the command
        byte mask = 1;
        int byteOffset = 7;
        cmd[byteOffset] = 0;
        for (int j = 0; j < localCnt; j++, index++) {
            // get this point's value
            if (data[index]) {
                cmd[byteOffset] += mask;
            }
            // advance the mask and offset index
            if ((mask <<= 1) == 0) {
                mask = 1;
                byteOffset++;
                cmd[byteOffset] = 0;
            }
        }

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int j = 0; j < 6; j++) {
            if (cmd[j] != resp[j]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    @Override
    public int[] readHoldingRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        int[] ret = new int[count];
        int index = 0;

        byte[] resp;
        /*
         * construct the command issue and get results, putting the results
         * away at index and then incrementing index for the next command
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = 0;
        cmd[5] = (byte) count;

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == count * 2) {
            int byteOffset = 3;
            for (int j = 0; j < count; j++, index++) {
                int val = resp[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1'] & 0xff;
                val <<= 8;
                val += resp[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1'] & 0xff;

                ret[index] = val;

                byteOffset += 2;
            }
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }
        return ret;
    }

    @Override
    public int[] readInputRegisters(int unitAddr, int dataAddress, int count) throws ModbusProtocolException {

        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        int[] ret = new int[count];
        int index = 0;

        byte[] resp;
        /*
         * construct the command issue and get results, putting the results
         * away at index and then incrementing index for the next command
         */
        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_REGS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = 0;
        cmd[5] = (byte) count;

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3 || resp.length < (resp[2] & 0xff) + 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        if ((resp[2] & 0xff) == count * 2) {
            int byteOffset = 3;
            for (int j = 0; j < count; j++, index++) {
                int val = resp[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1'] & 0xff;
                val <<= 8;
                val += resp[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1'] & 0xff;

                ret[index] = val;

                byteOffset += 2;
            }
        } else {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_ADDRESS);
        }
        return ret;
    }

    @Override
    public void writeSingleRegister(int unitAddr, int dataAddress, int data) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.PRESET_SINGLE_REG;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (data >> 8);
        cmd[5] = (byte) data;

        /*
         * send the message and get the response
         */
        byte[] resp = this.comm.msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int i = 0; i < 6; i++) {
            if (cmd[i] != resp[i]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    @Override
    public void writeMultipleRegister(int unitAddr, int dataAddress, int[] data) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        int localCnt = data.length;
        /*
         * construct the command, issue and verify response
         */
        int dataLength = localCnt * 2;
        byte[] cmd = new byte[dataLength + 7];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.PRESET_MULTIPLE_REGS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (localCnt / 256);
        cmd[5] = (byte) (localCnt % 256);
        cmd[6] = (byte) dataLength;

        // put the data on the command
        int byteOffset = 7;
        int index = 0;
        for (int j = 0; j < localCnt; j++, index++) {
            cmd[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(0) - '1'] = (byte) (data[index] >> 8);
            cmd[byteOffset + ModbusDataOrder.MODBUS_WORD_ORDER_BIG_ENDIAN.charAt(1) - '1'] = (byte) data[index];

            byteOffset += 2;
        }

        /*
         * send the message and get the response
         */
        byte[] resp = this.comm.msgTransaction(cmd);

        /*
         * process the response
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        for (int j = 0; j < 6; j++) {
            if (cmd[j] != resp[j]) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
            }
        }
    }

    @Override
    public boolean[] readExceptionStatus(int unitAddr) throws ModbusProtocolException {
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        boolean[] ret = new boolean[8];
        int index = 0;

        byte[] resp;
        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_EXCEPTION_STATUS;

        /*
         * send the message and get the response
         */
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 3) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        byte mask = 1;
        for (int j = 0; j < 8; j++, index++) {
            // get this point's value
            if ((resp[2] & mask) == mask) {
                ret[index] = true;
            } else {
                ret[index] = false;
            }
            // advance the mask and offset index
            if ((mask <<= 1) == 0) {
                mask = 1;
            }
        }

        return ret;
    }

    @Override
    public ModbusCommEvent getCommEventCounter(int unitAddr) throws ModbusProtocolException {
        ModbusCommEvent mce = new ModbusCommEvent();
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_COUNTER;

        /*
         * send the message and get the response
         */
        byte[] resp;
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < 6) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        int val = resp[2] & 0xff;
        val <<= 8;
        val += resp[3] & 0xff;
        mce.setStatus(val);
        val = resp[4] & 0xff;
        val <<= 8;
        val += resp[5] & 0xff;
        mce.setEventCount(val);

        return mce;
    }

    @Override
    public ModbusCommEvent getCommEventLog(int unitAddr) throws ModbusProtocolException {
        ModbusCommEvent mce = new ModbusCommEvent();
        if (!this.connConfigd) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.NOT_CONNECTED);
        }

        /*
         * construct the command issue and get results
         */
        byte[] cmd = new byte[2];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.GET_COMM_EVENT_LOG;

        /*
         * send the message and get the response
         */
        byte[] resp;
        resp = this.comm.msgTransaction(cmd);

        /*
         * process the response (address & CRC already confirmed)
         */
        if (resp.length < (resp[2] & 0xff) + 3 || (resp[2] & 0xff) > 64 + 7) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_DATA_TYPE);
        }
        int val = resp[3] & 0xff;
        val <<= 8;
        val += resp[4] & 0xff;
        mce.setStatus(val);

        val = resp[5] & 0xff;
        val <<= 8;
        val += resp[6] & 0xff;
        mce.setEventCount(val);

        val = resp[7] & 0xff;
        val <<= 8;
        val += resp[8] & 0xff;
        mce.setMessageCount(val);

        int count = (resp[2] & 0xff) - 4;
        int[] events = new int[count];
        for (int j = 0; j < count; j++) {
            int bval = resp[9 + j] & 0xff;
            events[j] = bval;
        }
        mce.setEvents(events);

        return mce;
    }

    /**
     * Calculates and returns the next transaction index for Modbus TCP.
     *
     * @return the next transaction index.
     */
    private int getNextTransactionIndex() {
        transactionIndex++;
        if (transactionIndex > 0xffff) {
            transactionIndex = 0;
        }
        return transactionIndex;
    }

}
