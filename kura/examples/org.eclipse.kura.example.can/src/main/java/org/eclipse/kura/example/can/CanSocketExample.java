/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.can;

import java.io.IOException;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.protocol.can.CanConnectionService;
import org.eclipse.kura.protocol.can.CanMessage;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CanSocketExample implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(CanSocketExample.class);

    private static final String CAN_INTERFACE_NAME_PROP_NAME = "can.interface.name";
    private static final String CAN_IDENTIFIER_PROP_NAME = "can.identifier";
    private static final String CAN_IS_MASTER_PROP_NAME = "master";

    private static final String CAN_INTERFACE_DEFAULT = "can0";
    private static final Integer CAN_IDENTIFIER_DEFAULT = 1;
    private static final Boolean CAN_IS_MASTER_DEFAULT = false;

    private volatile CanConnectionService canConnection;

    private byte index = 0;
    private Thread worker;

    public void setCanConnectionService(CanConnectionService canConnection) {
        this.canConnection = canConnection;
    }

    public void unsetCanConnectionService(CanConnectionService canConnection) {
        this.canConnection = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activating...");

        try {
            this.canConnection.connectCanSocket();
        } catch (IOException e) {
            logger.error("failed to connect can socket", e);
            return;
        }

        updated(properties);

        logger.info("activating...done");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivating...");
        cancelCurrentTask();

        try {
            this.canConnection.disconnectCanSocket();
        } catch (IOException e) {
            logger.error("failed to disconnect can socket", e);
        }
        logger.info("deactivating...done");
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("updating...");

        cancelCurrentTask();
        final boolean isMaster = (Boolean) properties.getOrDefault(CAN_IS_MASTER_PROP_NAME, CAN_IS_MASTER_DEFAULT);

        if (isMaster) {
            final String interfaceName = (String) properties.getOrDefault(CAN_INTERFACE_NAME_PROP_NAME,
                    CAN_INTERFACE_DEFAULT);
            final int canId = (Integer) properties.getOrDefault(CAN_IDENTIFIER_PROP_NAME, CAN_IDENTIFIER_DEFAULT);
            startSenderThread(interfaceName, canId, 0);
        } else {
            startReceiverThread();
        }

        logger.debug("updating done...");
    }

    private void startSenderThread(String interfaceName, int canId, int dest) {
        this.worker = new Thread(() -> {
            while (!Thread.interrupted()) {
                int id = 0x500 + (canId << 4) + dest;
                StringBuilder sb = new StringBuilder("Try to send can frame with message = ");
                byte btest[] = new byte[8];
                for (int i = 0; i < 8; i++) {
                    btest[i] = (byte) (this.index + i);
                    sb.append(btest[i]);
                    sb.append(" ");
                }
                sb.append(" and id = ");
                sb.append(id);
                logger.info(sb.toString());

                try {
                    this.canConnection.sendCanMessage(interfaceName, id, btest);
                } catch (Exception e) {
                    logger.warn("Failed to send CAN frame", e);
                }

                this.index++;
                if (this.index > 14) {
                    this.index = 0;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "CanSenderThread");
        this.worker.start();
    }

    private void startReceiverThread() {
        this.worker = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    logger.info("Wait for a request");
                    final CanMessage cm = this.canConnection.receiveCanMessage(-1, 0xFFFF);
                    logger.info("request received");
                    byte[] b = cm.getData();
                    if (b != null) {
                        StringBuilder sb = new StringBuilder("received : ");
                        for (byte element : b) {
                            sb.append(element);
                            sb.append(";");
                        }
                        sb.append(" on id = ");
                        sb.append(cm.getCanId());
                        logger.info(sb.toString());
                    } else {
                        logger.warn("receive=null");
                    }
                } catch (IOException e) {
                    logger.warn("CanConnection Crash : {}", e.getMessage());
                }
            }
        }, "CanReceiverThread");
        this.worker.start();
    }

    private void cancelCurrentTask() {
        if (this.worker != null) {
            this.worker.interrupt();
            try {
                this.worker.join(1000);
            } catch (InterruptedException e) {
            }
            this.worker = null;
        }
    }
}
