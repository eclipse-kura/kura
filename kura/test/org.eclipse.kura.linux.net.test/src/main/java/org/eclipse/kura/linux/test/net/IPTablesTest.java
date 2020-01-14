/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class IPTablesTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(IPTablesTest.class);

    private static final String TEST_ENABLE_FAILED_MESSAGE = "testEnable failed: ";
    private static final String TEST_ENABLE_MESSAGE = "testEnable: config appends some rules";

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
    private LinuxFirewall firewall;

    private static final String TMPDIR = "/tmp/" + IPTablesTest.class.getName();
    private static String oldConfig = TMPDIR + "/iptables_"
            + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    private CommandExecutorService executorService;

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
        dependencyLatch.countDown();
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
        dependencyLatch.countDown();
    }

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @BeforeClass
    public void setUp() {
        firewall = new LinuxFirewall(this.executorService);

        File tmpDir = new File(TMPDIR);
        tmpDir.mkdirs();

        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
            System.exit(1);
        }

        // Backup current iptables config
        try (FileOutputStream fos = new FileOutputStream(oldConfig); PrintWriter pw = new PrintWriter(fos);) {
            logger.info("Backing up current iptables config to {}", oldConfig);

            // Write current config to file
            pw.write(getCurrentIptablesConfig());
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            fail("Error backing up current iptables config");
            System.exit(1);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testDisable() {
        logger.info("Test disable firewall");

        try {
            firewall.disable();

            String config = getCurrentIptablesConfig();
            assertFalse("testDisable: config does not append to any tables", config.contains("-A"));
        } catch (KuraException e) {
            fail("testDisable failed: " + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testEnable() {
        logger.info("Test enable firewall");

        try {
            firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue(TEST_ENABLE_MESSAGE, config.contains("-A"));
        } catch (KuraException e) {
            fail(TEST_ENABLE_FAILED_MESSAGE + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testLocalRule() {
        logger.info("Test local rule");

        int testPort = 12345;

        try {
            firewall.addLocalRule(testPort, "tcp", null, null, null, null, null, null);
            firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue(TEST_ENABLE_MESSAGE, config.contains("-A"));
            assertTrue("testEnable: config has a rule that uses the test port - " + testPort,
                    config.contains(Integer.toString(testPort)));
        } catch (KuraException e) {
            fail(TEST_ENABLE_FAILED_MESSAGE + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testNatRule() {
        logger.info("Test NAT rule");

        String testIface = "test123";

        try {
            firewall.addNatRule(testIface, testIface, false);
            firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue(TEST_ENABLE_MESSAGE, config.contains("-A"));
            assertTrue("testEnable: config has a rule that uses the test values", config.contains(testIface));
        } catch (KuraException e) {
            fail(TEST_ENABLE_FAILED_MESSAGE + e);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testPortForwardRule() {
        logger.info("Test port forward rule");

        String testInboundIface = "testInboundIface";
        String testOutboundIface = "testOutboundIface";
        String testAddress = "12.34.56.78";
        int testPort1 = 12345;
        int testPort2 = 65432;

        try {
            firewall.addPortForwardRule(testInboundIface, testOutboundIface, testAddress, "tcp", testPort1, testPort2,
                    false, null, null, null, null);
            firewall.enable();

            String config = getCurrentIptablesConfig();
            assertTrue(TEST_ENABLE_MESSAGE, config.contains("-A"));

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
            fail(TEST_ENABLE_FAILED_MESSAGE + e);
        }
    }

    @Override
    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @AfterClass()
    public void tearDown() {
        // Restore old iptables config
        logger.info("Restoring iptables config from {}", oldConfig);

        Command command = new Command(new String[] { "iptables-restore", "<", oldConfig });
        command.setExecuteInAShell(true);
        CommandStatus status = this.executorService.execute(command);
        if (status.getExitStatus() != 0) {
            fail("Error restoring iptables config");
        }
    }

    private String getCurrentIptablesConfig() throws KuraException {
        Command command = new Command(new String[] { "iptables-save" });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        command.setOutputStream(out);
        CommandStatus status = this.executorService.execute(command);
        Integer exitValue = status.getExitStatus();
        if (exitValue != 0) {
            logger.error("error executing command --- iptables-save --- exit value = {}", exitValue);
            throw new KuraProcessExecutionErrorException("Error saving iptables config");
        }
        return new String(out.toByteArray(), Charsets.UTF_8);
    }

}
