/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.kura.wire.WireEnvelope;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Allows to implement an aggregation strategy for a given set of ports. See {@link BarrierAggregatorFactory} and
 * {@link CachingAggregatorFactory} for examples of possible strategies.
 *
 * @since 1.4
 */
@ProviderType
public interface PortAggregator {

    public void onWireReceive(Consumer<List<WireEnvelope>> envelopes);
}
