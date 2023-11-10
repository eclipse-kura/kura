/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  3 PORT d.o.o.
 ******************************************************************************/
package org.eclipse.kura.deployment.agent.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.system.SystemService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.PortFactory;
import org.mockserver.socket.tls.KeyStoreFactory;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class DeploymentAgentTest {

    private static final String VERSION_1_0_0 = "1.0.0";
    private static final String DP_NAME = "dpName";
    private DeploymentAgent deploymentAgent = new DeploymentAgent();
    private String dpaConfigurationFilepath;
    private DeploymentAgent spiedDeploymentAgent;

    private MarketplacePackageDescriptor resultingPackageDescriptor;

    private SystemService systemServiceMock = mock(SystemService.class);
    private SslManagerService sslManagerServiceMock = mock(SslManagerService.class);
    private Exception occurredException;

    private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
    private static ClientAndServer mockServer;

    @BeforeClass
    public static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(PortFactory.findFreePort());
    }

    @AfterClass
    public static void stopMockServer() {
        mockServer.stop();
    }

    @After
    public void resetMockServer() {
        mockServer.reset();
    }

    @Test
    public void testInstallDeploymentPackageAsyncAlreadyDeploying() throws Exception {
        // test the exception thrown when package is already queued

        DeploymentAgent svc = new DeploymentAgent();

        Set<String> set = new HashSet<>();

        TestUtil.setFieldValue(svc, "instPackageUrls", set);

        String url = "dpUrl";

        set.add(url);

        assertTrue(svc.isInstallingDeploymentPackage(url));

        try {
            svc.installDeploymentPackageAsync(url);
            fail("Exception was expected");
        } catch (Exception e) {
            // OK
        }

        assertEquals(1, set.size());
        assertTrue(set.contains(url));
    }

    @Test
    public void testUninstallDeploymentPackageAsyncAlreadyDeploying() throws Exception {
        // test the exception thrown when package is already queued

        DeploymentAgent svc = new DeploymentAgent();

        Set<String> set = new HashSet<>();

        TestUtil.setFieldValue(svc, "uninstPackageNames", set);

        String name = DP_NAME;

        set.add(name);

        assertTrue(svc.isUninstallingDeploymentPackage(name));

        try {
            svc.uninstallDeploymentPackageAsync(name);
            fail("Exception was expected");
        } catch (Exception e) {
            // OK
        }

        assertEquals(1, set.size());
        assertTrue(set.contains(name));
    }

    @Test(timeout = 1400)
    public void testInstaller() throws Throwable {
        // test the installer method

        DeploymentAgent svc = new DeploymentAgent();

        Set<String> set = new HashSet<>();

        TestUtil.setFieldValue(svc, "instPackageUrls", set);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Object notifier = new Object();
        doAnswer(invocation -> {
            Event event = invocation.getArgument(0, Event.class);

            assertFalse((boolean) event.getProperty("successful"));
            assertEquals("UNKNOWN", event.getProperty("deploymentpackage.name"));

            synchronized (notifier) {
                notifier.notifyAll(); // continue the test
            }
            throw new InterruptedException("test"); // stop the installer thread
        }).when(eaMock).postEvent(any());

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

        Set<String> set = new HashSet<>();

        String name = DP_NAME;
        final Properties deployedPackages = new Properties();
        deployedPackages.put(name, "file:///tmp/nonExistingDp.dp");

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        TestUtil.setFieldValue(svc, "uninstPackageNames", set);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Object notifier = new Object();
        doAnswer(invocation -> {
            Event event = invocation.getArgument(0, Event.class);

            assertTrue((boolean) event.getProperty("successful"));

            synchronized (notifier) {
                notifier.notifyAll(); // continue the test
            }
            throw new InterruptedException("test"); // stop the installer thread
        }).when(eaMock).postEvent(any());

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
            Event event = invocation.getArgument(0, Event.class);

            assertTrue((boolean) event.getProperty("successful"));

            invoked.set(true);

            return null;
        }).when(eaMock).postEvent(any());

        DeploymentPackage dp = mock(DeploymentPackage.class);
        when(dp.getName()).thenReturn(DP_NAME);
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
        when(dp.getName()).thenReturn(DP_NAME);
        when(dp.getVersion()).thenReturn(new Version(VERSION_1_0_0));

        DeploymentAgent svc = new DeploymentAgent();

        DeploymentAdmin daMock = mock(DeploymentAdmin.class);
        svc.setDeploymentAdmin(daMock);
        when(daMock.installDeploymentPackage(any())).thenReturn(dp);

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String packagesPath = "target";
        TestUtil.setFieldValue(svc, "packagesPath", packagesPath);

        String url = "file:///tmp/testdp.dp";

        File f = new File("/tmp/testdp.dp");
        f.createNewFile();
        f.deleteOnExit();

        TestUtil.invokePrivate(svc, "installDeploymentPackageInternal", url);

        verify(daMock, times(1)).installDeploymentPackage(any());

        FileReader reader = new FileReader(dpaConfPath);
        char[] buf = new char[200];
        int read = reader.read(buf);
        reader.close();

        assertTrue(read > 0);
        String str = new String(buf);

        assertTrue("DP file location should have been added to config file",
                str.contains("file\\:target/" + DP_NAME + "_" + VERSION_1_0_0 + ".dp")
                        || str.contains("file\\:target\\\\" + DP_NAME + "_" + VERSION_1_0_0 + ".dp"));

        assertTrue("DP file should have been copied",
                new File("target/" + DP_NAME + "_" + VERSION_1_0_0 + ".dp").exists());
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
        verify(deployedPackages, times(0)).store((FileOutputStream) any(), any());
    }

    @Test
    public void testAddPackageToConfFileStoreException() throws Throwable {
        // test adding packages to configuration file, but fail doing so

        final Properties deployedPackages = spy(new Properties());

        when(deployedPackages.entrySet()).thenCallRealMethod();
        doCallRealMethod().when(deployedPackages).setProperty(any(), any());

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        doThrow(new IOException("test")).when(deployedPackages).store((FileOutputStream) any(), any());

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String dpName = "testdp";
        String url = "file:///tmp/testdp.dp";

        TestUtil.invokePrivate(svc, "addPackageToConfFile", dpName, url);

        verify(deployedPackages, times(1)).setProperty(dpName, url);
        verify(deployedPackages, times(1)).store((FileOutputStream) any(), any());
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
        verify(deployedPackages, times(0)).store((FileOutputStream) any(), any());
    }

    @Test
    public void testRemovePackageToConfFileStoreException() throws Throwable {
        // test removing packages from configuration file, but fail doing so

        final Properties props = new Properties();
        props.put("testdp", "file:///tmp/testdp.dp");

        final Properties deployedPackages = spy(props);
        when(deployedPackages.entrySet()).thenCallRealMethod();

        DeploymentAgent svc = new DeploymentAgent() {

            @Override
            protected Properties readDeployedPackages() {
                return deployedPackages;
            }
        };

        doThrow(new IOException("test")).when(deployedPackages).store((FileOutputStream) any(), any());

        String dpaConfPath = "target/dpa.properties";
        TestUtil.setFieldValue(svc, "dpaConfPath", dpaConfPath);

        String dpName = "testdp";

        TestUtil.invokePrivate(svc, "removePackageFromConfFile", dpName);

        verify(deployedPackages, times(1)).remove(dpName);
        verify(deployedPackages, times(1)).store((FileOutputStream) any(), any());
    }

    @Test
    public void testRemovePackageToConfFile() throws Throwable {
        // test removing packages from configuration file

        final Properties props = new Properties();
        props.put("testdp", "file:///tmp/testdp.dp");

        final Properties deployedPackages = spy(props);
        when(deployedPackages.entrySet()).thenCallRealMethod();

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
        verify(deployedPackages, times(1)).store((FileOutputStream) any(), any());
    }

    @Test
    public void shouldNotInstallPackageWithURLInConfFile() throws Exception {
        givenDeploymentAgent();
        givenConfigurationFile("target/dpa.properties");
        givenDpWithUrlScheme("dp-with-url", "http://fake-url/dp-with-url.dp\n");

        whenActivate();

        thenNoPackageInstalled();
    }

    @Test
    public void getMarketplacePackageDescriptorShouldWorkWithCompatible() {
        givenDeploymentAgent();
        givenDeploymentAgentUsingSSLContectDefinedByMockServer();
        givenSystemServiceReturnsCurrentKuraVersion("5.4.0");

        givenAMockServerThatReturns("54435", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<marketplace>\n"
                + "  <node id=\"5514714\" name=\"AI Wire Component for Eclipse Kura 5\" url=\"https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5\">\n"
                + "    <type>iot_package</type>\n" + "    <owner>Matteo Maiero</owner>\n"
                + "    <favorited>0</favorited>\n" + "    <installstotal>0</installstotal>\n"
                + "    <installsrecent>0</installsrecent>\n" + "    <shortdescription><![CDATA[]]></shortdescription>\n"
                + "    <body><![CDATA[<p><strong>OFFICIAL ADD-ON for Eclipse Kura</strong>&nbsp; - This wire component enables Eclipse Kura to interact with an Inference Engine to perform machine learning-related tasks.</p>\n"
                + "\n"
                + "<p>This package is an official add-on provided and maintained by the Eclipse Kura Development Team</p>\n"
                + "\n"
                + "<p>To install the package, simply drag and drop the Eclipse Marketplace link into the ESF/Kura Packages section of the Web UI.</p>\n"
                + "\n" + "<p><strong>Compatibility</strong></p>\n" + "\n"
                + "<p>The bundle requires Eclipse Kura 5.1.0+.</p>\n" + "]]></body>\n"
                + "    <created>1648566806</created>\n" + "    <changed>1685628355</changed>\n"
                + "    <foundationmember>1</foundationmember>\n" + "    <homepageurl></homepageurl>\n"
                + "    <image><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/badge_logo/public/iot-package/logo/Kura_logo_2_44.png?itok=gr-2SSey]]></image>\n"
                + "    <screenshot><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/medium/public/iot-package/screenshot/kura_marketplace_drag_drop_60.png?itok=pitMd0Qe]]></screenshot>\n"
                + "    <license>EPL 2.0</license>\n" + "    <companyname><![CDATA[Eurotech]]></companyname>\n"
                + "    <status>Production/Stable</status>\n" + "    <supporturl><![CDATA[]]></supporturl>\n"
                + "    <version>1.2.0</version>\n" + "    <min_java_version>java_8</min_java_version>\n"
                + "    <updateurl>https://download.eclipse.org/kura/releases/5.3.0/org.eclipse.kura.wire.ai.component.provider-1.2.0.dp</updateurl>\n"
                + "    <packagetypes>wire_component</packagetypes>\n"
                + "    <sourceurl>https://github.com/eclipse/kura/tree/KURA_5.3.0_RELEASE/kura/org.eclipse.kura.wire.ai.component.provider</sourceurl>\n"
                + "    <versioncompatibility>\n" + "      <from>5.1.0</from>\n" + "      <to></to>\n"
                + "    </versioncompatibility>\n" + "    <environmentrequirements/>\n" + "  </node>\n"
                + "</marketplace>");

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://localhost:" + mockServer.getLocalPort() + "/node/54435/api/p");

        thenNoExceptionOccurred();
        thenDescriptorIsEqualTo(MarketplacePackageDescriptor.builder().nodeId("5514714")
                .url("https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5")
                .dpUrl("https://download.eclipse.org/kura/releases/5.3.0/org.eclipse.kura.wire.ai.component.provider-1.2.0.dp")
                .minKuraVersion("5.1.0").maxKuraVersion("").currentKuraVersion("5.4.0").isCompatible(true).build());
    }

    @Test
    public void getMarketplacePackageDescriptorShouldWorkWithNotCompatible() {
        givenDeploymentAgent();
        givenDeploymentAgentUsingSSLContectDefinedByMockServer();
        givenSystemServiceReturnsCurrentKuraVersion("5.0.0");
        givenAMockServerThatReturns("54435", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<marketplace>\n"
                + "  <node id=\"5514714\" name=\"AI Wire Component for Eclipse Kura 5\" url=\"https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5\">\n"
                + "    <type>iot_package</type>\n" + "    <owner>Matteo Maiero</owner>\n"
                + "    <favorited>0</favorited>\n" + "    <installstotal>0</installstotal>\n"
                + "    <installsrecent>0</installsrecent>\n" + "    <shortdescription><![CDATA[]]></shortdescription>\n"
                + "    <body><![CDATA[<p><strong>OFFICIAL ADD-ON for Eclipse Kura</strong>&nbsp; - This wire component enables Eclipse Kura to interact with an Inference Engine to perform machine learning-related tasks.</p>\n"
                + "\n"
                + "<p>This package is an official add-on provided and maintained by the Eclipse Kura Development Team</p>\n"
                + "\n"
                + "<p>To install the package, simply drag and drop the Eclipse Marketplace link into the ESF/Kura Packages section of the Web UI.</p>\n"
                + "\n" + "<p><strong>Compatibility</strong></p>\n" + "\n"
                + "<p>The bundle requires Eclipse Kura 5.1.0+.</p>\n" + "]]></body>\n"
                + "    <created>1648566806</created>\n" + "    <changed>1685628355</changed>\n"
                + "    <foundationmember>1</foundationmember>\n" + "    <homepageurl></homepageurl>\n"
                + "    <image><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/badge_logo/public/iot-package/logo/Kura_logo_2_44.png?itok=gr-2SSey]]></image>\n"
                + "    <screenshot><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/medium/public/iot-package/screenshot/kura_marketplace_drag_drop_60.png?itok=pitMd0Qe]]></screenshot>\n"
                + "    <license>EPL 2.0</license>\n" + "    <companyname><![CDATA[Eurotech]]></companyname>\n"
                + "    <status>Production/Stable</status>\n" + "    <supporturl><![CDATA[]]></supporturl>\n"
                + "    <version>1.2.0</version>\n" + "    <min_java_version>java_8</min_java_version>\n"
                + "    <updateurl>https://download.eclipse.org/kura/releases/5.3.0/org.eclipse.kura.wire.ai.component.provider-1.2.0.dp</updateurl>\n"
                + "    <packagetypes>wire_component</packagetypes>\n"
                + "    <sourceurl>https://github.com/eclipse/kura/tree/KURA_5.3.0_RELEASE/kura/org.eclipse.kura.wire.ai.component.provider</sourceurl>\n"
                + "    <versioncompatibility>\n" + "      <from>5.1.0</from>\n" + "      <to></to>\n"
                + "    </versioncompatibility>\n" + "    <environmentrequirements/>\n" + "  </node>\n"
                + "</marketplace>");

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://localhost:" + mockServer.getLocalPort() + "/node/54435/api/p");

        thenNoExceptionOccurred();
        thenDescriptorIsEqualTo(MarketplacePackageDescriptor.builder().nodeId("5514714")
                .url("https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5")
                .dpUrl("https://download.eclipse.org/kura/releases/5.3.0/org.eclipse.kura.wire.ai.component.provider-1.2.0.dp")
                .minKuraVersion("5.1.0").maxKuraVersion("").currentKuraVersion("5.0.0").isCompatible(false).build());
    }

    @Test
    public void getMarketplacePackageDescriptorShouldThrowWithNullNode() {
        givenDeploymentAgent();
        givenDeploymentAgentUsingSSLContectDefinedByMockServer();
        givenSystemServiceReturnsCurrentKuraVersion("5.0.0");
        givenAMockServerThatReturns("54435", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<marketplace>\n"
                + "  <node id=\"5514714\" name=\"AI Wire Component for Eclipse Kura 5\" url=\"https://marketplace.eclipse.org/content/ai-wire-component-eclipse-kura-5\">\n"
                + "    <type>iot_package</type>\n" + "    <owner>Matteo Maiero</owner>\n"
                + "    <favorited>0</favorited>\n" + "    <installstotal>0</installstotal>\n"
                + "    <installsrecent>0</installsrecent>\n" + "    <shortdescription><![CDATA[]]></shortdescription>\n"
                + "    <body><![CDATA[<p><strong>OFFICIAL ADD-ON for Eclipse Kura</strong>&nbsp; - This wire component enables Eclipse Kura to interact with an Inference Engine to perform machine learning-related tasks.</p>\n"
                + "\n"
                + "<p>This package is an official add-on provided and maintained by the Eclipse Kura Development Team</p>\n"
                + "\n"
                + "<p>To install the package, simply drag and drop the Eclipse Marketplace link into the ESF/Kura Packages section of the Web UI.</p>\n"
                + "\n" + "<p><strong>Compatibility</strong></p>\n" + "\n"
                + "<p>The bundle requires Eclipse Kura 5.1.0+.</p>\n" + "]]></body>\n"
                + "    <created>1648566806</created>\n" + "    <changed>1685628355</changed>\n"
                + "    <foundationmember>1</foundationmember>\n" + "    <homepageurl></homepageurl>\n"
                + "    <image><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/badge_logo/public/iot-package/logo/Kura_logo_2_44.png?itok=gr-2SSey]]></image>\n"
                + "    <screenshot><![CDATA[https://marketplace.eclipse.org/sites/default/files/styles/medium/public/iot-package/screenshot/kura_marketplace_drag_drop_60.png?itok=pitMd0Qe]]></screenshot>\n"
                + "    <license>EPL 2.0</license>\n" + "    <companyname><![CDATA[Eurotech]]></companyname>\n"
                + "    <status>Production/Stable</status>\n" + "    <supporturl><![CDATA[]]></supporturl>\n"
                + "    <version>1.2.0</version>\n" + "    <min_java_version>java_8</min_java_version>\n"
                + "    <packagetypes>wire_component</packagetypes>\n"
                + "    <sourceurl>https://github.com/eclipse/kura/tree/KURA_5.3.0_RELEASE/kura/org.eclipse.kura.wire.ai.component.provider</sourceurl>\n"
                + "    <versioncompatibility>\n" + "      <from>5.1.0</from>\n" + "      <to></to>\n"
                + "    </versioncompatibility>\n" + "    <environmentrequirements/>\n" + "  </node>\n"
                + "</marketplace>");

        whenGetMarketplacePackageDescriptorIsCalledFor(
                "https://localhost:" + mockServer.getLocalPort() + "/node/54435/api/p");

        thenExceptionOccurred(IllegalStateException.class);
    }

    /*
     * GIVEN
     */

    private void givenSystemServiceReturnsCurrentKuraVersion(String returnedVersion) {
        when(this.systemServiceMock.getKuraMarketplaceCompatibilityVersion()).thenReturn(returnedVersion);
    }

    private void givenAMockServerThatReturns(String nodeId, String responseXML) {
        String url = String.format("/node/%s/api/p", nodeId);
        mockServer.withSecure(true).when(request().withMethod("GET").withPath(url))
                .respond(response().withBody(responseXML));
    }

    private void givenDpWithUrlScheme(String dpName, String dpUrl) throws IOException {
        FileWriter writer = new FileWriter(dpaConfigurationFilepath);
        writer.write(String.join("=", dpName, dpUrl) + "\n");
        writer.close();
    }

    private void givenDeploymentAgent() {
        Properties properties = new Properties();
        properties.put("kura.packages", "fake-packages-path");
        when(systemServiceMock.getProperties()).thenReturn(properties);
        this.deploymentAgent.setSystemService(systemServiceMock);
        this.spiedDeploymentAgent = spy(this.deploymentAgent);
    }

    private void givenDeploymentAgentUsingSSLContectDefinedByMockServer() {
        try {
            when(sslManagerServiceMock.getSSLSocketFactory())
                    .thenReturn(new KeyStoreFactory(new MockServerLogger()).sslContext().getSocketFactory());
        } catch (GeneralSecurityException | IOException e) {
            fail();
        }

        this.deploymentAgent.setSslManagerService(sslManagerServiceMock);

    }

    private void givenConfigurationFile(String dpaConfigurationFilepath) throws NoSuchFieldException {
        this.dpaConfigurationFilepath = dpaConfigurationFilepath;
        System.setProperty(DPA_CONF_PATH_PROPNAME, dpaConfigurationFilepath);
    }

    /*
     * WHEN
     */

    private void whenActivate() {
        this.deploymentAgent.activate();
    }

    private void whenGetMarketplacePackageDescriptorIsCalledFor(String url) {
        try {
            this.resultingPackageDescriptor = this.deploymentAgent.getMarketplacePackageDescriptor(url);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * THEN
     */

    private void thenNoPackageInstalled() throws Exception {
        verify(this.spiedDeploymentAgent, times(0)).installDeploymentPackageAsync(anyString());
    }

    private void thenDescriptorIsEqualTo(MarketplacePackageDescriptor expectedDescriptor) {
        assertEquals(expectedDescriptor, this.resultingPackageDescriptor);
    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

}
