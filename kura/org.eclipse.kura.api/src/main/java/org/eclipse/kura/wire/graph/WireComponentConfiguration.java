/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.graph;

import java.util.Collections;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class represents the single Wire Graph Component configuration. It
 * contains the component configuration description and additional properties
 * that can be used for the component displaying in the composer.<br>
 * <br>
 *
 * @see ComponentConfiguration
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.4
 */
@ProviderType
public class WireComponentConfiguration {

    private final ComponentConfiguration configuration;
    private final Map<String, Object> properties;

    public WireComponentConfiguration(ComponentConfiguration configuration, Map<String, Object> properties) {
        this.configuration = configuration;
        this.properties = Collections.unmodifiableMap(properties);
    }

    public ComponentConfiguration getConfiguration() {
        return this.configuration;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

}
