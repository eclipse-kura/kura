/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.kura.protocol.modbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.kura.KuraConnectionStatus;
import org.eclipse.kura.protocol.modbus.test.ModbusServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocolDeviceTest {

    private static final Logger logger = LoggerFactory.getLogger(ModbusProtocolDeviceTest.class);

    private static ModbusServer modbusServer;

    private ModbusProtocolDevice modbusDevice;

    @BeforeClass
    public static void startServer() throws Exception {
        modbusServer = new ModbusServer();
        modbusServer.start(32345);
        logger.info("MODBUS server started");
    }

    @AfterClass
    public static void stopServer() throws IOException {
        modbusServer.stop();
        logger.info("MODBUS server stopped");
    }

    @Before
    public void connect() throws ModbusProtocolException {
        modbusDevice = new ModbusProtocolDevice();
        Properties connectionConfig = new Properties();
        connectionConfig.setProperty("connectionType", ModbusProtocolDevice.PROTOCOL_CONNECTION_TYPE_ETHER_TCP);
        connectionConfig.setProperty("ipAddress", "127.0.0.1");
        connectionConfig.setProperty("ethport", "32345");
        connectionConfig.setProperty("respTimeout", "10000");
        connectionConfig.setProperty("transmissionMode", ModbusTransmissionMode.RTU);
        modbusDevice.configureConnection(connectionConfig);
        modbusDevice.connect();
    }

    @After
    public void disconnect() throws ModbusProtocolException {
        modbusDevice.disconnect();
    }

    @Test
    public void testGetConnectStatus() throws ModbusProtocolException {
        int connectStatus = modbusDevice.getConnectStatus();
        assertEquals(KuraConnectionStatus.CONNECTED, connectStatus);
    }

    @Test
    public void testReadCoils() throws ModbusProtocolException {
        boolean[] coils = modbusDevice.readCoils(1, 0, 1);
        assertEquals(1, coils.length);
        assertTrue(coils[0]);
    }

    @Test
    public void testWriteSingleCoil() throws ModbusProtocolException {
        modbusDevice.writeSingleCoil(1, 0, true);
        assertTrue("No exception", true);
    }

    @Test
    public void testWriteMultipleCoils() throws ModbusProtocolException {
        modbusDevice.writeMultipleCoils(1, 1, new boolean[] { true, false, true, true, false });
        assertTrue("No exception", true);
    }

    @Test
    public void testReadHoldingRegisters() throws ModbusProtocolException {
        int[] holdingReg = modbusDevice.readHoldingRegisters(1, 0, 1);
        assertEquals(1, holdingReg.length);
        assertEquals(2, holdingReg[0]);
    }

    @Test
    public void testWriteSingleRegister() throws ModbusProtocolException {
        modbusDevice.writeSingleRegister(1, 0, 37);
        assertTrue("No exception", true);
    }

    @Test
    public void testWriteMultipleRegister() throws ModbusProtocolException {
        modbusDevice.writeMultipleRegister(1, 0, new int[] { 12, 24, 46, 58 });
        assertTrue("No exception", true);
    }

    @Test
    public void testReadDiscreteInputs() throws ModbusProtocolException {
        boolean[] discreteInputs = modbusDevice.readDiscreteInputs(1, 1, 4);
        assertEquals(4, discreteInputs.length);
        assertTrue(discreteInputs[0]);
        assertTrue(discreteInputs[1]);
        assertTrue(discreteInputs[2]);
        assertTrue(discreteInputs[3]);
    }

    @Test
    public void testReadInputRegisters() throws ModbusProtocolException {
        int[] inputRegs = modbusDevice.readInputRegisters(1, 8, 1);
        assertEquals(inputRegs.length, 1);
        assertEquals(10, inputRegs[0]);
    }

}
