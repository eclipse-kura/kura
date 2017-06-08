/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.kura.protocol.modbus.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusHandler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ModbusHandler.class);

    private Socket socket = null;

    public ModbusHandler(Socket socket) {
        super("ModbusHandler");
        this.socket = socket;
    }

    public void run() {
        try (OutputStream out = socket.getOutputStream(); InputStream in = socket.getInputStream()) {
            byte[] input = new byte[256];
            int length = in.read(input);
            byte[] output = handleRequest(input);
            out.write(output);
        } catch (IOException e) {
            logger.error("ModbusHandler", e);
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("ModbusHandler", e);
            }
        }
    }

    private byte[] handleRequest(byte[] input) {
        byte command = input[7];
        switch (command) {
        case 1:
            return new byte[] { 0, 1, 0, 0, 0, 4, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, };           // readColis
        case 5:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 5, 0, 0, input[10], 0, 0, 0, 0 };             // writeSingleCoil
        case 15:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 15, 0, 1, 0, 5, 0, 0, 0, 0, 0 };              // writeMultiplecoils
        case 3:
            return new byte[] { 0, 1, 0, 0, 0, 5, 1, 3, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0 };            // readHoldingRegister
        case 6:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 6, 0, 0, 0, input[11], 0, 0, 0 };             // wrieSingleRigister
        case 16:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 16, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0 };           // writeMultipleRegisters
        case 2:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 2, 1, 127, 0, 0, 0, 0, 0, 0, 0, 0 };          // readDiscreteInputs
        case 4:
            return new byte[] { 0, 1, 0, 0, 0, 6, 1, 4, 2, 0, 10, 0, 0, 0, 0, 0, 0, 0 };           // readInputRegisters
        case 11:
            return new byte[] {};                                                                  // getCommEventCouner
        case 7:
            return new byte[] {};                                                                  // readExceptionStatus
        case 12:
            return new byte[] {};                                                                  // getCommEventLog
        }
        return new byte[] {};
    }
}