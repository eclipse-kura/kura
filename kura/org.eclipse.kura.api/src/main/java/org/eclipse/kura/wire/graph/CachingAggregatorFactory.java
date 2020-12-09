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
 ******************************************************************************/
package org.eclipse.kura.wire.graph;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A marker interface for a factory that returns a {@link PortAggregator} that acts as a cache for a provided set of
 * input ports.
 * The cache behaves as follows:
 *
 * <ul>
 * <li>It maintains, for each port, a slot that can contain a WireEnvelope.</li>
 * <li>When a WireEnvelope is received on a port, the corresponding slot is filled with it. If the slot is not empty,
 * its content is replaced</li>
 * <li>When a WireEnvelope is received, the current contents of the slots are provided to the registered callback, even
 * if some of the slots are empty.</li>
 * <li>The slots are never cleared.</li>
 * </ul>
 *
 * @since 1.4
 */
@ProviderType
public interface CachingAggregatorFactory extends PortAggregatorFactory {

}
