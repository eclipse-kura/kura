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
package org.eclipse.kura.rest.packages.provider.test;

import static java.util.Collections.singletonMap;
import static org.eclipse.kura.core.testutil.json.JsonProjection.self;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.testutil.json.JsonProjection;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

@RunWith(Parameterized.class)
public class PackagesRestServiceTest extends AbstractRequestHandlerTest {

    @Test
    public void getShouldWorkWithEmptyList() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("[]");
    }

    @Test
    public void postInstallShouldWorkWithEmptyList() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("POST"), "/_install", "{'url':'http://localhost:8080/testPackage.dp'}");

        thenRequestSucceeds();

        verify(deploymentAgentService).isInstallingDeploymentPackage(anyString());
    }

    @Test
    public void getShouldWorkWithNonEmptyList() throws KuraException {
        DeploymentPackage[] deploymentPackages = new DeploymentPackage[1];
        deploymentPackages[0] = new DeploymentPackage() {

            @Override
            public boolean uninstallForced() throws DeploymentException {
                return false;
            }

            @Override
            public void uninstall() throws DeploymentException {

            }

            @Override
            public boolean isStale() {
                return false;
            }

            @Override
            public Version getVersion() {
                return new Version("1.0.0");
            }

            @Override
            public String[] getResources() {
                return null;
            }

            @Override
            public ServiceReference getResourceProcessor(String arg0) {
                return null;
            }

            @Override
            public String getResourceHeader(String arg0, String arg1) {
                return null;
            }

            @Override
            public String getName() {
                return "testPackage";
            }

            @Override
            public URL getIcon() {
                return null;
            }

            @Override
            public String getHeader(String arg0) {
                return null;
            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public BundleInfo[] getBundleInfos() {
                return null;
            }

            @Override
            public Bundle getBundle(String arg0) {
                return null;
            }
        };
        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deploymentPackages);

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("[]");

        // Verify listDeploymentPackages() was called once
        verify(deploymentAdmin).listDeploymentPackages();
    }

    public PackagesRestServiceTest(Transport transport) {
        super(transport);
    }

    private static DeploymentAgentService deploymentAgentService = Mockito.mock(DeploymentAgentService.class);
    private static DeploymentAdmin deploymentAdmin = Mockito.mock(DeploymentAdmin.class);

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport("deploy/v2"));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        final Dictionary<String, Object> deploymentServiceProperties = new Hashtable<>();
        deploymentServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        deploymentServiceProperties.put("kura.service.pid", "mockDeploymentService");

        final Dictionary<String, Object> deploymentAdminProperties = new Hashtable<>();
        deploymentAdminProperties.put("service.ranking", Integer.MIN_VALUE);
        deploymentAdminProperties.put("kura.service.pid", "mockDeploymentAdmin");

        FrameworkUtil.getBundle(PackagesRestServiceTest.class).getBundleContext()
                .registerService(DeploymentAgentService.class, deploymentAgentService, deploymentServiceProperties);

        FrameworkUtil.getBundle(PackagesRestServiceTest.class).getBundleContext().registerService(DeploymentAdmin.class,
                deploymentAdmin, deploymentAdminProperties);
    }

}
