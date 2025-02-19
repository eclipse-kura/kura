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

import com.google.gson.annotations.SerializedName;

import inference.GrpcService.InferStatistics;

public class InferenceStatistics {

    @SerializedName(value = "success")
    private final Statistic success;
    @SerializedName(value = "fail")
    private final Statistic fail;
    @SerializedName(value = "queue")
    private final Statistic queue;
    @SerializedName(value = "compute_input")
    private final Statistic computeInput;
    @SerializedName(value = "compute_infer")
    private final Statistic computeInfer;
    @SerializedName(value = "compute_output")
    private final Statistic computeOutput;
    @SerializedName(value = "cache_hit")
    private final Statistic cacheHit;
    @SerializedName(value = "cache_miss")
    private final Statistic cacheMiss;

    public InferenceStatistics(InferStatistics stats) {
        this.success = new Statistic(stats.getSuccess());
        this.fail = new Statistic(stats.getFail());
        this.queue = new Statistic(stats.getQueue());
        this.computeInput = new Statistic(stats.getComputeInput());
        this.computeInfer = new Statistic(stats.getComputeInfer());
        this.computeOutput = new Statistic(stats.getComputeOutput());
        this.cacheHit = new Statistic(stats.getCacheHit());
        this.cacheMiss = new Statistic(stats.getCacheMiss());
    }

    public Statistic getSuccess() {
        return this.success;
    }

    public Statistic getFail() {
        return this.fail;
    }

    public Statistic getQueue() {
        return this.queue;
    }

    public Statistic getComputeInput() {
        return this.computeInput;
    }

    public Statistic getComputeInfer() {
        return this.computeInfer;
    }

    public Statistic getComputeOutput() {
        return this.computeOutput;
    }

    public Statistic getCacheHit() {
        return this.cacheHit;
    }

    public Statistic getCacheMiss() {
        return this.cacheMiss;
    }

}
