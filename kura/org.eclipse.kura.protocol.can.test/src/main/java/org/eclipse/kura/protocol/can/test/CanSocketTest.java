/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.protocol.can.test;

import java.io.IOException;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.protocol.can.CanConnectionService;
import org.eclipse.kura.protocol.can.CanMessage;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CanSocketTest implements ConfigurableComponent {

    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().startsWith("mac");

    private static final Logger logger = LoggerFactory.getLogger(CanSocketTest.class);

    private volatile CanConnectionService canConnection;
    private volatile String interfaceName;
    private volatile int canId;
    private volatile boolean isMaster;

    private Map<String, Object> properties;
    private Thread pollThread;
    private int orig;
    private byte index = 0;

    public void setCanConnectionService(CanConnectionService canConnection) {
        this.canConnection = canConnection;
    }

    public void unsetCanConnectionService(CanConnectionService canConnection) {
        this.canConnection = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.properties = properties;
        logger.info("activating can test");
        this.interfaceName = "can0";
        this.canId = 0;
        this.orig = 0;
        this.isMaster = false;

        if (this.properties != null) {
            if (this.properties.get("can.name") != null) {
                this.interfaceName = (String) this.properties.get("can.name");
            }
            if (this.properties.get("can.identifier") != null) {
                this.canId = (Integer) this.properties.get("can.identifier");
            }
            if (this.properties.get("master") != null) {
                this.isMaster = (Boolean) this.properties.get("master");
            }
        }

        startCanTestThread();
    }

    protected void deactivate(ComponentContext componentContext) {
        terminatePollThread();
    }

    public void updated(Map<String, Object> properties) {
        logger.debug("updated...");

        this.properties = properties;
        if (this.properties != null) {
            if (this.properties.get("can.name") != null) {
                this.interfaceName = (String) this.properties.get("can.name");
            }
            if (this.properties.get("can.identifier") != null) {
                this.canId = (Integer) this.properties.get("can.identifier");
            }
            if (this.properties.get("master") != null) {
                this.isMaster = (Boolean) this.properties.get("master");
            }
        }
    }

    @Before
    public void setup() {
        Assume.assumeFalse(IS_MAC);
    }

    @Test
    public void startCanTestThread() {
        terminatePollThread();

        this.pollThread = new Thread(new Runnable() {

            @Override
            public void run() {
                if (CanSocketTest.this.canConnection != null && !IS_MAC) {
                    boolean threadDone = false;
                    while (!threadDone) {
                        threadDone = doCanTest();
                    }
                }
            }
        });
        this.pollThread.start();
    }

    public boolean doCanTest() {
        byte[] b;
        CanMessage cm = null;
        if (this.isMaster) {
            if (this.orig >= 0) {
                try {
                    testSendImpl(this.interfaceName, this.canId, this.orig);
                } catch (KuraException e) {
                    logger.warn("CanConnection Crash : {}", e.getMessage());
                    return false;
                } catch (IOException e) {
                    logger.warn("CanConnection Crash : {}", e.getMessage());
                    return false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        } else {
            logger.info("Wait for a request");
            try {
                cm = this.canConnection.receiveCanMessage(-1, 0x7FF);
            } catch (IOException e) {
                logger.warn("CanConnection Crash : {}", e.getMessage());
                return false;
            }
            b = cm.getData();
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
        }
        return false;
    }

    public void testSendImpl(String ifName, int orig, int dest) throws KuraException, IOException {
        if (this.canConnection == null || orig < 0) {
            return;
        }
        int id = 0x500 + (orig << 4) + dest;
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

        this.canConnection.sendCanMessage(ifName, id, btest);

        this.index++;
        if (this.index > 14) {
            this.index = 0;
        }
    }

    private void terminatePollThread() {
        if (this.pollThread != null) {
            this.pollThread.interrupt();
            try {
                this.pollThread.join(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        this.pollThread = null;
    }
}
