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
package org.eclipse.kura.core.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class SystemServiceTest extends TestCase {

    private static SystemService systemService = null;
    private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
    private static boolean onCloudbees = false;

    @Override
    @BeforeClass
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
        }
    }

    public static void setSystemService(SystemService sms) {
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
        System.out.println("MAC: " + actual);

        Pattern regex = Pattern.compile("[0-9a-fA-F:]{12}");
        Matcher match = regex.matcher(actual);

        assertEquals("getPrimaryMacAddress() length", 17, actual.length());
        assertTrue("getPrimaryMacAddress() is string with colons", match.find());
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
                "Linux" 									// Ubuntu
        };

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
    public void testGetOsVersion() {
        String osVersion = System.getProperty("os.version");
        StringBuilder sbOsVersion = new StringBuilder();
        sbOsVersion.append(osVersion);

        BufferedReader in = null;
        File linuxKernelVersion = null;
        FileReader fr = null;
        try {
            linuxKernelVersion = new File("/proc/sys/kernel/version");
            if (linuxKernelVersion.exists()) {
                StringBuilder kernelVersionData = new StringBuilder();
                fr = new FileReader(linuxKernelVersion);
                in = new BufferedReader(fr);
                String tempLine = null;
                while ((tempLine = in.readLine()) != null) {
                    kernelVersionData.append(" ");
                    kernelVersionData.append(tempLine);
                }
                sbOsVersion.append(kernelVersionData.toString());
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
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
    public void getModelName() {
        assertNotNull(systemService.getModelName());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getPartNumber() {
        assertNotNull(systemService.getPartNumber());
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void getSerialNumber() {
        assertNotNull(systemService.getSerialNumber());
    }
}
