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
package org.eclipse.kura.rest.deployment.agent.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.internal.rest.deployment.agent.DeploymentPackageInfo;
import org.eclipse.kura.internal.rest.deployment.agent.DeploymentRestService;
import org.eclipse.kura.rest.deployment.agent.api.DeploymentRequestStatus;
import org.eclipse.kura.rest.deployment.agent.api.InstallRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DeploymentRestServiceIntegrationTest {

    private static final String INSTALL_URL_PART = "_install";

    private static final String HEATER_URL = "http://kura-repo.s3.us-west-2.amazonaws.com/drivers/5.0.0-RELEASE/Kura/org.eclipse.kura.demo.heater_1.0.600.dp";
    private static final String EXAMPLE_PUBLISHER_URL = "http://kura-repo.s3.us-west-2.amazonaws.com/drivers/5.0.0-RELEASE/Kura/org.eclipse.kura.example.publisher_1.0.600.dp";
    
    private static final String HEATER_PID = "org.eclipse.kura.demo.heater";
    private static final String EXAMPLE_PUBLISHER_PID = "org.eclipse.kura.example.publisher";

    private static final String REST_SERVICE_PID = "org.eclipse.kura.internal.rest.provider.RestService";

    private static final Logger logger = LoggerFactory.getLogger(DeploymentRestServiceIntegrationTest.class);

    private static final String BASE_URL = "http://127.0.0.1:8181/services/deploy/v2";

    private static CountDownLatch dependencyLatch = new CountDownLatch(4);

    private static ConfigurationService cfgSvc;

    private static ConfigurableComponent restService;

    private static DeploymentAgentService deploymentAgentService;

    private static DeploymentRestService deploymentRestService;

    private static final String ROLES_KEY = "roles";

    @BeforeClass
    public static void setup() {
        try {
            dependencyLatch.await(10, TimeUnit.SECONDS);
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Before
    public void beforeTestSetup() throws KuraException {
        ComponentConfiguration cc = cfgSvc.getComponentConfiguration(REST_SERVICE_PID);

        String[] currentRoles = (String[]) cc.getConfigurationProperties().get(ROLES_KEY);

        for (String role : currentRoles) {
            if (role.contains("deploy")) {
                return;
            }
        }

        String[] roles = { "assets;deploy", "", "" };
        Map<String, Object> updatedProp = new HashMap<>();
        updatedProp.put(ROLES_KEY, roles);
        try {
            cfgSvc.updateConfiguration(REST_SERVICE_PID, updatedProp, false);
            Thread.sleep(5000);
        } catch (KuraException | InterruptedException e) {
            logger.error("Unable to update configuration!");
        }
    }

    @After
    public void preTestCleanup() throws InterruptedException {
        try {
            deploymentAgentService.uninstallDeploymentPackageAsync(HEATER_PID);
            Thread.sleep(5000);
            deploymentAgentService.uninstallDeploymentPackageAsync(EXAMPLE_PUBLISHER_PID);
            Thread.sleep(5000);
        } catch (Exception e) {
            logger.error("Could not clean before test");
        }
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        DeploymentRestServiceIntegrationTest.cfgSvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        DeploymentRestServiceIntegrationTest.cfgSvc = null;
    }

    public void bindRestService(ConfigurableComponent restService) {
        DeploymentRestServiceIntegrationTest.restService = restService;
        dependencyLatch.countDown();
    }

    public void unbindRestService(ConfigurableComponent restService) {
        DeploymentRestServiceIntegrationTest.cfgSvc = null;
    }

    public void bindDeploymentAgent(DeploymentAgentService deploymentAgentService) {
        DeploymentRestServiceIntegrationTest.deploymentAgentService = deploymentAgentService;
        dependencyLatch.countDown();
    }

    public void unbindDeploymentAgent(DeploymentAgentService deploymentAgentService) {
        DeploymentRestServiceIntegrationTest.deploymentAgentService = null;
    }

    public void bindDeploymentAgentRestSvc(DeploymentRestService deploymentRestService) {
        DeploymentRestServiceIntegrationTest.deploymentRestService = deploymentRestService;
        dependencyLatch.countDown();
    }

    public void unbindDeploymentAgentRestSvc(DeploymentRestService deploymentRestService) {
        DeploymentRestServiceIntegrationTest.deploymentRestService = null;
    }

    @Test
    public void testServiceExists() {
        assertNotNull(DeploymentRestServiceIntegrationTest.cfgSvc);
        assertNotNull(DeploymentRestServiceIntegrationTest.restService);
        assertNotNull(DeploymentRestServiceIntegrationTest.deploymentAgentService);
        assertNotNull(DeploymentRestServiceIntegrationTest.deploymentRestService);
    }

    @Test
    public void testListDeploymentPackages() {
        List<DeploymentPackageInfo> deploymentPackages = listDeploymentPackages();

        assertNotNull(deploymentPackages);
    }

    @Test
    public void testInstallDeploymentPackageNullInstallRequest() {
        WebTarget target = createRestClient(INSTALL_URL_PART);
        Gson gson = new Gson();
        Type type = new TypeToken<InstallRequest>() {
        }.getType();
        String jsonString = gson.toJson(null, type);
        Response response = target.request().post(Entity.json(jsonString));

        assertNotNull(response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testInstallDeploymentPackageEmptyInstallRequest() {
        String packageUrl = "";

        Response response = doPostInstall(packageUrl);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testInstallDeploymentPackageWrongInstallRequest() {
        String packageUrl = "http://wrongrequest";

        Response response = doPostInstall(packageUrl);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    private Response doPostInstall(String packageUrl) {
        WebTarget target = createRestClient(INSTALL_URL_PART);

        InstallRequest installRequest = new InstallRequest(packageUrl);
        Gson gson = new Gson();
        Type type = new TypeToken<InstallRequest>() {
        }.getType();
        String jsonString = gson.toJson(installRequest, type);
        return target.request().post(Entity.json(jsonString));
    }

    @Test
    public void testInstallDeploymentPackage() throws InterruptedException {
        List<DeploymentPackageInfo> oldDeploymentPackages = listDeploymentPackages();

        Response response = doInstallHeater();

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Thread.sleep(5000);

        List<DeploymentPackageInfo> newDeploymentPackages = listDeploymentPackages();

        assertEquals(newDeploymentPackages.size(), oldDeploymentPackages.size() + 1L);
    }

    private Response doInstallHeater() {
        WebTarget target = createRestClient(INSTALL_URL_PART);

        InstallRequest installRequest = new InstallRequest(HEATER_URL);

        Gson gson = new Gson();
        Type type = new TypeToken<InstallRequest>() {
        }.getType();
        String jsonString = gson.toJson(installRequest, type);
        return target.request().post(Entity.json(jsonString));
    }
    
    private Response doInstallExamplePublisher() {
        WebTarget target = createRestClient(INSTALL_URL_PART);

        InstallRequest installRequest = new InstallRequest(EXAMPLE_PUBLISHER_URL);

        Gson gson = new Gson();
        Type type = new TypeToken<InstallRequest>() {
        }.getType();
        String jsonString = gson.toJson(installRequest, type);
        return target.request().post(Entity.json(jsonString));
    }

    @Test
    public void testUninstallDeploymentPackage() throws InterruptedException {

        testInstallDeploymentPackage();

        List<DeploymentPackageInfo> oldDeploymentPackages = listDeploymentPackages();

        Response response = doUninstallHeater();

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Thread.sleep(5000);

        List<DeploymentPackageInfo> newDeploymentPackages = listDeploymentPackages();

        assertEquals(oldDeploymentPackages.size() - 1L, newDeploymentPackages.size());
    }

    private Response doUninstallHeater() {
        WebTarget target = createRestClient(HEATER_PID);

        return target.request().delete();
    }

    @Test
    public void testInstallingSameDP() {
        doInstallHeater();

        Response response = doInstallHeater();

        String jsonString = response.readEntity(String.class);
        Gson gson = new Gson();
        Type type = new TypeToken<String>() {
        }.getType();
        String result = gson.fromJson(jsonString, type);
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(DeploymentRequestStatus.INSTALLING.name(), result);
    }
    
    @Test
    public void testInstallingDifferentDPs() {
        doInstallHeater();

        Response response = doInstallExamplePublisher();
        String jsonString = response.readEntity(String.class);
        Gson gson = new Gson();
        Type type = new TypeToken<String>() {
        }.getType();
        String result = gson.fromJson(jsonString, type);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(DeploymentRequestStatus.REQUEST_RECEIVED.name(), result);
    }

    private List<DeploymentPackageInfo> listDeploymentPackages() {
        Response response = doGETRequest("");
        String jsonString = response.readEntity(String.class);
        Gson gson = new Gson();
        Type type = new TypeToken<List<DeploymentPackageInfo>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }

    private Response doGETRequest(String... uriValues) {
        WebTarget target = createRestClient(uriValues);
        return target.request(MediaType.APPLICATION_JSON).get();
    }

    private WebTarget createRestClient(String... uriValues) {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin", "admin");
        Client client = ClientBuilder.newClient();
        client.register(feature);
        UriBuilder uriBuilder = UriBuilder.fromUri(BASE_URL);
        for (String uriValue : uriValues) {
            uriBuilder.path(uriValue);
        }
        return client.target(uriBuilder.toString());
    }

}
