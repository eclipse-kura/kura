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
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.slf4j.Logger;

/**
 * Installation of an ethernet connection to communicate
 */
class EthernetCommunicate extends Communicate {

    private static int transactionIndex = 0;
    private final Logger s_logger;
    private int m_txMode;
    private int m_respTout;

    InputStream inputStream;
    OutputStream outputStream;
    Socket socket;
    int port;
    String ipAddress;
    boolean modbusTcpIp;
    boolean modbusTcpRtu;
    boolean connected = false;

    public EthernetCommunicate(Properties connectionConfig, Logger logger, int txMode,
            int respTout) throws ModbusProtocolException {
        s_logger = logger;
        s_logger.debug("Configure TCP connection");

        m_txMode = txMode;
        m_respTout = respTout;

        String connectionType = connectionConfig.getProperty("connectionType");
        
        if (ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP.equals(connectionType)) {
            this.modbusTcpIp = true;
            this.modbusTcpRtu = false;
        } else {
            this.modbusTcpIp = false;
            this.modbusTcpRtu = true;
        }
        
        String sPort;

        if ((sPort = connectionConfig.getProperty("ethport")) == null
                || (this.ipAddress = connectionConfig.getProperty("ipAddress")) == null) {
            throw new ModbusProtocolException(ModbusProtocolErrorCode.INVALID_CONFIGURATION);
        }
        this.port = Integer.valueOf(sPort).intValue();
        this.socket = null;
    }

    @Override
    public void connect() {
        if (!this.connected) {
            try {
                this.socket = createSocket();
                try {
                    this.inputStream = this.socket.getInputStream();
                    this.outputStream = this.socket.getOutputStream();
                    this.connected = true;
                    s_logger.info("TCP connected");
                } catch (IOException e) {
                    disconnect();
                    s_logger.error("Failed to get socket streams: " + e);
                }
            } catch (IOException e) {
                s_logger.error("Failed to connect to remote: " + e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (this.socket == null) {
            return;
        }
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
                s_logger.error("Error closing TCP: " + eClose);
            }
            this.inputStream = null;
            this.outputStream = null;
            this.connected = false;
            this.socket = null;
        }
    }

    @Override
    public int getConnectStatus() {
        if (this.connected) {
            return KuraConnectionStatus.CONNECTED;
        } else {
            return KuraConnectionStatus.DISCONNECTED;
        }
    }

    @Override
    public byte[] msgTransaction(byte[] msg) throws ModbusProtocolException {
        byte[] cmd = null;

        // ---------------------------------------------- Send Message
        // ---------------------------------------------------
        if (this.m_txMode == ModbusTransmissionMode.RTU_MODE) {
            if (this.modbusTcpIp) {
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
            s_logger.error("Socket disconnect in send: " + e);
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
        if (this.modbusTcpIp) {
            minimumLength += 6;
        }
        while (!endFrame) {
            try {
                this.socket.setSoTimeout(this.m_respTout);
                int resp = this.inputStream.read(response, respIndex, 1);
                if (resp > 0) {
                    respIndex += resp;
                    if (this.modbusTcpIp) {
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
                                    minimumLength = response[8] + 9;
                                }
                            }
                        } else if (respIndex == minimumLength) {
                            endFrame = true;
                        }
                    } else {

                    }
                } else {
                    s_logger.error("Socket disconnect in recv");
                    throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, "Recv failure");
                }
            } catch (SocketTimeoutException e) {
                String failMsg = "Recv timeout";
                s_logger.warn(failMsg);
                throw new ModbusProtocolException(ModbusProtocolErrorCode.TRANSACTION_FAILURE, failMsg);
            } catch (IOException e) {
                s_logger.error("Socket disconnect in recv: " + e);
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

    protected Socket createSocket() throws IOException {
        return new Socket(this.ipAddress, this.port);
    }

    /**
     * Calculates and returns the next transaction index for Modbus TCP.
     * 
     * @return the next transaction index.
     */
    protected int getNextTransactionIndex() {
        transactionIndex++;
        if (transactionIndex > 0xffff) {
            transactionIndex = 0;
        }
        return transactionIndex;
    }
}