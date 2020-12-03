/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

import java.util.List;

import org.eclipse.kura.configuration.ComponentConfiguration;

/**
 * Utility class to serialize a set of configurations.
 * This is used to serialize a full snapshot.
 */
public class XmlComponentConfigurations {

    private List<ComponentConfiguration> configurations;

    public XmlComponentConfigurations() {
    }

    public List<ComponentConfiguration> getConfigurations() {
        return this.configurations;
    }

    public void setConfigurations(List<ComponentConfiguration> configurations) {
        this.configurations = configurations;
    }
}
