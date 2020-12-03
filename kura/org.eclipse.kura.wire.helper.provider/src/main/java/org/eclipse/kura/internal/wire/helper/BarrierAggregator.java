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

public class BarrierAggregator implements PortAggregator {

    private int fullSlots = 0;
    private final List<WireEnvelope> envelopes;
    private Consumer<List<WireEnvelope>> consumer = envelopes -> {
        // do nothing
    };

    public BarrierAggregator(List<ReceiverPort> ports) {
        requireNonNull(ports);
        envelopes = new ArrayList<>(ports.size());

        for (int i = 0; i < ports.size(); i++) {
            envelopes.add(null);
            final Integer port = i;

            ports.get(i).onWireReceive(envelope -> {
                synchronized (envelopes) {
                    if (envelopes.get(port) == null) {
                        fullSlots++;
                    }
                    envelopes.set(port, envelope);
                    if (fullSlots == envelopes.size()) {
                        consumer.accept(envelopes);
                        clearSlots();
                    }
                }
            });
        }
    }

    private void clearSlots() {
        fullSlots = 0;
        for (int i = 0; i < envelopes.size(); i++) {
            envelopes.set(i, null);
        }
    }

    @Override
    public void onWireReceive(Consumer<List<WireEnvelope>> consumer) {
        requireNonNull(consumer);
        this.consumer = consumer;
    }

}
