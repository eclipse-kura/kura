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
package org.eclipse.kura.linux.net.dhcp;

import static org.junit.Assert.assertEquals;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

public class DhcpServerManagerTest {

    @Test
    public void testGetConfigFilename() throws NoSuchFieldException {
        String interfaceName = "eth0";
        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.NONE);
        String fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.DHCPD);
        fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/dhcpd-eth0.conf", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.UDHCPD);
        fileName = DhcpServerManager.getConfigFilename(interfaceName);
        assertEquals("/etc/udhcpd-eth0.conf", fileName);
    }

    @Test
    public void testGetPidFilename() throws NoSuchFieldException {
        String interfaceName = "eth0";
        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.NONE);
        String fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.DHCPD);
        fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/dhcpd-eth0.pid", fileName);

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.UDHCPD);
        fileName = DhcpServerManager.getPidFilename(interfaceName);
        assertEquals("/var/run/udhcpd-eth0.pid", fileName);
    }

}
