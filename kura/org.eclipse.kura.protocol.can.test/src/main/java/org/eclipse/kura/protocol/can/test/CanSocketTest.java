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

    private static final Logger s_logger = LoggerFactory.getLogger(CanSocketTest.class);

    private CanConnectionService m_canConnection;
    private Map<String, Object> m_properties;
    private Thread m_pollThread;
    private boolean thread_done = false;
    private String m_ifName;
    private int m_canId;
    private int m_orig;
    private boolean m_isMaster;
    private byte indice = 0;

    public void setCanConnectionService(CanConnectionService canConnection) {
        this.m_canConnection = canConnection;
    }

    public void unsetCanConnectionService(CanConnectionService canConnection) {
        this.m_canConnection = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.m_properties = properties;
        s_logger.info("activating can test");
        this.m_ifName = "can0";
        this.m_canId = 0;
        this.m_orig = 0;
        this.m_isMaster = false;

        if (this.m_properties != null) {
            if (this.m_properties.get("can.name") != null) {
                this.m_ifName = (String) this.m_properties.get("can.name");
            }
            if (this.m_properties.get("can.identifier") != null) {
                this.m_canId = (Integer) this.m_properties.get("can.identifier");
            }
            if (this.m_properties.get("master") != null) {
                this.m_isMaster = (Boolean) this.m_properties.get("master");
            }
        }

        startCanTestThread();
    }

    protected void deactivate(ComponentContext componentContext) {
        if (this.m_pollThread != null) {
            this.m_pollThread.interrupt();
            try {
                this.m_pollThread.join(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        this.m_pollThread = null;
    }

    public void updated(Map<String, Object> properties) {
        s_logger.debug("updated...");

        this.m_properties = properties;
        if (this.m_properties != null) {
            if (this.m_properties.get("can.name") != null) {
                this.m_ifName = (String) this.m_properties.get("can.name");
            }
            if (this.m_properties.get("can.identifier") != null) {
                this.m_canId = (Integer) this.m_properties.get("can.identifier");
            }
            if (this.m_properties.get("master") != null) {
                this.m_isMaster = (Boolean) this.m_properties.get("master");
            }
        }
    }

    @Before
    public void setup() {
        Assume.assumeFalse(IS_MAC);
    }

    @Test
    public void startCanTestThread() {
        if (this.m_pollThread != null) {
            this.m_pollThread.interrupt();
            try {
                this.m_pollThread.join(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            this.m_pollThread = null;
        }

        this.m_pollThread = new Thread(new Runnable() {

            @Override
            public void run() {
                if (CanSocketTest.this.m_canConnection != null && !IS_MAC) {
                    while (!CanSocketTest.this.thread_done) {
                        CanSocketTest.this.thread_done = doCanTest();
                    }
                }
            }
        });
        this.m_pollThread.start();
    }

    public boolean doCanTest() {
        byte[] b;
        CanMessage cm = null;
        if (this.m_isMaster) {
            if (this.m_orig >= 0) {
                try {
                    testSendImpl(this.m_ifName, this.m_canId, this.m_orig);
                } catch (KuraException e) {
                    s_logger.warn("CanConnection Crash : {}", e.getMessage());
                    return false;
                } catch (IOException e) {
                    s_logger.warn("CanConnection Crash : {}", e.getMessage());
                    return false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        } else {
            s_logger.info("Wait for a request");
            try {

                cm = this.m_canConnection.receiveCanMessage(-1, 0x7FF);

            } catch (IOException e) {
                s_logger.warn("CanConnection Crash : {}", e.getMessage());
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
                s_logger.info(sb.toString());
            } else {
                s_logger.warn("receive=null");
            }
        }
        return false;
    }

    public void testSendImpl(String ifName, int orig, int dest) throws KuraException, IOException {
        if (this.m_canConnection == null || orig < 0) {
            return;
        }
        int id = 0x500 + (orig << 4) + dest;
        StringBuilder sb = new StringBuilder("Try to send can frame with message = ");
        byte btest[] = new byte[8];
        for (int i = 0; i < 8; i++) {
            btest[i] = (byte) (this.indice + i);
            sb.append(btest[i]);
            sb.append(" ");
        }
        sb.append(" and id = ");
        sb.append(id);
        s_logger.info(sb.toString());

        this.m_canConnection.sendCanMessage(ifName, id, btest);

        this.indice++;
        if (this.indice > 14) {
            this.indice = 0;
        }
    }
}
