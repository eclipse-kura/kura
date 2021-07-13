/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.inventory;

import static org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants.ARGS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.JsonMarshallUnmarshallImpl;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public class InventoryHandlerV1Test {

    private static final String TEST_JSON = "testJson";

    @Test(expected = KuraException.class)
    public void testDoGetNoResources() throws KuraException, NoSuchFieldException {
        InventoryHandlerV1 handler = new InventoryHandlerV1();

        List<String> resourcesList = Collections.emptyList();
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);
        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoGetOtherwise() throws KuraException, NoSuchFieldException {
        InventoryHandlerV1 handler = new InventoryHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();

        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doGet(null, message);
    }

    @Test
    public void testDoGetPackagesEmptyList() throws KuraException, NoSuchFieldException {
        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(0, packagesArray.length);

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("deploymentPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[0];

        TestUtil.setFieldValue(inventory, "deploymentAdmin", deploymentAdmin);
        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);

        KuraMessage resMessage = inventory.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetPackagesOneElementListNoBundleInfos() throws KuraException, NoSuchFieldException {
        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[1];
        DeploymentPackage dp = mock(DeploymentPackage.class);
        deployedPackages[0] = dp;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(1, packagesArray.length);
                assertEquals(dp.getName(), packagesArray[0].getName());
                assertEquals(dp.getVersion().toString(), packagesArray[0].getVersion());

                assertEquals(0, packagesArray[0].getBundleInfos().length);

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("deploymentPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        TestUtil.setFieldValue(inventory, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);
        when(dp.getName()).thenReturn("heater");
        when(dp.getVersion()).thenReturn(new Version("1.0.0"));
        when(dp.getBundleInfos()).thenReturn(new BundleInfo[0]);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
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

        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

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

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("deploymentPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        TestUtil.setFieldValue(inventory, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);
        when(dp.getName()).thenReturn("heater");
        when(dp.getVersion()).thenReturn(new Version("1.0.0"));
        when(dp.getBundleInfos()).thenReturn(bundleInfos);
        when(bundleInfo.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundleInfo.getVersion()).thenReturn(new Version("1.0.0"));

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesNoBundleInstalled() throws KuraException, NoSuchFieldException {
        String xml = TEST_JSON;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

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
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(new Bundle[0]);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(xml, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleUninstalled() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("UNINSTALLED", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleInstalled() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("INSTALLED", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.INSTALLED);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleResolved() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("RESOLVED", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleStarting() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("STARTING", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.STARTING);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleStopping() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("STOPPING", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.STOPPING);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetBundlesBundleActive() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemBundles bundles = (SystemBundles) object;
                SystemBundle[] bundleArray = bundles.getBundles();
                assertEquals(1, bundleArray.length);

                assertEquals(bundle.getSymbolicName(), bundleArray[0].getName());
                assertEquals(bundle.getVersion().toString(), bundleArray[0].getVersion());
                assertEquals(bundle.getBundleId(), bundleArray[0].getId());
                assertEquals("ACTIVE", bundleArray[0].getState());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("bundles");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void doGetSystemPackagesEmpty() throws KuraException {
        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemPackages packages = (SystemPackages) object;
                List<SystemPackage> packageList = packages.getSystemPackages();
                assertTrue(packageList.isEmpty());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("systemPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        SystemService ssMock = mock(SystemService.class);
        inventory.setSystemService(ssMock);

        List<SystemResourceInfo> packages = new ArrayList<>();
        when(ssMock.getSystemPackages()).thenReturn(packages);

        KuraMessage resMessage = inventory.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void doGetSystemPackagesFailed() throws KuraException {
        InventoryHandlerV1 inventory = new InventoryHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("systemPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        SystemService ssMock = mock(SystemService.class);
        inventory.setSystemService(ssMock);
        when(ssMock.getSystemPackages())
                .thenThrow(new KuraProcessExecutionErrorException("Failed to retrieve system packages."));

        KuraMessage resMessage = inventory.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_ERROR, resPayload.getResponseCode());
    }

    @Test
    public void doGetSystemPackages() throws KuraException {
        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemPackages packages = (SystemPackages) object;
                List<SystemPackage> packageList = packages.getSystemPackages();
                assertEquals(1, packageList.size());
                assertEquals("package1", packageList.get(0).getName());
                assertEquals("1.0.0", packageList.get(0).getVersion());
                assertEquals("DEB", packageList.get(0).getTypeString());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("systemPackages");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        SystemService ssMock = mock(SystemService.class);
        inventory.setSystemService(ssMock);
        List<SystemResourceInfo> packages = new ArrayList<>();
        packages.add(new SystemResourceInfo("package1", "1.0.0", SystemResourceType.DEB));
        when(ssMock.getSystemPackages()).thenReturn(packages);

        KuraMessage resMessage = inventory.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void doGetInventory() throws KuraException, NoSuchFieldException {
        Bundle[] bundles = new Bundle[1];
        Bundle bundle = mock(Bundle.class);
        bundles[0] = bundle;

        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[1];
        DeploymentPackage dp = mock(DeploymentPackage.class);
        deployedPackages[0] = dp;

        BundleInfo[] bundleInfos = new BundleInfo[1];
        BundleInfo bundleInfo = mock(BundleInfo.class);
        bundleInfos[0] = bundleInfo;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemResourcesInfo resources = (SystemResourcesInfo) object;
                List<SystemResourceInfo> resourceList = resources.getSystemResources();
                assertEquals(3, resourceList.size());
                assertEquals("bundle1", resourceList.get(0).getName());
                assertEquals("2.0.0", resourceList.get(0).getVersion());
                assertEquals("BUNDLE", resourceList.get(0).getTypeString());
                assertEquals("dp1", resourceList.get(1).getName());
                assertEquals("3.0.0", resourceList.get(1).getVersion());
                assertEquals("DP", resourceList.get(1).getTypeString());
                assertEquals("package1", resourceList.get(2).getName());
                assertEquals("1.0.0", resourceList.get(2).getVersion());
                assertEquals("DEB", resourceList.get(2).getTypeString());

                return TEST_JSON;
            }
        };

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("inventory");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload request = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(request, reqResources);

        SystemService ssMock = mock(SystemService.class);
        inventory.setSystemService(ssMock);
        List<SystemResourceInfo> packages = new ArrayList<>();
        packages.add(new SystemResourceInfo("package1", "1.0.0", SystemResourceType.DEB));
        when(ssMock.getSystemPackages()).thenReturn(packages);

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);
        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("bundle1");
        when(bundle.getVersion()).thenReturn(new Version("2.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);

        TestUtil.setFieldValue(inventory, "deploymentAdmin", deploymentAdmin);

        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deployedPackages);
        when(dp.getName()).thenReturn("dp1");
        when(dp.getVersion()).thenReturn(new Version("3.0.0"));
        when(dp.getBundleInfos()).thenReturn(bundleInfos);
        when(bundleInfo.getSymbolicName()).thenReturn("bundle2");
        when(bundleInfo.getVersion()).thenReturn(new Version("4.0.0"));

        KuraMessage resMessage = inventory.doGet(null, message);
        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test(expected = KuraException.class)
    public void testDoDel() throws Exception {
        InventoryHandlerV1 handler = new InventoryHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doDel(null, message);
    }

    @Test(expected = KuraException.class)
    public void testDoExec() throws Exception {
        InventoryHandlerV1 handler = new InventoryHandlerV1();

        List<String> resourcesList = new ArrayList<>();
        resourcesList.add("test");
        Map<String, Object> reqResources = new HashMap<>();
        reqResources.put(ARGS_KEY.value(), resourcesList);

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        KuraMessage message = new KuraMessage(reqPayload, reqResources);

        handler.doExec(null, message);
    }

    @Test
    public void testBundleStartStopNotFound() throws BundleException {
        final List<Bundle> bundles = Arrays.asList(mockBundle("foo", "1.0"), mockBundle("bar", "2.0"));

        InventoryHandlerV1 handler = new InventoryHandlerV1();
        handler.activate(mockComponentContext(bundles));

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_start"), "{\"name\":\"baz\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_stop"), "{\"name\":\"baz\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_start"), "{\"name\":\"baz\",\"version\":\"1.0\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_stop"), "{\"name\":\"baz\",\"version\":\"1.0\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_start"), "{\"name\":\"foo\",\"version\":\"2.0\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        try {
            handler.doExec(mock(RequestHandlerContext.class),
                    requestMessage(Arrays.asList("bundles", "_stop"), "{\"name\":\"bar\",\"version\":\"3.0\"}"));
            fail("should have failed");
        } catch (final KuraException e) {
            assertEquals(KuraErrorCode.NOT_FOUND, e.getCode());
        }

        for (final Bundle bundle : bundles) {
            Mockito.verify(bundle, times(0)).start();
            Mockito.verify(bundle, times(0)).stop();
        }
    }

    @Test
    public void testStartBundleWithVersion() throws BundleException, KuraException {
        final Bundle foo = mockBundle("foo", "1.0");
        final Bundle bar = mockBundle("bar", "2.0");

        InventoryHandlerV1 handler = new InventoryHandlerV1();
        handler.activate(mockComponentContext(Arrays.asList(foo, bar)));

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class),
                requestMessage(Arrays.asList("bundles", "_start"), "{\"name\":\"foo\",\"version\":\"1.0.0\"}"));

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());

        Mockito.verify(foo, times(1)).start();
        Mockito.verify(foo, times(0)).stop();
        Mockito.verify(bar, times(0)).start();
        Mockito.verify(bar, times(0)).stop();
    }

    @Test
    public void testStartBundleWithoutVersion() throws BundleException, KuraException {
        final Bundle foo = mockBundle("foo", "1.0");
        final Bundle bar = mockBundle("bar", "2.0");

        InventoryHandlerV1 handler = new InventoryHandlerV1();
        handler.activate(mockComponentContext(Arrays.asList(foo, bar)));

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class),
                requestMessage(Arrays.asList("bundles", "_start"), "{\"name\":\"foo\"}"));

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());

        Mockito.verify(foo, times(1)).start();
        Mockito.verify(foo, times(0)).stop();
        Mockito.verify(bar, times(0)).start();
        Mockito.verify(bar, times(0)).stop();
    }

    @Test
    public void testStopBundleWithVersion() throws BundleException, KuraException {
        final Bundle foo = mockBundle("foo", "1.0");
        final Bundle bar = mockBundle("bar", "2.0");

        InventoryHandlerV1 handler = new InventoryHandlerV1();
        handler.activate(mockComponentContext(Arrays.asList(foo, bar)));

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class),
                requestMessage(Arrays.asList("bundles", "_stop"), "{\"name\":\"foo\",\"version\":\"1.0.0\"}"));

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());

        Mockito.verify(foo, times(0)).start();
        Mockito.verify(foo, times(1)).stop();
        Mockito.verify(bar, times(0)).start();
        Mockito.verify(bar, times(0)).stop();
    }

    @Test
    public void testStopBundleWithoutVersion() throws BundleException, KuraException {
        final Bundle foo = mockBundle("foo", "1.0");
        final Bundle bar = mockBundle("bar", "2.0");

        InventoryHandlerV1 handler = new InventoryHandlerV1();
        handler.activate(mockComponentContext(Arrays.asList(foo, bar)));

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class),
                requestMessage(Arrays.asList("bundles", "_stop"), "{\"name\":\"foo\"}"));

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());

        Mockito.verify(foo, times(0)).start();
        Mockito.verify(foo, times(1)).stop();
        Mockito.verify(bar, times(0)).start();
        Mockito.verify(bar, times(0)).stop();
    }

    private KuraMessage requestMessage(final List<String> resources, final String body) {

        KuraRequestPayload reqPayload = new KuraRequestPayload();
        reqPayload.setBody(body.getBytes(StandardCharsets.UTF_8));
        KuraMessage message = new KuraMessage(reqPayload, Collections.singletonMap(ARGS_KEY.value(), resources));

        return message;
    }

    @SuppressWarnings("unchecked")
    private ComponentContext mockComponentContext(final List<Bundle> bundles) {
        final Bundle[] asArray = bundles.toArray(new Bundle[bundles.size()]);

        final BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundles()).thenReturn(asArray);

        final JsonMarshallUnmarshallImpl jsonMarshaller = new JsonMarshallUnmarshallImpl();

        final ServiceReference<Unmarshaller> ref = Mockito.mock(ServiceReference.class);

        when(bundleContext.getService(ref)).thenReturn(jsonMarshaller);
        try {
            when(bundleContext.getServiceReferences(Mockito.eq(Unmarshaller.class), Mockito.anyString()))
                    .thenReturn(Arrays.asList(ref));
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }

        final ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        return componentContext;
    }

    private Bundle mockBundle(final String symbolicName, final String version) {
        final Bundle result = mock(Bundle.class);

        when(result.getSymbolicName()).thenReturn(symbolicName);
        when(result.getVersion()).thenReturn(new Version(version));

        return result;
    }
}
