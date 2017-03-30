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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class IPTablesTest extends TestCase {

    private static final Logger s_logger = LoggerFactory.getLogger(IPTablesTest.class);

    private static CountDownLatch dependencyLatch = new CountDownLatch(0);	// initialize with number of dependencies

    private static LinuxFirewall s_firewall;

    private static final String TMPDIR = "/tmp/" + IPTablesTest.class.getName();
    private static String oldConfig = TMPDIR + "/iptables_"
            + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @BeforeClass
    public void setUp() {
        s_firewall = LinuxFirewall.getInstance();

        File tmpDir = new File(TMPDIR);
        tmpDir.mkdirs();

        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
            System.exit(1);
        }

        // Backup current iptables config
        try {
            s_logger.info("Backing up current iptables config to " + oldConfig);

            // Write current config to file
            FileOutputStream fos = new FileOutputStream(oldConfig);
            PrintWriter pw = new PrintWriter(fos);
            pw.write(getCurrentIptablesConfig());
            pw.flush();
            fos.getFD().sync();
            pw.close();
            fos.close();
        } catch (Exception e) {
            fail("Error backing up current iptables config");
            System.exit(1);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisable() {
        s_logger.info("Test disable firewall");

        try {
            s_firewall.disable();

            String config = getCurrentIptablesConfig();
            assertFalse("testDisable: config does not append to any tables", config.contains("-A"));
        } catch (KuraException e) {
            fail("testDisable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testEnable() {
        s_logger.info("Test enable firewall");

        try {
            s_firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue("testEnable: config appends some rules", config.contains("-A"));
        } catch (KuraException e) {
            fail("testEnable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testLocalRule() {
        s_logger.info("Test local rule");

        int testPort = 12345;

        try {
            s_firewall.addLocalRule(testPort, "tcp", null, null, null, null, null, null);
            s_firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue("testEnable: config appends some rules", config.contains("-A"));
            assertTrue("testEnable: config has a rule that uses the test port - " + testPort,
                    config.contains(Integer.toString(testPort)));
        } catch (KuraException e) {
            fail("testEnable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testNatRule() {
        s_logger.info("Test NAT rule");

        String testIface = "test123";

        try {
            s_firewall.addNatRule(testIface, testIface, false);
            s_firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue("testEnable: config appends some rules", config.contains("-A"));
            assertTrue("testEnable: config has a rule that uses the test values", config.contains(testIface));
        } catch (KuraException e) {
            fail("testEnable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPortForwardRule() {
        s_logger.info("Test port forward rule");

        String testInboundIface = "testInboundIface";
        String testOutboundIface = "testOutboundIface";
        String testAddress = "12.34.56.78";
        int testPort1 = 12345;
        int testPort2 = 65432;

        try {
            s_firewall.addPortForwardRule(testInboundIface, testOutboundIface, testAddress, "tcp", testPort1, testPort2,
                    false, null, null, null, null);
            s_firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue("testEnable: config appends some rules", config.contains("-A"));

            // Look for a rule containing all of the test values
            boolean pass = false;
            String[] lines = config.split("\n");
            for (String line : lines) {
                if (line.contains(testInboundIface) && line.contains(testAddress)
                        && line.contains(Integer.toString(testPort1)) && line.contains(Integer.toString(testPort2))) {
                    pass = true;
                    break;
                }
            }

            assertTrue("testEnable: config has a port forward rule that uses the test values", pass);
        } catch (KuraException e) {
            fail("testEnable failed: " + e);
        }
    }

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @AfterClass()
    public void tearDown() {
        // Restore old iptables config
        SafeProcess proc = null;
        try {
            s_logger.info("Restoring iptables config from " + oldConfig);

            // Read current config from file
            FileReader fr = new FileReader(new File(oldConfig));

            int in;
            StringBuffer data = new StringBuffer();
            while ((in = fr.read()) != -1) {
                data.append((char) in);
            }
            fr.close();

            // Restore old config
            proc = ProcessUtil.exec("iptables-restore");
            OutputStreamWriter osr = new OutputStreamWriter(proc.getOutputStream());
            BufferedWriter bw = new BufferedWriter(osr);
            bw.write(data.toString());
            bw.flush();
            bw.close();

            proc.waitFor();
        } catch (Exception e) {
            fail("Error restoring iptables config");
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    private String getCurrentIptablesConfig() throws KuraException {
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec("iptables-save");
            if (proc.waitFor() != 0) {
                s_logger.error("error executing command --- iptables-save --- exit value = " + proc.exitValue());
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
            }

            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
            BufferedReader br = new BufferedReader(isr);

            StringBuffer configBuffer = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                configBuffer.append(line).append("\n");
            }
            br.close();

            proc.waitFor();

            return configBuffer.toString();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

}
