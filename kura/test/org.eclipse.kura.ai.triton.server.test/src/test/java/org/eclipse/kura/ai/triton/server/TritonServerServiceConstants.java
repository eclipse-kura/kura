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

public class TritonServerServiceConstants {

    protected static final String TRITON_METRICS_RESPONSE = //
            "# HELP nv_inference_request_success Number of successful inference requests, all batch sizes\n" //
                    + "# TYPE nv_inference_request_success counter\n" //
                    + "nv_inference_request_success{model=\"identity_long\",version=\"1\"} 29\n" //
                    + "nv_inference_request_success{model=\"preprocessor\",version=\"1\"} 29\n" //
                    + "# HELP nv_inference_request_failure Number of failed inference requests, all batch sizes\n" //
                    + "# TYPE nv_inference_request_failure counter\n" //
                    + "nv_inference_request_failure{model=\"identity_long\",reason=\"OTHER\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"identity_long\",reason=\"CANCELED\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"identity_long\",reason=\"BACKEND\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"identity_long\",reason=\"REJECTED\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"preprocessor\",reason=\"OTHER\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"preprocessor\",reason=\"BACKEND\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"preprocessor\",reason=\"CANCELED\",version=\"1\"} 0\n" //
                    + "nv_inference_request_failure{model=\"preprocessor\",reason=\"REJECTED\",version=\"1\"} 0\n" //
                    + "# HELP nv_inference_count Number of inferences performed (does not include cached requests)\n" //
                    + "# TYPE nv_inference_count counter\n" //
                    + "nv_inference_count{model=\"identity_long\",version=\"1\"} 29\n" //
                    + "nv_inference_count{model=\"preprocessor\",version=\"1\"} 29\n" //
                    + "# HELP nv_inference_exec_count Number of model executions performed (does not include cached requests)\n" //
                    + "# TYPE nv_inference_exec_count counter\n" //
                    + "nv_inference_exec_count{model=\"identity_long\",version=\"1\"} 29\n" //
                    + "nv_inference_exec_count{model=\"preprocessor\",version=\"1\"} 29\n" //
                    + "# HELP nv_inference_request_duration_us Cumulative inference request duration in microseconds (includes cached requests)\n" //
                    + "# TYPE nv_inference_request_duration_us counter\n" //
                    + "nv_inference_request_duration_us{model=\"identity_long\",version=\"1\"} 20944\n" //
                    + "nv_inference_request_duration_us{model=\"preprocessor\",version=\"1\"} 240737\n" //
                    + "# HELP nv_inference_queue_duration_us Cumulative inference queuing duration in microseconds (includes cached requests)\n" //
                    + "# TYPE nv_inference_queue_duration_us counter\n" //
                    + "nv_inference_queue_duration_us{model=\"identity_long\",version=\"1\"} 4776\n" //
                    + "nv_inference_queue_duration_us{model=\"preprocessor\",version=\"1\"} 5656\n" //
                    + "# HELP nv_inference_compute_input_duration_us Cumulative compute input duration in microseconds (does not include cached requests)\n" //
                    + "# TYPE nv_inference_compute_input_duration_us counter\n" //
                    + "nv_inference_compute_input_duration_us{model=\"identity_long\",version=\"1\"} 357\n" //
                    + "nv_inference_compute_input_duration_us{model=\"preprocessor\",version=\"1\"} 7734\n" //
                    + "# HELP nv_inference_compute_infer_duration_us Cumulative compute inference duration in microseconds (does not include cached requests)\n" //
                    + "# TYPE nv_inference_compute_infer_duration_us counter\n" //
                    + "nv_inference_compute_infer_duration_us{model=\"identity_long\",version=\"1\"} 3222\n" //
                    + "nv_inference_compute_infer_duration_us{model=\"preprocessor\",version=\"1\"} 208308\n" //
                    + "# HELP nv_inference_compute_output_duration_us Cumulative inference compute output duration in microseconds (does not include cached requests)\n" //
                    + "# TYPE nv_inference_compute_output_duration_us counter\n" //
                    + "nv_inference_compute_output_duration_us{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_inference_compute_output_duration_us{model=\"preprocessor\",version=\"1\"} 18288\n" //
                    + "# HELP nv_cache_num_hits_per_model Number of cache hits per model\n" //
                    + "# TYPE nv_cache_num_hits_per_model counter\n" //
                    + "nv_cache_num_hits_per_model{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_cache_num_hits_per_model{model=\"preprocessor\",version=\"1\"} 0\n" //
                    + "# HELP nv_cache_hit_duration_per_model Total cache hit duration per model, in microseconds\n" //
                    + "# TYPE nv_cache_hit_duration_per_model counter\n" //
                    + "nv_cache_hit_duration_per_model{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_cache_hit_duration_per_model{model=\"preprocessor\",version=\"1\"} 0\n" //
                    + "# HELP nv_cache_num_misses_per_model Number of cache misses per model\n" //
                    + "# TYPE nv_cache_num_misses_per_model counter\n" //
                    + "nv_cache_num_misses_per_model{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_cache_num_misses_per_model{model=\"preprocessor\",version=\"1\"} 0\n" //
                    + "# HELP nv_cache_miss_duration_per_model Total cache miss (insert+lookup) duration per model, in microseconds\n" //
                    + "# TYPE nv_cache_miss_duration_per_model counter\n" //
                    + "nv_cache_miss_duration_per_model{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_cache_miss_duration_per_model{model=\"preprocessor\",version=\"1\"} 0\n" //
                    + "# HELP input_byte_size_counter Cumulative input byte size of all requests received by the model\n" //
                    + "# TYPE input_byte_size_counter counter\n" //
                    + "input_byte_size_counter{model=\"identity_long\",version=\"1\"} 232\n" //
                    + "# HELP nv_inference_pending_request_count Instantaneous number of pending requests awaiting execution per-model.\n" //
                    + "# TYPE nv_inference_pending_request_count gauge\n" //
                    + "nv_inference_pending_request_count{model=\"identity_long\",version=\"1\"} 0\n" //
                    + "nv_inference_pending_request_count{model=\"preprocessor\",version=\"1\"} 0\n" //
                    + "# HELP nv_model_load_duration_secs Model load time in seconds\n" //
                    + "# TYPE nv_model_load_duration_secs gauge\n" //
                    + "nv_model_load_duration_secs{model=\"identity_long\",version=\"1\"} 0.002341632\n" //
                    + "nv_model_load_duration_secs{model=\"preprocessor\",version=\"1\"} 1.618407264\n" //
                    + "# HELP nv_pinned_memory_pool_total_bytes Pinned memory pool total memory size, in bytes\n" //
                    + "# TYPE nv_pinned_memory_pool_total_bytes gauge\n" //
                    + "nv_pinned_memory_pool_total_bytes 268435456\n" //
                    + "# HELP nv_pinned_memory_pool_used_bytes Pinned memory pool used memory size, in bytes\n" //
                    + "# TYPE nv_pinned_memory_pool_used_bytes gauge\n" //
                    + "nv_pinned_memory_pool_used_bytes 0\n" //
                    + "# HELP nv_gpu_utilization GPU utilization rate [0.0 - 1.0)\n" //
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
    protected static final String TRITON_EXPECTED_METRICS = "gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ed {\"gpu_uuid\":\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ed\"," //
            + "\"gpu_stats\":{\"nv_gpu_power_limit\":\"60.000000\",\"nv_gpu_utilization\":\"0.000000\",\"nv_gpu_memory_used_bytes\":\"617611264.000000\"," //
            + "\"nv_gpu_power_usage\":\"20.085000\",\"nv_gpu_memory_total_bytes\":\"16101933056.000000\"}}" //
            + " gpu.metrics.GPU-340cec52-80ba-c0df-8511-5f9680aae0ff {\"gpu_uuid\":\"GPU-340cec52-80ba-c0df-8511-5f9680aae0ff\",\"gpu_stats\":{\"nv_gpu_power_limit\":\"60.000000\"," //
            + "\"nv_gpu_utilization\":\"0.000000\",\"nv_gpu_memory_used_bytes\":\"617611264.000000\",\"nv_gpu_power_usage\":\"20.085000\",\"nv_gpu_memory_total_bytes\":\"16101933056.000000\"}}";
    protected static final String TRITON_STATS_RESPONSE = "{\"model_stats\":[{\"name\":\"identity_long\",\"version\":\"1\",\"last_inference\":1739867342484," //
            + "\"inference_count\":42,\"execution_count\":42,\"inference_stats\":{\"success\":{\"count\":42,\"ns\":30097440},\"fail\":{\"count\":0,\"ns\":0}," //
            + "\"queue\":{\"count\":42,\"ns\":7137056},\"compute_input\":{\"count\":42,\"ns\":532768},\"compute_infer\":{\"count\":42,\"ns\":4664640}," //
            + "\"compute_output\":{\"count\":42,\"ns\":4736},\"cache_hit\":{\"count\":0,\"ns\":0},\"cache_miss\":{\"count\":0,\"ns\":0}}," //
            + "\"batch_stats\":[{\"batch_size\":1,\"compute_input\":{\"count\":42,\"ns\":532768},\"compute_infer\":{\"count\":42,\"ns\":4664640}," //
            + "\"compute_output\":{\"count\":42,\"ns\":4736}}]},{\"name\":\"preprocessor\",\"version\":\"1\",\"last_inference\":1739867342480," //
            + "\"inference_count\":42,\"execution_count\":42,\"inference_stats\":{\"success\":{\"count\":42,\"ns\":278516928},\"fail\":{\"count\":0,\"ns\":0}," //
            + "\"queue\":{\"count\":42,\"ns\":8943104},\"compute_input\":{\"count\":42,\"ns\":11304704},\"compute_infer\":{\"count\":42,\"ns\":230288448}," //
            + "\"compute_output\":{\"count\":42,\"ns\":26880672},\"cache_hit\":{\"count\":0,\"ns\":0},\"cache_miss\":{\"count\":0,\"ns\":0}}," //
            + "\"batch_stats\":[{\"batch_size\":1,\"compute_input\":{\"count\":42,\"ns\":11304704},\"compute_infer\":{\"count\":42,\"ns\":230288448}," //
            + "\"compute_output\":{\"count\":42,\"ns\":26880672}}]}";
    protected static final String TRITON_EXPECTED_STATS = "model.metrics.identity_long.1 {\"name\":\"identity_long\",\"version\":\"1\",\"last_inference\":1739867342484," //
            + "\"inference_count\":42,\"execution_count\":42,\"inference_stats\":{\"success\":{\"count\":42,\"ns\":30097440},\"fail\":{\"count\":0,\"ns\":0}," //
            + "\"queue\":{\"count\":42,\"ns\":7137056},\"compute_input\":{\"count\":42,\"ns\":532768},\"compute_infer\":{\"count\":42,\"ns\":4664640}," //
            + "\"compute_output\":{\"count\":42,\"ns\":4736},\"cache_hit\":{\"count\":0,\"ns\":0},\"cache_miss\":{\"count\":0,\"ns\":0}}," //
            + "\"batch_stats\":[{\"batch_size\":1,\"compute_input\":{\"count\":42,\"ns\":532768},\"compute_infer\":{\"count\":42,\"ns\":4664640}," //
            + "\"compute_output\":{\"count\":42,\"ns\":4736}}]} model.metrics.preprocessor.1 {\"name\":\"preprocessor\",\"version\":\"1\",\"last_inference\":1739867342480," //
            + "\"inference_count\":42,\"execution_count\":42,\"inference_stats\":{\"success\":{\"count\":42,\"ns\":278516928},\"fail\":{\"count\":0,\"ns\":0}," //
            + "\"queue\":{\"count\":42,\"ns\":8943104},\"compute_input\":{\"count\":42,\"ns\":11304704},\"compute_infer\":{\"count\":42,\"ns\":230288448}," //
            + "\"compute_output\":{\"count\":42,\"ns\":26880672},\"cache_hit\":{\"count\":0,\"ns\":0},\"cache_miss\":{\"count\":0,\"ns\":0}}," //
            + "\"batch_stats\":[{\"batch_size\":1,\"compute_input\":{\"count\":42,\"ns\":11304704},\"compute_infer\":{\"count\":42,\"ns\":230288448}," //
            + "\"compute_output\":{\"count\":42,\"ns\":26880672}}]}";
}
