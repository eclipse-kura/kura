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
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.junit.After;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TritonServerServiceContainerImplTest extends TritonServerServiceStepDefinitions {

    private static final String TRITON_REPOSITORY_PATH = "/fake-repository-path";
    private static final String TRITON_METRICS_RESPONSE = "# HELP nv_inference_request_success Number of successful inference requests, all batch sizes\n"
            + "# TYPE nv_inference_request_success counter\n"
            + "nv_inference_request_success{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_request_success{model=\"identity_long\",version=\"1\"} 1\n"
            + "# HELP nv_inference_request_failure Number of failed inference requests, all batch sizes\n"
            + "# TYPE nv_inference_request_failure counter\n"
            + "nv_inference_request_failure{model=\"preprocessor\",reason=\"OTHER\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"preprocessor\",reason=\"BACKEND\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"preprocessor\",reason=\"CANCELED\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"preprocessor\",reason=\"REJECTED\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"identity_long\",reason=\"OTHER\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"identity_long\",reason=\"CANCELED\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"identity_long\",reason=\"BACKEND\",version=\"1\"} 0\n"
            + "nv_inference_request_failure{model=\"identity_long\",reason=\"REJECTED\",version=\"1\"} 0\n"
            + "# HELP nv_inference_count Number of inferences performed (does not include cached requests)\n"
            + "# TYPE nv_inference_count counter\n" + "nv_inference_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "# HELP nv_inference_exec_count Number of model executions performed (does not include cached requests)\n"
            + "# TYPE nv_inference_exec_count counter\n"
            + "nv_inference_exec_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_exec_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "# HELP nv_inference_request_duration_us Cumulative inference request duration in microseconds (includes cached requests)\n"
            + "# TYPE nv_inference_request_duration_us counter\n"
            + "nv_inference_request_duration_us{model=\"preprocessor\",version=\"1\"} 59877\n"
            + "nv_inference_request_duration_us{model=\"identity_long\",version=\"1\"} 459\n"
            + "# HELP nv_inference_queue_duration_us Cumulative inference queuing duration in microseconds (includes cached requests)\n"
            + "# TYPE nv_inference_queue_duration_us counter\n"
            + "nv_inference_queue_duration_us{model=\"preprocessor\",version=\"1\"} 473\n"
            + "nv_inference_queue_duration_us{model=\"identity_long\",version=\"1\"} 162\n"
            + "# HELP nv_inference_compute_input_duration_us Cumulative compute input duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_input_duration_us counter\n"
            + "nv_inference_compute_input_duration_us{model=\"preprocessor\",version=\"1\"} 316\n"
            + "nv_inference_compute_input_duration_us{model=\"identity_long\",version=\"1\"} 8\n"
            + "# HELP nv_inference_compute_infer_duration_us Cumulative compute inference duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_infer_duration_us counter\n"
            + "nv_inference_compute_infer_duration_us{model=\"preprocessor\",version=\"1\"} 58653\n"
            + "nv_inference_compute_infer_duration_us{model=\"identity_long\",version=\"1\"} 41\n"
            + "# HELP nv_inference_compute_output_duration_us Cumulative inference compute output duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_output_duration_us counter\n"
            + "nv_inference_compute_output_duration_us{model=\"preprocessor\",version=\"1\"} 404\n"
            + "nv_inference_compute_output_duration_us{model=\"identity_long\",version=\"1\"} 0\n"
            + "# HELP input_byte_size_counter Cumulative input byte size of all requests received by the model\n"
            + "# TYPE input_byte_size_counter counter\n"
            + "input_byte_size_counter{model=\"identity_long\",version=\"1\"} 8\n"
            + "# HELP nv_inference_pending_request_count Instantaneous number of pending requests awaiting execution per-model.\n"
            + "# TYPE nv_inference_pending_request_count gauge\n"
            + "nv_inference_pending_request_count{model=\"preprocessor\",version=\"1\"} 0\n"
            + "nv_inference_pending_request_count{model=\"identity_long\",version=\"1\"} 0\n"
            + "# HELP nv_model_load_duration_secs Model load time in seconds\n"
            + "# TYPE nv_model_load_duration_secs gauge\n"
            + "nv_model_load_duration_secs{model=\"preprocessor\",version=\"1\"} 1.611378656\n"
            + "nv_model_load_duration_secs{model=\"identity_long\",version=\"1\"} 0.004683712\n"
            + "# HELP nv_pinned_memory_pool_total_bytes Pinned memory pool total memory size, in bytes\n"
            + "# TYPE nv_pinned_memory_pool_total_bytes gauge\n" + "nv_pinned_memory_pool_total_bytes 268435456\n"
            + "# HELP nv_pinned_memory_pool_used_bytes Pinned memory pool used memory size, in bytes\n"
            + "# TYPE nv_pinned_memory_pool_used_bytes gauge\n" + "nv_pinned_memory_pool_used_bytes 0\n"
            + "# HELP nv_inference_request_summary_us Summary of inference request duration in microseconds (includes cached requests)\n"
            + "# TYPE nv_inference_request_summary_us summary\n"
            + "nv_inference_request_summary_us_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_request_summary_us_sum{model=\"preprocessor\",version=\"1\"} 59877\n"
            + "nv_inference_request_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.5\"} 59877\n"
            + "nv_inference_request_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.9\"} 59877\n"
            + "nv_inference_request_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.95\"} 59877\n"
            + "nv_inference_request_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.99\"} 59877\n"
            + "nv_inference_request_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.999\"} 59877\n"
            + "nv_inference_request_summary_us_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "nv_inference_request_summary_us_sum{model=\"identity_long\",version=\"1\"} 459\n"
            + "nv_inference_request_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.5\"} 459\n"
            + "nv_inference_request_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.9\"} 459\n"
            + "nv_inference_request_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.95\"} 459\n"
            + "nv_inference_request_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.99\"} 459\n"
            + "nv_inference_request_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.999\"} 459\n"
            + "# HELP nv_inference_queue_summary_us Summary of inference queuing duration in microseconds (includes cached requests)\n"
            + "# TYPE nv_inference_queue_summary_us summary\n"
            + "nv_inference_queue_summary_us_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_queue_summary_us_sum{model=\"preprocessor\",version=\"1\"} 473\n"
            + "nv_inference_queue_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.5\"} 473\n"
            + "nv_inference_queue_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.9\"} 473\n"
            + "nv_inference_queue_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.95\"} 473\n"
            + "nv_inference_queue_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.99\"} 473\n"
            + "nv_inference_queue_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.999\"} 473\n"
            + "nv_inference_queue_summary_us_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "nv_inference_queue_summary_us_sum{model=\"identity_long\",version=\"1\"} 162\n"
            + "nv_inference_queue_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.5\"} 162\n"
            + "nv_inference_queue_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.9\"} 162\n"
            + "nv_inference_queue_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.95\"} 162\n"
            + "nv_inference_queue_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.99\"} 162\n"
            + "nv_inference_queue_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.999\"} 162\n"
            + "# HELP nv_inference_compute_input_summary_us Cumulative compute input duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_input_summary_us summary\n"
            + "nv_inference_compute_input_summary_us_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_compute_input_summary_us_sum{model=\"preprocessor\",version=\"1\"} 316\n"
            + "nv_inference_compute_input_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.5\"} 316\n"
            + "nv_inference_compute_input_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.9\"} 316\n"
            + "nv_inference_compute_input_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.95\"} 316\n"
            + "nv_inference_compute_input_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.99\"} 316\n"
            + "nv_inference_compute_input_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.999\"} 316\n"
            + "nv_inference_compute_input_summary_us_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "nv_inference_compute_input_summary_us_sum{model=\"identity_long\",version=\"1\"} 8\n"
            + "nv_inference_compute_input_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.5\"} 8\n"
            + "nv_inference_compute_input_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.9\"} 8\n"
            + "nv_inference_compute_input_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.95\"} 8\n"
            + "nv_inference_compute_input_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.99\"} 8\n"
            + "nv_inference_compute_input_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.999\"} 8\n"
            + "# HELP nv_inference_compute_infer_summary_us Cumulative compute inference duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_infer_summary_us summary\n"
            + "nv_inference_compute_infer_summary_us_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_compute_infer_summary_us_sum{model=\"preprocessor\",version=\"1\"} 58653\n"
            + "nv_inference_compute_infer_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.5\"} 58653\n"
            + "nv_inference_compute_infer_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.9\"} 58653\n"
            + "nv_inference_compute_infer_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.95\"} 58653\n"
            + "nv_inference_compute_infer_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.99\"} 58653\n"
            + "nv_inference_compute_infer_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.999\"} 58653\n"
            + "nv_inference_compute_infer_summary_us_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "nv_inference_compute_infer_summary_us_sum{model=\"identity_long\",version=\"1\"} 41\n"
            + "nv_inference_compute_infer_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.5\"} 41\n"
            + "nv_inference_compute_infer_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.9\"} 41\n"
            + "nv_inference_compute_infer_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.95\"} 41\n"
            + "nv_inference_compute_infer_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.99\"} 41\n"
            + "nv_inference_compute_infer_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.999\"} 41\n"
            + "# HELP nv_inference_compute_output_summary_us Cumulative inference compute output duration in microseconds (does not include cached requests)\n"
            + "# TYPE nv_inference_compute_output_summary_us summary\n"
            + "nv_inference_compute_output_summary_us_count{model=\"preprocessor\",version=\"1\"} 1\n"
            + "nv_inference_compute_output_summary_us_sum{model=\"preprocessor\",version=\"1\"} 404\n"
            + "nv_inference_compute_output_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.5\"} 404\n"
            + "nv_inference_compute_output_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.9\"} 404\n"
            + "nv_inference_compute_output_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.95\"} 404\n"
            + "nv_inference_compute_output_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.99\"} 404\n"
            + "nv_inference_compute_output_summary_us{model=\"preprocessor\",version=\"1\",quantile=\"0.999\"} 404\n"
            + "nv_inference_compute_output_summary_us_count{model=\"identity_long\",version=\"1\"} 1\n"
            + "nv_inference_compute_output_summary_us_sum{model=\"identity_long\",version=\"1\"} 0\n"
            + "nv_inference_compute_output_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.5\"} 0\n"
            + "nv_inference_compute_output_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.9\"} 0\n"
            + "nv_inference_compute_output_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.95\"} 0\n"
            + "nv_inference_compute_output_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.99\"} 0\n"
            + "nv_inference_compute_output_summary_us{model=\"identity_long\",version=\"1\",quantile=\"0.999\"} 0";
    private static final String TRITON_EXPECTED_METRICS = "nv_inference_compute_output_summary_us-preprocessor-1-0.99 404\n"
            + "nv_pinned_memory_pool_total_bytes 268435456\n"
            + "nv_inference_compute_output_summary_us-preprocessor-1-0.95 404\n"
            + "nv_inference_exec_count-identity_long-1 1\n" + "nv_inference_request_success-preprocessor-1 1\n"
            + "nv_inference_request_failure-identity_long-CANCELED-1 0\n"
            + "nv_inference_request_summary_us-identity_long-1-0.999 459\n"
            + "nv_inference_request_summary_us-preprocessor-1-0.99 59877\n"
            + "nv_model_load_duration_secs-identity_long-1 0.004683712\n"
            + "nv_inference_compute_infer_summary_us-preprocessor-1-0.5 58653\n"
            + "nv_inference_request_summary_us-preprocessor-1-0.95 59877\n" + "nv_pinned_memory_pool_used_bytes 0\n"
            + "nv_inference_queue_summary_us-identity_long-1-0.5 162\n"
            + "nv_inference_queue_summary_us-identity_long-1-0.9 162\n"
            + "nv_inference_compute_input_summary_us_count-preprocessor-1 1\n"
            + "nv_inference_compute_input_summary_us_sum-preprocessor-1 316\n"
            + "nv_inference_compute_infer_summary_us-identity_long-1-0.9 41\n"
            + "nv_inference_queue_summary_us-preprocessor-1-0.999 473\n"
            + "nv_inference_compute_infer_summary_us-identity_long-1-0.5 41\n"
            + "nv_inference_request_summary_us_count-preprocessor-1 1\n"
            + "nv_model_load_duration_secs-preprocessor-1 1.611378656\n"
            + "nv_inference_request_failure-identity_long-REJECTED-1 0\n"
            + "nv_inference_compute_input_summary_us-identity_long-1-0.999 8\n"
            + "nv_inference_compute_infer_summary_us_count-preprocessor-1 1\n"
            + "nv_inference_queue_summary_us_count-identity_long-1 1\n"
            + "nv_inference_request_success-identity_long-1 1\n" + "nv_inference_count-preprocessor-1 1\n"
            + "nv_inference_compute_output_summary_us_count-preprocessor-1 1\n"
            + "nv_inference_request_summary_us_sum-preprocessor-1 59877\n"
            + "nv_inference_request_summary_us-preprocessor-1-0.9 59877\n"
            + "nv_inference_compute_input_summary_us-preprocessor-1-0.5 316\n"
            + "nv_inference_compute_output_summary_us_sum-preprocessor-1 404\n"
            + "nv_inference_compute_input_summary_us-preprocessor-1-0.9 316\n"
            + "nv_inference_compute_input_summary_us_count-identity_long-1 1\n"
            + "nv_inference_request_failure-preprocessor-CANCELED-1 0\n"
            + "nv_inference_compute_input_summary_us_sum-identity_long-1 8\n"
            + "nv_inference_compute_input_summary_us-preprocessor-1-0.99 316\n"
            + "nv_inference_count-identity_long-1 1\n" + "nv_inference_request_summary_us-preprocessor-1-0.5 59877\n"
            + "nv_inference_compute_infer_duration_us-preprocessor-1 58653\n"
            + "nv_inference_compute_input_summary_us-preprocessor-1-0.95 316\n"
            + "nv_inference_compute_output_summary_us-preprocessor-1-0.5 404\n"
            + "nv_inference_compute_output_summary_us_count-identity_long-1 1\n"
            + "nv_inference_compute_infer_summary_us_sum-identity_long-1 41\n"
            + "nv_inference_compute_output_summary_us-preprocessor-1-0.9 404\n"
            + "nv_inference_request_summary_us-preprocessor-1-0.999 59877\n"
            + "nv_inference_compute_infer_summary_us-preprocessor-1-0.999 58653\n"
            + "nv_inference_queue_duration_us-identity_long-1 162\n"
            + "nv_inference_compute_output_summary_us-identity_long-1-0.9 0\n"
            + "nv_inference_request_summary_us-identity_long-1-0.95 459\n"
            + "nv_inference_compute_output_duration_us-identity_long-1 0\n"
            + "nv_inference_queue_duration_us-preprocessor-1 473\n"
            + "nv_inference_request_summary_us-identity_long-1-0.9 459\n"
            + "nv_inference_compute_output_summary_us_sum-identity_long-1 0\n"
            + "nv_inference_request_summary_us-identity_long-1-0.5 459\n"
            + "input_byte_size_counter-identity_long-1 8\n"
            + "nv_inference_compute_infer_summary_us-identity_long-1-0.99 41\n"
            + "nv_inference_request_summary_us-identity_long-1-0.99 459\n"
            + "nv_inference_compute_infer_summary_us-identity_long-1-0.95 41\n"
            + "nv_inference_request_summary_us_sum-identity_long-1 459\n"
            + "nv_inference_compute_output_summary_us-identity_long-1-0.5 0\n"
            + "nv_inference_pending_request_count-identity_long-1 0\n"
            + "nv_inference_queue_summary_us-preprocessor-1-0.95 473\n"
            + "nv_inference_compute_output_summary_us-identity_long-1-0.99 0\n"
            + "nv_inference_queue_summary_us-preprocessor-1-0.99 473\n"
            + "nv_inference_compute_output_summary_us-identity_long-1-0.95 0\n"
            + "nv_inference_compute_output_duration_us-preprocessor-1 404\n"
            + "nv_inference_exec_count-preprocessor-1 1\n"
            + "nv_inference_compute_infer_summary_us_count-identity_long-1 1\n"
            + "nv_inference_request_duration_us-preprocessor-1 59877\n"
            + "nv_inference_compute_infer_summary_us-preprocessor-1-0.99 58653\n"
            + "nv_inference_compute_infer_summary_us-preprocessor-1-0.95 58653\n"
            + "nv_inference_compute_input_summary_us-identity_long-1-0.95 8\n"
            + "nv_inference_request_failure-identity_long-BACKEND-1 0\n"
            + "nv_inference_compute_input_summary_us-identity_long-1-0.99 8\n"
            + "nv_inference_queue_summary_us-preprocessor-1-0.9 473\n"
            + "nv_inference_compute_infer_duration_us-identity_long-1 41\n"
            + "nv_inference_queue_summary_us-preprocessor-1-0.5 473\n"
            + "nv_inference_compute_output_summary_us-identity_long-1-0.999 0\n"
            + "nv_inference_compute_input_duration_us-identity_long-1 8\n"
            + "nv_inference_compute_input_summary_us-identity_long-1-0.5 8\n"
            + "nv_inference_queue_summary_us-identity_long-1-0.999 162\n"
            + "nv_inference_queue_summary_us_sum-preprocessor-1 473\n"
            + "nv_inference_queue_summary_us-identity_long-1-0.99 162\n"
            + "nv_inference_compute_input_summary_us-identity_long-1-0.9 8\n"
            + "nv_inference_pending_request_count-preprocessor-1 0\n"
            + "nv_inference_queue_summary_us-identity_long-1-0.95 162\n"
            + "nv_inference_compute_input_duration_us-preprocessor-1 316\n"
            + "nv_inference_request_duration_us-identity_long-1 459\n"
            + "nv_inference_request_summary_us_count-identity_long-1 1\n"
            + "nv_inference_compute_infer_summary_us_sum-preprocessor-1 58653\n"
            + "nv_inference_compute_input_summary_us-preprocessor-1-0.999 316\n"
            + "nv_inference_compute_infer_summary_us-preprocessor-1-0.9 58653\n"
            + "nv_inference_queue_summary_us_sum-identity_long-1 162\n"
            + "nv_inference_compute_infer_summary_us-identity_long-1-0.999 41\n"
            + "nv_inference_queue_summary_us_count-preprocessor-1 1\n"
            + "nv_inference_request_failure-identity_long-OTHER-1 0\n"
            + "nv_inference_request_failure-preprocessor-REJECTED-1 0\n"
            + "nv_inference_request_failure-preprocessor-OTHER-1 0\n"
            + "nv_inference_compute_output_summary_us-preprocessor-1-0.999 404\n"
            + "nv_inference_request_failure-preprocessor-BACKEND-1 0";

    private Map<String, Object> properties = new HashMap<>();
    private Optional<String> rawMetrics;
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
    public void shouldGetEmptyRawMetricsWhenMetricsAreDisabled() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.FALSE);
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse("These are raw metrics!", 200);

        whenRawMetricsAreRetrieved();

        thenRawMetricsAreEmpty();
    }

    @Test
    public void shouldGetEmptyRawMetricsWhenResponseIsNot200() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.FALSE);
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse("These are raw metrics!", 404);

        whenRawMetricsAreRetrieved();

        thenRawMetricsAreEmpty();
    }

    @Test
    public void shouldGetRawMetrics() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.TRUE);
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse("These are raw metrics!", 200);

        whenRawMetricsAreRetrieved();

        thenRawMetricsAre("These are raw metrics!");
    }

    @Test
    public void shouldGetEmptyMetricsWhenMetricsAreDisabled() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.FALSE);
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse("These are raw metrics!", 200);

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
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse("These are raw metrics!", 500);

        whenMetricsAreRetrieved();

        thenMetricsAreEmpty();
    }

    @Test
    public void shouldGetMetrics() throws IOException, KuraException {
        givenPropertyWith("container.image", TRITON_IMAGE_NAME);
        givenPropertyWith("container.image.tag", TRITON_IMAGE_TAG);
        givenPropertyWith("local.model.repository.path", TRITON_REPOSITORY_PATH);
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.metrics", Boolean.TRUE);
        givenPropertyWith("enable.gpu.metrics", Boolean.FALSE);
        givenPropertyWith("enable.cpu.metrics", Boolean.TRUE);
        givenPropertyWith("metrics.config", "myProp=foo;yourProp=bar");
        givenPropertyWith("metrics.interval", "99");
        givenTritonServerServiceContainerImpl(this.properties);
        givenMetricsServerWithResponse(TRITON_METRICS_RESPONSE, 200);
        givenExpectedMetrics();

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

    private void givenExpectedMetrics() {
        Arrays.asList(TRITON_EXPECTED_METRICS.split("\n")).stream().forEach(line -> {
            String[] items = line.split(" ");
            this.expectedMetrics.put(items[0], items[1]);
        });
    }

    /*
     * When
     */
    private void whenRawMetricsAreRetrieved() throws KuraException {
        this.rawMetrics = this.tritonServerService.getRawMetrics();
    }

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

    private void thenRawMetricsAre(String expectedValue) {
        assertEquals(expectedValue, this.rawMetrics.get());
    }

    private void thenRawMetricsAreEmpty() {
        assertTrue(this.rawMetrics.isEmpty());
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
