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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GpuMetricsParser {

    private final List<String> rawMetrics;
    private final Map<String, GpuMetrics> gpuMetricsMap = new HashMap<>();
    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final Gson gson = gsonBuilder.create();

    public GpuMetricsParser(List<String> metrics) {
        this.rawMetrics = metrics;
    }

    /**
     * Parse the gpu metrics provided by a Triton Server. An example of metrics are the following:
     * 
     * # HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)
     * # TYPE nv_gpu_utilization gauge
     * nv_gpu_utilization{gpu_uuid="GPU-340cec52-80ba-c0df-8511-5f9680aae0ed"} 0.000000
     * # HELP nv_gpu_memory_total_bytes GPU total memory, in bytes
     * # TYPE nv_gpu_memory_total_bytes gauge
     * nv_gpu_memory_total_bytes{gpu_uuid="GPU-340cec52-80ba-c0df-8511-5f9680aae0ed"} 16101933056.000000
     * # HELP nv_gpu_memory_used_bytes GPU used memory, in bytes
     * # TYPE nv_gpu_memory_used_bytes gauge
     * nv_gpu_memory_used_bytes{gpu_uuid="GPU-340cec52-80ba-c0df-8511-5f9680aae0ed"} 617611264.000000
     * 
     * The lines beginning with a # are filtered. The metric name is the 'gpu_uuid' field.
     * The value is converted in json format. For example:
     * 
     * <pre>
     * {
     *     "gpu_uuid" : "GPU-340cec52-80ba-c0df-8511-5f9680aae0ed",
     *     "gpu_stats" : {
     *         "gpu_utilization": "0.000000"
     *     }
     * }
     * </pre>
     */
    public Map<String, String> parse() {
        Map<String, String> metrics = new HashMap<>();
        rawMetrics.stream().filter(
                line -> !line.startsWith("#") && (line.contains("_gpu_") || line.equals("nv_energy_consumption")))
                .forEach(line -> {
                    Optional<String> uuid = parseUuid(line);
                    Optional<String> name = parseName(line);
                    String value = parseValue(line);
                    if (!uuid.isPresent() || !name.isPresent()) {
                        return;
                    }
                    if (gpuMetricsMap.containsKey(uuid.get())) {
                        this.gpuMetricsMap.get(uuid.get()).addGpuMetric(name.get(), value);
                    } else {
                        GpuMetrics gpuMetrics = new GpuMetrics(uuid.get());
                        gpuMetrics.addGpuMetric(name.get(), value);
                        this.gpuMetricsMap.put(uuid.get(), gpuMetrics);
                    }
                });
        this.gpuMetricsMap.forEach((key, value) -> metrics.put("gpu.metrics." + key, gson.toJson(value)));
        return metrics;
    }

    private Optional<String> parseName(String line) {
        Optional<String> name = Optional.empty();
        String[] elements = line.split("\\{");
        if (elements.length >= 1) {
            name = Optional.of(elements[0]);
        }
        return name;
    }

    private Optional<String> parseUuid(String line) {
        Optional<String> uuid = Optional.empty();
        String[] elements = line.split("gpu_uuid=\"");
        if (elements.length >= 2) {
            String subElement = elements[1].split("\"}")[0];
            if (subElement != null && !subElement.isEmpty()) {
                uuid = Optional.of(subElement);
            }
        }
        return uuid;
    }

    private String parseValue(String line) {
        String value = "";
        String[] elements = line.split("\\s+");
        if (elements.length >= 2) {
            value = elements[1];
        }
        return value;
    }

}
