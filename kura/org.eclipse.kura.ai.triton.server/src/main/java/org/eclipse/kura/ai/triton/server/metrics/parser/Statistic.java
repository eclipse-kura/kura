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

import inference.GrpcService.StatisticDuration;

public class Statistic {

    @SerializedName(value = "count")
    private final long count;
    @SerializedName(value = "ns")
    private final long time;

    public Statistic(StatisticDuration stat) {
        this.count = stat.getCount();
        this.time = stat.getNs();
    }

    public Statistic(long count, long time) {
        this.count = count;
        this.time = time;
    }

    public long getCount() {
        return this.count;
    }

    public long getTime() {
        return this.time;
    }
}
