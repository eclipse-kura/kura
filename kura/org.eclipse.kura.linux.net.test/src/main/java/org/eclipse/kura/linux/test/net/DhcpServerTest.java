/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.test.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpServerFactory;
import org.eclipse.kura.linux.net.dhcp.DhcpServerImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class DhcpServerTest extends TestCase {

    private static final Logger s_logger = LoggerFactory.getLogger(DhcpServerTest.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(0);	// initialize with number of dependencies

    private static DhcpServerImpl s_dhcpServer;

    private static final String TMPDIR = "/tmp/" + DhcpServerTest.class.getName();
    private static String oldConfigBackup = TMPDIR + "/dhcpd.conf.backup";

    private static final String s_testInterface = "eth0";

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @BeforeClass
    public void setUp() {
        try {
            s_dhcpServer = DhcpServerFactory.getInstance(s_testInterface, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        File tmpDir = new File(TMPDIR);
        tmpDir.mkdirs();

        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
            System.exit(1);
        }

        // Backup current dhcpd config
        try {
            s_logger.info("Backing up current dhcpd config to " + oldConfigBackup);

            // Read current config from file
            File oldConfig = new File(s_dhcpServer.getConfigFilename());
            StringBuffer data = new StringBuffer();

            if (oldConfig.exists()) {
                FileReader fr = new FileReader(oldConfig);

                int in;
                while ((in = fr.read()) != -1) {
                    data.append((char) in);
                }
                fr.close();
            }

            // Write current config to file
            FileOutputStream fos = new FileOutputStream(oldConfigBackup);
            PrintWriter pw = new PrintWriter(fos);
            pw.write(data.toString());
            pw.flush();
            fos.getFD().sync();
            pw.close();
            fos.close();
        } catch (Exception e) {
            fail("Error backing up current dhcpd config");
            System.exit(1);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisable() {
        s_logger.info("Test disable dhcp server");

        try {
            s_dhcpServer.disable();
            assertFalse("dhcp server is disabled", s_dhcpServer.isRunning());
        } catch (Exception e) {
            fail("testDisable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testEnable() {
        s_logger.info("Test enable dhcp server");

        try {
            // Setup note: Assumes the existence of the test interface, and that it can be brought up with an ip address
            LinuxNetworkUtil.disableInterface(s_testInterface);
            LinuxNetworkUtil.enableInterface(s_testInterface);

            String ip = LinuxNetworkUtil.getCurrentIpAddress(s_testInterface);
            assertNotNull(ip);

            String[] ip_parts = ip.split("\\.");
            String subnet = ip_parts[0] + "." + ip_parts[1] + "." + ip_parts[2] + ".0";
            String rangeFrom = ip_parts[0] + "." + ip_parts[1] + "." + ip_parts[2] + ".200";
            String rangeTo = ip_parts[0] + "." + ip_parts[1] + "." + ip_parts[2] + ".255";

            try {
				DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(s_testInterface, true, 3600, 10000, false);
				DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(
						(IP4Address) IPAddress.parseHostAddress(subnet),
						(IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24, null,
						(IP4Address) IPAddress.parseHostAddress(rangeFrom),
						(IP4Address) IPAddress.parseHostAddress(rangeTo), null);
				
	            s_dhcpServer.setConfig(new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4));
	            s_dhcpServer.enable();
	            assertTrue("dhcp server is enabled", s_dhcpServer.isRunning());
            } catch (KuraException e) {
            	fail("testEnable failed: " + e);
            }
        } catch (Exception e) {
            fail("testEnable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSettings() {
        s_logger.info("Test get/set dhcp settings");

        try {
            boolean enabled = true;
            boolean passDns = true;
            int defaultLeaseTime = 13579;
            int maximumLeaseTime = 97531;
            IP4Address routerAddress = (IP4Address) IPAddress.parseHostAddress("192.168.2.1");
            IP4Address rangeFrom = (IP4Address) IPAddress.parseHostAddress("192.168.2.33");
            IP4Address rangeTo = (IP4Address) IPAddress.parseHostAddress("192.168.2.44");
            IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress("255.255.255.0");
            IP4Address subnet = (IP4Address) IPAddress.parseHostAddress(
                    NetworkUtil.calculateNetwork(routerAddress.getHostAddress(), subnetMask.getHostAddress()));
            short prefix = NetworkUtil.getNetmaskShortForm(subnetMask.getHostAddress());
            List<IP4Address> dnsServers = new ArrayList<IP4Address>();
            dnsServers.add((IP4Address) IPAddress.parseHostAddress("8.8.8.8"));
 
            try {
            	DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(s_testInterface, enabled, defaultLeaseTime, maximumLeaseTime, passDns);
				DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix,
						routerAddress, rangeFrom, rangeTo, dnsServers);
				
				// This assumes an existing subnet config from the previous test
				DhcpServerConfigIP4 dhcpServerConfig4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
				s_dhcpServer.setConfig(dhcpServerConfig4);
            	s_dhcpServer.enable();
            	 assertEquals(dhcpServerConfig4, s_dhcpServer.getDhcpServerConfig(enabled, passDns));
            } catch (KuraException e) {
            	 fail("testEnable failed: " + e);
            }
        } catch (Exception e) {
            fail("testEnable failed: " + e);
        }
    }

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @AfterClass()
    public void tearDown() {

        if (s_dhcpServer != null) {
            try {
                s_dhcpServer.disable();
            } catch (Exception e) {
                // continue anyway
            }
        }

        // Restore old dhcpd config
        try {
            s_logger.info("Restoring dhcpd config from " + oldConfigBackup);

            // Read current config from file
            File backupFile = new File(oldConfigBackup);
            StringBuffer data = new StringBuffer();

            if (backupFile.exists()) {
                FileReader fr = new FileReader(backupFile);

                int in;
                while ((in = fr.read()) != -1) {
                    data.append((char) in);
                }
                fr.close();
            }

            // Write backup config to file
            FileOutputStream fos = new FileOutputStream(s_dhcpServer.getConfigFilename());
            PrintWriter pw = new PrintWriter(fos);
            pw.write(data.toString());
            pw.flush();
            fos.getFD().sync();
            pw.close();
            fos.close();
        } catch (Exception e) {
            fail("Error restoring dhcpd config");
        }
    }

}
