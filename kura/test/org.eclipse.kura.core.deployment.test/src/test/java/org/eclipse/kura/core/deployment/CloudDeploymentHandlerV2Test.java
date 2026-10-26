/*******************************************************************************
 * Copyright (c) 2018, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.deployment;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.download.impl.DownloadImpl;
import org.eclipse.kura.core.deployment.download.impl.HttpDownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.impl.KuraNotifyPayload;
import org.eclipse.kura.core.deployment.hook.DeploymentHookManager;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.install.InstallImpl;
import org.eclipse.kura.core.deployment.install.KuraInstallPayload;
import org.eclipse.kura.core.deployment.uninstall.UninstallImpl;
import org.eclipse.kura.core.deployment.util.FileUtilities;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.eclipse.kura.deployment.hook.RequestContext;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.system.SystemService;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public class CloudDeploymentHandlerV2Test {

    private static final String COMPONENT_OPTIONS_FIELD = "componentOptions";
    private static final String TEST_XML = "testXml";
    private static final String DOWNLOADED_FILE_NAME = "limited_1.0.0.dp";

    private Properties systemProperties = new Properties();
    private byte[] servedPackage;
    private boolean sendContentLength;
    private Path downloadsDirectory;

    @Test(expected = NullPointerException.class)
    public void testActivateNoDeploymentHookManagerException() throws NoSuchFieldException, KuraException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        ComponentContext componentContext = mock(ComponentContext.class);

        handler.activate(componentContext, new HashMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void testActivateNoSystemServiceException() throws NoSuchFieldException, KuraException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);

        handler.activate(componentContext, new HashMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void testActivateNullSystemServicePropertiesException() throws NoSuchFieldException, KuraException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        SystemService systemService = mock(SystemService.class);

        handler.setSystemService(systemService);
        when(systemService.getProperties()).thenReturn(null);

        handler.activate(componentContext, new HashMap<>());

    }

    @Test(expected = ComponentException.class)
    @Ignore
    public void testActivateMissingDpaPathException()
            throws NoSuchFieldException, KuraException, InvalidSyntaxException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        SystemService systemService = mock(SystemService.class);

        handler.setSystemService(systemService);
        when(systemService.getProperties()).thenReturn(new Properties());

        BundleContext bundleContext = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleContext.createFilter(anyString())).thenReturn(filter);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        handler.activate(componentContext, new HashMap<>());

    }

    @Test(expected = ComponentException.class)
    @Ignore
    public void testActivateMissingPackagesPath() throws NoSuchFieldException, KuraException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        SystemService systemService = mock(SystemService.class);

        handler.setSystemService(systemService);
        when(systemService.getProperties()).thenReturn(new Properties());
        System.setProperty("dpa.configuration", "/opt/eclipse/kura/kura/dpa.properties");

        handler.activate(componentContext, new HashMap<>());
    }

    @Test(expected = ComponentException.class)
    @Ignore
    public void testActivateMissingKuraDataDir() throws NoSuchFieldException, KuraException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        SystemService systemService = mock(SystemService.class);

        Properties systemServiceProps = new Properties();
        systemServiceProps.setProperty("kura.packages", "/opt/eclipse/kura/kura/packages");

        handler.setSystemService(systemService);
        when(systemService.getProperties()).thenReturn(systemServiceProps);
        System.setProperty("dpa.configuration", "/opt/eclipse/kura/kura/dpa.properties");

        handler.activate(componentContext, new HashMap<>());
    }

    @Test
    public void testActivateOk() throws NoSuchFieldException, KuraException, InvalidSyntaxException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        SystemService systemService = mock(SystemService.class);

        Properties systemServiceProps = new Properties();
        systemServiceProps.setProperty("kura.packages", "/opt/eclipse/kura/kura/packages");
        systemServiceProps.setProperty("kura.data", "/opt/eclipse/kura/data");

        handler.setSystemService(systemService);
        when(systemService.getProperties()).thenReturn(systemServiceProps);
        System.setProperty("dpa.configuration", "/opt/eclipse/kura/kura/dpa.properties");

        BundleContext bundleContext = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(bundleContext.createFilter(anyString())).thenReturn(filter);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        handler.activate(componentContext, new HashMap<>());

        assertNotNull(TestUtil.getFieldValue(handler, "installImplementation"));

        handler.deactivate(componentContext);
        assertNull(TestUtil.getFieldValue(handler, "bundleContext"));
    }

    @Test(expected = KuraException.class)
    public void testDoGetNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoGetOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test
    public void testDoGetDownloadNoPendingRequest() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("download");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        TestUtil.setFieldValue(deployment, "pendingPackageUrl", null);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(0, resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE));
        assertEquals(100, resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS));
        assertEquals(DownloadStatus.ALREADY_DONE.getStatusString(),
                resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS));

    }

    @Test
    public void testDoGetDownloadPendingRequest() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2();
        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("download");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        DeploymentPackageDownloadOptions dlOptions = mock(DeploymentPackageDownloadOptions.class);
        TestUtil.setFieldValue(deployment, "downloadOptions", dlOptions);
        TestUtil.setFieldValue(deployment, "pendingPackageUrl", "someValidUrl");

        DownloadImpl dlMock = mock(DownloadImpl.class);
        TestUtil.setFieldValue(deployment, "downloadImplementation", dlMock);

        DownloadCountingOutputStream stream = mock(DownloadCountingOutputStream.class);
        when(dlMock.getDownloadHelper()).thenReturn(stream);

        when(stream.getDownloadTransferStatus()).thenReturn(DownloadStatus.IN_PROGRESS);
        when(stream.getDownloadTransferProgressPercentage()).thenReturn(10L);

        when(dlOptions.getJobId()).thenReturn(1234L);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(0, resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE));
        assertEquals(10, resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS));
        assertEquals(DownloadStatus.IN_PROGRESS.getStatusString(),
                resPayload.getMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS));
        assertEquals(1234L, resPayload.getMetric(KuraNotifyPayload.METRIC_JOB_ID));

    }

    @Test
    public void testDoGetInstallNoPendingRequest() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("install");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        InstallImpl ilMock = new InstallImpl(null, null, null);
        TestUtil.setFieldValue(deployment, "installImplementation", ilMock);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertNotNull(resPayload.getTimestamp());
        assertEquals(InstallStatus.IDLE.getStatusString(),
                resPayload.getMetric(KuraInstallPayload.METRIC_INSTALL_STATUS));
    }

    @Test
    public void testDoGetInstallInProgress() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2();
        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("install");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        String dpName = "heater";
        String dpVersion = "1.0.0";
        DeploymentPackageInstallOptions options = new DeploymentPackageInstallOptions(dpName, dpVersion);
        InstallImpl ilMock = new InstallImpl(null, null, null);
        TestUtil.setFieldValue(deployment, "installImplementation", ilMock);
        TestUtil.setFieldValue(deployment, "isInstalling", true);
        TestUtil.setFieldValue(ilMock, "options", options);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertNotNull(resPayload.getTimestamp());
        assertEquals(InstallStatus.IN_PROGRESS.getStatusString(),
                resPayload.getMetric(KuraInstallPayload.METRIC_INSTALL_STATUS));
    }

    @Test
    public void testDoGetPackagesEmptyList() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(0, packagesArray.length);

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("packages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[0];

        TestUtil.setFieldValue(deployment, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetPackagesOneElementListNoBundleInfos() throws KuraException, NoSuchFieldException {
        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[1];
        DeploymentPackage dp = mock(DeploymentPackage.class);
        deployedPackages[0] = dp;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(1, packagesArray.length);
                assertEquals(dp.getName(), packagesArray[0].getName());
                assertEquals(dp.getVersion().toString(), packagesArray[0].getVersion());

                assertEquals(0, packagesArray[0].getBundleInfos().length);

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("packages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        TestUtil.setFieldValue(deployment, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);
        when(dp.getName()).thenReturn("heater");
        when(dp.getVersion()).thenReturn(new Version("1.0.0"));
        when(dp.getBundleInfos()).thenReturn(new BundleInfo[0]);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetPackagesOneElementList() throws KuraException, NoSuchFieldException {
        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[1];
        DeploymentPackage dp = mock(DeploymentPackage.class);
        deployedPackages[0] = dp;

        BundleInfo[] bundleInfos = new BundleInfo[1];
        BundleInfo bundleInfo = mock(BundleInfo.class);
        bundleInfos[0] = bundleInfo;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(1, packagesArray.length);
                assertEquals(dp.getName(), packagesArray[0].getName());
                assertEquals(dp.getVersion().toString(), packagesArray[0].getVersion());

                SystemBundle[] bis = packagesArray[0].getBundleInfos();
                assertEquals(1, bis.length);
                assertEquals(bundleInfo.getSymbolicName(), bis[0].getName());
                assertEquals(bundleInfo.getVersion().toString(), bis[0].getVersion());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("packages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        TestUtil.setFieldValue(deployment, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);
        when(dp.getName()).thenReturn("heater");
        when(dp.getVersion()).thenReturn(new Version("1.0.0"));
        when(dp.getBundleInfos()).thenReturn(bundleInfos);
        when(bundleInfo.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundleInfo.getVersion()).thenReturn(new Version("1.0.0"));

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesNoBundleInstalled() throws KuraException, NoSuchFieldException {
        String xml = TEST_XML;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(0, bundleArray.length);

                return xml;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(new Bundle[0]);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(xml, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleUninstalled() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("UNINSTALLED", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleInstalled() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("INSTALLED", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.INSTALLED);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleResolved() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("RESOLVED", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleStarting() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("STARTING", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.STARTING);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleStopping() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("STOPPING", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.STOPPING);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleActive() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        CloudDeploymentHandlerV2 deployment = new CloudDeploymentHandlerV2() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("ACTIVE", bundleArray[0].getState());

                return TEST_XML;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(deployment, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);

        KuraMessage resMessage = deployment.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_XML, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test(expected = KuraException.class)
    public void testDoDelNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doDel(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoDelOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doDel(null, message);
    }

    @Test
    public void testDoDelNormal() throws Exception {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DownloadImpl dlMock = mock(DownloadImpl.class);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        DownloadCountingOutputStream stream = mock(DownloadCountingOutputStream.class);
        when(dlMock.getDownloadHelper()).thenReturn(stream);

        when(dlMock.deleteDownloadedFile()).thenReturn(true);

        handler.doDel(null, message);

        verify(dlMock).getDownloadHelper();
        verify(dlMock).deleteDownloadedFile();
        verify(stream).cancelDownload();
    }

    @Test(expected = KuraException.class)
    public void testDoDelException() throws Exception {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DownloadImpl dlMock = mock(DownloadImpl.class);
        String mesg = "test";
        when(dlMock.getDownloadHelper()).thenThrow(new RuntimeException(mesg));
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doDel(null, message);

        verify(dlMock).getDownloadHelper();
    }

    @Test(expected = KuraException.class)
    public void testDoExecNoResources() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecOtherwise() throws KuraException, NoSuchFieldException {
        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallDeploymentOptionsException() throws KuraException, NoSuchFieldException {
        // fail immediately after calling doExecInstall

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallPendingPackage() throws KuraException, NoSuchFieldException {
        // fail at pending test

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        KuraException ex = new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        when(dlMock.isAlreadyDownloaded()).thenThrow(ex);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallNotDownloaded() throws KuraException, NoSuchFieldException {
        // fail because not downloaded

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        when(dlMock.isAlreadyDownloaded()).thenReturn(false);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallDownloadedExceptionInstalling() throws KuraException, NoSuchFieldException {
        // fail without file

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected File getDpDownloadFile(DeploymentPackageInstallOptions options) throws IOException {
                throw new IOException("test");
            }
        };
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        when(dlMock.isAlreadyDownloaded()).thenReturn(true);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        TestUtil.setFieldValue(handler, "isInstalling", false);

        handler.doExec(null, message);
    }

    @Test
    public void testDoExecInstallDownloaded() throws KuraException, NoSuchFieldException, InterruptedException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected File getDpDownloadFile(final DeploymentPackageInstallOptions options) throws IOException {
                String dpName = FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".dp");
                String packageFilename = new StringBuilder().append("/tmp").append(File.separator).append(dpName)
                        .toString();
                return new File(packageFilename);
            }
        };

        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "heater");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        DownloadImpl dlMock = mock(DownloadImpl.class);
        when(dlMock.isAlreadyDownloaded()).thenReturn(true);
        TestUtil.setFieldValue(handler, "downloadImplementation", dlMock);

        TestUtil.setFieldValue(handler, "isInstalling", false);

        InstallImpl installImpl = mock(InstallImpl.class);
        TestUtil.setFieldValue(handler, "installImplementation", installImpl);

        RequestHandlerContext requestContext = new RequestHandlerContext(null, Collections.emptyMap());
        KuraMessage resMessage = handler.doExec(requestContext, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_OK,
                resPayload.getResponseCode());

        Thread.sleep(500);
        assertNull(TestUtil.getFieldValue(handler, "installOptions"));
        assertFalse((boolean) TestUtil.getFieldValue(handler, "isInstalling"));
    }

    @Test(expected = KuraException.class)
    public void testDoExecDownloadOptionsException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecDownload

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test
    public void testDoExecDownloadPendingUrl() throws KuraException, NoSuchFieldException {
        // fail at pending url

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "pendingPackageUrl", "url");

        KuraMessage resMessage = handler.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        TestUtil.setFieldValue(handler, "pendingPackageUrl", null);

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                resPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", resPayload.getTimestamp());
        assertEquals("Body should match", "Another resource is already in download",
                new String(resPayload.getBody(), Charset.forName("UTF-8")));
        assertEquals(DownloadStatus.IN_PROGRESS.getStatusString(),
                resPayload.getMetric(CloudDeploymentHandlerV2.METRIC_DOWNLOAD_STATUS));
    }

    @Test
    public void testDoExecDownloadIsDownloadedException() throws KuraException, NoSuchFieldException {
        // fail with DownloadImpl exception

        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        Exception ex = new KuraException(KuraErrorCode.NOT_CONNECTED);
        when(dlMock.isAlreadyDownloaded()).thenThrow(ex);

        KuraMessage resMessage = handler.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_ERROR,
                resPayload.getResponseCode());
        assertNotNull("Response timestamp should be set", resPayload.getTimestamp());
        assertEquals("Body should match", "Error checking download status",
                new String(resPayload.getBody(), Charset.forName("UTF-8")));

        verify(dlMock).isAlreadyDownloaded();
    }

    @Test(expected = KuraException.class)
    public void testDoExecDownloadDownloaderException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecDownload

        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        when(dlMock.isAlreadyDownloaded()).thenReturn(true);

        doThrow(new RuntimeException("test")).when(dlMock).setSslManager(null);

        handler.doExec(null, message);

        verify(dlMock).isAlreadyDownloaded();
        verify(dlMock).setSslManager(null);
    }

    @Test
    public void testDoExecDownloadSuccessfulNoInstall() throws KuraException, NoSuchFieldException {
        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };

        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI,
                "https://s3-us-west-2.amazonaws.com/kura-repo/drivers/3.0.0-RELEASE/org.eclipse.kura.demo.heater_1.0.100.dp");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "http");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "heater");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        SslManagerService sslManagerService = mock(SslManagerService.class);
        handler.setSslManagerService(sslManagerService);

        handler.setSystemService(systemServiceWith(new Properties()));

        when(dlMock.isAlreadyDownloaded()).thenReturn(false);
        when(dtsMock.getClientId()).thenReturn("ClientId");
        TestUtil.setFieldValue(handler, "pendingPackageUrl", null);

        RequestHandlerContext requestContext = new RequestHandlerContext(null, Collections.emptyMap());
        KuraMessage resMessage = handler.doExec(requestContext, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
    }

    @Test
    public void testDoExecDownloadFailure()
            throws KuraException, NoSuchFieldException, IOException, InterruptedException {
        DownloadImpl dlMock = mock(DownloadImpl.class);

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return dlMock;
            }
        };

        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        DeploymentHookManager dhmMock = mock(DeploymentHookManager.class);
        handler.setDeploymentHookManager(dhmMock);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "http://heater.value");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "http");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "heater");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        SslManagerService sslManagerService = mock(SslManagerService.class);
        handler.setSslManagerService(sslManagerService);

        handler.setSystemService(systemServiceWith(new Properties()));

        when(dtsMock.getClientId()).thenReturn("ClientId");

        when(dlMock.isAlreadyDownloaded()).thenReturn(false);
        doThrow(new KuraException(KuraErrorCode.INTERNAL_ERROR)).when(dlMock).downloadDeploymentPackageInternal();

        RequestHandlerContext requestContext = new RequestHandlerContext(null, Collections.emptyMap());
        KuraMessage resMessage = handler.doExec(requestContext, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());

        Thread.sleep(500);
        assertNull(TestUtil.getFieldValue(handler, "pendingPackageUrl"));
    }

    @Test(expected = KuraException.class)
    public void testDoExecUninstallOptionsException() throws KuraException, NoSuchFieldException {
        // fail soon after calling doExecUninstall

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecUninstallPendingUrl() throws KuraException, NoSuchFieldException {
        // fail at installing/pending package name

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "isInstalling", false);
        TestUtil.setFieldValue(handler, "pendingUninstPackageName", "");

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecUninstallIsDownloadedException() throws KuraException, NoSuchFieldException {
        // fail with UninstallImpl exception
        String testMesg = "testMesg";

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected UninstallImpl createUninstallImpl() {
                throw new RuntimeException(testMesg);
            }
        };
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_UNINSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        DataTransportService dtsMock = mock(DataTransportService.class);
        handler.setDataTransportService(dtsMock);

        when(dtsMock.getClientId()).thenReturn("ClientId");

        TestUtil.setFieldValue(handler, "isInstalling", true);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecStopNoBundleId() throws KuraException, NoSuchFieldException {
        // don't fail before the actual call

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_STOP);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecStartNoBundleId() throws KuraException, NoSuchFieldException {
        // fail because of missing/null bundle id

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_START);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecStartNFE() throws KuraException, NoSuchFieldException {
        // fail because of String bundle id

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_START);
        resourcesList.add("aa");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecStartNullBundle() throws KuraException, NoSuchFieldException {
        // fail because of null bundle

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_START);
        resourcesList.add("99");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "bundleContext", ctxMock);

        when(ctxMock.getBundle(99)).thenReturn(null);

        handler.doExec(null, message);

        verify(ctxMock).getBundle(99);
    }

    @Test(expected = KuraException.class)
    public void testDoExecStartBundleExc() throws KuraException, NoSuchFieldException, BundleException {
        // fail because of start exception

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_START);
        resourcesList.add("99");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "bundleContext", ctxMock);

        Bundle bundleMock = mock(Bundle.class);
        when(ctxMock.getBundle(99)).thenReturn(bundleMock);

        BundleException ex = new BundleException("test");
        doThrow(ex).when(bundleMock).start();

        handler.doExec(null, message);

        verify(ctxMock).getBundle(99);
        verify(bundleMock).start();
    }

    @Test
    public void testDoExecStopBundleExc() throws KuraException, NoSuchFieldException, BundleException {
        // OK

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_STOP);
        resourcesList.add("99");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        BundleContext ctxMock = mock(BundleContext.class);
        TestUtil.setFieldValue(handler, "bundleContext", ctxMock);

        Bundle bundleMock = mock(Bundle.class);
        when(ctxMock.getBundle(99)).thenReturn(bundleMock);

        KuraMessage resMessage = handler.doExec(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals("Response code should match expected", KuraResponsePayload.RESPONSE_CODE_OK,
                resPayload.getResponseCode());

        verify(ctxMock).getBundle(99);
        verify(bundleMock).stop();
    }

    private ServiceReference<DeploymentHook> mockHookReference(String kuraServicePid, DeploymentHook deploymentHook) {
        @SuppressWarnings("unchecked")
        final ServiceReference<DeploymentHook> reference = mock(ServiceReference.class);
        when(reference.getProperty(anyString())).then(invocation -> {
            String arg = invocation.getArgument(0, String.class);
            if (ConfigurationService.KURA_SERVICE_PID.equals(arg)) {
                return kuraServicePid;
            }
            return deploymentHook;
        });
        return reference;
    }

    private DeploymentHookManager getDeploymentHookManager() throws NoSuchFieldException {
        final DeploymentHookManager result = new DeploymentHookManager();
        final BundleContext mockBundleContext = mock(BundleContext.class);
        when(mockBundleContext.getService(any())).then(invocation -> {
            final ServiceReference<?> ref = invocation.getArgument(0, ServiceReference.class);
            return ref.getProperty("service");
        });
        TestUtil.setFieldValue(result, "bundleContext", mockBundleContext);
        return result;
    }

    @Test(expected = KuraException.class)
    public void testDoExecDownloadNoRegisteredHookException() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "http://localhost:1234");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "HTTP");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_REQUEST_TYPE, "someRequestType");

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallNoRegisteredHookException() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_REQUEST_TYPE, "someRequestType");

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecDownloadShouldAbortOnPreDownload() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("testHook", new DeploymentHook() {

            @Override
            public void preDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
                throw new IllegalStateException("Aborted by hook");
            }

            @Override
            public void postInstall(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
            }
        }));
        final Properties associations = new Properties();
        associations.setProperty("testRequest", "testHook");
        manager.updateAssociations(associations);

        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, true);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_REQUEST_TYPE, "testRequest");

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExecInstallShouldAbortOnPostDownload() throws KuraException, NoSuchFieldException {

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2();
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD, new CloudDeploymentHandlerV2Options(new HashMap<>()));

        final DeploymentHookManager manager = getDeploymentHookManager();
        manager.bindHook(mockHookReference("testHook", new DeploymentHook() {

            @Override
            public void preDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postInstall(RequestContext context, Map<String, Object> properties) throws KuraException {
            }

            @Override
            public void postDownload(RequestContext context, Map<String, Object> properties) throws KuraException {
                throw new IllegalStateException("Aborted by hook");
            }
        }));
        final Properties associations = new Properties();
        associations.setProperty("testRequest", "testHook");
        manager.updateAssociations(associations);
        assertTrue(manager.getHook("testRequest") != null);

        handler.setDeploymentHookManager(manager);
        handler.setDataTransportService(mock(DataTransportService.class));
        final DownloadImpl mockDownloadImpl = mock(DownloadImpl.class);
        when(mockDownloadImpl.isAlreadyDownloaded()).thenReturn(true);
        TestUtil.setFieldValue(handler, "downloadImplementation", mockDownloadImpl);

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add(CloudDeploymentHandlerV2.RESOURCE_INSTALL);
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "test");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1237L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, true);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_REQUEST_TYPE, "testRequest");

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    /*
     * Download size limit (file.transfer.size.max)
     */

    @Test
    public void downloadShouldSucceedWhenSizeIsWithinLimit() throws Exception {
        givenMaxDownloadSizeProperty("2000");
        givenServedPackage(2000, true);

        whenDownloadIsRequested();

        thenPackageIsDownloaded();
    }

    @Test
    public void downloadShouldBeRejectedWhenContentLengthExceedsLimit() throws Exception {
        givenMaxDownloadSizeProperty("2000");
        givenServedPackage(2001, true);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void downloadShouldBeRejectedWhenStreamExceedsLimitWithoutContentLength() throws Exception {
        givenMaxDownloadSizeProperty("2000");
        givenServedPackage(2001, false);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void downloadShouldUseDefaultLimitWhenPropertyIsMissing() throws Exception {
        givenServedPackage((int) CloudDeploymentHandlerV2.MAX_DOWNLOAD_SIZE_DEFAULT + 1, true);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void downloadShouldUseDefaultLimitWhenPropertyIsInvalid() throws Exception {
        givenMaxDownloadSizeProperty("not-a-number");
        givenServedPackage((int) CloudDeploymentHandlerV2.MAX_DOWNLOAD_SIZE_DEFAULT + 1, true);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void downloadShouldNotBeLimitedWhenPropertyIsNegative() throws Exception {
        givenMaxDownloadSizeProperty("-1");
        givenServedPackage((int) CloudDeploymentHandlerV2.MAX_DOWNLOAD_SIZE_DEFAULT + 1, false);

        whenDownloadIsRequested();

        thenPackageIsDownloaded();
    }

    @Test
    public void downloadShouldBeRejectedWhenPropertyIsZeroAndContentLengthIsSent() throws Exception {
        givenMaxDownloadSizeProperty("0");
        givenServedPackage(1, true);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void downloadShouldBeRejectedWhenPropertyIsZeroWithoutContentLength() throws Exception {
        givenMaxDownloadSizeProperty("0");
        givenServedPackage(1, false);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @Test
    public void emptyDownloadShouldBeRejectedWhenPropertyIsZero() throws Exception {
        givenMaxDownloadSizeProperty("0");
        givenServedPackage(0, true);

        whenDownloadIsRequested();

        thenNoFileIsLeft();
    }

    @After
    public void deleteDownloadsDirectory() throws IOException {
        if (this.downloadsDirectory != null) {
            Files.deleteIfExists(this.downloadsDirectory.resolve(DOWNLOADED_FILE_NAME));
            Files.deleteIfExists(this.downloadsDirectory);
        }
    }

    private void givenMaxDownloadSizeProperty(String value) {
        this.systemProperties.setProperty(CloudDeploymentHandlerV2.MAX_DOWNLOAD_SIZE_PROPNAME, value);
    }

    private void givenServedPackage(int size, boolean sendContentLength) {
        this.servedPackage = new byte[size];
        new Random(size).nextBytes(this.servedPackage);
        this.sendContentLength = sendContentLength;
    }

    private void whenDownloadIsRequested() throws Exception {
        this.downloadsDirectory = Files.createTempDirectory("downloads");

        CloudDeploymentHandlerV2 handler = new CloudDeploymentHandlerV2() {

            @Override
            protected DownloadImpl createDownloadImpl(DeploymentPackageDownloadOptions options) {
                return new DownloadImpl(options, this) {

                    @Override
                    protected DownloadCountingOutputStream getDownloadInstance(String protocol,
                            DownloadOptions downloadOptions) {
                        return new HttpDownloadCountingOutputStream(downloadOptions) {

                            @Override
                            protected HttpURLConnection openConnection(URL url) throws IOException {
                                return mockServedConnection();
                            }
                        };
                    }
                };
            }
        };

        Map<String, Object> componentProperties = new HashMap<>();
        componentProperties.put("downloads.directory", this.downloadsDirectory.toString());
        TestUtil.setFieldValue(handler, COMPONENT_OPTIONS_FIELD,
                new CloudDeploymentHandlerV2Options(componentProperties));
        TestUtil.setFieldValue(handler, "pendingPackageUrl", null);
        handler.setDeploymentHookManager(mock(DeploymentHookManager.class));
        DataTransportService dataTransportService = mock(DataTransportService.class);
        when(dataTransportService.getClientId()).thenReturn("ClientId");
        handler.setDataTransportService(dataTransportService);
        handler.setSystemService(systemServiceWith(this.systemProperties));

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI, "http://localhost/limited.dp");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_PROTOCOL, "HTTP");
        reqPayload.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_INSTALL, false);
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_NAME, "limited");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_DP_VERSION, "1.0.0");
        reqPayload.addMetric(DeploymentPackageOptions.METRIC_JOB_ID, 1234L);
        reqPayload.addMetric(DeploymentPackageInstallOptions.METRIC_DP_INSTALL_SYSTEM_UPDATE, false);

        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), Collections.singletonList(CloudDeploymentHandlerV2.RESOURCE_DOWNLOAD));

        handler.doExec(new RequestHandlerContext(null, Collections.emptyMap()),
                new KuraMessage(reqPayload, reqResources));

        ((Future<?>) TestUtil.getFieldValue(handler, "downloaderFuture")).get(10, TimeUnit.SECONDS);
    }

    private void thenPackageIsDownloaded() throws IOException {
        assertArrayEquals(this.servedPackage,
                Files.readAllBytes(this.downloadsDirectory.resolve(DOWNLOADED_FILE_NAME)));
    }

    private void thenNoFileIsLeft() {
        assertFalse(Files.exists(this.downloadsDirectory.resolve(DOWNLOADED_FILE_NAME)));
    }

    private HttpURLConnection mockServedConnection() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(this.servedPackage));
        if (this.sendContentLength) {
            when(connection.getHeaderField("Content-Length")).thenReturn(String.valueOf(this.servedPackage.length));
        }
        return connection;
    }

    private static SystemService systemServiceWith(Properties properties) {
        SystemService systemService = mock(SystemService.class);
        when(systemService.getProperties()).thenReturn(properties);
        return systemService;
    }

    public void testPublishMessage() {
        fail("Not yet implemented");
    }

    public void testInstallDownloadedFile() {
        fail("Not yet implemented");
    }

}
