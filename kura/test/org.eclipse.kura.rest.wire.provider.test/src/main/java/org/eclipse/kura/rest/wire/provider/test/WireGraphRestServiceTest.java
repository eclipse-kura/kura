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

import static org.junit.Assert.assertEquals;
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

@RunWith(Parameterized.class)
public class WireGraphRestServiceTest extends AbstractRequestHandlerTest {

    @Test
    public void getEmptyGraph() {
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("GET"), "/graph");

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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph",
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
        givenEmptyWireGraph();

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" + "  \"configs\": [\n" + "    {\n"
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" + "  \"configs\": [\n" + "    {\n"
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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
        givenNoFactoryComponentsWithPid("otherComponent");

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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
                + "      \"pid\": \"otherComponent\",\n" //
                + "      \"properties\": {\n" //
                + "        \"db.server.enabled\": {\n" //
                + "          \"value\": false,\n" //
                + "          \"type\": \"BOOLEAN\"\n" //
                + "        },\n" //
                + "        \"db.server.commandline\": {\n" //
                + "          \"value\": \"foo bar baz\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.core.db.H2DbServer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenComponentConfigurationEquals("otherComponent", "db.server.commandline", "foo bar baz");
    }

    @Test
    public void updateNonGraphFactoryConfiguration() {
        givenEmptyWireGraph();
        givenFactoryComponent("otherComponent", "org.eclipse.kura.core.db.H2DbServer",
                Collections.singletonMap("db.server.commandline", "bar"));

        whenRequestIsPerformed(new MethodSpec("PUT"), "/graph", "{\n" //
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
                + "      \"pid\": \"otherComponent\",\n" //
                + "      \"properties\": {\n" //
                + "        \"db.server.enabled\": {\n" //
                + "          \"value\": false,\n" //
                + "          \"type\": \"BOOLEAN\"\n" //
                + "        },\n" //
                + "        \"db.server.commandline\": {\n" //
                + "          \"value\": \"foo bar baz\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        },\n" //
                + "        \"service.factoryPid\": {\n" //
                + "          \"value\": \"org.eclipse.kura.core.db.H2DbServer\",\n" //
                + "          \"type\": \"STRING\"\n" //
                + "        }\n" //
                + "      }\n" //
                + "    }" //
                + "  ]\n" //
                + "}"); //

        thenRequestSucceeds();
        thenComponentConfigurationEquals("otherComponent", "db.server.commandline", "foo bar baz");
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
            this.configurationService.deleteFactoryConfiguration(pid, false);
        } catch (KuraException e) {
            fail("failed to delete factory configuration for pid: " + pid);
        }
    }

    private void givenFactoryComponent(final String pid, final String factoryPid,
            final Map<String, Object> properties) {

        givenNoFactoryComponentsWithPid(pid);

        try {
            ServiceUtil.createFactoryConfiguration(configurationService, ConfigurableComponent.class, pid, factoryPid,
                    properties).get(30, TimeUnit.SECONDS);
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
            ServiceUtil.trackService(ConfigurableComponent.class, Optional.of("(kura.service.pid=" + pid + ")")).get(30,
                    TimeUnit.SECONDS);
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

}
