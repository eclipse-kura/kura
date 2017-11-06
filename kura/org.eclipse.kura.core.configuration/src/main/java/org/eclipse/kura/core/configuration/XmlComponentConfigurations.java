/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
