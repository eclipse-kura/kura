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

package org.eclipse.kura.wire.graph;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A marker interface for a factory that returns a {@link PortAggregator} that acts as a barrier for a provided set of
 * input ports.
 * The barrier behaves as follows:
 * 
 * <ul>
 * <li>It maintains, for each port, a slot that can contain a WireEnvelope.</li>
 * <li>When a WireEnvelope is received on a port, the corresponding slot is filled with it. If the slot is not empty,
 * its content is replaced</li>
 * <li>When all slots are filled, their contents are provided to the registered callback.</li>
 * <li>The slots are cleared when the callback returns.</li>
 * </ul>
 * 
 * @since 1.4
 */
@ProviderType
public interface BarrierAggregatorFactory extends PortAggregatorFactory {

}
