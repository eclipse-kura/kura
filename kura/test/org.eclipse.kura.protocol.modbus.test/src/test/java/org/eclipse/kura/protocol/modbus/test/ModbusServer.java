/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.protocol.modbus.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusServer {

    private static final Logger logger = LoggerFactory.getLogger(ModbusServer.class);

    CountDownLatch latch = new CountDownLatch(1);

    private boolean listening = true;
    private ServerSocket serverSocket;

    public void start(int port) throws IOException, InterruptedException {
        new Thread() {

            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    latch.countDown();   // make sure server is ready before running tests
                    while (listening) {
                        ModbusHandler modbusHandler = new ModbusHandler(serverSocket.accept());
                        modbusHandler.start();
                    }
                } catch (IOException e) {
                    logger.error("ModbusServer fatal error", e);
                    System.exit(-1);
                }
            }
        }.start();
        latch.await();  // wait for server socket is ready
    }

    public void stop() throws IOException {
        // serverSocket.close();
        listening = false;
    }

}
