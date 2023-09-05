/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.system.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

public class SystemServiceTest {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String LINUX = "Linux"; // Ubuntu
    private static SystemService systemService = null;
    private static CommandExecutorService executorService = null;
    private static CountDownLatch dependencyLatch = new CountDownLatch(2);	// initialize with number of dependencies
    private boolean onCloudbees = false;

    @BeforeClass
    public static void setUp() {
        // Wait for OSGi dependencies
        try {
            if (!dependencyLatch.await(10, TimeUnit.SECONDS)) {
                fail("OSGi dependencies unfulfilled");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    protected void setExecutorService(CommandExecutorService ces) {
        executorService = ces;
        dependencyLatch.countDown();
    }

    protected void setSystemService(SystemService sms) {
        systemService = sms;
        onCloudbees = systemService.getOsName().contains("Cloudbees");
        dependencyLatch.countDown();
    }

    @Test
    public void testDummy() {
        assertTrue(true);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(systemService);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetPrimaryMacAddress() {

        String actual = systemService.getPrimaryMacAddress();

        if (actual != null && !actual.isEmpty()) {
            Pattern regex = Pattern.compile("[0-9a-fA-F:]{12}");
            Matcher match = regex.matcher(actual);

            assertEquals("getPrimaryMacAddress() length", 17, actual.length());
            assertTrue("getPrimaryMacAddress() is string with colons", match.find());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetPlatform() {
        String[] expected = { "dynacor",   					// emulated
                "Ubuntu",   									// Ubuntu
                "BeagleBone"								// BeagleBone
        };

        try {
            boolean foundMatch = false;
            for (String possibility : expected) {
                if (systemService.getPlatform().equals(possibility)) {
                    foundMatch = true;
                    break;
                }
            }
            assertTrue(foundMatch);
        } catch (Exception e) {
            fail("getPlatform() failed: " + e.getMessage());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetOsDistro() {
        String[] expected = { "DevOsDitribution",   			// emulated
                LINUX };

        try {
            boolean foundMatch = false;
            for (String possibility : expected) {
                if (systemService.getOsDistro().equals(possibility)) {
                    foundMatch = true;
                    break;
                }
            }
            assertTrue(foundMatch);
        } catch (Exception e) {
            fail("getOsDistro() failed: " + e.getMessage());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetOsDistroVersion() {
        String[] expected = { "DevOsDitributionVersion",   	// emulated
                "N/A" 										// Ubuntu
        };

        try {
            boolean foundMatch = false;
            for (String possibility : expected) {
                if (systemService.getOsDistroVersion().equals(possibility)) {
                    foundMatch = true;
                    break;
                }
            }
            assertTrue(foundMatch);
        } catch (Exception e) {
            fail("getOsDistroVersion() failed: " + e.getMessage());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetOsArch() {
        String expected = System.getProperty("os.arch");
        String actual = systemService.getOsArch();

        assertNotNull("getOsArch() not null", actual);
        assertEquals("getOsArch() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetOsName() {
        String expected = System.getProperty("os.name");
        if (onCloudbees) {
            expected = "Linux (Cloudbees)";
        }

        String actual = systemService.getOsName();

        assertNotNull("getOsName() not null", actual);
        assertEquals("getOsName() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetOsVersion() throws IOException {
        String osVersion = System.getProperty("os.version");
        StringBuilder sbOsVersion = new StringBuilder();
        sbOsVersion.append(osVersion);

        File linuxKernelVersion = null;
        linuxKernelVersion = new File("/proc/sys/kernel/version");
        if (linuxKernelVersion.exists()) {
            StringBuilder kernelVersionData = new StringBuilder();
            try (FileReader fr = new FileReader(linuxKernelVersion); BufferedReader in = new BufferedReader(fr)) {
                String tempLine = null;
                while ((tempLine = in.readLine()) != null) {
                    kernelVersionData.append(" ");
                    kernelVersionData.append(tempLine);
                }
                sbOsVersion.append(kernelVersionData.toString());
            }
        }

        String expected = sbOsVersion.toString();
        String actual = systemService.getOsVersion();

        assertNotNull("getOsVersion() not null", actual);
        assertEquals("getOsVersion() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetJavaVersion() {
        String expected = System.getProperty("java.runtime.version");
        String actual = systemService.getJavaVersion();

        assertNotNull("getJavaVersion() not null", actual);
        assertEquals("getJavaVersion() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetJavaVmName() {
        String expected = System.getProperty("java.vm.name");
        String actual = systemService.getJavaVmName();

        assertNotNull("getJavaVmName() not null", actual);
        assertEquals("getJavaVmName() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetJavaVmVersion() {
        String expected = System.getProperty("java.vm.version");
        String actual = systemService.getJavaVmVersion();

        assertNotNull("getJavaVmVersion() not null", actual);
        assertEquals("getJavaVmVersion() value", expected, actual);
    }

    @Test
    public void shouldReturnJavaVmVendor() {
        assertNotNull(systemService.getJavaVmVendor());
    }

    @Test
    public void shouldReturnJdkVendorVersion() {
        assertNull(systemService.getJdkVendorVersion());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetFileSeparator() {
        String expected = System.getProperty("file.separator");
        String actual = systemService.getFileSeparator();

        assertNotNull("getFileSeparator() not null", actual);
        assertEquals("getFileSeparator() value", expected, actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testJavaHome() {
        String actual = systemService.getJavaHome();
        assertNotNull("getJavaHome() not null", actual);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetProductVersion() {
        assertTrue(true);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testKuraTemporaryConfigDirectory() {
        assertNotNull(systemService.getKuraTemporaryConfigDirectory());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetBiosVersion() {
        assertNotNull(systemService.getBiosVersion());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getDeviceName() {
        assertNotNull(systemService.getDeviceName());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getFirmwareVersion() {
        assertNotNull(systemService.getFirmwareVersion());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getModelId() {
        assertNotNull(systemService.getModelId());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getModelName() throws NoSuchFieldException {
        // remove the default value so that the command is run
        Properties def = (Properties) TestUtil.getFieldValue(systemService.getProperties(), "defaults");
        def.remove(SystemService.KEY_MODEL_NAME);

        String modelName = systemService.getModelName();

        assertNotNull(modelName);

        String osName = systemService.getOsName();
        if (!osName.contains("indows")) {
            if (LINUX.equals(osName)) {
                CommandStatus status = executorService.execute(new Command(new String[] { "dmidecode" }));
                if (!status.getExitStatus().isSuccessful()) {
                    assertEquals(UNKNOWN, modelName);
                }

                // note: this assert works locally and on travis, but not on hudson
                // assertNotEquals("UNKNOWN", modelName);

                assertNotEquals("DevModelName", modelName);
            }
        } else {
            assertEquals(UNKNOWN, modelName);
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getPartNumber() {
        assertNotNull(systemService.getPartNumber());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getSerialNumber() throws NoSuchFieldException {
        // remove the default value so that the command is run
        Properties def = (Properties) TestUtil.getFieldValue(systemService.getProperties(), "defaults");
        def.remove(SystemService.KEY_SERIAL_NUM);

        String serialNumber = systemService.getSerialNumber();

        assertNotNull(serialNumber);

        String osName = systemService.getOsName();
        if (!osName.contains("indows")) {
            if (LINUX.equals(osName)) {
                CommandStatus status = executorService.execute(new Command(new String[] { "dmidecode" }));
                if (!status.getExitStatus().isSuccessful()) {
                    assertEquals(UNKNOWN, serialNumber);
                }
                // note: this assert works locally and on travis, but not on hudson
                // assertNotEquals("UNKNOWN", serialNumber);

                assertNotEquals("DevSerialNumber", serialNumber);
            }
        } else {
            assertEquals(UNKNOWN, serialNumber);
        }
    }

}
