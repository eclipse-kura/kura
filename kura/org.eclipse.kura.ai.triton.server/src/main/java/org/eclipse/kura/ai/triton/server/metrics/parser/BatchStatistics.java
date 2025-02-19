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

import inference.GrpcService.InferBatchStatistics;

public class BatchStatistics {

    @SerializedName(value = "batch_size")
    private final long batchSize;
    @SerializedName(value = "compute_input")
    private final Statistic computeInput;
    @SerializedName(value = "compute_infer")
    private final Statistic computeInfer;
    @SerializedName(value = "compute_output")
    private final Statistic computeOutput;

    public BatchStatistics(InferBatchStatistics stat) {
        this.batchSize = stat.getBatchSize();
        this.computeInput = new Statistic(stat.getComputeInput());
        this.computeInfer = new Statistic(stat.getComputeInfer());
        this.computeOutput = new Statistic(stat.getComputeOutput());
    }

    public long getBatchSize() {
        return this.batchSize;
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

}