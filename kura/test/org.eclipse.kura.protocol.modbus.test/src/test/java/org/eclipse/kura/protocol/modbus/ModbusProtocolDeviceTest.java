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

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModbusProtocolDeviceTest {

    @Test
    public void testGetProtocolName() {
        ModbusProtocolDevice device = new ModbusProtocolDevice();
        assertEquals("modbus", device.getProtocolName());
    }

    @Test(expected = ModbusProtocolException.class)
    public void testConnectInvalidConfig() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        device.connect();
    }

    @Test
    public void testConnect() throws ModbusProtocolException, IOException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        device.connect();

        verify(mockCommunicate, times(1)).connect();
    }

    @Test
    public void testDisconnectInvalidConfig() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);
        TestUtil.setFieldValue(device, "m_protConfigd", true);

        device.disconnect();

        assertFalse((boolean) TestUtil.getFieldValue(device, "m_protConfigd"));
    }

    @Test
    public void testDisconnect() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);
        TestUtil.setFieldValue(device, "m_protConfigd", true);

        device.disconnect();

        verify(mockCommunicate, times(1)).disconnect();
        assertFalse((boolean) TestUtil.getFieldValue(device, "m_protConfigd"));
    }

    @Test
    public void testGetConnectStatus() throws NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        // Never connected
        TestUtil.setFieldValue(device, "m_connConfigd", false);
        assertEquals(KuraConnectionStatus.NEVERCONNECTED, device.getConnectStatus());

        // Disconnected
        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        when(mockCommunicate.getConnectStatus()).thenReturn(KuraConnectionStatus.DISCONNECTED);
        assertEquals(KuraConnectionStatus.DISCONNECTED, device.getConnectStatus());

        // Connected
        when(mockCommunicate.getConnectStatus()).thenReturn(KuraConnectionStatus.CONNECTED);
        assertEquals(KuraConnectionStatus.CONNECTED, device.getConnectStatus());
    }

    @Test(expected = ModbusProtocolException.class)
    public void testReadCoilsNotConnected() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        device.readCoils(0, 0, 1);
    }

    @Test
    public void testReadCoils() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        int unitAddr = 5;
        int dataAddress = 10;
        int count = 3;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_COIL_STATUS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (count / 256);
        cmd[5] = (byte) (count % 256);

        byte[] response = new byte[4];
        response[0] = cmd[0];
        response[1] = cmd[1];
        response[2] = 1;
        response[3] = 0x05; // b0 and b2 are set

        when(mockCommunicate.msgTransaction(cmd)).thenReturn(response);

        boolean[] expected = { true, false, true };
        boolean[] result = device.readCoils(unitAddr, dataAddress, count);

        assertArrayEquals(expected, result);
    }

    @Test(expected = ModbusProtocolException.class)
    public void testReadDiscreteInputsNotConnected() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        device.readDiscreteInputs(0, 0, 1);
    }

    @Test
    public void testReadDiscreteInputs() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        int unitAddr = 5;
        int dataAddress = 10;
        int count = 3;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_INPUT_STATUS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (count / 256);
        cmd[5] = (byte) (count % 256);

        byte[] response = new byte[4];
        response[0] = cmd[0];
        response[1] = cmd[1];
        response[2] = 1;
        response[3] = 0x05; // b0 and b2 are set

        when(mockCommunicate.msgTransaction(cmd)).thenReturn(response);

        boolean[] expected = { true, false, true };
        boolean[] result = device.readDiscreteInputs(unitAddr, dataAddress, count);

        assertArrayEquals(expected, result);
    }

    @Test(expected = ModbusProtocolException.class)
    public void testWriteSingleCoilNotConnected() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        device.writeSingleCoil(0, 0, true);
    }

    @Test
    public void testWriteSingleCoil() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        int unitAddr = 5;
        int dataAddress = 10;
        boolean data = true;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.FORCE_SINGLE_COIL;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = data == true ? (byte) 0xff : (byte) 0;
        cmd[5] = 0;

        byte[] response = cmd;

        when(mockCommunicate.msgTransaction(cmd)).thenReturn(response);

        device.writeSingleCoil(unitAddr, dataAddress, data);

        verify(mockCommunicate, times(1)).msgTransaction(cmd);
    }

    @Test(expected = ModbusProtocolException.class)
    public void testWriteMultipleCoilsNotConnected() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        boolean[] data = { true, false, true };
        device.writeMultipleCoils(0, 0, data);
    }

    @Test
    public void testWriteMultipleCoils() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        int unitAddr = 5;
        int dataAddress = 10;
        boolean[] data = { true, false, true };

        int dataLength = (data.length + 7) / 8;
        byte[] cmd = new byte[dataLength + 7];
        cmd[0] = (byte) unitAddr;
        cmd[1] = ModbusFunctionCodes.FORCE_MULTIPLE_COILS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = (byte) (data.length / 256);
        cmd[5] = (byte) (data.length % 256);
        cmd[6] = (byte) dataLength;
        cmd[7] = 0x05; // b0 and b2 are set

        byte[] response = cmd;

        when(mockCommunicate.msgTransaction(cmd)).thenReturn(response);

        device.writeMultipleCoils(unitAddr, dataAddress, data);

        verify(mockCommunicate, times(1)).msgTransaction(cmd);
    }

    @Test(expected = ModbusProtocolException.class)
    public void testReadHoldingRegistersNotConnected() throws ModbusProtocolException, NoSuchFieldException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        TestUtil.setFieldValue(device, "m_connConfigd", false);

        device.readHoldingRegisters(0, 0, 1);
    }

    @Test
    public void testReadHoldingRegisters() throws NoSuchFieldException, ModbusProtocolException {
        ModbusProtocolDevice device = new ModbusProtocolDevice();

        Communicate mockCommunicate = mock(Communicate.class);
        TestUtil.setFieldValue(device, "m_comm", mockCommunicate);
        TestUtil.setFieldValue(device, "m_connConfigd", true);

        int unitAddr = 5;
        int dataAddress = 10;
        int count = 3;

        byte[] cmd = new byte[6];
        cmd[0] = (byte) unitAddr;
        cmd[1] = (byte) ModbusFunctionCodes.READ_HOLDING_REGS;
        cmd[2] = (byte) (dataAddress / 256);
        cmd[3] = (byte) (dataAddress % 256);
        cmd[4] = 0;
        cmd[5] = (byte) count;

        byte[] response = new byte[9];
        response[0] = cmd[0];
        response[1] = cmd[1];
        response[2] = 3 * 2;

        response[3] = 0x12;
        response[4] = 0x34;

        response[5] = 0x56;
        response[6] = 0x78;

        response[7] = (byte) 0x9A;
        response[8] = (byte) 0xBC;

        when(mockCommunicate.msgTransaction(cmd)).thenReturn(response);

        int[] expected = { 0x1234, 0x5678, 0x9ABC };
        int[] result = device.readHoldingRegisters(unitAddr, dataAddress, count);

        assertArrayEquals(expected, result);
    }

}
