/*******************************************************************************
 * Copyright (c) 2022, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.junit.After;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TritonServerServiceContainerImplTest extends TritonServerServiceStepDefinitions {

    private static final String TRITON_REPOSITORY_PATH = "/fake-repository-path";
    private static final String TRITON_METRICS_RESPONSE = "# HELP nv_inference_request_success Number of successful inference requests, all batch sizes\n"
            + "# TYPE nv_inference_request_success counter\n" + "nv_gpu_power_usage{version=\"1\"} 67";
    private static final String TRITON_EXPECTED_METRICS = "nv_gpu_power_usage 67";
    private static final String TRITON_STATS_RESPONSE = "{\n" + "    \"model_stats\": [\n" + "        {\n"
            + "            \"name\": \"identity_long\",\n" + "            \"version\": \"1\",\n"
            + "            \"last_inference\": 1739438112754,\n" + "            \"inference_count\": 898,\n"
            + "            \"execution_count\": 898,\n" + "            \"inference_stats\": {\n"
            + "                \"success\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 656986336\n" + "                },\n" + "                \"fail\": {\n"
            + "                    \"count\": 0,\n" + "                    \"ns\": 0\n" + "                },\n"
            + "                \"queue\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 185327136\n" + "                },\n"
            + "                \"compute_input\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 12632384\n" + "                },\n"
            + "                \"compute_infer\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 108874496\n" + "                },\n"
            + "                \"compute_output\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 115616\n" + "                },\n" + "                \"cache_hit\": {\n"
            + "                    \"count\": 0,\n" + "                    \"ns\": 0\n" + "                },\n"
            + "                \"cache_miss\": {\n" + "                    \"count\": 0,\n"
            + "                    \"ns\": 0\n" + "                }\n" + "            },\n"
            + "            \"response_stats\": {},\n" + "            \"batch_stats\": [\n" + "                {\n"
            + "                    \"batch_size\": 0,\n" + "                    \"compute_input\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 12632384\n"
            + "                    },\n" + "                    \"compute_infer\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 108874496\n"
            + "                    },\n" + "                    \"compute_output\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 115616\n"
            + "                    }\n" + "                }\n" + "            ],\n"
            + "            \"memory_usage\": []\n" + "        },\n" + "        {\n"
            + "            \"name\": \"preprocessor\",\n" + "            \"version\": \"1\",\n"
            + "            \"last_inference\": 1739438112750,\n" + "            \"inference_count\": 898,\n"
            + "            \"execution_count\": 898,\n" + "            \"inference_stats\": {\n"
            + "                \"success\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 3050715552\n" + "                },\n" + "                \"fail\": {\n"
            + "                    \"count\": 0,\n" + "                    \"ns\": 0\n" + "                },\n"
            + "                \"queue\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 228940352\n" + "                },\n"
            + "                \"compute_input\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 277586752\n" + "                },\n"
            + "                \"compute_infer\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 1936970560\n" + "                },\n"
            + "                \"compute_output\": {\n" + "                    \"count\": 898,\n"
            + "                    \"ns\": 581808512\n" + "                },\n" + "                \"cache_hit\": {\n"
            + "                    \"count\": 0,\n" + "                    \"ns\": 0\n" + "                },\n"
            + "                \"cache_miss\": {\n" + "                    \"count\": 0,\n"
            + "                    \"ns\": 0\n" + "                }\n" + "            },\n"
            + "            \"response_stats\": {},\n" + "            \"batch_stats\": [\n" + "                {\n"
            + "                    \"batch_size\": 1,\n" + "                    \"compute_input\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 277586752\n"
            + "                    },\n" + "                    \"compute_infer\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 1936970560\n"
            + "                    },\n" + "                    \"compute_output\": {\n"
            + "                        \"count\": 898,\n" + "                        \"ns\": 581808512\n"
            + "                    }\n" + "                }\n" + "            ],\n"
            + "            \"memory_usage\": []\n" + "        }\n" + "    ]\n" + "}";
    private static final String TRITON_EXPECTED_STATS = "<kura.service.pid>.identity_long {\n"
            + "    \"name\": \"identity_long\",\n" + "    \"version\": \"1\",\n"
            + "    \"last_inference\": 1739438112754,\n" + "    \"inference_count\": 898,\n"
            + "    \"execution_count\": 898,\n" + "    \"inference_stats\": {\n" + "        \"success\": {\n"
            + "            \"count\": 898,\n" + "            \"ns\": 656986336\n" + "        },\n"
            + "        \"fail\": {\n" + "            \"count\": 0,\n" + "            \"ns\": 0\n" + "        },\n"
            + "        \"queue\": {\n" + "            \"count\": 898,\n" + "            \"ns\": 185327136\n"
            + "        },\n" + "        \"compute_input\": {\n" + "            \"count\": 898,\n"
            + "            \"ns\": 12632384\n" + "        },\n" + "        \"compute_infer\": {\n"
            + "            \"count\": 898,\n" + "            \"ns\": 108874496\n" + "        },\n"
            + "        \"compute_output\": {\n" + "            \"count\": 898,\n" + "            \"ns\": 115616\n"
            + "        },\n" + "        \"cache_hit\": {\n" + "            \"count\": 0,\n" + "            \"ns\": 0\n"
            + "        },\n" + "        \"cache_miss\": {\n" + "            \"count\": 0,\n" + "            \"ns\": 0\n"
            + "        }\n" + "    },\n" + "    \"response_stats\": {},\n" + "    \"batch_stats\": [\n" + "        {\n"
            + "            \"batch_size\": 0,\n" + "            \"compute_input\": {\n"
            + "                \"count\": 898,\n" + "                \"ns\": 12632384\n" + "            },\n"
            + "            \"compute_infer\": {\n" + "                \"count\": 898,\n"
            + "                \"ns\": 108874496\n" + "            },\n" + "            \"compute_output\": {\n"
            + "                \"count\": 898,\n" + "                \"ns\": 115616\n" + "            }\n"
            + "        }\n" + "    ],\n" + "    \"memory_usage\": []\n" + "}";

    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> metrics;
    private Map<String, String> expectedMetrics = new HashMap<>();
    private HttpServer httpServer;

    @Test
    public void isConfigurationValidWorksWithContainerConfiguration() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(true);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidImage() throws IOException {
        givenPropertyWith("container.image", null);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidImageTag() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", null);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isConfigurationValidWorksWithInvalidModelRepository() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", "");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsConfigurationValidReturns(false);
    }

    @Test
    public void isModelEncryptionEnabledWorkWhenPasswordIsNotSet() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsModelEncryptionEnabled(false);
    }

    @Test
    public void isModelEncryptionEnabledWorksWhenPasswordIsSet() throws IOException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("local.model.repository.password", "keyboards");
        givenTritonServerServiceContainerImpl(this.properties);

        thenIsModelEncryptionEnabled(true);
    }

    @Test
    public void shouldGetEmptyMetricsWhenMetricsAreDisabled() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.FALSE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse(TRITON_METRICS_RESPONSE, 200);
        givenStatsWithResponse(TRITON_STATS_RESPONSE);

        whenMetricsAreRetrieved();

        thenMetricsAreEmpty();
    }

    @Test
    public void shouldGetEmptyMetricsWhenResponseIsNot200() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.FALSE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse(TRITON_METRICS_RESPONSE, 500);
        givenStatsWithResponse(TRITON_STATS_RESPONSE);

        whenMetricsAreRetrieved();

        thenMetricsAreEmpty();
    }

    // Add test for wrong stats...

    @Test
    public void shouldGetMetrics() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse(TRITON_METRICS_RESPONSE, 200);
        givenStatsWithResponse(TRITON_STATS_RESPONSE);
        givenExpectedMetricsAndStats();

        whenMetricsAreRetrieved();

        thenMetricsAre(this.expectedMetrics);
    }

    /*
     * Given
     */
    private void givenPropertyWith(String name, Object value) {
        this.properties.put(name, value);
    }

    private void givenMetricsServerWithResponse(String response, int responseCode) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(4002), 0);
        this.httpServer.createContext("/metrics", new MetricsHandler(response, responseCode));

        this.httpServer.setExecutor(null);
        this.httpServer.start();
    }

    private class MetricsHandler implements HttpHandler {

        private String response = "Standard response.";
        private int responseCode = 200;

        public MetricsHandler(String response, int responseCode) {
            this.response = response;
            this.responseCode = responseCode;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(this.responseCode, this.response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(this.response.getBytes());
            os.close();
        }
    }

    @After
    public void closeHttpServer() {
        if (this.httpServer != null) {
            this.httpServer.stop(10);
        }
    }

    private void givenStatsWithResponse(String response) {
        // do something here...
    }

    private void givenExpectedMetricsAndStats() {
        Arrays.asList(TRITON_EXPECTED_METRICS.split("\n")).stream().forEach(line -> {
            String[] items = line.split(" ");
            this.expectedMetrics.put(items[0], items[1]);
        });
        String key = TRITON_EXPECTED_STATS.split(" ")[0];
        String value = TRITON_EXPECTED_STATS.substring(key.length());
        this.expectedMetrics.put(key, value);
    }

    /*
     * When
     */
    private void whenMetricsAreRetrieved() throws KuraException {
        this.metrics = this.tritonServerService.getMetrics();
    }

    /*
     * Then
     */
    private void thenIsConfigurationValidReturns(boolean expectedValue) {
        assertEquals(expectedValue, this.tritonServerService.isConfigurationValid());
    }

    private void thenIsModelEncryptionEnabled(boolean expectedValue) {
        assertEquals(expectedValue, this.tritonServerService.isModelEncryptionEnabled());
    }

    private void thenMetricsAreEmpty() {
        assertNotNull(this.metrics);
        assertTrue(this.metrics.isEmpty());
    }

    private void thenMetricsAre(Map<String, String> expectedValue) {
        assertNotNull(this.metrics);
        assertEquals(expectedValue, this.metrics);
    }
}
