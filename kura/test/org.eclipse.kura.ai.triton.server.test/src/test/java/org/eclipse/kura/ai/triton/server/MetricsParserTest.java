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

    private static final String GPU_METRIC = //
            "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
                    + "# TYPE nv_gpu_utilization gauge\n" //
                    + "nv_gpu_utilization{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 0.000000\n" //
                    + "# HELP nv_gpu_memory_total_bytes GPU total memory, in bytes\n" //
                    + "# TYPE nv_gpu_memory_total_bytes gauge\n" //
                    + "nv_gpu_memory_total_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 16101933056.000000\n" //
                    + "# HELP nv_gpu_memory_used_bytes GPU used memory, in bytes\n"
                    + "# TYPE nv_gpu_memory_used_bytes gauge\n" //
                    + "nv_gpu_memory_used_bytes{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 617611264.000000\n" //
                    + "# HELP nv_gpu_power_usage GPU power usage in watts\n" + "# TYPE nv_gpu_power_usage gauge\n" //
                    + "nv_gpu_power_usage{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 20.085000\n" //
                    + "# HELP nv_gpu_power_limit GPU power management limit in watts\n"
                    + "# TYPE nv_gpu_power_limit gauge"; //
    private static final String SINGLE_GPU_METRIC = //
            "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
                    + "# TYPE nv_gpu_utilization gauge\n" //
                    + "nv_gpu_utilization{gpu_uuid=\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"} 0.000000"; //
    private static final String EXPECTED_SINGLE_GPU_METRIC = //
            "{\"gpu_uuid\":\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\",\"gpu_stats\":{\"nv_gpu_utilization\":\"0.000000\"}}";

    @Test
    public void shouldParseSingleGpuMetric() {
        givenMetricsParser(Arrays.asList(SINGLE_GPU_METRIC.split("\n")));

        whenParse();

        thenSingleMetricIsParsed("GPU-340cec52-80ba-c0df-8511-5f9680aae0ed");
        thenSingleMetricIs("GPU-340cec52-80ba-c0df-8511-5f9680aae0ed", EXPECTED_SINGLE_GPU_METRIC);
    }

    private void givenMetricsParser(List<String> metrics) {
        this.metricsParser = new GpuMetricsParser(metrics);
    }

    private void whenParse() {
        this.metricsMap = this.metricsParser.parse();
    }

    private void thenSingleMetricIsParsed(String expectedMetricName) {
        assertFalse(this.metricsMap.isEmpty());
        assertTrue(this.metricsMap.containsKey(expectedMetricName));
    }

    private void thenSingleMetricIs(String expectedMetricName, String expectedMetric) {
        System.out.println(this.metricsMap);
        assertEquals(expectedMetric.trim().replace("\n", ""), this.metricsMap.get(expectedMetricName));
    }
}
