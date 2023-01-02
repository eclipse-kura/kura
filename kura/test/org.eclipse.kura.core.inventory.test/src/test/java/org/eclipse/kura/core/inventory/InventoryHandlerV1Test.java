/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.core.inventory.resources.ContainerImage;
import org.eclipse.kura.core.inventory.resources.ContainerImages;
import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;
import org.eclipse.kura.core.inventory.resources.SystemBundle;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackage;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackage;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.JsonMarshallUnmarshallImpl;
import org.eclipse.kura.internal.xml.marshaller.unmarshaller.XmlMarshallUnmarshallImpl;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
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

    public static final String RESOURCE_DEPLOYMENT_PACKAGES = "deploymentPackages";
    public static final String RESOURCE_BUNDLES = "bundles";
    public static final String RESOURCE_SYSTEM_PACKAGES = "systemPackages";
    public static final String RESOURCE_DOCKER_CONTAINERS = "containers";
    public static final String RESOURCE_CONTAINER_IMAGES = "images";
    public static final String INVENTORY = "inventory";
    private static final String START = "_start";
    private static final String STOP = "_stop";
    private static final String DELETE = "_delete";

    private static final List<String> START_CONTAINER = Arrays.asList(RESOURCE_DOCKER_CONTAINERS, START);
    private static final List<String> STOP_CONTAINER = Arrays.asList(RESOURCE_DOCKER_CONTAINERS, STOP);

    private static final List<String> DELETE_IMAGE = Arrays.asList(RESOURCE_CONTAINER_IMAGES, DELETE);

    private static String TEST_JSON = "testJson";
    private static String TEST_XML = "testXML";

    private static final String REGISTRY_URL = "https://test";
    private static final String REGISTRY_USERNAME = "test";
    private static final String REGISTRY_PASSWORD = "test1";

    private ContainerInstanceDescriptor dockerContainer1;
    private ContainerInstanceDescriptor dockerContainer2;

    private ImageConfiguration containerImage1;
    private ImageInstanceDescriptor containerInstanceImage1;
    private ImageConfiguration containerImage2;
    private ImageInstanceDescriptor containerInstanceImage2;

    private ContainerOrchestrationService mockContainerOrchestrationService;

    private DockerContainer dockerContainerObject;
    private DockerContainers dockerContainersObject;

    private ContainerImage containerImageObject;
    private ContainerImages containerImagesObject;

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
    public void testDoGetPackagesOneElementListNotSigned() throws KuraException, NoSuchFieldException {
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
                assertEquals(false, packagesArray[0].isSigned());

                SystemBundle[] bis = packagesArray[0].getBundleInfos();
                assertEquals(1, bis.length);
                assertEquals(bundleInfo.getSymbolicName(), bis[0].getName());
                assertEquals(bundleInfo.getVersion().toString(), bis[0].getVersion());
                assertEquals(false, bis[0].isSigned());

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
    public void testDoGetPackagesTwoElementsListPartiallySigned() throws KuraException, NoSuchFieldException {
        DeploymentAdmin deploymentAdmin = mock(DeploymentAdmin.class);
        DeploymentPackage[] deployedPackages = new DeploymentPackage[1];
        DeploymentPackage dp = mock(DeploymentPackage.class);
        deployedPackages[0] = dp;

        BundleInfo[] bundleInfos = new BundleInfo[2];
        BundleInfo bundleInfo = mock(BundleInfo.class);
        bundleInfos[0] = bundleInfo;
        
        BundleInfo bundleInfo2 = mock(BundleInfo.class);
        bundleInfos[1] = bundleInfo2;

        Bundle[] bundles = new Bundle[2];
        Bundle bundle = mock(Bundle.class);
        Bundle bundle2 = mock(Bundle.class);
        bundles[0] = bundle;
        bundles[1] = bundle2;

        InventoryHandlerV1 inventory = new InventoryHandlerV1() {

            @Override
            protected String marshal(Object object) {
                SystemDeploymentPackages packages = (SystemDeploymentPackages) object;
                SystemDeploymentPackage[] packagesArray = packages.getDeploymentPackages();

                assertEquals(1, packagesArray.length);
                assertEquals(dp.getName(), packagesArray[0].getName());
                assertEquals(dp.getVersion().toString(), packagesArray[0].getVersion());
                assertEquals(false, packagesArray[0].isSigned());

                SystemBundle[] bis = packagesArray[0].getBundleInfos();
                assertEquals(2, bis.length);
                assertEquals(bundleInfo.getSymbolicName(), bis[0].getName());
                assertEquals(bundleInfo.getVersion().toString(), bis[0].getVersion());
                assertEquals(false, bis[0].isSigned());
                assertEquals(bundleInfo2.getSymbolicName(), bis[1].getName());
                assertEquals(bundleInfo2.getVersion().toString(), bis[1].getVersion());
                assertEquals(true, bis[1].isSigned());

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
        when(bundleInfo2.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater2");
        when(bundleInfo2.getVersion()).thenReturn(new Version("2.0.0"));

        BundleContext context = mock(BundleContext.class);
        TestUtil.setFieldValue(inventory, "bundleContext", context);

        when(context.getBundles()).thenReturn(bundles);
        when(bundle.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater");
        when(bundle.getVersion()).thenReturn(new Version("1.0.0"));
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundle.getState()).thenReturn(Bundle.INSTALLED);
        
        when(bundle2.getSymbolicName()).thenReturn("org.eclipse.kura.demo.heater2");
        when(bundle2.getVersion()).thenReturn(new Version("2.0.0"));
        when(bundle2.getBundleId()).thenReturn(2L);
        when(bundle2.getState()).thenReturn(Bundle.ACTIVE);
        Map<X509Certificate, List<X509Certificate>> signingCerts = new HashMap<>();
        signingCerts.put(getEmptyX509Cert(), null);
        when(bundle2.getSignerCertificates(Bundle.SIGNERS_ALL)).thenReturn(signingCerts);

        KuraMessage resMessage = inventory.doGet(null, message);

        KuraResponsePayload resPayload = (KuraResponsePayload) resMessage.getPayload();

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, resPayload.getResponseCode());
        assertEquals(TEST_JSON, new String(resPayload.getBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testDoGetPackagesOneElementListSigned() throws KuraException, NoSuchFieldException {
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
                assertEquals(true, packagesArray[0].isSigned());

                SystemBundle[] bis = packagesArray[0].getBundleInfos();
                assertEquals(1, bis.length);
                assertEquals(bundleInfo.getSymbolicName(), bis[0].getName());
                assertEquals(bundleInfo.getVersion().toString(), bis[0].getVersion());
                assertEquals(true, bis[0].isSigned());

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
        
        Map<X509Certificate, List<X509Certificate>> signingCerts = new HashMap<>();
        signingCerts.put(getEmptyX509Cert(), null);
        when(bundle.getSignerCertificates(Bundle.SIGNERS_ALL)).thenReturn(signingCerts);

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
                assertEquals(true, bundleArray[0].isSigned());

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
        
        Map<X509Certificate, List<X509Certificate>> signingCerts = new HashMap<>();
        signingCerts.put(getEmptyX509Cert(), null);
        when(bundle.getSignerCertificates(Bundle.SIGNERS_ALL)).thenReturn(signingCerts);

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
                assertEquals(false, bundleArray[0].isSigned());

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
                assertEquals(false, bundleArray[0].isSigned());

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
                assertEquals(false, bundleArray[0].isSigned());

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
                assertEquals(true, bundleArray[0].isSigned());

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
        
        Map<X509Certificate, List<X509Certificate>> signingCerts = new HashMap<>();
        signingCerts.put(getEmptyX509Cert(), null);
        when(bundle.getSignerCertificates(Bundle.SIGNERS_ALL)).thenReturn(signingCerts);

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
        givenTwoDockerContainers();

        this.mockContainerOrchestrationService = mock(ContainerOrchestrationService.class, Mockito.RETURNS_DEEP_STUBS);

        when(this.mockContainerOrchestrationService.listContainerDescriptors())
                .thenReturn(Arrays.asList(this.dockerContainer1, this.dockerContainer2));

        when(this.mockContainerOrchestrationService.listImageInstanceDescriptors())
                .thenReturn(Arrays.asList(this.containerInstanceImage1, this.containerInstanceImage2));

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
                assertEquals(7, resourceList.size());
                assertEquals("bundle1", resourceList.get(0).getName());
                assertEquals("2.0.0", resourceList.get(0).getVersion());
                assertEquals("BUNDLE", resourceList.get(0).getTypeString());
                assertEquals("dockerContainer1", resourceList.get(1).getName());
                assertEquals("nginx:latest", resourceList.get(1).getVersion());
                assertEquals("DOCKER", resourceList.get(1).getTypeString());
                assertEquals("dockerContainer2", resourceList.get(2).getName());
                assertEquals("nginx:latest", resourceList.get(2).getVersion());
                assertEquals("DOCKER", resourceList.get(2).getTypeString());
                assertEquals("dp1", resourceList.get(3).getName());
                assertEquals("3.0.0", resourceList.get(3).getVersion());
                assertEquals("DP", resourceList.get(3).getTypeString());
                assertEquals("nginx", resourceList.get(4).getName());
                assertEquals("latest", resourceList.get(4).getVersion());
                assertEquals("CONTAINER_IMAGE", resourceList.get(4).getTypeString());
                assertEquals("nginx", resourceList.get(5).getName());
                assertEquals("alpine", resourceList.get(5).getVersion());
                assertEquals("CONTAINER_IMAGE", resourceList.get(5).getTypeString());
                assertEquals("package1", resourceList.get(6).getName());
                assertEquals("1.0.0", resourceList.get(6).getVersion());
                assertEquals("DEB", resourceList.get(6).getTypeString());

                return TEST_JSON;
            }
        };

        inventory.setContainerOrchestrationService(this.mockContainerOrchestrationService);

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

    // region Container Related Tests

    @Test
    public void testContainerMarshalingJSON() throws BundleException, KuraException {
        givenTwoDockerContainers();
        giventheFollowingContainerSetupToMarshal();

        whenContainersArePassedToMarshaler();

        thenCheckIfContainerMatchesJSON();
    }

    @Test
    public void testContainerUnMarshalingJSON() throws BundleException, KuraException {
        givenTheFollowingContainerJson();

        whenAContainerJsonIsPassedToMarshaler();

        thenCheckIfJsonMatchesContainer();
    }

    @Test
    public void testContainerMarshalingXML() throws BundleException, KuraException {
        givenTwoDockerContainers();
        giventheFollowingContainerSetupToMarshal();

        whenContainersArePassedToMarshalerXML();

        thenCheckIfContainerMatchesXML();
    }

    @Test
    public void testListContainerDoGet() throws BundleException, KuraException {
        givenTwoDockerContainers();

        whenTheFollowingJsonKuraPayloadDoGet(Arrays.asList(RESOURCE_DOCKER_CONTAINERS), "");

        thenCheckIfContainerWereListed();
    }

    @Test
    public void testStartContainerWithoutVersion() throws BundleException, KuraException, InterruptedException {
        givenTwoDockerContainers();

        whenTheFollowingJsonKuraPayloadDoExec(START_CONTAINER, "{\"name\":\"dockerContainer1\"}");

        thenCheckIfContainerOneHasStarted();
    }

    @Test
    public void testStopContainerWithoutVersion() throws BundleException, KuraException, InterruptedException {
        givenTwoDockerContainers();

        whenTheFollowingJsonKuraPayloadDoExec(STOP_CONTAINER, "{\"name\":\"dockerContainer1\"}");

        thenCheckIfContainerOneHasStopped();

    }

    // endregion

    // region Image Related Tests

    @Test
    public void testContainerImageMarshalingJSON() throws BundleException, KuraException {
        givenTwoDockerContainers();
        giventheFollowingImagesSetupToMarshal();

        whenImagesArePassedToMarshaler();

        thenCheckIfImageMatchesJSON();
    }

    @Test
    public void testContainerImageUnMarshalingJSON() throws BundleException, KuraException {
        givenTheFollowingImageJson();

        whenAImageJsonIsPassedToMarshaler();

        thenCheckIfJsonMatchesImage();
    }

    @Test
    public void testContainerImageMarshalingXML() throws BundleException, KuraException {
        givenTwoDockerContainers();
        giventheFollowingImagesSetupToMarshal();

        whenImagesArePassedToMarshalerXML();

        thenCheckIfImageMatchesXML();
    }

    @Test
    public void testListImageDoGet() throws BundleException, KuraException {
        givenTwoDockerContainers();

        whenTheFollowingJsonKuraPayloadDoGet(Arrays.asList(RESOURCE_CONTAINER_IMAGES), "");

        thenCheckIfImagesHaveBeenListed();
    }

    @Test
    public void testDeleteImage() throws BundleException, KuraException, InterruptedException {
        givenTwoDockerContainers();

        whenTheFollowingJsonContainerImageKuraPayloadDoExec(DELETE_IMAGE,
                "{\"name\":\"nginx\",\"version\":\"latest\"}");
        thenCheckIfImageHasDelete();
    }

    @Test
    public void testContainerImageDataStuct() throws BundleException, KuraException, InterruptedException {
        thenCompareOutputOfContainerImage();
    }

    // endregion

    /**
     * given
     */

    private void giventheFollowingContainerSetupToMarshal() {

        this.dockerContainerObject = new DockerContainer(this.dockerContainer1);

        this.dockerContainersObject = new DockerContainers(Arrays.asList(this.dockerContainerObject));
    }

    private void giventheFollowingImagesSetupToMarshal() {

        this.containerImageObject = new ContainerImage(this.containerInstanceImage1);

        this.containerImagesObject = new ContainerImages(Arrays.asList(this.containerImageObject));
    }

    private void givenTwoDockerContainers() {
        this.dockerContainer1 = ContainerInstanceDescriptor.builder().setContainerName("dockerContainer1")
                .setContainerImage("nginx").setContainerImageTag("latest").setContainerID("1234").build();
        this.dockerContainer2 = ContainerInstanceDescriptor.builder().setContainerName("dockerContainer2")
                .setContainerImage("nginx").setContainerID("124344").build();

        this.containerImage1 = new ImageConfiguration.ImageConfigurationBuilder().setImageName("nginx")
                .setImageTag("latest").setImageDownloadTimeoutSeconds(200)
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of(REGISTRY_URL),
                        REGISTRY_USERNAME, new Password(REGISTRY_PASSWORD))))
                .build();
        this.containerInstanceImage1 = new ImageInstanceDescriptor.ImageInstanceDescriptorBuilder()
                .setImageName(this.containerImage1.getImageName()).setImageTag(this.containerImage1.getImageTag())
                .setImageId("SHA256:3h278f34yhufy3h").build();
        this.containerImage2 = new ImageConfiguration.ImageConfigurationBuilder().setImageName("nginx")
                .setImageTag("alpine").setImageDownloadTimeoutSeconds(200)
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of(REGISTRY_URL),
                        REGISTRY_USERNAME, new Password(REGISTRY_PASSWORD))))
                .build();
        this.containerInstanceImage2 = new ImageInstanceDescriptor.ImageInstanceDescriptorBuilder()
                .setImageName(this.containerImage2.getImageName()).setImageTag(this.containerImage2.getImageTag())
                .setImageId("SHA256:dfiyegfyuwehf978ew4hu").build();
    }

    private void givenTheFollowingContainerJson() {
        TEST_JSON = "{\"name\":\"test\",\"version\":\"nginx:latest\"}";
    }

    private void givenTheFollowingImageJson() {

        TEST_JSON = "{\"name\":\"nginx\",\"version\":\"latest\"}";
    }

    /**
     * when
     */
    private void whenContainersArePassedToMarshaler() throws BundleException, KuraException {
        JsonMarshallUnmarshallImpl marsh = new JsonMarshallUnmarshallImpl();
        TEST_JSON = marsh.marshal(this.dockerContainersObject);
    }

    private void whenImagesArePassedToMarshaler() throws BundleException, KuraException {
        JsonMarshallUnmarshallImpl marsh = new JsonMarshallUnmarshallImpl();
        TEST_JSON = marsh.marshal(this.containerImagesObject);
    }

    private void whenContainersArePassedToMarshalerXML() throws BundleException, KuraException {
        XmlMarshallUnmarshallImpl marsh = new XmlMarshallUnmarshallImpl();
        TEST_XML = marsh.marshal(this.dockerContainersObject);
    }

    private void whenImagesArePassedToMarshalerXML() throws BundleException, KuraException {
        XmlMarshallUnmarshallImpl marsh = new XmlMarshallUnmarshallImpl();
        TEST_XML = marsh.marshal(this.containerImagesObject);
    }

    private void whenAContainerJsonIsPassedToMarshaler() throws BundleException, KuraException {
        JsonMarshallUnmarshallImpl marsh = new JsonMarshallUnmarshallImpl();
        this.dockerContainerObject = marsh.unmarshal(TEST_JSON, DockerContainer.class);
    }

    private void whenAImageJsonIsPassedToMarshaler() throws BundleException, KuraException {
        JsonMarshallUnmarshallImpl marsh = new JsonMarshallUnmarshallImpl();
        this.containerImageObject = marsh.unmarshal(TEST_JSON, ContainerImage.class);
    }

    private void whenTheFollowingJsonKuraPayloadDoExec(List<String> request, String payload)
            throws BundleException, KuraException {

        InventoryHandlerV1 handler = Mockito.spy(new InventoryHandlerV1());

        this.mockContainerOrchestrationService = mock(ContainerOrchestrationService.class, Mockito.RETURNS_DEEP_STUBS);

        when(this.mockContainerOrchestrationService.listContainerDescriptors())
                .thenReturn(Arrays.asList(this.dockerContainer1, this.dockerContainer2));

        KuraMessage theMessage = requestMessage(request, payload);

        handler.setContainerOrchestrationService(this.mockContainerOrchestrationService);
        handler.activate(mock(ComponentContext.class, Mockito.RETURNS_MOCKS));

        // convert ContainerDescriptor to a DockerContainer
        DockerContainer testContainer = new DockerContainer(this.dockerContainer1);

        doReturn(testContainer).when(handler).unmarshal(payload, DockerContainer.class);

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class, Mockito.RETURNS_DEEP_STUBS),
                theMessage);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());
    }

    private void whenTheFollowingJsonContainerImageKuraPayloadDoExec(List<String> request, String payload)
            throws BundleException, KuraException {

        InventoryHandlerV1 handler = Mockito.spy(new InventoryHandlerV1());

        this.mockContainerOrchestrationService = mock(ContainerOrchestrationService.class, Mockito.RETURNS_DEEP_STUBS);

        when(this.mockContainerOrchestrationService.listImageInstanceDescriptors())
                .thenReturn(Arrays.asList(this.containerInstanceImage1, this.containerInstanceImage2));

        KuraMessage theMessage = requestMessage(request, payload);

        handler.setContainerOrchestrationService(this.mockContainerOrchestrationService);
        handler.activate(mock(ComponentContext.class, Mockito.RETURNS_MOCKS));

        // convert ContainerDescriptor to a DockerContainer
        ContainerImage testImageInstance = new ContainerImage(this.containerInstanceImage1);

        doReturn(testImageInstance).when(handler).unmarshal(payload, ContainerImage.class);

        final KuraMessage response = handler.doExec(mock(RequestHandlerContext.class, Mockito.RETURNS_DEEP_STUBS),
                theMessage);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK,
                ((KuraResponsePayload) response.getPayload()).getResponseCode());
    }

    private void whenTheFollowingJsonKuraPayloadDoGet(List<String> request, String payload)
            throws BundleException, KuraException {

        InventoryHandlerV1 handler = Mockito.spy(new InventoryHandlerV1());

        this.mockContainerOrchestrationService = mock(ContainerOrchestrationService.class, Mockito.RETURNS_DEEP_STUBS);

        when(this.mockContainerOrchestrationService.listContainerDescriptors())
                .thenReturn(Arrays.asList(this.dockerContainer1, this.dockerContainer2));

        KuraMessage theMessage = requestMessage(request, payload);

        handler.setContainerOrchestrationService(this.mockContainerOrchestrationService);
        handler.activate(mock(ComponentContext.class, Mockito.RETURNS_MOCKS));

        // convert ContainerDescriptor to a DockerContainer
        DockerContainer testContainer = new DockerContainer(this.dockerContainer1);

        doReturn(testContainer).when(handler).unmarshal(payload, DockerContainer.class);

        handler.doGet(null, theMessage);
    }

    /**
     * then
     *
     * @throws KuraException
     */

    private void thenCheckIfContainerMatchesJSON() {
        assertEquals(
                "{\"containers\":[{\"name\":\"dockerContainer1\",\"version\":\"nginx:latest\",\"type\":\"DOCKER\",\"state\":\"uninstalled\"}]}",
                TEST_JSON);
    }

    private void thenCheckIfImageMatchesJSON() {
        assertEquals("{\"images\":[{\"name\":\"nginx\",\"version\":\"latest\",\"type\":\"CONTAINER_IMAGE\"}]}",
                TEST_JSON);
    }

    private void thenCheckIfContainerMatchesXML() {
        /**
         * <[<?xml version="1.0" encoding="UTF-8"?> <containers> <container>
         * <name>dockerContainer1</name> <version>nginx:latest</version>
         * <state>uninstalled</state> </container>
         * </containers> ]>
         */
        String containerXMLExpected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><containers><container><name>dockerContainer1</name><version>nginx:latest</version><state>uninstalled</state></container></containers>";
        assertEquals(containerXMLExpected.replaceAll("\\s+", ""), TEST_XML.replaceAll("\\s+", ""));
    }

    private void thenCheckIfImageMatchesXML() {
        /**
         * <[<?xml version="1.0" encoding="UTF-8"?> <containers> <container>
         * <name>dockerContainer1</name> <version>nginx:latest</version> </container>
         * </containers> ]>
         */
        String containerXMLExpected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><images><image><name>nginx</name><version>latest</version></image></images>";
        assertEquals(containerXMLExpected.replaceAll("\\s+", ""), TEST_XML.replaceAll("\\s+", ""));
    }

    private void thenCheckIfJsonMatchesContainer() {
        assertEquals("test", this.dockerContainerObject.getContainerName());

    }

    private void thenCheckIfJsonMatchesImage() {
        assertEquals("nginx", this.containerImageObject.getName());

    }

    private void thenCheckIfContainerWereListed() throws KuraException {
        Mockito.verify(this.mockContainerOrchestrationService, times(1)).listContainerDescriptors();
    }

    private void thenCheckIfImagesHaveBeenListed() throws KuraException {
        Mockito.verify(this.mockContainerOrchestrationService, times(1)).listImageInstanceDescriptors();
    }

    private void thenCheckIfContainerOneHasStarted() throws KuraException, InterruptedException {
        Mockito.verify(this.mockContainerOrchestrationService, times(1))
                .startContainer(this.dockerContainer1.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .stopContainer(this.dockerContainer1.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .startContainer(this.dockerContainer2.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .stopContainer(this.dockerContainer2.getContainerId());
    }

    private void thenCheckIfContainerOneHasStopped() throws KuraException, InterruptedException {
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .startContainer(this.dockerContainer1.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(1))
                .stopContainer(this.dockerContainer1.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .startContainer(this.dockerContainer2.getContainerId());
        Mockito.verify(this.mockContainerOrchestrationService, times(0))
                .stopContainer(this.dockerContainer2.getContainerId());
    }

    private void thenCheckIfImageHasDelete() throws KuraException, InterruptedException {
        Mockito.verify(this.mockContainerOrchestrationService, times(1))
                .deleteImage(this.containerInstanceImage1.getImageId());
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
            when(bundleContext.getServiceReferences(ArgumentMatchers.eq(Unmarshaller.class),
                    ArgumentMatchers.anyString())).thenReturn(Arrays.asList(ref));
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

    private void thenCompareOutputOfContainerImage() {
        this.containerImageObject = new ContainerImage("test", "latest");

        String imageName = "test";
        String imageTag = "latest";
        String imageId = "3e3rf32e2wsd2f";
        String imageAuthor = "Greg";
        String imageArch = "ARM64";
        long imageSize = 9883829;

        this.containerImageObject.setImageName(imageName);
        this.containerImageObject.setImageTag(imageTag);
        this.containerImageObject.setImageId(imageId);
        this.containerImageObject.setImageAuthor(imageAuthor);
        this.containerImageObject.setImageArch(imageArch);
        this.containerImageObject.setImageSize(imageSize);

        this.containerImagesObject = new ContainerImages(new LinkedList<ContainerImage>());
        this.containerImagesObject.setContainerImages(Arrays.asList(this.containerImageObject));

        assertEquals(this.containerImageObject.getImageName(), imageName);
        assertEquals(this.containerImageObject.getImageTag(), imageTag);
        assertEquals(this.containerImageObject.getImageId(), imageId);
        assertEquals(this.containerImageObject.getImageAuthor(), imageAuthor);
        assertEquals(this.containerImageObject.getImageArch(), imageArch);
        assertEquals(this.containerImageObject.getImageSize(), imageSize);
        assertEquals(this.containerImagesObject.getContainerImages().get(0).getName(), imageName);
    }
    
    private X509Certificate getEmptyX509Cert() {
        return new X509Certificate() {
            
            @Override
            public boolean hasUnsupportedCriticalExtension() {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public Set<String> getNonCriticalExtensionOIDs() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public byte[] getExtensionValue(String arg0) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Set<String> getCriticalExtensionOIDs() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public void verify(PublicKey arg0, String arg1) throws CertificateException, NoSuchAlgorithmException,
                    InvalidKeyException, NoSuchProviderException, SignatureException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void verify(PublicKey arg0) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException,
                    NoSuchProviderException, SignatureException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public String toString() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public PublicKey getPublicKey() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public byte[] getEncoded() throws CertificateEncodingException {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public int getVersion() {
                // TODO Auto-generated method stub
                return 0;
            }
            
            @Override
            public byte[] getTBSCertificate() throws CertificateEncodingException {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public boolean[] getSubjectUniqueID() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Principal getSubjectDN() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public byte[] getSignature() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public byte[] getSigAlgParams() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String getSigAlgOID() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public String getSigAlgName() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public BigInteger getSerialNumber() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Date getNotBefore() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Date getNotAfter() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public boolean[] getKeyUsage() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public boolean[] getIssuerUniqueID() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Principal getIssuerDN() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public int getBasicConstraints() {
                // TODO Auto-generated method stub
                return 0;
            }
            
            @Override
            public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
                // TODO Auto-generated method stub
                
            }
        };
    }

}
