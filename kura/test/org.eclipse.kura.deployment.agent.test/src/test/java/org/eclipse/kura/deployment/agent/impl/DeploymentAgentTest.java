/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.deployment.agent.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class DeploymentAgentTest {

    @Test
    public void testInstallDeploymentPackageAsyncAlreadyDeploying() throws Exception {
        // test the exception thrown when package is already queued

        DeploymentAgent svc = new DeploymentAgent();

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TestUtil.setFieldValue(svc, "instPackageUrls", queue);

        String url = "dpUrl";

        queue.offer(url);

        assertTrue(svc.isInstallingDeploymentPackage(url));

        try {
            svc.installDeploymentPackageAsync(url);
            fail("Exception was expected");
        } catch (Exception e) {
            // OK
        }

        assertEquals(1, queue.size());
        assertTrue(queue.contains(url));
    }

    @Test(timeout = 400)
    public void testInstallDeploymentPackageAsync() throws Exception {
        // test asynch deployment queue

        DeploymentAgent svc = new DeploymentAgent();

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TestUtil.setFieldValue(svc, "instPackageUrls", queue);

        String url = "dpUrl";

        assertFalse(svc.isInstallingDeploymentPackage(url));

        new Thread(() -> {
            try {
                svc.installDeploymentPackageAsync(url);
            } catch (Exception e) {
            }
        }).start();

        synchronized (queue) {
            queue.wait(500);
        }

        assertEquals(1, queue.size());
        assertTrue(queue.contains(url));
    }

    @Test
    public void testUninstallDeploymentPackageAsyncAlreadyDeploying() throws Exception {
        // test the exception thrown when package is already queued

        DeploymentAgent svc = new DeploymentAgent();

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TestUtil.setFieldValue(svc, "uninstPackageNames", queue);

        String name = "dpName";

        queue.offer(name);

        assertTrue(svc.isUninstallingDeploymentPackage(name));

        try {
            svc.uninstallDeploymentPackageAsync(name);
            fail("Exception was expected");
        } catch (Exception e) {
            // OK
        }

        assertEquals(1, queue.size());
        assertTrue(queue.contains(name));
    }

    @Test(timeout = 400)
    public void testUninstallDeploymentPackageAsync() throws Exception {
        // test asynch undeployment queue

        DeploymentAgent svc = new DeploymentAgent();

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TestUtil.setFieldValue(svc, "uninstPackageNames", queue);

        String name = "dpName";

        assertFalse(svc.isUninstallingDeploymentPackage(name));

        new Thread(() -> {
            try {
                svc.uninstallDeploymentPackageAsync(name);
            } catch (Exception e) {
            }
        }).start();

        synchronized (queue) {
            queue.wait(500);
        }

        assertEquals(1, queue.size());
        assertTrue(queue.contains(name));
    }

    @Test(timeout = 1400)
    public void testInstaller() throws Throwable {
        // test the installer method

        DeploymentAgent svc = new DeploymentAgent();

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TestUtil.setFieldValue(svc, "instPackageUrls", queue);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Object notifier = new Object();
        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertFalse((boolean) event.getProperty("successful"));
            assertEquals("UNKNOWN", event.getProperty("deploymentpackage.name"));

            synchronized (notifier) {
                notifier.notifyAll(); // continue the test
            }
            throw new InterruptedException("test"); // stop the installer thread
        }).when(eaMock).postEvent(anyObject());

        Thread t = new Thread(() -> {
            try {
                TestUtil.invokePrivate(svc, "installer");
            } catch (Throwable e) {
            }
        });
        t.start();

        Thread.sleep(5); // just wait a bit so that installer can wait for queue to fill

        String url = "myUrl"; // intentionally invalid protocol
        svc.installDeploymentPackageAsync(url);

        synchronized (notifier) {
            notifier.wait(1500); // not timing out means that the expected event was sent
        }
    }

    @Test(timeout = 2400)
    public void testUninstaller() throws Throwable {
        // test the uninstaller method

        Queue<String> queue = new ConcurrentLinkedQueue<>();

        String name = "dpName";
        final Properties deployedPackages = new Properties();
        deployedPackages.put(name, "file:///tmp/nonExistingDp.dp");

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        TestUtil.setFieldValue(svc, "uninstPackageNames", queue);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Object notifier = new Object();
        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertTrue((boolean) event.getProperty("successful"));

            synchronized (notifier) {
                notifier.notifyAll(); // continue the test
            }
            throw new InterruptedException("test"); // stop the installer thread
        }).when(eaMock).postEvent(anyObject());

        DeploymentAdmin daMock = mock(DeploymentAdmin.class);
        svc.setDeploymentAdmin(daMock);

        DeploymentPackage dp = mock(DeploymentPackage.class);
        when(daMock.getDeploymentPackage(name)).thenReturn(dp);

        Thread t = new Thread(() -> {
            try {
                TestUtil.invokePrivate(svc, "uninstaller");
            } catch (Throwable e) {
            }
        });
        t.start();

        Thread.sleep(5); // just wait a bit so that uninstaller can wait for queue to fill

        svc.uninstallDeploymentPackageAsync(name);

        synchronized (notifier) {
            notifier.wait(2500); // not timing out means that the expected event was sent
        }
    }

    @Test
    public void testPostInstalledEvent() throws Throwable {
        // test the post-installation event

        DeploymentAgent svc = new DeploymentAgent();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        AtomicBoolean invoked = new AtomicBoolean(false);
        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertTrue((boolean) event.getProperty("successful"));

            invoked.set(true);

            return null;
        }).when(eaMock).postEvent(anyObject());

        DeploymentPackage dp = mock(DeploymentPackage.class);
        when(dp.getName()).thenReturn("dpName");
        when(dp.getVersion()).thenReturn(new Version(1, 1, 0));

        TestUtil.invokePrivate(svc, "postInstalledEvent", dp, "dpUrl", true, null);

        assertTrue(invoked.get());
    }

    @Test
    public void testInstallFromConfigExc() throws Throwable {
        // test installation of packages stored in the configuration file - installation exception

        AtomicInteger invoked = new AtomicInteger(0);
        Set<String> paths = new HashSet<>();

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            public void installDeploymentPackageAsync(String url) throws Exception {
                paths.add(url);
                invoked.incrementAndGet();

                throw new Exception("test");
            }
        };

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        FileWriter writer = new FileWriter(dpaConfPath);
        writer.write("dp1=file:/tmp/testdp1.dp\ndp2=file:/tmp/testdp2.dp\n");
        writer.close();

        TestUtil.invokePrivate(svc, "installPackagesFromConfFile");

        assertEquals(2, invoked.get());
        assertTrue(paths.contains("file:/tmp/testdp1.dp"));
        assertTrue(paths.contains("file:/tmp/testdp2.dp"));
    }

    @Test
    public void testInstallFromConfig() throws Throwable {
        // test installation of packages stored in the configuration file

        AtomicInteger invoked = new AtomicInteger(0);
        Set<String> paths = new HashSet<>();

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            public void installDeploymentPackageAsync(String url) throws Exception {
                paths.add(url);
                invoked.incrementAndGet();
            }
        };

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        FileWriter writer = new FileWriter(dpaConfPath);
        writer.write("dp1=file:/tmp/testdp1.dp\ndp2=file:/tmp/testdp2.dp\n");
        writer.close();

        TestUtil.invokePrivate(svc, "installPackagesFromConfFile");

        assertEquals(2, invoked.get());
        assertTrue(paths.contains("file:/tmp/testdp1.dp"));
        assertTrue(paths.contains("file:/tmp/testdp2.dp"));
    }

    @Test
    public void testInstallDeploymentPackageInternal() throws Throwable {
        // test installation of packages stored in the configuration file

        DeploymentPackage dp = mock(DeploymentPackage.class);
        when(dp.getName()).thenReturn("dpName");

        DeploymentAgent svc = new DeploymentAgent();

        DeploymentAdmin daMock = mock(DeploymentAdmin.class);
        svc.setDeploymentAdmin(daMock);
        when(daMock.installDeploymentPackage(anyObject())).thenReturn(dp);

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String packagesPath = "target";
        TestUtil.setFieldValue(svc, "packagesPath", packagesPath);

        String url = "file:///tmp/testdp.dp";

        File f = new File("/tmp/testdp.dp");
        f.createNewFile();
        f.deleteOnExit();

        TestUtil.invokePrivate(svc, "installDeploymentPackageInternal", url);

        verify(daMock, times(1)).installDeploymentPackage(anyObject());

        FileReader reader = new FileReader(dpaConfPath);
        char[] buf = new char[200];
        int read = reader.read(buf);
        reader.close();

        assertTrue(read > 0);
        String str = new String(buf);

        assertTrue("DP file location should have been added to config file",
                str.contains("file\\:target/testdp.dp") || str.contains("file\\:target\\\\testdp.dp"));

        assertTrue("DP file should have been copied", new File("target/testdp.dp").exists());
    }

    @Test
    public void testAddPackageToConfFileNoConfigFile() throws Throwable {
        // test adding packages to configuration file that is not set

        final Properties deployedPackages = mock(Properties.class);
        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        String dpName = "testdp";
        String url = "file:///tmp/testdp.dp";

        TestUtil.invokePrivate(svc, "addPackageToConfFile", dpName, url);

        verify(deployedPackages, times(1)).setProperty(dpName, url);
        verify(deployedPackages, times(0)).store((FileOutputStream) anyObject(), anyObject());
    }

    @Test
    public void testAddPackageToConfFileStoreException() throws Throwable {
        // test adding packages to configuration file, but fail doing so

        final Properties deployedPackages = mock(Properties.class);
        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        doThrow(new IOException("test")).when(deployedPackages).store((FileOutputStream) anyObject(), anyObject());

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String dpName = "testdp";
        String url = "file:///tmp/testdp.dp";

        TestUtil.invokePrivate(svc, "addPackageToConfFile", dpName, url);

        verify(deployedPackages, times(1)).setProperty(dpName, url);
        verify(deployedPackages, times(1)).store((FileOutputStream) anyObject(), anyObject());
    }

    @Test
    public void testRemovePackageToConfFileNoConfigFile() throws Throwable {
        // test removing packages from configuration file that is not set

        Properties deployedPackages = mock(Properties.class);

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        String dpName = "testdp";

        TestUtil.invokePrivate(svc, "removePackageFromConfFile", dpName);

        verify(deployedPackages, times(1)).remove(dpName);
        verify(deployedPackages, times(0)).store((FileOutputStream) anyObject(), anyObject());
    }

    @Test
    public void testRemovePackageToConfFileStoreException() throws Throwable {
        // test removing packages from configuration file, but fail doing so

        final Properties deployedPackages = mock(Properties.class);
        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        doThrow(new IOException("test")).when(deployedPackages).store((FileOutputStream) anyObject(), anyObject());

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String dpName = "testdp";

        TestUtil.invokePrivate(svc, "removePackageFromConfFile", dpName);

        verify(deployedPackages, times(1)).remove(dpName);
        verify(deployedPackages, times(1)).store((FileOutputStream) anyObject(), anyObject());
    }

    @Test
    public void testRemovePackageToConfFile() throws Throwable {
        // test removing packages from configuration file

        final Properties deployedPackages = mock(Properties.class);
        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String dpName = "testdp";

        TestUtil.invokePrivate(svc, "removePackageFromConfFile", dpName);

        verify(deployedPackages, times(1)).remove(dpName);
        verify(deployedPackages, times(1)).store((FileOutputStream) anyObject(), anyObject());
    }

}
