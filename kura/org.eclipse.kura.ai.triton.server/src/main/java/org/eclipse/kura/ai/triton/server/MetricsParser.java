/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetricsParser {

    private final List<String> rawMetrics;

    public MetricsParser(List<String> metrics) {
        this.rawMetrics = metrics;
    }

    /*
     * # HELP nv_inference_compute_output_duration_us Cumulative inference compute output duration in microseconds (does
     * not include cached requests)
     * # TYPE nv_inference_compute_output_duration_us counter
     * nv_inference_compute_output_duration_us{model="preprocessor",version="1"} 851348
     * nv_inference_compute_output_duration_us{model="identity_long",version="1"} 0
     * <metric-name>{model=<model_name>,version=<version>} <value>
     * 
     * nv_inference_request_failure{model="preprocessor",reason="OTHER",version="1"} 0
     * nv_inference_request_failure{model="preprocessor",reason="BACKEND",version="1"} 0
     * nv_inference_request_failure{model="preprocessor",reason="CANCELED",version="1"} 0
     * nv_inference_request_failure{model="preprocessor",reason="REJECTED",version="1"} 0
     * nv_inference_request_failure{model="identity_long",reason="OTHER",version="1"} 0
     * nv_inference_request_failure{model="identity_long",reason="CANCELED",version="1"} 0
     * nv_inference_request_failure{model="identity_long",reason="BACKEND",version="1"} 0
     * nv_inference_request_failure{model="identity_long",reason="REJECTED",version="1"} 0
     * <metric-name>{model=<model_name>,reason=<reason>,version=<version>} <value>
     * 
     * nv_pinned_memory_pool_used_bytes 0
     * <metric-name> <value>
     */
    public Map<String, String> parse() {
        Map<String, String> metrics = new HashMap<>();
        rawMetrics.stream().filter(line -> !line.startsWith("#")).forEach(line -> {
            String[] elements = line.split("\\s+");
            // key: <metric_name>-<model-name>-<version>
            // key: <metric_name>-<model-name>-<reason>-<version>
            // key: <metric_name>
            // value: <value>
            Optional<String> key = parseKey(elements);
            String value = parseValue(elements);
            if (key.isPresent()) {
                metrics.put(key.get(), value);
            }
        });
        return metrics;
    }

    private Optional<String> parseKey(String[] elements) {
        Optional<String> key = Optional.empty();
        if (elements.length < 1) {
            return key;
        }

        StringBuilder keyBuilder = new StringBuilder();
        String[] metricElements = elements[0].split("\\{");
        keyBuilder.append(metricElements[0]);
        if (metricElements.length > 1) {
            String[] configs = metricElements[1].replace("\"", "").replace("}", "").split(",");
            for (String config : configs) {
                keyBuilder.append("-").append(config.split("=")[1]);
            }
        }
        return Optional.of(keyBuilder.toString());
    }

    private String parseValue(String[] elements) {
        String value = "";
        if (elements.length >= 2) {
            value = elements[1];
        }
        return value;
    }

    // private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
    // private final String rawMetrics;
    //
    // public MetricsParser(String metrics) {
    // this.rawMetrics = metrics;
    // }
    //
    // /*
    // * # HELP nv_inference_compute_output_duration_us Cumulative inference compute output duration in microseconds
    // (does
    // * not include cached requests)
    // * # TYPE nv_inference_compute_output_duration_us counter
    // * nv_inference_compute_output_duration_us{model="preprocessor",version="1"} 851348
    // * nv_inference_compute_output_duration_us{model="identity_long",version="1"} 0
    // * <metric-name>{model=<model_name>,version=<version>} <value>
    // *
    // * nv_inference_request_failure{model="preprocessor",reason="OTHER",version="1"} 0
    // * nv_inference_request_failure{model="preprocessor",reason="BACKEND",version="1"} 0
    // * nv_inference_request_failure{model="preprocessor",reason="CANCELED",version="1"} 0
    // * nv_inference_request_failure{model="preprocessor",reason="REJECTED",version="1"} 0
    // * nv_inference_request_failure{model="identity_long",reason="OTHER",version="1"} 0
    // * nv_inference_request_failure{model="identity_long",reason="CANCELED",version="1"} 0
    // * nv_inference_request_failure{model="identity_long",reason="BACKEND",version="1"} 0
    // * nv_inference_request_failure{model="identity_long",reason="REJECTED",version="1"} 0
    // * <metric-name>{model=<model_name>,reason=<reason>,version=<version>} <value>
    // *
    // * nv_pinned_memory_pool_used_bytes 0
    // * <metric-name> <value>
    // */
    // public Map<String, String> parse() {
    // Map<String, String> metrics = new HashMap<>();
    // Stream<String> lines = NEWLINE_PATTERN.splitAsStream(rawMetrics);
    // lines.filter(line -> !line.startsWith("#")).forEach(line -> {
    // String[] elements = line.split("\\s+");
    // // key: <metric_name>-<model-name>-<version>
    // // key: <metric_name>-<model-name>-<reason>-<version>
    // // key: <metric_name>
    // // value: <value>
    // Optional<String> key = parseKey(elements);
    // String value = parseValue(elements);
    // if (key.isPresent()) {
    // metrics.put(key.get(), value);
    // }
    // });
    // return metrics;
    // }
    //
    // private Optional<String> parseKey(String[] elements) {
    // Optional<String> key = Optional.empty();
    // if (elements.length < 1) {
    // return key;
    // }
    //
    // StringBuilder keyBuilder = new StringBuilder();
    // String[] metricElements = elements[0].split("\\{");
    // keyBuilder.append(metricElements[0]);
    // if (metricElements.length > 1) {
    // String[] configs = metricElements[1].replace("\"", "").replace("}", "").split(",");
    // for (String config : configs) {
    // keyBuilder.append("-").append(config.split("=")[1]);
    // }
    // }
    // return Optional.of(keyBuilder.toString());
    // }
    //
    // private String parseValue(String[] elements) {
    // String value = "";
    // if (elements.length >= 2) {
    // value = elements[1];
    // }
    // return value;
    // }
}
