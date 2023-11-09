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
package org.eclipse.kura.internal.rest.cloudconnection.provider.test;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.FactoryPidAndCloudServicePid;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PidAndFactoryPidAndCloudConnectionPid;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

@RunWith(Parameterized.class)
public class CloudConnectionEndpointsTest extends AbstractRequestHandlerTest {

    private static final String CLOUD_ENDPOINT_INSTANCE_TEST = "org.eclipse.kura.cloud.CloudService-test";

    private static final String MQTT_APP_ID = "CC-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static final String REST_APP_ID = "cloudconnection/v1";

    private static CloudConnectionFactory cloudConnectionFactory;

    private Gson gson = new Gson();

    private FactoryPidAndCloudServicePid factoryPidAndCloudServicePid;

    private PidAndFactoryPidAndCloudConnectionPid pidAndFactoryPidAndCloudConnectionPid;

    private static final String EXPECTED_GET_CLOUD_ENTRIES_RESPONSE = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/getCloudEntriesResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    private static final String EXPECTED_GET_STACK_CONFIGURATIONS_RESPONSE = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/getStackConfigurationsResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    private static final String EXPECTED_GET_CLOUD_COMPONENT_FACTORIES_RESPONSE = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/getCloudComponentFactoriesResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Transport> transports() {
        return Arrays.asList(new MqttTransport(MQTT_APP_ID), new RestTransport(REST_APP_ID));
    }

    public CloudConnectionEndpointsTest(Transport transport) {
        super(transport);

    }

    @BeforeClass
    public static void setup() {
        try {
            cloudConnectionFactory = ServiceUtil.trackService(CloudConnectionFactory.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            cloudConnectionFactory.createConfiguration(CLOUD_ENDPOINT_INSTANCE_TEST);
        } catch (Exception e) {
            fail("Unable to create the test CloudEndpoint");
        }
    }

    @Test
    public void shouldGetCloudEntries() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/cloudEntries",
                gson.toJson(this.factoryPidAndCloudServicePid));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_CLOUD_ENTRIES_RESPONSE);
    }

    @Test
    public void shouldGetStackConfigurations() {
        givenFactoryPidAndCloudServicePid("org.eclipse.kura.cloud.CloudService",
                "org.eclipse.kura.cloud.CloudService-test");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/stackConfigurations",
                gson.toJson(this.factoryPidAndCloudServicePid));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_STACK_CONFIGURATIONS_RESPONSE);
    }

    @Test
    public void shouldCreateCloudService() {
        givenFactoryPidAndCloudServicePid("org.eclipse.kura.cloud.CloudService",
                "org.eclipse.kura.cloud.CloudService-1");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudService",
                gson.toJson(this.factoryPidAndCloudServicePid));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldDeleteCloudService() {
        givenExistingCloudService("org.eclipse.kura.cloud.CloudService-todelete");

        givenFactoryPidAndCloudServicePid("org.eclipse.kura.cloud.CloudService",
                "org.eclipse.kura.cloud.CloudService-todelete");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE), "/cloudService",
                gson.toJson(this.factoryPidAndCloudServicePid));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldGetCloudComponentFactories() {
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/cloudComponentFactories");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_CLOUD_COMPONENT_FACTORIES_RESPONSE);
    }

    @Test
    public void shouldCreatePublisherInstance() {
        givenPidAndFactoryPidAndCloudConnectionPid( //
                "testPub", //
                "org.eclipse.kura.cloud.publisher.CloudPublisher", //
                "org.eclipse.kura.cloud.CloudService-test");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubInstance");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldCreateSubscriberInstance() {
        givenPidAndFactoryPidAndCloudConnectionPid( //
                "testSub", //
                "org.eclipse.kura.cloud.publisher.CloudSubscriber", //
                "org.eclipse.kura.cloud.CloudService-test");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubInstance");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void shouldDeletePubSubInstance() {

    }

    private void givenPidAndFactoryPidAndCloudConnectionPid(String pid, String factoryPid, String cloudConnectionPid) {
        this.pidAndFactoryPidAndCloudConnectionPid = new PidAndFactoryPidAndCloudConnectionPid(pid, factoryPid,
                cloudConnectionPid);
    }

    private void givenExistingCloudService(String cloudServicePid) {
        try {
            cloudConnectionFactory.createConfiguration(cloudServicePid);
        } catch (KuraException e) {
            e.printStackTrace();
            fail("Unable to create the test CloudService");
        }
    }

    private void givenFactoryPidAndCloudServicePid(String factoryPid, String cloudServicePid) {
        this.factoryPidAndCloudServicePid = new FactoryPidAndCloudServicePid(factoryPid, cloudServicePid);
    }

}
