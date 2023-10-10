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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

@RunWith(Parameterized.class)
public class PackagesRestServiceTest extends AbstractRequestHandlerTest {

    private final ArrayList<DeploymentPackage> deploymentPackages = new ArrayList<>();
    private Exception occurredException;

    @Test
    public void getShouldWorkWithEmptyList() {
        givenDeploymentPackageList();

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenNoExceptionOccurred();
        thenResponseBodyEqualsJson("[]");
    }

    @Test
    public void getShouldWorkWithNonEmptyList() {
        givenDeploymentPackageWith("testPackage", "1.0.0");
        givenDeploymentPackageWith("anotherAwesomePackage", "4.2.0");
        givenDeploymentPackageList();

        whenRequestIsPerformed(new MethodSpec("GET"), "");

        thenRequestSucceeds();
        thenNoExceptionOccurred();
        thenResponseBodyEqualsJson(
                "[{\"name\":\"testPackage\",\"version\":\"1.0.0\"},{\"name\":\"anotherAwesomePackage\",\"version\":\"4.2.0\"}]");
    }

    @Test
    public void installShouldWorkWithEmptyRequest() {
        whenRequestIsPerformed(new MethodSpec("POST"), "/_install");

        thenResponseCodeIs(400);

        thenNoExceptionOccurred();
        thenInstallIsNeverCalled();
    }

    @Test
    public void installShouldWorkWithValidURL() {
        whenRequestIsPerformed(new MethodSpec("POST"), "/_install", "{'url':'http://localhost:8080/testPackage.dp'}");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenInstallIsCalledWith("http://localhost:8080/testPackage.dp");
        thenResponseBodyEqualsJson("\"REQUEST_RECEIVED\"");
    }

    @Test
    public void installShouldWorkWithValidURLWhenARequestWasAlreadyIssued() {
        givenAnInstallationRequestWasAlreadyIssuedFor("http://localhost:8080/testPackage.dp");

        whenRequestIsPerformed(new MethodSpec("POST"), "/_install", "{'url':'http://localhost:8080/testPackage.dp'}");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenInstallIsNeverCalled();
        thenResponseBodyEqualsJson("\"INSTALLING\"");
    }

    @Test
    public void uninstallShouldWorkWithValidPackageName() {
        whenRequestIsPerformed(new MethodSpec("DELETE"), "/testPackage");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenUninstallIsCalledWith("testPackage");
        thenResponseBodyEqualsJson("\"REQUEST_RECEIVED\"");
    }

    @Test
    public void uninstallShouldWorkWithValidPackageNameWhenARequestWasAlreadyIssued() {
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage");

        whenRequestIsPerformed(new MethodSpec("DELETE"), "/testPackage");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenUninstallIsNeverCalled();
        thenResponseBodyEqualsJson("\"UNINSTALLING\"");
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

    private void givenAnInstallationRequestWasAlreadyIssuedFor(String url) {
        when(deploymentAgentService.isInstallingDeploymentPackage(url)).thenReturn(true);
    }

    private void givenAnUninstallationRequestWasAlreadyIssuedFor(String packageName) {
        when(deploymentAgentService.isUninstallingDeploymentPackage(packageName)).thenReturn(true);
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

    private void thenInstallIsNeverCalled() {
        try {
            verify(deploymentAgentService, never()).installDeploymentPackageAsync(anyString());
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void thenUninstallIsCalledWith(String packageName) {
        try {
            verify(deploymentAgentService).uninstallDeploymentPackageAsync(packageName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void thenUninstallIsNeverCalled() {
        try {
            verify(deploymentAgentService, never()).uninstallDeploymentPackageAsync(anyString());
        } catch (Exception e) {
            this.occurredException = e;
        }
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
}
