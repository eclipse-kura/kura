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
package org.eclipse.kura.ai.triton.server.metrics.parser;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ModelStatistics {

    @SerializedName(value = "name")
    private final String name;
    @SerializedName(value = "version")
    private final String version;
    @SerializedName(value = "last_inference")
    private final long lastInference;
    @SerializedName(value = "inference_count")
    private final long inferenceCount;
    @SerializedName(value = "execution_count")
    private final long executionCount;
    @SerializedName(value = "inference_stats")
    private final InferenceStatistics inferenceStats;
    @SerializedName(value = "batch_stats")
    private List<BatchStatistics> batchStats = new ArrayList<>();

    public ModelStatistics(inference.GrpcService.ModelStatistics modelStatistics) {
        this.name = modelStatistics.getName();
        this.version = modelStatistics.getVersion();
        this.lastInference = modelStatistics.getLastInference();
        this.inferenceCount = modelStatistics.getInferenceCount();
        this.executionCount = modelStatistics.getExecutionCount();
        this.inferenceStats = new InferenceStatistics(modelStatistics.getInferenceStats());
        modelStatistics.getBatchStatsList().forEach(batch -> batchStats.add(new BatchStatistics(batch)));
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public long getLastInference() {
        return this.lastInference;
    }

    public long getInferenceCount() {
        return this.inferenceCount;
    }

    public long getExecutionCount() {
        return this.executionCount;
    }

    public InferenceStatistics getInferenceStats() {
        return this.inferenceStats;
    }

    public List<BatchStatistics> getBatchStats() {
        return this.batchStats;
    }

}