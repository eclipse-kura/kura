/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.wire.provider.test;

import static com.eclipsesource.json.Json.parse;
import static org.eclipse.kura.core.testutil.json.JsonProjection.self;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.testutil.json.JsonProjection;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.util.wire.test.GraphBuilder;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

@RunWith(Parameterized.class)
public class WireGraphRestServiceTest extends AbstractRequestHandlerTest {

    private static final String TEST_ASSET_PID = "testAsset";
    private static final String TEST_DRIVER_PID = "testDriver";
    private static final String TEST_DRIVER_FACTORY_PID = "org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver";
    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    @Test
    public void getEmptyGraph() {
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("GET"), "/graph/snapshot");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"configs\":[{\"pid\":\"org.eclipse.kura.wire.graph.WireGraphService\",\"properties\":{\"WireGraph\":{\"value\":\"{\\\"components\\\":[],\\\"wires\\\":[]}\",\"type\":\"STRING\"}}}]}");
    }

    @Test
    public void deleteGraph() {
        givenWireGraphWith(testEmitterReceiver("foo"), testEmitterReceiver("bar"), wire("foo", "bar"));

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/graph");

        thenRequestSucceeds();
        thenWireGraphIsEmpty();
    }

    @Test
    public void deleteGraphWithUpdateRequest() {
        givenWireGraphWith(testEmitterReceiver("foo"), testEmitterReceiver("bar"), wire("foo", "bar"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot",
                "{\"configs\":[{\"pid\":\"org.eclipse.kura.wire.graph.WireGraphService\",\"properties\":{\"WireGraph\":{\"value\":\"{}\",\"type\":\"STRING\"}}}]}");

        thenRequestSucceeds();
        thenWireGraphIsEmpty();
    }

    @Test
    public void deleteEmptyGraph() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/graph");

        thenRequestSucceeds();
        thenWireGraphIsEmpty();
    }

    @Test
    public void createGraphFromEmptyState() throws InterruptedException {
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" + "  \"configs\": [\n" + "    {\n"
                + "      \"pid\": \"test1\",\n" + "      \"properties\": {\n" + "        \"service.factoryPid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" + "          \"type\": \"STRING\"\n"
                + "        }\n" + "      }\n" + "    },\n" + "    {\n" + "      \"pid\": \"log1\",\n"
                + "      \"properties\": {\n" + "        \"service.factoryPid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.Logger\",\n" + "          \"type\": \"STRING\"\n"
                + "        }\n" + "      }\n" + "    },\n" + "    {\n"
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" + "      \"properties\": {\n"
                + "        \"WireGraph\": {\n"
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}},{\\\"pid\\\":\\\"log1\\\",\\\"inputPortCount\\\":1,\\\"outputPortCount\\\":0,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-60,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[{\\\"emitter\\\":\\\"test1\\\",\\\"emitterPort\\\":0,\\\"receiver\\\":\\\"log1\\\",\\\"receiverPort\\\":0}]}\",\n"
                + "          \"type\": \"STRING\"\n" + "        },\n" + "        \"kura.service.pid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n"
                + "          \"type\": \"STRING\"\n" + "        },\n" + "        \"service.pid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n"
                + "          \"type\": \"STRING\"\n" + "        }\n" + "      }\n" + "    }\n" + "  ]\n" + "}");

        thenRequestSucceeds();
        thenCurrentGraphContainsComponent("test1");
        thenCurrentGraphContainsComponent("log1");
        thenCurrentGraphContainsWire("test1", "log1");
        thenComponentExists("test1", WireComponent.class);
        thenComponentExists("log1", WireComponent.class);
    }

    @Test
    public void addWireComponentToExistingGraph() throws InterruptedException {
        givenWireGraphWith(wireComponent("test1", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" + "  \"configs\": [\n" + "    {\n"
                + "      \"pid\": \"log1\",\n" + "      \"properties\": {\n" + "        \"service.factoryPid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.Logger\",\n" + "          \"type\": \"STRING\"\n"
                + "        }\n" + "      }\n" + "    },\n" + "    {\n"
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" + "      \"properties\": {\n"
                + "        \"WireGraph\": {\n"
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}},{\\\"pid\\\":\\\"log1\\\",\\\"inputPortCount\\\":1,\\\"outputPortCount\\\":0,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-60,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}]}\",\n"
                + "          \"type\": \"STRING\"\n" + "        },\n" + "        \"kura.service.pid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n"
                + "          \"type\": \"STRING\"\n" + "        },\n" + "        \"service.pid\": {\n"
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n"
                + "          \"type\": \"STRING\"\n" + "        }\n" + "      }\n" + "    }\n" + "  ]\n" + "}");

        thenRequestSucceeds();
        thenCurrentGraphContainsComponent("test1");
        thenCurrentGraphContainsComponent("log1");
        thenComponentExists("test1", WireComponent.class);
        thenComponentExists("log1", WireComponent.class);
    }

    @Test
    public void rejectUpdateIfConfigurationForComponentToBeCreatedIsNotSpecified() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "    \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "    \"properties\": {\n" //
                + "      \"WireGraph\": {\n" //
                + "        \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}]}\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"kura.service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }]\n" //
                + "}");

        thenResponseCodeIs(400);
    }

    @Test
    public void rejectUpdateIfFactoryPidForComponentToBeCreatedIsNotSpecified() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "    \"pid\": \"test1\",\n" //
                + "    \"properties\": {}\n" //
                + "  }, {\n" //
                + "    \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "    \"properties\": {\n" //
                + "      \"WireGraph\": {\n" //
                + "        \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}]}\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"kura.service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }]\n" //
                + "}");

        thenResponseCodeIs(400);
    }

    @Test
    public void rejectUpdateIfInputPortCountIsNotSpecified() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "    \"pid\": \"test1\",\n" //
                + "    \"properties\": {\n" //
                + "      \"service.factoryPid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.Logger\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }, {\n" //
                + "    \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "    \"properties\": {\n" //
                + "      \"WireGraph\": {\n" //
                + "        \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}]}\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"kura.service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }]\n" //
                + "}");

        thenResponseCodeIs(400);
    }

    @Test
    public void rejectUpdateIfOutputPortCountIsNotSpecified() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "    \"pid\": \"test1\",\n" //
                + "    \"properties\": {\n" //
                + "      \"service.factoryPid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.Logger\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }, {\n" //
                + "    \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "    \"properties\": {\n" //
                + "      \"WireGraph\": {\n" //
                + "        \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"test1\\\",\\\"inputPortCount\\\":0,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-260,\\\"y\\\":-40},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}]}\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"kura.service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }]\n" //
                + "}");

        thenResponseCodeIs(400);
    }

    @Test
    public void deleteExistingComponentNotReferencedByProvidedGraph() {
        givenWireGraphWith(wireComponent("toBeDeleted", "org.eclipse.kura.wire.Logger"),
                wireComponent("toBeKept", "org.eclipse.kura.wire.Logger"));
        givenDeleteTrackerForPid("toBeDeleted");

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "    \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "    \"properties\": {\n" //
                + "      \"WireGraph\": {\n" //
                + "        \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"toBeKept\\\",\\\"inputPortCount\\\":1,\\\"outputPortCount\\\":0,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-240,\\\"y\\\":-80},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[]}\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"kura.service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      },\n" //
                + "      \"service.pid\": {\n" //
                + "        \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "        \"type\": \"STRING\"\n" //
                + "      }\n" //
                + "    }\n" //
                + "  }]\n" //
                + "}");

        thenRequestSucceeds();
        thenCurrentGraphContainsComponent("toBeKept");
        thenCurrentGraphDoesNotContainsComponent("toBeDeleted");
        thenComponentExists("toBeKept", WireComponent.class);
        thenTrackedComponentIsDeleted("toBeDeleted");
    }

    @Test
    public void fillDefaultRenderingPropertiesIfNotSpecified() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"WireGraph\": {\n" //
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"timer\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1}],\\\"wires\\\":[]}\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"kura.service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]\n" //
                + "}");

        thenRequestSucceeds();
        thenWireComponentPropertyEquals("timer", "position.x", 0.0f);
        thenWireComponentPropertyEquals("timer", "position.y", 0.0f);
    }

    @Test
    public void updateWireComponentConfigurationProperty() {
        givenWireGraphWith(wireComponent("test1", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"WireGraph\": {\n" //
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"timer\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-300,\\\"y\\\":-60},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[]}\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"kura.service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenWireComponentConfigurationPropertyEquals("timer", "simple.interval", 100);
    }

    @Test
    public void createWireComponentWithModifiedConfigurationProperty() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"WireGraph\": {\n" //
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"timer\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-300,\\\"y\\\":-60},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[]}\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"kura.service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenWireComponentConfigurationPropertyEquals("timer", "simple.interval", 100);
    }

    @Test
    public void createNonGraphFactoryConfiguration() {
        givenEmptyWireGraph();
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"WireGraph\": {\n" //
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"timer\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-300,\\\"y\\\":-60},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[]}\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"kura.service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"test.property\": {\n" //
                + "          \"value\": \"foo bar baz\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_DRIVER_PID, "test.property", "foo bar baz");
    }

    @Test
    public void updateNonGraphFactoryConfiguration() {
        givenEmptyWireGraph();
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID,
                Collections.singletonMap("test.property", "bar"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph/snapshot", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"WireGraph\": {\n" //
                + "          \"value\": \"{\\\"components\\\":[{\\\"pid\\\":\\\"timer\\\",\\\"inputPortCount\\\":0,\\\"outputPortCount\\\":1,\\\"renderingProperties\\\":{\\\"position\\\":{\\\"x\\\":-300,\\\"y\\\":-60},\\\"inputPortNames\\\":{},\\\"outputPortNames\\\":{}}}],\\\"wires\\\":[]}\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"kura.service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.pid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.graph.WireGraphService\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "    {\n" //
                + "      \"pid\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"test.property\": {\n" //
                + "          \"value\": \"foo bar baz\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_DRIVER_PID, "test.property", "foo bar baz");
    }

    @Test
    public void getGraphTopology() {
        givenWireGraphWith(testEmitterReceiver("foo"), testEmitterReceiver("bar"), wire("foo", "bar"));

        whenRequestIsPerformed(new MethodSpec("GET"), "/graph/topology");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\n" //
                + "    \"components\": [\n" //
                + "        {\n" //
                + "            \"pid\": \"foo\",\n" //
                + "            \"inputPortCount\": 1,\n" //
                + "            \"outputPortCount\": 1,\n" //
                + "            \"renderingProperties\": {\n" //
                + "                \"position\": {\n" //
                + "                    \"x\": 0,\n" //
                + "                    \"y\": 0\n" //
                + "                },\n" //
                + "                \"inputPortNames\": {},\n" //
                + "                \"outputPortNames\": {}\n" //
                + "            }\n" //
                + "        },\n" //
                + "        {\n" //
                + "            \"pid\": \"bar\",\n" //
                + "            \"inputPortCount\": 1,\n" //
                + "            \"outputPortCount\": 1,\n" //
                + "            \"renderingProperties\": {\n" //
                + "                \"position\": {\n" //
                + "                    \"x\": 0,\n" //
                + "                    \"y\": 0\n" //
                + "                },\n" //
                + "                \"inputPortNames\": {},\n" //
                + "                \"outputPortNames\": {}\n" //
                + "            }\n" //
                + "        }\n" //
                + "    ],\n" //
                + "    \"wires\": [\n" //
                + "        {\n" //
                + "            \"emitter\": \"foo\",\n" //
                + "            \"emitterPort\": 0,\n" //
                + "            \"receiver\": \"bar\",\n" //
                + "            \"receiverPort\": 0\n" //
                + "        }\n" //
                + "    ]\n" //
                + "}");
    }

    @Test
    public void getConfigsWireComponent() {
        givenWireGraphWith(wireComponent("test1", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("POST"), "/configs/byPid", "{\"pids\":[\"test1\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("test1"), self().field("configs").arrayItem(0).field("pid"));
        thenResponseElementIs(Json.value(10),
                self().field("configs").arrayItem(0).field("properties").field("simple.interval").field("value"));
        thenResponseElementIs(Json.value("INTEGER"),
                self().field("configs").arrayItem(0).field("properties").field("simple.interval").field("type"));
    }

    @Test
    public void getConfigsDriver() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configs/byPid", "{\"pids\":[\"" + TEST_DRIVER_PID + "\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value(TEST_DRIVER_PID), self().field("configs").arrayItem(0).field("pid"));
    }

    @Test
    public void getConfigsAsset() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenFactoryComponent(TEST_ASSET_PID, WIRE_ASSET_FACTORY_PID,
                Collections.singletonMap("driver.pid", TEST_DRIVER_PID), SelfConfiguringComponent.class);

        whenRequestIsPerformed(new MethodSpec("POST"), "/configs/byPid", "{\"pids\":[\"" + TEST_ASSET_PID + "\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value(TEST_ASSET_PID), self().field("configs").arrayItem(0).field("pid"));
    }

    @Test
    public void getConfigsUnpermitted() {
        whenRequestIsPerformed(new MethodSpec("POST"), "/configs/byPid",
                "{\"pids\":[\"org.eclipse.kura.internal.rest.provider.RestService\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"configs\":[]}");
    }

    @Test
    public void getGraphConfigsNotFound() {
        givenWireGraphWith(wireComponent("test1", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("POST"), "/configs/byPid", "{\"pids\":[\"test2\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"configs\":[]}");
    }

    @Test
    public void updateConfigsWireComponent() {
        givenWireGraphWith(wireComponent("timer", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenRequestSucceeds();
        thenComponentConfigurationEquals("timer", "simple.interval", 100);
    }

    @Test
    public void updateConfigsWireComponentNotInGraph() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"timer\",\n" //
                + "      \"properties\": {\n" //
                + "        \"simple.interval\": {\n" //
                + "          \"value\": 100,\n" //
                + "          \"type\": \"INTEGER\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.wire.Timer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    },\n" //
                + "  ]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void updateConfigCreateDriver() {
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"test.property\": {\n" //
                + "          \"value\": \"foo\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_DRIVER_PID, "test.property", "foo");
    }

    @Test
    public void updateConfigModifyDriver() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID,
                Collections.singletonMap("test.property", "bar"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"test.property\": {\n" //
                + "          \"value\": \"foo\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_DRIVER_PID, "test.property", "foo");
    }

    @Test
    public void updateConfigCreateAsset() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenNoFactoryComponentsWithPid(TEST_ASSET_PID);

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"" + TEST_ASSET_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"driver.pid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + WIRE_ASSET_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_ASSET_PID, "driver.pid", TEST_DRIVER_PID);
    }

    @Test
    public void updateConfigModifyAsset() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenFactoryComponent(TEST_ASSET_PID, WIRE_ASSET_FACTORY_PID, Collections.singletonMap("driver.pid", "bar"),
                SelfConfiguringComponent.class);

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"" + TEST_ASSET_PID + "\",\n" //
                + "      \"properties\": {\n" //
                + "        \"driver.pid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"" + WIRE_ASSET_FACTORY_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenRequestSucceeds();
        thenComponentConfigurationEquals(TEST_ASSET_PID, "driver.pid", TEST_DRIVER_PID);
    }

    @Test
    public void updateConfigUnpermitted() {

        whenRequestIsPerformed(new MethodSpec("PUT"), "/configs", "{\n" //
                + "  \"configs\": [{\n" //
                + "      \"pid\": \"org.eclipse.kura.internal.rest.provider.RestService\",\n" //
                + "      \"properties\": {\n" //
                + "        \"driver.pid\": {\n" //
                + "          \"value\": \"" + TEST_DRIVER_PID + "\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }\n" //
                + "  ]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void deleteComponentInGraph() {
        givenWireGraphWith(wireComponent("timer", "org.eclipse.kura.wire.Timer"));

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/configs/byPid", "{\"pids\":[\"timer\"]}");

        thenResponseCodeIs(400);
    }

    @Test
    public void deleteDriver() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenDeleteTrackerForPid(TEST_DRIVER_PID);

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/configs/byPid",
                "{\"pids\":[\"" + TEST_DRIVER_PID + "\"]}");

        thenRequestSucceeds();
        thenTrackedComponentIsDeleted(TEST_DRIVER_PID);
    }

    @Test
    public void deleteAsset() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenFactoryComponent(TEST_ASSET_PID, WIRE_ASSET_FACTORY_PID,
                Collections.singletonMap("driver.pid", TEST_DRIVER_PID), SelfConfiguringComponent.class);
        givenDeleteTrackerForPid(TEST_ASSET_PID);

        whenRequestIsPerformed(new MethodSpec("DELETE", "DEL"), "/configs/byPid",
                "{\"pids\":[\"" + TEST_ASSET_PID + "\"]}");

        thenRequestSucceeds();
        thenTrackedComponentIsDeleted(TEST_ASSET_PID);
    }

    @Test
    public void getMetadata() {
        givenEmptyWireGraph();
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);

        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata");

        thenRequestSucceeds();
        thenResponseElementExists(self().field("driverOCDs")
                .anyArrayItem(self().field("pid").matching(Json.value(TEST_DRIVER_FACTORY_PID))));
        thenResponseElementExists(self().field("wireComponentDefinitions")
                .anyArrayItem(self().matching(parse(Snippets.TEST_EMITTER_RECEIVER_DEFINITION))));
        thenResponseElementIs(parse(Snippets.BASE_CHANNEL_DESCRIPTOR), self().field("assetChannelDescriptor"));
    }

    @Test
    public void getWireComponentFactoryPids() {

        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/wireComponents/factoryPids");

        thenRequestSucceeds();
        thenResponseElementExists(self().field("pids")
                .anyArrayItem(self().matching(Json.value("org.eclipse.kura.util.wire.test.TestEmitterReceiver"))));
    }

    @Test
    public void getWireComponentDefinitions() {

        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/wireComponents/definitions");

        thenRequestSucceeds();
        thenResponseElementExists(self().field("wireComponentDefinitions")
                .anyArrayItem(self().matching(parse(Snippets.TEST_EMITTER_RECEIVER_DEFINITION))));
    }

    @Test
    public void getWireComponentDefinitionsByFactoryPid() {

        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/wireComponents/definitions/byFactoryPid",
                "{\"pids\":[\"org.eclipse.kura.util.wire.test.TestEmitterReceiver\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(parse(Snippets.TEST_EMITTER_RECEIVER_DEFINITION),
                self().field("wireComponentDefinitions").arrayItem(0));
        thenResponseElementDoesNotExists(self().field("wireComponentDefinitions").arrayItem(1));
    }

    @Test
    public void getWireComponentDefinitionsNotExisting() {

        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/wireComponents/definitions/byFactoryPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{}");
    }

    @Test
    public void getDriverFactoryPids() {
        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/drivers/factoryPids");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"pids\":[\"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\"]}");
    }

    @Test
    public void getDriverPidsNoDrivers() {
        givenNoFactoryComponentsWithPid(TEST_DRIVER_PID);
        whenRequestIsPerformed(new MethodSpec("GET"), "/drivers/pids");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"components\":[]}");
    }

    @Test
    public void getDriverPidsSingleDriver() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("GET"), "/drivers/pids");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"components\":[{\"pid\":\"testDriver\",\"factoryPid\":\"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\"}]}");
    }

    @Test
    public void getDriverOCDs() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/drivers/ocds");

        thenRequestSucceeds();
        thenResponseElementExists(
                self().field("driverOCDs").anyArrayItem(self().matching(Json.parse(Snippets.TEST_DRIVER_DEFINITION))));
    }

    @Test
    public void getDriverOCDsByFactoryPid() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/drivers/ocds/byFactoryPid",
                "{\"pids\":[\"org.eclipse.kura.util.test.driver.ChannelDescriptorTestDriver\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.parse(Snippets.TEST_DRIVER_DEFINITION), self().field("driverOCDs").arrayItem(0));
        thenResponseElementDoesNotExists(self().field("driverOCDs").arrayItem(1));
    }

    @Test
    public void getDriverOCDsByFactoryPidNotFound() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/drivers/ocds/byFactoryPid", "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{}");
    }

    @Test
    public void getDriverChannelDescriptors() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/drivers/channelDescriptors");

        thenRequestSucceeds();
        thenResponseElementExists(self().field("driverChannelDescriptors")
                .anyArrayItem(self().matching(Json.parse(Snippets.TEST_DRIVER_DESCRIPTOR))));
    }

    @Test
    public void getDriverChannelDescriptorsByPid() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/drivers/channelDescriptors/byPid",
                "{\"pids\":[\"testDriver\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.parse(Snippets.TEST_DRIVER_DESCRIPTOR),
                self().field("driverChannelDescriptors").arrayItem(0));
        thenResponseElementDoesNotExists(self().field("driverChannelDescriptors").arrayItem(1));
    }

    @Test
    public void getDriverChannelDescriptorsByPidNotFound() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        whenRequestIsPerformed(new MethodSpec("POST"), "/metadata/drivers/channelDescriptors/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{}");
    }

    @Test
    public void getAssetChannelDescriptor() {
        whenRequestIsPerformed(new MethodSpec("GET"), "/metadata/assets/channelDescriptor");

        thenRequestSucceeds();
        thenResponseElementIs(Json.parse(Snippets.BASE_CHANNEL_DESCRIPTOR), self().field("assetChannelDescriptor"));
    }

    @Test
    public void getAssetPidsNoAssets() {
        givenNoFactoryComponentsWithPid(TEST_ASSET_PID);

        whenRequestIsPerformed(new MethodSpec("GET"), "/assets/pids");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson("{\"components\":[]}");
    }

    @Test
    public void getAssetPidsSingleAsset() {
        givenFactoryComponent(TEST_DRIVER_PID, TEST_DRIVER_FACTORY_PID, Collections.emptyMap());
        givenFactoryComponent(TEST_ASSET_PID, WIRE_ASSET_FACTORY_PID,
                Collections.singletonMap("driver.pid", TEST_DRIVER_PID), SelfConfiguringComponent.class);

        whenRequestIsPerformed(new MethodSpec("GET"), "/assets/pids");

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(
                "{\"components\":[{\"pid\":\"testAsset\",\"factoryPid\":\"org.eclipse.kura.wire.WireAsset\"}]}");
    }

    private final WireGraphService wireGraphService;
    private final ConfigurationService configurationService;
    private final Map<String, CompletableFuture<Void>> deleteTrackers = new HashMap<>();

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport("wire/v1"), new MqttTransport("WIRE-V1"));
    }

    public WireGraphRestServiceTest(final Transport transport) throws InterruptedException, ExecutionException,
            TimeoutException, KuraException, InvalidSyntaxException, IOException {
        super(transport);
        this.wireGraphService = ServiceUtil.trackService(WireGraphService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);
        this.configurationService = ServiceUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);
    }

    private void givenEmptyWireGraph() {
        try {

            final CompletableFuture<Void> wireGraphServiceUpdate = WireTestUtil
                    .modified("(kura.service.pid=org.eclipse.kura.wire.graph.WireGraphService)");

            wireGraphService.delete();

            wireGraphServiceUpdate.get(30, TimeUnit.SECONDS);

        } catch (final Exception e) {
            fail("failed to delete existing graph: " + e);
        }
    }

    private void givenWireGraphWith(final WireGraphCustomizer... customizers) {
        final GraphBuilder builder = new GraphBuilder();

        for (final WireGraphCustomizer customizer : customizers) {
            customizer.apply(builder);
        }

        try {
            builder.replaceExistingGraph(FrameworkUtil.getBundle(WireGraphRestServiceTest.class).getBundleContext(),
                    wireGraphService).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("failed to update wire graph " + e);
        }
    }

    private void givenDeleteTrackerForPid(final String pid) {
        try {
            this.deleteTrackers.put(pid, ServiceUtil.removed("(kura.service.pid=" + pid + ")"));
        } catch (Exception e) {
            fail("failed to setup delete tracker for pid " + pid);
        }
    }

    private void givenNoFactoryComponentsWithPid(final String pid) {

        try {
            this.configurationService.deleteFactoryConfiguration(pid, true);
        } catch (KuraException e) {
            fail("failed to delete factory configuration for pid: " + pid);
        }

    }

    private void givenFactoryComponent(final String pid, final String factoryPid,
            final Map<String, Object> properties) {

        givenFactoryComponent(pid, factoryPid, properties, ConfigurableComponent.class);
    }

    private void givenFactoryComponent(final String pid, final String factoryPid, final Map<String, Object> properties,
            final Class<?> serviceInterface) {

        givenNoFactoryComponentsWithPid(pid);

        try {
            ServiceUtil.createFactoryConfiguration(configurationService, serviceInterface, pid, factoryPid, properties)
                    .get(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            fail("failed to create factory configuration for pid " + pid);
        }
    }

    private static WireGraphCustomizer testEmitterReceiver(final String pid) {
        return builder -> builder.addTestEmitterReceiver(pid);
    }

    private static WireGraphCustomizer wireComponent(final String pid, final String factoryPid) {
        return builder -> builder.addWireComponent(pid, factoryPid);
    }

    private static WireGraphCustomizer wire(final String emitterPid, final String receiverPid) {
        return builder -> builder.addWire(emitterPid, receiverPid);
    }

    private interface WireGraphCustomizer {

        void apply(GraphBuilder builder);
    }

    private void thenWireGraphIsEmpty() {
        try {
            final WireGraphConfiguration config = this.wireGraphService.get();
            assertEquals(0, config.getWireComponentConfigurations().size());
            assertEquals(0, config.getWireConfigurations().size());

        } catch (final Exception e) {
            fail("failed to verify that the graph is empty " + e);
        }
    }

    private void thenCurrentGraphContainsComponent(final String componentPid) {
        final WireGraphConfiguration config = expectCurrentWireGraphConfiguration();

        assertTrue("graph does not contain component with pid " + componentPid, config.getWireComponentConfigurations()
                .stream().map(c -> c.getConfiguration().getPid()).anyMatch(componentPid::equals));
    }

    private void thenWireComponentPropertyEquals(final String pid, final String propertyKey,
            final Object expectedValue) {
        final WireComponentConfiguration wireComponentConfig = expectWireComponentConfiguration(pid);

        assertEquals(expectedValue, wireComponentConfig.getProperties().get(propertyKey));
    }

    private void thenComponentConfigurationEquals(final String pid, final String propertyKey,
            final Object expectedValue) {
        try {
            ServiceUtil.trackService("*", Optional.of("(kura.service.pid=" + pid + ")")).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("component with pid " + pid + " cannot be found");
        }

        try {
            assertEquals(expectedValue,
                    configurationService.getComponentConfiguration(pid).getConfigurationProperties().get(propertyKey));
        } catch (KuraException e) {
            fail("failed to get component configuration for " + pid);
        }
    }

    private void thenWireComponentConfigurationPropertyEquals(final String pid, final String propertyKey,
            final Object expectedValue) {

        final WireComponentConfiguration wireComponentConfig = expectWireComponentConfiguration(pid);

        assertEquals(expectedValue,
                wireComponentConfig.getConfiguration().getConfigurationProperties().get(propertyKey));
    }

    private void thenCurrentGraphDoesNotContainsComponent(final String componentPid) {
        final WireGraphConfiguration config = expectCurrentWireGraphConfiguration();

        assertTrue("graph does not contain component with pid " + componentPid, config.getWireComponentConfigurations()
                .stream().map(c -> c.getConfiguration().getPid()).noneMatch(componentPid::equals));
    }

    private void thenCurrentGraphContainsWire(final String emitterPid, final String receiverPid) {
        final WireGraphConfiguration config = expectCurrentWireGraphConfiguration();

        assertTrue("graph does not contain a wire from " + emitterPid + " to " + receiverPid,
                config.getWireConfigurations().stream()
                        .anyMatch(c -> emitterPid.equals(c.getEmitterPid()) && receiverPid.equals(c.getReceiverPid())));
    }

    private void thenComponentExists(final String pid, final Class<?> clazz) {
        try {
            WireTestUtil.trackService(clazz, Optional.of("(kura.service.pid=" + pid + ")")).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("failed to track component with pid " + pid);
        }
    }

    private void thenTrackedComponentIsDeleted(final String pid) {
        final CompletableFuture<Void> tracker = Optional.ofNullable(this.deleteTrackers.remove(pid))
                .orElseThrow(() -> new IllegalStateException("component " + pid + " is not tracked for deletion"));

        try {
            tracker.get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("component with pid " + pid + " has not been deleted");
        }
    }

    private WireGraphConfiguration expectCurrentWireGraphConfiguration() {
        try {
            return this.wireGraphService.get();
        } catch (final Exception e) {
            fail("failed to get current wire graph " + e);
            throw new IllegalStateException();
        }
    }

    private WireComponentConfiguration expectWireComponentConfiguration(final String pid) {
        final WireGraphConfiguration config = expectCurrentWireGraphConfiguration();

        return config.getWireComponentConfigurations().stream().filter(c -> pid.equals(c.getConfiguration().getPid()))
                .findFirst().orElseThrow(() -> new IllegalStateException("component with pid " + pid + " not found"));
    }

    private void thenResponseElementIs(final JsonValue expected, final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));

        final JsonValue actual = applyProjection(root, projection);

        assertEquals("after applying " + projection + " to " + root, expected, actual);
    }

    private void thenResponseElementExists(final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));

        final JsonValue actual = applyProjection(root, projection);

        assertNotNull("element " + projection + " does not exist in " + root, actual);
    }

    private void thenResponseElementDoesNotExists(final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));

        final JsonValue actual = applyProjection(root, projection);

        assertNull("element " + projection + " does not exist in " + root, actual);
    }

    private JsonValue applyProjection(final JsonValue root, final JsonProjection projection) {

        try {
            return projection.apply(root);
        } catch (final Exception e) {
            fail("failed to apply " + projection + " to " + root);
            throw new IllegalStateException("unreachable");
        }
    }

}
