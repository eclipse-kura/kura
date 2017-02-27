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
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.stubbing.OngoingStubbing;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SerialCommunicateTest {

    private static final Logger s_logger = LoggerFactory.getLogger(ModbusProtocolDevice.class);

    @Test
    public void testSerialCommunicate() throws ModbusProtocolException, IOException, NoSuchFieldException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_SERIAL);
        properties.setProperty("port", "/dev/ttyS9999");
        properties.setProperty("baudRate", "9600");
        properties.setProperty("stopBits", "1");
        properties.setProperty("parity", "0");
        properties.setProperty("bitsPerWord", "8");

        String uri = new CommURI.Builder("/dev/ttyS9999").withBaudRate(9600).withDataBits(8).withStopBits(1)
                .withParity(0).withTimeout(2000).build().toString();

        ConnectionFactory mockConnFactory = mock(ConnectionFactory.class);
        CommConnection mockCommConnection = mock(CommConnection.class);
        when(mockConnFactory.createConnection(uri, 1, false)).thenReturn(mockCommConnection);

        when(mockCommConnection.openInputStream()).thenReturn(mock(InputStream.class));
        when(mockCommConnection.openOutputStream()).thenReturn(mock(OutputStream.class));

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        SerialCommunicate comm = new SerialCommunicate(mockConnFactory, properties, s_logger, txMode, responseTimeout);

        assertEquals(txMode, (int) TestUtil.getFieldValue(comm, "m_txMode"));
        assertEquals(responseTimeout, (int) TestUtil.getFieldValue(comm, "m_respTout"));
        assertNotNull(TestUtil.getFieldValue(comm, "in"));
        assertNotNull(TestUtil.getFieldValue(comm, "out"));
        assertNotNull(TestUtil.getFieldValue(comm, "conn"));
        assertEquals(KuraConnectionStatus.CONNECTED, comm.getConnectStatus());

        verify(mockConnFactory, times(1)).createConnection(uri, 1, false);
        verify(mockCommConnection, atLeastOnce()).openInputStream();
        verify(mockCommConnection, atLeastOnce()).openOutputStream();
    }

    @Test
    public void testDisconnect() throws IOException, ModbusProtocolException, NoSuchFieldException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_SERIAL);
        properties.setProperty("port", "/dev/ttyS9999");
        properties.setProperty("baudRate", "9600");
        properties.setProperty("stopBits", "1");
        properties.setProperty("parity", "0");
        properties.setProperty("bitsPerWord", "8");

        String uri = new CommURI.Builder("/dev/ttyS9999").withBaudRate(9600).withDataBits(8).withStopBits(1)
                .withParity(0).withTimeout(2000).build().toString();

        ConnectionFactory mockConnFactory = mock(ConnectionFactory.class);
        CommConnection mockCommConnection = mock(CommConnection.class);
        when(mockConnFactory.createConnection(uri, 1, false)).thenReturn(mockCommConnection);

        when(mockCommConnection.openInputStream()).thenReturn(mock(InputStream.class));
        when(mockCommConnection.openOutputStream()).thenReturn(mock(OutputStream.class));

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 10;

        SerialCommunicate comm = new SerialCommunicate(mockConnFactory, properties, s_logger, txMode, responseTimeout);

        assertEquals(KuraConnectionStatus.CONNECTED, comm.getConnectStatus());

        // Disconnect
        comm.disconnect();

        assertEquals(KuraConnectionStatus.DISCONNECTED, comm.getConnectStatus());
        assertNull(TestUtil.getFieldValue(comm, "in"));
        assertNull(TestUtil.getFieldValue(comm, "out"));
        assertNull(TestUtil.getFieldValue(comm, "conn"));

        verify(mockCommConnection).close();
    }

    @Test
    public void testMsgTransaction() throws IOException, ModbusProtocolException {
        Properties properties = new Properties();
        properties.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_SERIAL);
        properties.setProperty("port", "/dev/ttyS9999");
        properties.setProperty("baudRate", "9600");
        properties.setProperty("stopBits", "1");
        properties.setProperty("parity", "0");
        properties.setProperty("bitsPerWord", "8");

        String uri = new CommURI.Builder("/dev/ttyS9999").withBaudRate(9600).withDataBits(8).withStopBits(1)
                .withParity(0).withTimeout(2000).build().toString();

        ConnectionFactory mockConnFactory = mock(ConnectionFactory.class);
        CommConnection mockCommConnection = mock(CommConnection.class);
        when(mockConnFactory.createConnection(uri, 1, false)).thenReturn(mockCommConnection);

        InputStream mockInputStream = mock(InputStream.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockCommConnection.openInputStream()).thenReturn(mockInputStream);
        when(mockCommConnection.openOutputStream()).thenReturn(mockOutputStream);

        int txMode = ModbusTransmissionMode.RTU_MODE;
        int responseTimeout = 1000;

        SerialCommunicate comm = new SerialCommunicate(mockConnFactory, properties, s_logger, txMode, responseTimeout);

        assertEquals(KuraConnectionStatus.CONNECTED, comm.getConnectStatus());

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

        byte[] command = new byte[message.length + 2];
        for (int i = 0; i < message.length; i++) {
            command[i] = message[i];
        }

        int crc = Crc16.getCrc16(message, message.length, 0x0ffff);
        command[message.length] = (byte) crc;
        command[message.length + 1] = (byte) (crc >> 8);

        int responseDataLength = (count + 7) / 8;
        byte[] modbusResponse = new byte[5 + responseDataLength];
        modbusResponse[0] = message[0];
        modbusResponse[1] = message[1];
        modbusResponse[2] = (byte) responseDataLength;
        modbusResponse[3] = 0; // b0 and b2 are set

        int modbusResponseCrc = Crc16.getCrc16(modbusResponse, 4, 0x0ffff);
        modbusResponse[4] = (byte) modbusResponseCrc;
        modbusResponse[5] = (byte) (modbusResponseCrc >> 8);

        OngoingStubbing<Integer> misAvailableStubbing = when(mockInputStream.available()).thenReturn(1).thenReturn(0);
        for (int i = 0; i < modbusResponse.length; i++) {
            misAvailableStubbing.thenReturn(1);
        }
        misAvailableStubbing.thenReturn(0);

        OngoingStubbing<Integer> misRead = when(mockInputStream.read()).thenReturn(0x00);
        for (int i = 0; i < modbusResponse.length; i++) {
            misRead.thenReturn((int) modbusResponse[i]);
        }

        // Send message
        byte[] response = comm.msgTransaction(message);

        verify(mockOutputStream, times(1)).write(command, 0, command.length);
        verify(mockOutputStream, times(1)).flush();

        assertArrayEquals(modbusResponse, response);
    }

}
