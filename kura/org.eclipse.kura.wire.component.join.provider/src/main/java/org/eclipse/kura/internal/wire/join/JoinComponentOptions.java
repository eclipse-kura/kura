/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.join;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.wire.graph.BarrierAggregatorFactory;
import org.eclipse.kura.wire.graph.CachingAggregatorFactory;
import org.eclipse.kura.wire.graph.PortAggregatorFactory;
import org.osgi.framework.BundleContext;

public class JoinComponentOptions {

    private static final String BARRIER_MODALITY_PROPERTY_KEY = "barrier";

    private static final boolean BARRIER_MODALITY_PROPERTY_DEFAULT = true;

    private final Map<String, Object> properties;
    private final BundleContext context;

    JoinComponentOptions(final Map<String, Object> properties, BundleContext context) {
        requireNonNull(properties, "Properties must be not null");
        this.properties = properties;
        this.context = context;
    }

    PortAggregatorFactory getPortAggregatorFactory() { // TODO fix service reference count
        final boolean useBarrier = (Boolean) properties.getOrDefault(BARRIER_MODALITY_PROPERTY_KEY,
                BARRIER_MODALITY_PROPERTY_DEFAULT);
        if (useBarrier) {
            return context.getService(context.getServiceReference(BarrierAggregatorFactory.class));
        } else {
            return context.getService(context.getServiceReference(CachingAggregatorFactory.class));
        }
    }

}
