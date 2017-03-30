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

import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.metatype.Tocd;

public class ComponentConfigurationImpl implements ComponentConfiguration {

    protected String pid;

    protected Tocd definition;

    protected Map<String, Object> properties;

    /**
     * Default constructor. Does not initialize any of the fields.
     */
    // Required by JAXB
    public ComponentConfigurationImpl() {
    }

    public ComponentConfigurationImpl(String pid, Tocd definition, Map<String, Object> properties) {
        super();
        this.pid = pid;
        this.definition = definition;
        this.properties = properties;
    }

    @Override
    public String getPid() {
        return this.pid;
    }

    @Override
    public Tocd getDefinition() {
        return this.definition;
    }

    @Override
    public Map<String, Object> getConfigurationProperties() {
        return this.properties;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setDefinition(Tocd definition) {
        this.definition = definition;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "ComponentConfigurationImpl [pid=" + pid + ", definition=" + definition + ", properties=" + properties
                + "]";
    }

}
