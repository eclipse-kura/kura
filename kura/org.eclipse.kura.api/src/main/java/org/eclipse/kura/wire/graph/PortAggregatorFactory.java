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

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * 
 * Defines a factory service API that can be used to instantiate a specific {@link PortAggregator} strategy.
 * 
 * @since 1.4
 */
@ProviderType
public interface PortAggregatorFactory {

    public PortAggregator build(List<ReceiverPort> ports);

}
