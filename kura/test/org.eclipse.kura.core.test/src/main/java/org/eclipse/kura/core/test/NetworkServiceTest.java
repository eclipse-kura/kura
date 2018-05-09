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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class NetworkServiceTest extends TestCase {

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);	// initialize with number of dependencies
    private static final String MAC_DELIM = ":";

    private static NetworkService networkService;

    @Override
    @BeforeClass
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    public void setNetworkService(NetworkService networkService) {
        NetworkServiceTest.networkService = networkService;
        dependencyLatch.countDown();
    }

    @Test
    public void testDummy() {
        assertTrue(true);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(NetworkServiceTest.networkService);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testInterfaceNamesList() throws Exception {
        List<String> interfaces = networkService.getAllNetworkInterfaceNames();
        System.out.println("network interface names: " + interfaces);
        assertNotNull(interfaces);
        assertTrue(interfaces.size() > 0);
        for (String interfaceName : interfaces) {
            assertFalse(interfaceName.isEmpty());
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testAllInterfaces() throws Exception {
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = networkService.getNetworkInterfaces();
        assertNotNull(interfaces);
        System.out.println("number of interfaces: " + interfaces.size());
        assertTrue(interfaces.size() > 0);

        for (NetInterface<? extends NetInterfaceAddress> ni : interfaces) {
            System.out.println("network interface name: " + ni.getName());
            System.out.println("network interface type: " + ni.getType());
            System.out.println("network interface mac: " + getMacFromHwAddress(ni.getHardwareAddress()));
            System.out.println("network interface state: " + ni.getState());

            assertNotNull(ni.getName());
            assertFalse(ni.getName().isEmpty());
            assertNotNull(ni.getType());
            assertNotNull(ni.getState());
            assertNotNull(ni.getNetInterfaceAddresses());

            for (NetInterfaceAddress ia : ni.getNetInterfaceAddresses()) {
                if (ia.getAddress() != null) {
                    System.out.println("   network interface address: " + ia.getAddress().getHostAddress());
                }
            }
        }
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testActiveInterfaces() throws Exception {
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = networkService.getActiveNetworkInterfaces();
        assertNotNull(interfaces);
        System.out.println("number of active interfaces: " + interfaces.size());
        assertTrue(interfaces.size() > 0);

        for (NetInterface<? extends NetInterfaceAddress> ni : interfaces) {
            System.out.println("network interface name: " + ni.getName());
            System.out.println("network interface type: " + ni.getType());
            System.out.println("network interface mac: " + getMacFromHwAddress(ni.getHardwareAddress()));
            System.out.println("network interface state: " + ni.getState());

            assertNotNull(ni.getName());
            assertFalse(ni.getName().isEmpty());
            assertNotNull(ni.getType());
            assertNotNull(ni.getState());
            assertNotNull(ni.getNetInterfaceAddresses());

            for (NetInterfaceAddress ia : ni.getNetInterfaceAddresses()) {
                if (ia.getAddress() != null) {
                    System.out.println("   network interface address: " + ia.getAddress().getHostAddress());
                }
            }
        }
    }

    private String getMacFromHwAddress(byte[] hwAddr) {
        StringBuilder sb = new StringBuilder();
        if (hwAddr != null) {
            for (int i = 0; i < hwAddr.length; i++) {
                if (i > 0) {
                    sb.append(MAC_DELIM);
                }
                sb.append(String.format("%1X", hwAddr[i]));
            }
        }
        return sb.toString();
    }
}
