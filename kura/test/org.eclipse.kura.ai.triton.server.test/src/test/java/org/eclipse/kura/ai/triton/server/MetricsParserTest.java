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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class MetricsParserTest {

    private GpuMetricsParser metricsParser;
    private Map<String, String> metricsMap;

    private static final String GPU_METRIC = "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
            + "# TYPE nv_gpu_utilization gauge\n" //
            + "nv_gpu_utilization{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 0.000000\n" //
            + "# HELP nv_gpu_memory_total_bytes GPU total memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_total_bytes gauge\n" //
            + "nv_gpu_memory_total_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 16101933056.000000\n" //
            + "# HELP nv_gpu_memory_used_bytes GPU used memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_used_bytes gauge\n" //
            + "nv_gpu_memory_used_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 617611264.000000\n" //
            + "# HELP nv_gpu_power_usage GPU power usage in watts\n" //
            + "# TYPE nv_gpu_power_usage gauge\n" //
            + "nv_gpu_power_usage{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 20.085000\n" //
            + "# HELP nv_gpu_power_limit GPU power management limit in watts\n" //
            + "# TYPE nv_gpu_power_limit gauge\n" //
            + "nv_gpu_power_limit{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 60.000000\n" //
            + "# HELP nv_energy_consumption GPU energy consumption in joules since the Triton Server started\n" //
            + "# TYPE nv_energy_consumption counter\n" //
            + "nv_energy_consumption{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 320.387000\n" //
            + "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
            + "# TYPE nv_gpu_utilization gauge\n" //
            + "nv_gpu_utilization{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 0.000000\n" //
            + "# HELP nv_gpu_memory_total_bytes GPU total memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_total_bytes gauge\n" //
            + "nv_gpu_memory_total_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 16101933056.000000\n" //
            + "# HELP nv_gpu_memory_used_bytes GPU used memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_used_bytes gauge\n" //
            + "nv_gpu_memory_used_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 617611264.000000\n" //
            + "# HELP nv_gpu_power_usage GPU power usage in watts\n" //
            + "# TYPE nv_gpu_power_usage gauge\n" //
            + "nv_gpu_power_usage{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 20.085000\n" //
            + "# HELP nv_gpu_power_limit GPU power management limit in watts\n" //
            + "# TYPE nv_gpu_power_limit gauge\n" //
            + "nv_gpu_power_limit{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 60.000000\n" //
            + "# HELP nv_energy_consumption GPU energy consumption in joules since the Triton Server started\n" //
            + "# TYPE nv_energy_consumption counter\n" //
            + "nv_energy_consumption{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 320.387000\n";
    private static final String SINGLE_GPU_METRIC = "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
            + "# TYPE nv_gpu_utilization gauge\n" //
            + "nv_gpu_utilization{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 0.000000\n" //
            + "# HELP nv_gpu_memory_total_bytes GPU total memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_total_bytes gauge\n" //
            + "nv_gpu_memory_total_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 16101933056.000000\n" //
            + "# HELP nv_gpu_memory_used_bytes GPU used memory, in bytes\n" //
            + "# TYPE nv_gpu_memory_used_bytes gauge\n" //
            + "nv_gpu_memory_used_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 617611264.000000\n" //
            + "# HELP nv_gpu_power_usage GPU power usage in watts\n" //
            + "# TYPE nv_gpu_power_usage gauge\n" //
            + "nv_gpu_power_usage{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 20.085000\n" //
            + "# HELP nv_gpu_power_limit GPU power management limit in watts\n" //
            + "# TYPE nv_gpu_power_limit gauge\n" //
            + "nv_gpu_power_limit{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 60.000000\n" //
            + "# HELP nv_energy_consumption GPU energy consumption in joules since the Triton Server started\n" //
            + "# TYPE nv_energy_consumption counter\n" //
            + "nv_energy_consumption{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 320.387000\n";
    private static final String SINGLE_GPU_METRIC_WITHOUT_UUID = "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
            + "# TYPE nv_gpu_utilization gauge\n" //
            + "nv_gpu_utilization{} 0.000000\n";
    private static final String SINGLE_GPU_METRIC_WITHOUT_NAME = "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
            + "# TYPE nv_gpu_utilization gauge\n" //
            + "{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\"} 0.000000\n";
    private static final String EXPECTED_GPU_METRIC_1 = "{\"gpu_uuid\":\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"," //
            + "\"gpu_stats\":{\"nv_gpu_power_limit\":\"60.000000\",\"nv_gpu_utilization\":\"0.000000\",\"nv_gpu_memory_used_bytes\":\"617611264.000000\"," //
            + "\"nv_gpu_power_usage\":\"20.085000\",\"nv_gpu_memory_total_bytes\":\"16101933056.000000\"}}";
    private static final String EXPECTED_GPU_METRIC_2 = "{\"gpu_uuid\":\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\",\"gpu_stats\":{\"nv_gpu_power_limit\":\"60.000000\"," //
            + "\"nv_gpu_utilization\":\"0.000000\",\"nv_gpu_memory_used_bytes\":\"617611264.000000\",\"nv_gpu_power_usage\":\"20.085000\",\"nv_gpu_memory_total_bytes\":\"16101933056.000000\"}}";

    @Test
    public void shouldParseSingleGpuMetric() {
        givenMetricsParser(Arrays.asList(SINGLE_GPU_METRIC.split("\n")));

        whenParse();

        thenMetricIs("gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ff", EXPECTED_GPU_METRIC_2);
    }

    @Test
    public void shouldParseMultipleGpuMetric() {
        givenMetricsParser(Arrays.asList(GPU_METRIC.split("\n")));

        whenParse();

        thenMetricIs("gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ed", EXPECTED_GPU_METRIC_1);
        thenMetricIs("gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ff", EXPECTED_GPU_METRIC_2);
    }

    @Test
    public void shouldNotParseIfUuidIsMissing() {
        givenMetricsParser(Arrays.asList(SINGLE_GPU_METRIC_WITHOUT_UUID.split("\n")));

        whenParse();

        thenMetricsAreEmpty();
    }

    @Test
    public void shouldNotParseIfNameIsMissing() {
        givenMetricsParser(Arrays.asList(SINGLE_GPU_METRIC_WITHOUT_NAME.split("\n")));

        whenParse();

        thenMetricsAreEmpty();
    }

    private void givenMetricsParser(List<String> metrics) {
        this.metricsParser = new GpuMetricsParser(metrics);
    }

    private void whenParse() {
        this.metricsMap = this.metricsParser.parse();
    }

    private void thenMetricIs(String expectedMetricName, String expectedMetric) {
        assertFalse(this.metricsMap.isEmpty());
        assertTrue(this.metricsMap.containsKey(expectedMetricName));
        assertEquals(expectedMetric.trim().replace("\n", ""), this.metricsMap.get(expectedMetricName));
    }

    private void thenMetricsAreEmpty() {
        assertTrue(this.metricsMap.isEmpty());
    }
}
