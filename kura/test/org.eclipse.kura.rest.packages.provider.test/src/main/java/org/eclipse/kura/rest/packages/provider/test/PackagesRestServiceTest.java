/*******************************************************************************
 * Copyright (c) 2023, 2026 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.deployment.agent.MarketplacePackageDescriptor;
import org.eclipse.kura.internal.rest.deployment.agent.ConcurrentUploadLimiter;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.eclipse.kura.internal.rest.deployment.agent.MultipartRequestBodySizeLimiter;
import org.eclipse.kura.system.SystemService;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;

@RunWith(Parameterized.class)
public class PackagesRestServiceTest extends AbstractRequestHandlerTest {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String MOCK_FILE_PATH = System.getProperty(JAVA_IO_TMPDIR) + File.separator + "mock.dp";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String FILE_TRANSFER_SIZE_MAX_PROP_NAME = "file.transfer.size.max";
    private static final String REST_UPLOAD_ENDPOINT = "http://localhost:8080/services/deploy/v2/_upload";

    private final ArrayList<DeploymentPackage> deploymentPackages = new ArrayList<>();
    private Exception occurredException;

    private String responseBody;
    private int responseCode;
    private Optional<String> uploadedFileUrl = Optional.empty();

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
        thenResponseCodeIs(200);
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
        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/testPackage");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenUninstallIsCalledWith("testPackage");
        thenResponseBodyEqualsJson("\"REQUEST_RECEIVED\"");
    }

    @Test
    public void uninstallShouldWorkWithValidPackageNameWhenARequestWasAlreadyIssued() {
        givenAnUninstallationRequestWasAlreadyIssuedFor("testPackage");

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/testPackage");

        thenRequestSucceeds();

        thenNoExceptionOccurred();
        thenUninstallIsNeverCalled();
        thenResponseBodyEqualsJson("\"UNINSTALLING\"");
    }

    @Test
    public void installShouldWorkWithFileUpload() {
        givenMockTemporaryFileAt(MOCK_FILE_PATH);

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenInstallIsCalledWithLocalUri();
        thenResposeStatusCodeIs(200);
        thenResponseBodyEquals("\"REQUEST_RECEIVED\"");
    }

    @Test
    public void installShouldWorkWithFileUploadBelowSizeLimit() throws Exception {
        givenUploadSizeLimit("4096");
        givenMockTemporaryFileWithSize(MOCK_FILE_PATH, 100);

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenInstallIsCalledWithLocalUri();
        thenResposeStatusCodeIs(200);
        thenResponseBodyEquals("\"REQUEST_RECEIVED\"");
    }

    @Test
    public void installShouldFailWithFileUploadAboveSizeLimit() throws Exception {
        givenUploadSizeLimit("4096");
        givenMockTemporaryFileWithSize(MOCK_FILE_PATH, 64 * 1024);

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenInstallIsNeverCalled();
        thenResposeStatusCodeIs(413);
    }

    @Test
    public void installShouldWorkWithLargeFileUploadWhenSizeLimitIsDisabled() throws Exception {
        givenUploadSizeLimit("-1");
        givenMockTemporaryFileWithSize(MOCK_FILE_PATH, 64 * 1024);

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenInstallIsCalledWithLocalUri();
        thenResposeStatusCodeIs(200);
    }

    @Test
    public void installShouldWorkWithFileUploadAndDeploymentAgentThrowing() throws Exception {
        givenMockTemporaryFileAt(MOCK_FILE_PATH);
        givenDeploymentAgentServiceThrowsExceptionOnInstall();

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenInstallIsCalledWithLocalUri();
        thenResposeStatusCodeIs(500);
        thenResponseBodyEquals("Error installing deployment package: mock.dp");
    }

    @Test
    public void uploadShouldFailWithBadRequestIfTheFileFormParameterIsMissing() {
        whenUploadIsPerformedWithFormField("notTheFile", MOCK_FILE_PATH);

        thenInstallIsNeverCalled();
        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(400);
        thenResponseBodyEquals("Bad request");
    }

    @Test
    public void uploadShouldBeAcceptedAfterABadRequest() {
        whenUploadIsPerformedWithFormField("notTheFile", MOCK_FILE_PATH);
        thenResposeStatusCodeIs(400);

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(200);
        thenInstallIsRequestedTimes(1);
    }

    @Test
    public void uploadShouldBeRejectedWhileAnotherInstallIsInProgress() {
        givenAnUploadedPackageIsBeingInstalled();

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(429);
        thenInstallIsRequestedTimes(1);
    }

    @Test
    public void uploadShouldBeAcceptedAfterTheInstallCompletes() {
        givenAnUploadedPackageIsBeingInstalled();

        whenTheInstallCompletes(this.uploadedFileUrl.get(), true);
        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(200);
        thenInstallIsRequestedTimes(2);
    }

    @Test
    public void uploadedFileShouldBeDeletedWhenTheInstallFails() {
        givenAnUploadedPackageIsBeingInstalled();

        whenTheInstallCompletes(this.uploadedFileUrl.get(), false);

        thenFirstUploadedFileExists(false);
    }

    @Test
    public void uploadShouldBeRejectedWhenTheCompletedInstallIsNotTheUploadedOne() {
        givenAnUploadedPackageIsBeingInstalled();

        whenTheInstallCompletes("file:/tmp/unrelated.dp", true);
        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(429);
        thenInstallIsRequestedTimes(1);
    }

    @Test
    public void uploadShouldBeAcceptedAfterTheInstallCouldNotBeQueued() throws Exception {
        givenDeploymentAgentServiceThrowsExceptionOnInstall();
        whenUploadIsPerformedWith(MOCK_FILE_PATH);
        thenResposeStatusCodeIs(500);

        givenDeploymentAgentServiceAcceptsInstallRequests();
        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(200);
    }

    @Test
    public void uploadShouldBeAcceptedWhenTheCompletionEventWasLost() {
        givenAnUploadedPackageIsBeingInstalled();
        givenTheDeploymentAgentIsNotInstallingAnything();

        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        thenNoExceptionOccurred();
        thenResposeStatusCodeIs(200);
        thenInstallIsRequestedTimes(2);
        thenFirstUploadedFileExists(false);
    }

    @Test
    public void getMarketplacePackageDescriptorShouldFailWithDeploymentAgentServiceThrowing() {
        givenDeploymentAgentServiceThrowsExceptionOnGetMarketplacePackageDescriptor();
        whenRequestIsPerformed(new MethodSpec("PUT"), "/_packageDescriptor",
                "{'url':'https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=42'}");

        thenNoExceptionOccurred();
        thenResponseCodeIs(500);
    }

    @Test
    public void getMarketplacePackageDescriptorShouldFailWithWrongUrl() {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/_packageDescriptor",
                "{'url':'https://marketplace.ellipse.org/marketplace-client-intro?mpc_install=69'}");

        thenNoExceptionOccurred();
        thenResponseCodeIs(400);
        thenDeploymentAgentServiceGetMarketplacePackageDescriptorIsNeverCalled();
    }

    @Test
    public void getMarketplacePackageDescriptorShouldWork() {
        givenDeploymentAgentServiceReturnsMarketplacePackageDescriptor(MarketplacePackageDescriptor.builder()
                .nodeId("testNodeId").url("testUrl").dpUrl("testDpUrl2").minKuraVersion("1.1.0").maxKuraVersion("5.3.0")
                .currentKuraVersion("5.4.0").isCompatible(true).build());

        whenRequestIsPerformed(new MethodSpec("PUT"), "/_packageDescriptor",
                "{'url':'https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=70'}");

        thenRequestSucceeds();
        thenResponseCodeIs(200);
        thenNoExceptionOccurred();
        thenDeploymentAgentServiceIsCalledWithUrl("https://marketplace.eclipse.org/node/70/api/p");
        thenResponseBodyEqualsJson(
                "{\"nodeId\":\"testNodeId\",\"url\":\"testUrl\",\"dpUrl\":\"testDpUrl2\",\"minKuraVersion\":\"1.1.0\",\"maxKuraVersion\":\"5.3.0\",\"currentKuraVersion\":\"5.4.0\",\"isCompatible\":true}");
    }

    public PackagesRestServiceTest(Transport transport) {
        super(transport);
        Mockito.reset(deploymentAdmin);
        Mockito.reset(deploymentAgentService);
    }

    private static DeploymentAgentService deploymentAgentService = Mockito.mock(DeploymentAgentService.class);
    private static DeploymentAdmin deploymentAdmin = Mockito.mock(DeploymentAdmin.class);
    private static MultipartRequestBodySizeLimiter requestBodySizeLimiter;
    private static ConcurrentUploadLimiter concurrentUploadLimiter;

    @Before
    public void createMockFile() {
        try {
            File file = new File(MOCK_FILE_PATH);
            file.createNewFile();
        } catch (Exception e) {
            fail();
        }

    }

    @After
    public void restoreUploadSizeLimit() {
        // the test environment does not limit the upload size
        givenUploadSizeLimit("-1");
    }

    @After
    public void cleanupMockFile() {
        try {
            Files.deleteIfExists(Paths.get(MOCK_FILE_PATH));
        } catch (IOException e) {
            fail();
        }
    }

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport("deploy/v2"));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        final BundleContext packagesRestServiceContext = FrameworkUtil.getBundle(PackagesRestServiceTest.class)
                .getBundleContext();

        final ServiceComponentRuntime scrService = ServiceUtil
                .trackService(ServiceComponentRuntime.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        final ComponentDescriptionDTO dto = scrService.getComponentDescriptionDTOs().stream()
                .filter(c -> Objects.equals("org.eclipse.kura.deployment.agent", c.name)).findAny()
                .orElseThrow(() -> new IllegalStateException("cannot find deployment agent component"));

        scrService.disableComponent(dto).timeout(30000).getValue();

        final Dictionary<String, Object> deploymentServiceProperties = new Hashtable<>();
        deploymentServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        deploymentServiceProperties.put("kura.service.pid", "mockDeploymentService");

        packagesRestServiceContext.registerService(DeploymentAgentService.class, deploymentAgentService,
                deploymentServiceProperties);

        final DeploymentRestService service = ServiceUtil.trackService(DeploymentRestService.class, Optional.empty())
                .get(30, TimeUnit.SECONDS);

        service.setDeploymentAdmin(deploymentAdmin);

        requestBodySizeLimiter = (MultipartRequestBodySizeLimiter) ServiceUtil.trackService(ContainerRequestFilter.class,
                Optional.of("(kura.service.pid=org.eclipse.kura.rest.deployment.agent.MultipartRequestBodySizeLimiter)"))
                .get(30, TimeUnit.SECONDS);

        concurrentUploadLimiter = ServiceUtil.trackService(ConcurrentUploadLimiter.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);
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

    private void givenMockTemporaryFileAt(String path) {
        assertTrue(Files.exists(Paths.get(path)));
    }

    private void givenMockTemporaryFileWithSize(String path, int size) throws IOException {
        Files.write(Paths.get(path), new byte[size]);
    }

    private void givenUploadSizeLimit(String limit) {
        final Properties properties = new Properties();
        properties.setProperty(FILE_TRANSFER_SIZE_MAX_PROP_NAME, limit);

        final SystemService systemService = Mockito.mock(SystemService.class);
        when(systemService.getProperties()).thenReturn(properties);

        requestBodySizeLimiter.setSystemService(systemService);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnInstall() throws Exception {
        doThrow(new RuntimeException()).when(deploymentAgentService).installDeploymentPackageAsync(anyString());
    }

    private void givenAnUploadedPackageIsBeingInstalled() {
        // a slot left over by a previous test is released as stale, the agent mock is reset for every test
        whenUploadIsPerformedWith(MOCK_FILE_PATH);

        assertEquals(200, this.responseCode);

        when(deploymentAgentService.isInstallingDeploymentPackage(anyString())).thenReturn(true);

        final ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        try {
            verify(deploymentAgentService).installDeploymentPackageAsync(url.capture());
        } catch (Exception e) {
            fail();
        }
        this.uploadedFileUrl = Optional.of(url.getValue());
    }

    private void givenTheDeploymentAgentIsNotInstallingAnything() {
        when(deploymentAgentService.isInstallingDeploymentPackage(anyString())).thenReturn(false);
    }

    private void givenDeploymentAgentServiceAcceptsInstallRequests() {
        Mockito.reset(deploymentAgentService);
        when(deploymentAgentService.isInstallingDeploymentPackage(anyString())).thenReturn(true);
    }

    private void givenDeploymentAgentServiceReturnsMarketplacePackageDescriptor(
            MarketplacePackageDescriptor descriptor) {
        when(deploymentAgentService.getMarketplacePackageDescriptor(anyString())).thenReturn(descriptor);
    }

    private void givenDeploymentAgentServiceThrowsExceptionOnGetMarketplacePackageDescriptor() {
        when(deploymentAgentService.getMarketplacePackageDescriptor(anyString())).thenThrow(new RuntimeException());
    }

    /*
     * WHEN
     */
    private void whenUploadIsPerformedWith(String filePath) {
        whenUploadIsPerformedWithFormField("file", filePath);
    }

    private void whenUploadIsPerformedWithFormField(String fieldName, String filePath) {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().credentials(USERNAME, PASSWORD)
                .build();
        final Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).register(feature).build();

        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        final FileDataBodyPart filePart = new FileDataBodyPart(fieldName, new File(filePath));
        final FormDataMultiPart multipart = (FormDataMultiPart) formDataMultiPart.bodyPart(filePart);

        final WebTarget target = client.target(REST_UPLOAD_ENDPOINT);
        final Response response = target.request().post(Entity.entity(multipart, multipart.getMediaType()));

        this.responseBody = response.readEntity(String.class);
        this.responseCode = response.getStatus();

        try {
            formDataMultiPart.close();
            multipart.close();
        } catch (IOException e) {
            this.occurredException = e;
        }
    }

    private void whenTheInstallCompletes(String url, boolean successful) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(DeploymentAgentService.EVENT_PACKAGE_URL, url);
        properties.put(DeploymentAgentService.EVENT_SUCCESSFUL, successful);

        concurrentUploadLimiter.handleEvent(new Event(DeploymentAgentService.EVENT_INSTALLED_TOPIC, properties));
    }

    /*
     * THEN
     */
    private void thenInstallIsRequestedTimes(int times) {
        try {
            verify(deploymentAgentService, times(times)).installDeploymentPackageAsync(anyString());
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void thenFirstUploadedFileExists(boolean expected) {
        try {
            assertEquals(expected, Files.exists(Paths.get(new URI(this.uploadedFileUrl.get()))));
        } catch (URISyntaxException e) {
            fail();
        }
    }

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

    private void thenInstallIsCalledWithLocalUri() {
        final String localUri = System.getProperty(JAVA_IO_TMPDIR) + File.separator;
        final Path localPath = Paths.get(localUri);

        try {
            verify(deploymentAgentService, times(1))
                    .installDeploymentPackageAsync(startsWith(localPath.toUri().toURL().toString()));
        } catch (Exception e) {
            fail();
        }
    }

    private void thenResposeStatusCodeIs(int expectedCode) {
        assertEquals(expectedCode, this.responseCode);
    }

    private void thenResponseBodyEquals(String expectedBody) {
        assertEquals(expectedBody, this.responseBody);
    }

    private void thenDeploymentAgentServiceIsCalledWithUrl(String expectedUrl) {
        verify(deploymentAgentService, times(1)).getMarketplacePackageDescriptor(expectedUrl);
    }

    private void thenDeploymentAgentServiceGetMarketplacePackageDescriptorIsNeverCalled() {
        verify(deploymentAgentService, never()).getMarketplacePackageDescriptor(anyString());
    }
}
