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
package org.eclipse.kura.core.configuration;

/**
 * Helper class to serialize a property in XML.
 */
public class XmlConfigPropertyAdapted {

    public enum ConfigPropertyType {
        STRING_TYPE, LONG_TYPE, DOUBLE_TYPE, FLOAT_TYPE, INTEGER_TYPE, BYTE_TYPE, CHAR_TYPE, BOOLEAN_TYPE, SHORT_TYPE, PASSWORD_TYPE
    }

    private String name;

    private boolean array;

    private boolean encrypted;

    private ConfigPropertyType type;

    private String[] values;

    public XmlConfigPropertyAdapted() {
    }

    public XmlConfigPropertyAdapted(String name, ConfigPropertyType type, String[] values) {
        super();

        this.name = name;
        this.type = type;
        this.values = values;
        this.encrypted = false;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getArray() {
        return this.array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public ConfigPropertyType getType() {
        return this.type;
    }

    public void setType(ConfigPropertyType type) {
        this.type = type;
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String[] getValues() {
        return this.values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }
}
