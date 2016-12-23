/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

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

    private String m_id;
    private String m_name;
    private String m_description;
    private GwtConfigParameterType m_type;
    private boolean m_required;
    private String m_default;
    private int m_cardinality;
    private Map<String, String> m_options;
    private String m_min;
    private String m_max;
    private String m_value;  // used for fields with single cardinality
    private String[] m_values; // used for fields with multiple cardinality

    public GwtConfigParameter() {
    }

    public GwtConfigParameter(GwtConfigParameter reference) {
        this.m_id = reference.getId();
        this.m_name = reference.getName();
        this.m_description = reference.getDescription();
        this.m_type = reference.getType();
        this.m_required = reference.isRequired();
        this.m_default = reference.getDefault();
        this.m_cardinality = reference.getCardinality();
        this.m_options = reference.getOptions();
        this.m_min = reference.getMin();
        this.m_max = reference.getMax();
        this.m_value = reference.getValue();
        this.m_values = reference.getValues();
    }

    public String getId() {
        return this.m_id;
    }

    public void setId(String id) {
        this.m_id = id;
    }

    public String getName() {
        return this.m_name;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public String getDescription() {
        return this.m_description;
    }

    public void setDescription(String description) {
        this.m_description = description;
    }

    public GwtConfigParameterType getType() {
        return this.m_type;
    }

    public void setType(GwtConfigParameterType type) {
        this.m_type = type;
    }

    public boolean isRequired() {
        return this.m_required;
    }

    public void setRequired(boolean required) {
        this.m_required = required;
    }

    public String getDefault() {
        return this.m_default;
    }

    public void setDefault(String default1) {
        this.m_default = default1;
    }

    public int getCardinality() {
        return this.m_cardinality;
    }

    public void setCardinality(int cardinality) {
        this.m_cardinality = cardinality;
    }

    public Map<String, String> getOptions() {
        return this.m_options;
    }

    public void setOptions(Map<String, String> options) {
        this.m_options = options;
    }

    public String getMin() {
        return this.m_min;
    }

    public void setMin(String min) {
        this.m_min = min;
    }

    public String getMax() {
        return this.m_max;
    }

    public void setMax(String max) {
        this.m_max = max;
    }

    public String getValue() {
        return this.m_value;
    }

    public void setValue(String value) {
        this.m_value = value;
    }

    public String[] getValues() {
        return this.m_values;
    }

    public void setValues(String[] values) {
        this.m_values = values;
    }
}
