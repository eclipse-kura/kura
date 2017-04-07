/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;

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
final class SerialCommunicate extends Communicate {

    private final Logger s_logger;
    private int m_txMode;
    private int m_respTout;

    InputStream in;
    OutputStream out;
    CommConnection conn = null;

    public SerialCommunicate(ConnectionFactory connFactory, Properties connectionConfig, Logger logger, int txMode,
            int respTout) throws ModbusProtocolException {
        s_logger = logger;
        s_logger.info("Configure serial connection");

        m_txMode = txMode;
        m_respTout = respTout;

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
        s_logger.info("Serial connection connected");
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
                s_logger.debug("Serial connection closed");
            } catch (IOException e) {
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, e.getMessage());
            }
            this.in = null;
            this.out = null;
            this.conn = null;
        }
    }

    @Override
    public int getConnectStatus() {
        if (this.conn == null) {
            return KuraConnectionStatus.DISCONNECTED;
        } else {
            return KuraConnectionStatus.CONNECTED;
        }
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

        if (this.m_txMode == ModbusTransmissionMode.RTU_MODE) {
            cmd = new byte[msg.length + 2];
            for (int i = 0; i < msg.length; i++) {
                cmd[i] = msg[i];
            }
            // Add crc calculation to end of message
            int crc = Crc16.getCrc16(msg, msg.length, 0x0ffff);
            cmd[msg.length] = (byte) crc;
            cmd[msg.length + 1] = (byte) (crc >> 8);
        } else if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE) {
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
                    if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE) {
                        minimumLength = 11;
                    }
                    int timeOut = this.m_respTout;
                    for (int maxLoop = 0; maxLoop < 1000; maxLoop++) {
                        boolean endFrame = false;
                        // while (respIndex < minimumLength) {
                        while (!endFrame) {
                            long start = System.currentTimeMillis();
                            while (this.in.available() == 0) {
                                try {
                                    Thread.sleep(5);    // avoid a high cpu load
                                } catch (InterruptedException e) {
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE,
                                            "Thread interrupted");
                                }

                                long elapsed = System.currentTimeMillis() - start;
                                if (elapsed > timeOut) {
                                    String failMsg = "Recv timeout";
                                    s_logger.warn(failMsg + " : " + elapsed + " minimumLength=" + minimumLength
                                            + " respIndex=" + respIndex);
                                    throw new ModbusProtocolException(ModbusProtocolErrorCode.RESPONSE_TIMEOUT,
                                            failMsg);
                                }
                            }
                            // address byte must match first
                            if (respIndex == 0) {
                                if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE) {
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

                            if (this.m_txMode == ModbusTransmissionMode.RTU_MODE) {
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
                        if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE) {
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
                            if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE
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
                                } else if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE
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
                                if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE) {
                                    byteCnt = (response[2] & 0xff) + 3;
                                } else {
                                    byteCnt = (response[2] & 0xff) + 5;
                                }
                                if (respIndex < byteCnt) {
                                    // wait for more data
                                    minimumLength = byteCnt;
                                } else if (this.m_txMode == ModbusTransmissionMode.ASCII_MODE
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
