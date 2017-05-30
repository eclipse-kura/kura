/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.net.dhcp;

import static org.junit.Assert.assertEquals;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

public class DhcpServerManagerTest {

    @Test
    public void testGetConfigFilename() throws NoSuchFieldException {
        String interfaceName = "eth0";
        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.NONE);
        String fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.DHCPD);
        fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/dhcpd-eth0.conf", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.UDHCPD);
        fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/udhcpd-eth0.conf", fileName);
    }

    @Test
    public void testGetPidFilename() throws NoSuchFieldException {
        String interfaceName = "eth0";
        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.NONE);
        String fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.DHCPD);
        fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/dhcpd-eth0.pid", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.UDHCPD);
        fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/udhcpd-eth0.pid", fileName);
    }

}
