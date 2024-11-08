/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.util.Arrays;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtConfigParameter implements IsSerializable {

    public enum GwtConfigParameterType {

        STRING,
        LONG,
        DOUBLE,
        FLOAT,
        INTEGER,
        BYTE,
        CHAR,
        BOOLEAN,
        SHORT,
        PASSWORD;

        GwtConfigParameterType() {
        }
    }

    private String id;
    private String name;
    private String description;
    private GwtConfigParameterType type;
    private GwtConfigParameterType subtype;

    private boolean required;
    private String defaultValue;
    private int cardinality;
    private Map<String, String> options;
    private String min;
    private String max;
    private String value;  // used for fields with single cardinality
    private String[] values; // used for fields with multiple cardinality

    public GwtConfigParameter() {
    }

    public GwtConfigParameter(GwtConfigParameter reference) {
        this.id = reference.getId();
        this.name = reference.getName();
        this.description = reference.getDescription();
        this.type = reference.getType();
        this.subtype = reference.getSubtype();
        this.required = reference.isRequired();
        this.defaultValue = reference.getDefault();
        this.cardinality = reference.getCardinality();
        this.options = reference.getOptions();
        this.min = reference.getMin();
        this.max = reference.getMax();
        this.value = reference.getValue();
        this.values = reference.getValues();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GwtConfigParameterType getType() {
        return this.type;
    }

    public void setType(GwtConfigParameterType type) {
        this.type = type;
    }

    public GwtConfigParameterType getSubtype() {
        return this.subtype;
    }

    public void setSubtype(GwtConfigParameterType subtype) {
        this.subtype = subtype;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefault() {
        return this.defaultValue;
    }

    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    public Map<String, String> getOptions() {
        return this.options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public String getMin() {
        return this.min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return this.max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String[] getValues() {
        return this.values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "GwtConfigParameter [id=" + this.id + ", name=" + this.name + ", description=" + this.description
                + ", type=" + this.type + ", subtype=" + this.subtype + ", required=" + this.required
                + ", defaultValue=" + this.defaultValue + ", cardinality=" + this.cardinality + ", options="
                + this.options + ", min=" + this.min + ", max=" + this.max + ", value=" + this.value + ", values="
                + Arrays.toString(this.values) + "]";
    }
}
