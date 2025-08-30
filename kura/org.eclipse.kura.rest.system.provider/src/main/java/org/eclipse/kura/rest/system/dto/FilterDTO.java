/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@SuppressWarnings("all")
@Schema(name = "Filter", description = "Filter criteria for system properties queries", example = "{\"names\": [\"javaVersion\", \"osName\"], \"groupNames\": [\"hardware\", \"java\"]}")
public class FilterDTO {

    @Schema(description = "List of specific property names to include in the response. If empty, all properties are returned.", example = "[\"javaVersion\", \"osName\", \"cpuVersion\"]", required = false)
    private List<String> names;

    @Schema(description = "List of property group names to include in the response (e.g., 'hardware', 'java', 'kura'). If empty, all groups are returned.", example = "[\"hardware\", \"java\"]", required = false)
    private List<String> groupNames;

    public FilterDTO() {
        this.names = new ArrayList<>();
        this.groupNames = new ArrayList<>();
    }

    public List<String> getNames() {
        return this.names;
    }

    public List<String> getGroupNames() {
        return this.groupNames;
    }

}
