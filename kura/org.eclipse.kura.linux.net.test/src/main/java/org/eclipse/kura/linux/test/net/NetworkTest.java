/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.linux.test.net;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.test.annotation.TestTarget;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import junit.framework.TestCase;

public class NetworkTest extends TestCase implements EventHandler {

    private static CountDownLatch dependencyLatch = new CountDownLatch(3);	// initialize with number of dependencies
    private static NetworkService s_networkService;
    private static SystemService s_systemService;
    private static EventAdmin s_eventAdmin;

    // private static final String PLATFORM_UNKNOWN = "unknown";

    /**
     * This is the profile that runs on cloudbees. Most tests are skipped
     */
    // private static final String PLATFORM_EMULATED = "emulated";

    /**
     * This is for an Ubuntu laptop with Network Manager. The hardware profile assumes:
     * LAN1 - onboard ethernet controller intiially disabled
     * WIFI1 - onboard Wifi device initially acting as the WAN interface via DHCP
     * LAN2 - USB/Ethernet controller initially disabled
     * WIFI2 - USB/Wifi devices initially disabled
     * * Ethernet cable connects LAN1 to LAN2
     */
    // private static String platform;

    @Override
    @BeforeClass
    public void setUp() {
        // Wait for OSGi dependencies
        try {
            dependencyLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("OSGi dependencies unfulfilled");
        }

        FrameworkUtil.getBundle(this.getClass()).getBundleContext();

        // install event listener for network events
        Dictionary<String, String[]> eventProps = new Hashtable<String, String[]>();
        String[] topic = { UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
                UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC };
        eventProps.put(EventConstants.EVENT_TOPIC, topic);
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(EventHandler.class.getName(), this, eventProps);
    }

    public void setNetworkService(NetworkService networkService) {
        NetworkTest.s_networkService = networkService;
        dependencyLatch.countDown();
    }

    public void setSystemService(SystemService systemService) {
        NetworkTest.s_systemService = systemService;
        dependencyLatch.countDown();
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        NetworkTest.s_eventAdmin = eventAdmin;
        dependencyLatch.countDown();
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testServiceExists() {
        assertNotNull(NetworkTest.s_networkService);
    }

    @Override
    public void handleEvent(Event event) {
        System.err.println("NetworkTest - GOT an EVENT: " + event.getTopic() + " " + event.toString());
    }
}
