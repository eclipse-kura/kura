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
 ******************************************************************************/
package org.eclipse.kura.internal.rest.cloudconnection.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.moquette.broker.Server;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudConnectionFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.CloudEndpointPidRequest;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.PidAndFactoryPidAndCloudEndpointPid;
import org.eclipse.kura.rest.configuration.api.PidAndFactoryPid;
import org.eclipse.kura.rest.configuration.api.PidSet;
import org.eclipse.kura.rest.configuration.api.UpdateComponentConfigurationRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@RunWith(Parameterized.class)
public class CloudConnectionEndpointsTest extends AbstractRequestHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(CloudConnectionEndpointsTest.class);

    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloud.CloudService";
    private static final String MQTT_DATA_TRANSPORT_STACK_COMPONENT_PREFIX = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport";

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

            final Server server = new Server();

            final Properties properties = new Properties();
            properties.put("port", "6666");
            properties.put("host", "0.0.0.0");

            server.startServer(properties);
        } catch (Exception e) {
            fail("Test setup failed");
        }
    }

    @Before
    public void createTestCloudStack() {
        givenExistingCloudEndpoint(testCloudInstancePid());
    }

    @After
    public void deleteTestCloudStack() {
        try {
            cloudConnectionFactory.deleteConfiguration(testCloudInstancePid());
        } catch (final Exception e) {
            logger.warn("failed to delete test cloud instance during cleanup", e);
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
        givenCloudConnectionFactoryPidAndCloudEndpointPid(CLOUD_SERVICE_FACTORY_PID, testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/stackComponentPids",
                gson.toJson(this.cloudConnectionFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
        thenResponseJsonArraySizeIs("pids", 3);
        thenResponseJsonArrayContains("pids", "org.eclipse.kura.cloud.CloudService-" + testCloudInstancesSuffix());
        thenResponseJsonArrayContains("pids",
                "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-" + testCloudInstancesSuffix());
        thenResponseJsonArrayContains("pids", "org.eclipse.kura.data.DataService-" + testCloudInstancesSuffix());

    }

    private void thenResponseJsonArraySizeIs(final String property, final int size) {
        final JsonObject response = gson.fromJson(
                expectResponse().getBody().orElseThrow(() -> new IllegalStateException("body is missing")),
                JsonObject.class);

        assertEquals(size, response.get(property).getAsJsonArray().size());
    }

    private void thenResponseJsonArrayContains(final String property, final String value) {
        final JsonObject response = gson.fromJson(
                expectResponse().getBody().orElseThrow(() -> new IllegalStateException("body is missing")),
                JsonObject.class);

        assertTrue(response.get(property).getAsJsonArray().contains(new JsonPrimitive(value)));
    }

    @Test
    public void shouldCreateCloudEndpoint() {
        givenCloudConnectionFactoryPidAndCloudEndpointPid(CLOUD_SERVICE_FACTORY_PID,
                "org.eclipse.kura.cloud.CloudService-createTest" + testCloudInstancesSuffix());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint",
                gson.toJson(this.cloudConnectionFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeleteCloudEndpoint() {
        givenCloudConnectionFactoryPidAndCloudEndpointPid(CLOUD_SERVICE_FACTORY_PID, testCloudInstancePid());

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
        givenPidAndFactoryPidAndCloudEndpointPid("test-pub-" + testCloudInstancesSuffix(),
                "org.eclipse.kura.cloud.publisher.CloudPublisher", testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubSub",
                gson.toJson(this.pidAndFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldCreateSubscriberInstance() {
        givenPidAndFactoryPidAndCloudEndpointPid("test-sub-" + testCloudInstancesSuffix(),
                "org.eclipse.kura.cloud.subscriber.CloudSubscriber", testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/pubSub",
                gson.toJson(this.pidAndFactoryPidAndCloudEndpointPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeletePublisherInstance() {
        givenPubSubInstance("pub-to-delete-" + testCloudInstancesSuffix(),
                "org.eclipse.kura.cloud.publisher.CloudPublisher", testCloudInstancePid());
        givenPid("pub-to-delete-" + testCloudInstancesSuffix());
        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/pubSub",
                gson.toJson(this.pidAndFactoryPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDeleteSubscriberInstance() {
        givenPubSubInstance("sub-to-delete-" + testCloudInstancesSuffix(),
                "org.eclipse.kura.cloud.subscriber.CloudSubscriber", testCloudInstancePid());
        givenPid("sub-to-delete-" + testCloudInstancesSuffix());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_DELETE, MQTT_METHOD_SPEC_DEL), "/pubSub",
                gson.toJson(this.pidAndFactoryPid));

        thenRequestSucceeds();
    }

    @Test
    public void shouldGetConfigurations() {
        givenPidSet(CLOUD_SERVICE_FACTORY_PID + '-' + testCloudInstancesSuffix(),
                "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-" + testCloudInstancesSuffix(), //
                "org.eclipse.kura.data.DataService-" + testCloudInstancesSuffix());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/configurations", gson.toJson(this.pidSet));

        thenRequestSucceeds();
        thenResponseBodyIsNotEmpty();
    }

    @Test
    public void shouldUpdateStackComponentConfigurations() {
        givenUpdateComponentConfigurationRequest("{" + //
                "\"configs\": [" +  //
                "{" + //
                "\"pid\": \"org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-" + testCloudInstancesSuffix()
                + "\"," + //
                "\"properties\": {" + //
                "\"broker-url\": {" + //
                "\"type\": \"STRING\"," + //
                "\"value\": \"mqtt://test.mosquitto.org:1883\"" + //
                "}," + //
                "\"topic.context.account-name\": {" + //
                "\"type\": \"STRING\"," + //
                "\"value\": \"account-name-testX2\"" + //
                "}," + //
                "\"client-id\": {" + //
                "\"type\": \"STRING\"," + //
                "\"value\": \"foobar\"" + //
                "}" + //
                "}" + //
                "}" + //
                "]," + //
                "\"takeSnapshot\" : true" + //
                "}");

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_PUT), "/configurations",
                gson.toJson(this.updateComponentConfigurationRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldConnectEndpoint() {
        givenCloudEndpointPidRequest(testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/connect",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldDisconnectEndpoint() {
        givenCloudEndpointPidRequest(testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/disconnect",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
    }

    @Test
    public void shouldCheckEndpointStatus() {
        givenCloudEndpointPidRequest(testCloudInstancePid());

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/cloudEndpoint/isConnected",
                gson.toJson(this.cloudEndpointPidRequest));

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"connected\": false}");
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

            final String mqttDataTransportPid = cloudEndpointPid.replace(CLOUD_SERVICE_FACTORY_PID,
                    MQTT_DATA_TRANSPORT_STACK_COMPONENT_PREFIX);

            final DataTransportService dataTransportService = ServiceUtil.trackService(DataTransportService.class,
                    Optional.of("(kura.service.pid=" + mqttDataTransportPid + ")")).get(30, TimeUnit.SECONDS);

            await(() -> {
                try {
                    return configurationService.getComponentConfiguration(mqttDataTransportPid) != null;
                } catch (Exception e) {
                    return false;
                }
            }, 5, TimeUnit.SECONDS);

            logger.info("Test MqttDataTransport with pid {} found...", mqttDataTransportPid);

            final Map<String, Object> prioperties = new HashMap<>();
            prioperties.put("client-id", "testClientId");
            prioperties.put("broker-url", "mqtt://localhost:6666");

            configurationService.updateConfiguration(mqttDataTransportPid, prioperties);

            await(() -> dataTransportService.getClientId() != null, 5, TimeUnit.SECONDS);

        } catch (Exception e) {
            final String message = "Unable to create the test CloudService with pid: " + cloudEndpointPid;
            logger.warn(message, e);
            fail(message);
        }
    }

    private void await(final Supplier<Boolean> predicate, final long duration, final TimeUnit timeUnit)
            throws InterruptedException {

        final long deadline = System.nanoTime() + timeUnit.toNanos(duration);

        while (!predicate.get() && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }

        if (!predicate.get()) {
            fail("condition did not occur");
        }
    }

    private void givenPubSubInstance(String pid, String factoryPid, String cloudEndpointPid) {
        try {
            configurationService.createFactoryConfiguration(factoryPid, pid, Collections.singletonMap(
                    CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), cloudEndpointPid), true);
        } catch (KuraException e) {
            final String message = "Unable to create pubSub instance";
            logger.warn(message, e);
            fail(message);
        }

    }

    private String testCloudInstancesSuffix() {
        return "test" + System.identityHashCode(this);
    }

    private String testCloudInstancePid() {
        return "org.eclipse.kura.cloud.CloudService-" + testCloudInstancesSuffix();
    }

}