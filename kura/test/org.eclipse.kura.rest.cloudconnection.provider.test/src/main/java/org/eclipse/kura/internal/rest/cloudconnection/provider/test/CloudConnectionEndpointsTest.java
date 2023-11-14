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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEndpointPidRequest;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PidAndFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

@RunWith(Parameterized.class)
public class CloudConnectionEndpointsTest extends AbstractRequestHandlerTest {

    private static final String CLOUD_ENDPOINT_INSTANCE_TEST = "org.eclipse.kura.cloud.CloudService-test";

    private static final String MQTT_APP_ID = "CLD-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static final String REST_APP_ID = "cloudconnection/v1";

    private static CloudConnectionFactory cloudConnectionFactory;
    private static ConfigurationService configurationService;

    private Gson gson = new Gson();

    private CloudConnectionFactoryPidAndCloudEndpointPid cloudConnectionFactoryPidAndCloudEndpointPid;
    private PidAndFactoryPidAndCloudEndpointPid pidAndFactoryPidAndCloudEndpointPid;
    private PidAndFactoryPid pidAndFactoryPid;
    private PidSet pidSet;

    private UpdateComponentConfigurationRequest updateComponentConfigurationRequest;
    private CloudEndpointPidRequest cloudEndpointPidRequest;

    private static final String EXPECTED_GET_STACK_COMPONENT_PIDS_RESPONSE = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/getStackComponentPidsResponse.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    private static final String UPDATE_COMPONENT_CONFIGURATION_REQUEST = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/updateConfigurationRequest.json"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");

    private static final String EXPECTED_IS_ENDPOINT_CONNECTED_RESPONSE = new Scanner(
            CloudConnectionEndpointsTest.class.getResourceAsStream("/isConnectedResponse.json"), "UTF-8")
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
            configurationService = ServiceUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            cloudConnectionFactory = ServiceUtil.trackService(CloudConnectionFactory.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
            cloudConnectionFactory.createConfiguration(CLOUD_ENDPOINT_INSTANCE_TEST);
        } catch (Exception e) {
            fail("Unable to create the test CloudEndpoint");
        }
    }

    @Test
    public void shouldGetCloudComponentInstances() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/instances");

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();
    }

    @Test
    public void shouldGetStackComponentPids() {
        givenCloudConnectionFactoryPidAndCloudEndpointPid("org.eclipse.kura.cloud.CloudService",
                CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/stackComponentPids",
                gson.toJson(this.cloudConnectionFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_GET_STACK_COMPONENT_PIDS_RESPONSE);
    }

    @Test
    public void shouldCreateCloudEndpoint() {
        givenCloudConnectionFactoryPidAndCloudEndpointPid("org.eclipse.kura.cloud.CloudService",
                "org.eclipse.kura.cloud.CloudService-createTest" + this.getTransportType());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint",
                gson.toJson(this.cloudConnectionFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeleteCloudEndpoint() {
        givenExistingCloudEndpoint("org.eclipse.kura.cloud.CloudService-toDelete" + this.getTransportType());
        givenCloudConnectionFactoryPidAndCloudEndpointPid("org.eclipse.kura.cloud.CloudService",
                "org.eclipse.kura.cloud.CloudService-toDelete" + this.getTransportType());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/cloudEndpoint",
                gson.toJson(this.cloudConnectionFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldGetCloudComponentFactories() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), "/factories");

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();
    }

    @Test
    public void shouldCreatePublisherInstance() {
        givenPidAndFactoryPidAndCloudEndpointPid("test-pub-" + this.getTransportType(),
                "org.eclipse.kura.cloud.publisher.CloudPublisher", CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubSub",
                gson.toJson(this.pidAndFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldCreateSubscriberInstance() {
        givenPidAndFactoryPidAndCloudEndpointPid("test-sub-" + this.getTransportType(),
                "org.eclipse.kura.cloud.subscriber.CloudSubscriber", CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubSub",
                gson.toJson(this.pidAndFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeletePublisherInstance() {
        givenPubSubInstance("pub-to-delete-" + this.getTransportType(),
                "org.eclipse.kura.cloud.publisher.CloudPublisher", CLOUD_ENDPOINT_INSTANCE_TEST);
        givenPid("pub-to-delete-" + this.getTransportType());
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/pubSub",
                gson.toJson(this.pidAndFactoryPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeleteSubscriberInstance() {
        givenPubSubInstance("sub-to-delete-" + this.getTransportType(),
                "org.eclipse.kura.cloud.subscriber.CloudSubscriber", CLOUD_ENDPOINT_INSTANCE_TEST);
        givenPid("sub-to-delete-" + this.getTransportType());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/pubSub",
                gson.toJson(this.pidAndFactoryPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldGetConfigurations() {
        givenPidSet(CLOUD_ENDPOINT_INSTANCE_TEST, "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-test", //
                "org.eclipse.kura.data.DataService-test");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/configurations", gson.toJson(this.pidSet));

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();
    }

    @Test
    public void shouldUpdateStackComponentConfigurations() {

        givenUpdateComponentConfigurationRequest(UPDATE_COMPONENT_CONFIGURATION_REQUEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_PUT), "/configurations",
                gson.toJson(this.updateComponentConfigurationRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldConnectEndpoint() {
        givenCloudEndpointPidRequest(CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/connect",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDisconnectEndpoint() {
        givenCloudEndpointPidRequest(CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/disconnect",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldCheckEndpointStatus() {
        givenCloudEndpointPidRequest(CLOUD_ENDPOINT_INSTANCE_TEST);

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/isConnected",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_IS_ENDPOINT_CONNECTED_RESPONSE);
    }

    private void givenCloudEndpointPidRequest(String pid) {
        this.cloudEndpointPidRequest = new CloudEndpointPidRequest(pid);
    }

    private void givenCloudConnectionFactoryPidAndCloudEndpointPid(String cloudConnectionFactoryPid,
            String cloudEndpointPid) {
        this.cloudConnectionFactoryPidAndCloudEndpointPid = new CloudConnectionFactoryPidAndCloudEndpointPid(
                cloudConnectionFactoryPid, cloudEndpointPid);
    }

    private void givenUpdateComponentConfigurationRequest(String jsonFileContent) {
        this.updateComponentConfigurationRequest = this.gson.fromJson(jsonFileContent,
                UpdateComponentConfigurationRequest.class);

    }

    private void givenPidAndFactoryPidAndCloudEndpointPid(String pid, String factoryPid, String cloudEndpointPid) {
        this.pidAndFactoryPidAndCloudEndpointPid = new PidAndFactoryPidAndCloudEndpointPid(pid, factoryPid,
                cloudEndpointPid);
    }

    private void givenPid(String pid) {
        this.pidAndFactoryPid = new PidAndFactoryPid(pid);
    }

    private void givenPidSet(String... pids) {
        this.pidSet = new PidSet(new HashSet<String>(Arrays.asList(pids)));
    }

    private void givenExistingCloudEndpoint(String cloudEndpointPid) {
        try {
            cloudConnectionFactory.createConfiguration(cloudEndpointPid);

        } catch (KuraException e) {
            e.printStackTrace();
            fail("Unable to create the test CloudService");
        }
    }

    private void givenPubSubInstance(String pid, String factoryPid, String cloudEndpointPid) {
        try {
            configurationService.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), cloudEndpointPid), true);
        } catch (KuraException e) {
            e.printStackTrace();
            fail("Unable to create pubSub instance");
        }

    }

}
