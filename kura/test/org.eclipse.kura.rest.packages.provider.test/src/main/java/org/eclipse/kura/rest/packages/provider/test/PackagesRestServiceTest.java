/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
import java.util.Objects;
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
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentPackageInfo;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
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
import org.osgi.framework.BundleContext;
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

    private ArrayList<DeploymentPackage> deploymentPackages = new ArrayList<>();
    private Exception occurredException;

    @Test
    public void getShouldWorkWithEmptyList() throws KuraException {
        givenDeploymentPackageList();

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("[]");
    }

    @Test
    public void getShouldWorkWithNonEmptyList() throws KuraException {
        givenDeploymentPackageWith("testPackage", "1.0.0");
        givenDeploymentPackageList();

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("[{\"name\":\"testPackage\",\"version\":\"1.0.0\"}]");
    }

    @Test
    public void postInstallShouldWorkWithEmptyList() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("POST"), "/_install", "{'url':'http://localhost:8080/testPackage.dp'}");

        thenRequestSucceeds();

        thenInstallIsCalledWith("http://localhost:8080/testPackage.dp");
    }

    public PackagesRestServiceTest(Transport transport) {
        super(transport);
        Mockito.reset(deploymentAdmin);
        Mockito.reset(deploymentAgentService);
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

        BundleContext packagesRestServiceContext = FrameworkUtil.getBundle(PackagesRestServiceTest.class)
                .getBundleContext();
        packagesRestServiceContext.registerService(DeploymentAgentService.class, deploymentAgentService,
                deploymentServiceProperties);

        // Inject mock deployment admin
        final ServiceReference<DeploymentRestService> deploymentRestServiceRef = packagesRestServiceContext
                .getServiceReference(DeploymentRestService.class);
        if (Objects.isNull(deploymentRestServiceRef)) {
            throw new IllegalStateException("Unable to find instance of: " + DeploymentRestService.class.getName());
        }

        final DeploymentRestService service = packagesRestServiceContext.getService(deploymentRestServiceRef);
        if (Objects.isNull(service)) {
            throw new IllegalStateException("Unable to get instance of: " + DeploymentRestService.class.getName());
        }
        service.setDeploymentAdmin(deploymentAdmin);

    }

    /*
     * GIVEN
     */
    private void givenDeploymentPackageWith(String name, String version) {
        DeploymentPackage dp = Mockito.mock(DeploymentPackage.class);
        when(dp.getName()).thenReturn(name);
        when(dp.getVersion()).thenReturn(new Version(version));
        this.deploymentPackages.add(dp);
    }

    private void givenDeploymentPackageList() {
        DeploymentPackage[] deploymentPackagesArray = new DeploymentPackage[this.deploymentPackages.size()];
        deploymentPackagesArray = this.deploymentPackages.toArray(deploymentPackagesArray);
        when(deploymentAdmin.listDeploymentPackages()).thenReturn(deploymentPackagesArray);
    }

    /*
     * THEN
     */
    private void thenInstallIsCalledWith(String url) {
        try {
            verify(deploymentAgentService).installDeploymentPackageAsync(url);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }
}
