/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.cloudconnection.provider.dto;

import java.util.Map;

import org.eclipse.kura.internal.rest.cloudconnection.provider.ConfigParameterType;

public class ConfigParameterDTO {

    private String id;
    private String name;
    private String description;
    private ConfigParameterType type;
    private boolean required;
    private String defaultValue;
    private int cardinality;
    private Map<String, String> options;
    private String min;
    private String max;
    private String value;  // used for fields with single cardinality
    private String[] values; // used for fields with multiple cardinality

    public ConfigParameterDTO(String id, String name, String description, ConfigParameterType type, boolean required,
            String defaultValue, int cardinality, Map<String, String> options, String min, String max, String value,
            String[] values) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.cardinality = cardinality;
        this.options = options;
        this.min = min;
        this.max = max;
        this.value = value;
        this.values = values;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public ConfigParameterType getType() {
        return this.type;
    }

    public boolean isRequired() {
        return this.required;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public int getCardinality() {
        return this.cardinality;
    }

    public Map<String, String> getOptions() {
        return this.options;
    }

    public String getMin() {
        return this.min;
    }

    public String getMax() {
        return this.max;
    }

    public String getValue() {
        return this.value;
    }

    public String[] getValues() {
        return this.values;
    }

}
