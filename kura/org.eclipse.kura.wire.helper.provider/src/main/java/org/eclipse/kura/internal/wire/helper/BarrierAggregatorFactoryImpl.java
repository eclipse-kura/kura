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

package org.eclipse.kura.internal.wire.helper;

import java.util.List;

import org.eclipse.kura.wire.graph.BarrierAggregatorFactory;
import org.eclipse.kura.wire.graph.PortAggregator;
import org.eclipse.kura.wire.graph.ReceiverPort;

public class BarrierAggregatorFactoryImpl implements BarrierAggregatorFactory {

    @Override
    public PortAggregator build(List<ReceiverPort> ports) {

        return new BarrierAggregator(ports);
    }

}
