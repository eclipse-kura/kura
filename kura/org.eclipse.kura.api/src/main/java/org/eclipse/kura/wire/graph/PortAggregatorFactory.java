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
