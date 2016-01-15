/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtConfigParameter implements Serializable {

	private static final long serialVersionUID = -1738441153196315880L;

	public enum GwtConfigParameterType implements Serializable, IsSerializable {		
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
	    GwtConfigParameterType() 
		{}
	}
	
	private String                 m_id;
	private String                 m_name;
	private String                 m_description;
	private GwtConfigParameterType m_type;
	private boolean                m_required;
	private String		           m_default;
	private int                    m_cardinality;
	private Map<String, String>    m_options;
	private String		           m_min;
	private String		           m_max;
	private String		           m_value;  // used for fields with single cardinality
	private String[]		       m_values; // used for fields with multiple cardinality
	
	public GwtConfigParameter()
	{}

	public String getId() {
		return m_id;
	}

	public void setId(String id) {
		m_id = id;
	}

	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}

	public String getDescription() {
		return m_description;
	}

	public void setDescription(String description) {
		m_description = description;
	}

	public GwtConfigParameterType getType() {
		return m_type;
	}

	public void setType(GwtConfigParameterType type) {
		m_type = type;
	}

	public boolean isRequired() {
		return m_required;
	}

	public void setRequired(boolean required) {
		m_required = required;
	}

	public String getDefault() {
		return m_default;
	}

	public void setDefault(String default1) {
		m_default = default1;
	}

	public int getCardinality() {
		return m_cardinality;
	}

	public void setCardinality(int cardinality) {
		m_cardinality = cardinality;
	}
	
	public Map<String, String> getOptions() {
		return m_options;
	}
	
	public void setOptions(Map<String, String> options) {
		m_options = options;
	}

	public String getMin() {
		return m_min;
	}

	public void setMin(String min) {
		m_min = min;
	}

	public String getMax() {
		return m_max;
	}

	public void setMax(String max) {
		m_max = max;
	}

	public String getValue() {
		return m_value;
	}

	public void setValue(String value) {
		m_value = value;
	}

	public String[] getValues() {
		return m_values;
	}

	public void setValues(String[] values) {
		m_values = values;
	}
}
