/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EthernetCommunicateTest {

    private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolDevice.class);

    @Test
    public void testEthernetCommunicate() throws ModbusProtocolException, NoSuchFieldException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP);
        properties.setProperty("ipAddress", "127.0.0.1");
        properties.setProperty("ethport", "12345");

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        EthernetCommunicate comm = new EthernetCommunicate(properties, s_logger, txMode, responseTimeout);

        assertEquals(txMode, (int) TestUtil.getFieldValue(comm, "m_txMode"));
        assertEquals(responseTimeout, (int) TestUtil.getFieldValue(comm, "m_respTout"));
        assertTrue((boolean) TestUtil.getFieldValue(comm, "modbusTcpIp"));
        assertEquals("127.0.0.1", (String) TestUtil.getFieldValue(comm, "ipAddress"));
        assertEquals(12345, (int) TestUtil.getFieldValue(comm, "port"));
        assertNull(TestUtil.getFieldValue(comm, "socket"));
    }

    @Test
    public void testConnect() throws ModbusProtocolException, NoSuchFieldException, IOException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP);
        properties.setProperty("ipAddress", "127.0.0.1");
        properties.setProperty("ethport", "12345");

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        Socket mockSocket = mock(Socket.class);

        EthernetCommunicate comm = new EthernetCommunicate(properties, s_logger, txMode, responseTimeout) {

            @Override
            protected Socket createSocket() throws IOException {
                return mockSocket;
            }
        };

        // Connect
        assertEquals(KuraConnectionStatus.DISCONNECTED, comm.getConnectStatus());

        comm.connect();

        assertEquals(KuraConnectionStatus.CONNECTED, comm.getConnectStatus());

        verify(mockSocket).getInputStream();
        verify(mockSocket).getOutputStream();
    }

    @Test
    public void testDisconnect() throws NoSuchFieldException, ModbusProtocolException, IOException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP);
        properties.setProperty("ipAddress", "127.0.0.1");
        properties.setProperty("ethport", "12345");

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        Socket mockSocket = mock(Socket.class);

        EthernetCommunicate comm = new EthernetCommunicate(properties, s_logger, txMode, responseTimeout) {

            @Override
            protected Socket createSocket() throws IOException {
                return mockSocket;
            }
        };

        // Connect
        assertEquals(KuraConnectionStatus.DISCONNECTED, comm.getConnectStatus());

        comm.connect();

        assertEquals(KuraConnectionStatus.CONNECTED, comm.getConnectStatus());

        // Disconnect
        when(mockSocket.isInputShutdown()).thenReturn(false);
        when(mockSocket.isOutputShutdown()).thenReturn(false);

        comm.disconnect();

        assertEquals(KuraConnectionStatus.DISCONNECTED, comm.getConnectStatus());

        verify(mockSocket).shutdownInput();
        verify(mockSocket).shutdownOutput();
        verify(mockSocket).close();
    }

    @Test
    public void testMsgTransaction() throws IOException, ModbusProtocolException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP);
        properties.setProperty("ipAddress", "127.0.0.1");
        properties.setProperty("ethport", "12345");

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = mock(InputStream.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        int mockTransactionIndex = 42;

        EthernetCommunicate comm = new EthernetCommunicate(properties, s_logger, txMode, responseTimeout) {

            @Override
            protected Socket createSocket() throws IOException {
                return mockSocket;
            }

            @Override
            protected int getNextTransactionIndex() {
                return mockTransactionIndex;
            }
        };

        // Connect
        comm.connect();

        // Mock the communication
        int unitAddr = 1;
        int dataAddress = 2;
        int count = 3;

        byte[] message = new byte[6];
        message[0] = (byte) unitAddr;
        message[1] = (byte) ModbusFunctionCodes.READ_COIL_STATUS;
        message[2] = 0;
        message[3] = (byte) dataAddress;
        message[4] = 0;
        message[5] = (byte) count;

        byte[] command = new byte[message.length + 6];
        command[0] = (byte) (mockTransactionIndex >> 8);
        command[1] = (byte) mockTransactionIndex;
        command[2] = 0;
        command[3] = 0;
        command[4] = 0;
        command[5] = (byte) message.length;
        for (int i = 0; i < message.length; i++) {
            command[i + 6] = message[i];
        }

        when(mockInputStream.available()).thenReturn(1).thenReturn(0);
        when(mockInputStream.read()).thenReturn(0x00);

        int responseDataLength = (count + 7) / 8;
        byte[] modbusResponse = new byte[9 + responseDataLength];
        modbusResponse[0] = (byte) (mockTransactionIndex >> 8);
        modbusResponse[1] = (byte) mockTransactionIndex;
        modbusResponse[2] = 0;
        modbusResponse[3] = 0;
        modbusResponse[4] = (byte) (message.length >> 8);
        modbusResponse[5] = (byte) message.length;
        modbusResponse[6] = message[0];
        modbusResponse[7] = message[1];
        modbusResponse[8] = (byte) responseDataLength;
        modbusResponse[9] = 0x05; // b0 and b2 are set

        when(mockInputStream.read(anyObject(), anyInt(), anyInt())).then(new Answer<Integer>() {

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();

                byte[] buffer = (byte[]) arguments[0];
                int offset = (int) arguments[1];
                int length = (int) arguments[2];
                int bytesRead = 0;

                for (int i = 0; i < length; i++) {
                    if ((offset + i) < modbusResponse.length) {
                        buffer[offset + i] = modbusResponse[offset + i];
                        bytesRead++;
                    } else {
                        break;
                    }
                }

                return bytesRead;
            }

        });

        // Send message
        byte[] response = comm.msgTransaction(message);

        verify(mockOutputStream, times(1)).write(command, 0, command.length);
        verify(mockOutputStream, times(1)).flush();
        verify(mockSocket, atLeastOnce()).setSoTimeout(responseTimeout);

        for (int i = 0; i < response.length; i++) {
            assertEquals(modbusResponse[6 + i], response[i]);
        }
    }

}
