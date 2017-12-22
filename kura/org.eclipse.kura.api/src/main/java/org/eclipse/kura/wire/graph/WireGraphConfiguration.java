/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.wire.graph;

import java.util.Collections;
import java.util.List;

import org.eclipse.kura.wire.WireConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class represents the entire Wire Graph. It contains both the list of
 * components and the list of arcs that represent the current Wire Graph.<br>
 * <br>
 *
 * @see WireComponentConfiguration
 * @see WireConfiguration
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.4
 */
@ProviderType
public class WireGraphConfiguration {

    private final List<WireComponentConfiguration> wireComponentConfigurations;
    private final List<MultiportWireConfiguration> wireConfigurations;

    public WireGraphConfiguration(List<WireComponentConfiguration> wireComponentConfigurations,
            List<MultiportWireConfiguration> wireConfigurations) {
        this.wireComponentConfigurations = Collections.unmodifiableList(wireComponentConfigurations);
        this.wireConfigurations = Collections.unmodifiableList(wireConfigurations);
    }

    public List<WireComponentConfiguration> getWireComponentConfigurations() {
        return this.wireComponentConfigurations;
    }

    public List<MultiportWireConfiguration> getWireConfigurations() {
        return this.wireConfigurations;
    }

}
