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
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GpuMetrics {

    @SerializedName(value = "gpu_uuid")
    private final String uuid;
    @SerializedName(value = "gpu_stats")
    private final Map<String, String> metrics;

    public GpuMetrics(final String gpuUuid) {
        this.uuid = gpuUuid;
        this.metrics = new HashMap<>();
    }

    public String getGpuUuid() {
        return this.uuid;
    }

    public Map<String, String> getGpuMetrics() {
        return this.metrics;
    }

    public void addGpuMetric(final String metricName, final String metricValue) {
        this.metrics.put(metricName, metricValue);
    }
}
