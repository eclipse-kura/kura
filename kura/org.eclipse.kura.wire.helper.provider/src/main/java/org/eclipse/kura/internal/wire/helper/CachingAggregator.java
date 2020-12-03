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

package org.eclipse.kura.internal.wire.helper;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.graph.PortAggregator;
import org.eclipse.kura.wire.graph.ReceiverPort;

public class CachingAggregator implements PortAggregator {

    private List<WireEnvelope> envelopes;
    private Consumer<List<WireEnvelope>> consumer = envelopes -> {
        // do nothing
    };

    public CachingAggregator(List<ReceiverPort> ports) {
        requireNonNull(ports);
        this.envelopes = new ArrayList<>(ports.size());

        for (int i = 0; i < ports.size(); i++) {
            this.envelopes.add(null);
            final Integer port = i;
            ports.get(i).onWireReceive(envelope -> {
                synchronized (envelopes) {
                    envelopes.set(port, envelope);
                    consumer.accept(envelopes);
                }
            });
        }
    }

    @Override
    public void onWireReceive(Consumer<List<WireEnvelope>> consumer) {
        requireNonNull(consumer);
        this.consumer = consumer;
    }

}
