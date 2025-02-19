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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelStatisticsParser {

    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final Gson gson = this.gsonBuilder.create();
    private final inference.GrpcService.ModelStatistics modelStatistics;

    public ModelStatisticsParser(inference.GrpcService.ModelStatistics modelStatistics) {
        this.modelStatistics = modelStatistics;
    }

    public Map<String, String> parse() {
        Map<String, String> statistics = new HashMap<>();

        String key = getKey();
        ModelStatistics tritonModelStatistics = new ModelStatistics(modelStatistics);
        statistics.put(key, gson.toJson(tritonModelStatistics));

        return statistics;
    }

    private String getKey() {
        return "model.metrics." + this.modelStatistics.getName() + "." + this.modelStatistics.getVersion();
    }
}
