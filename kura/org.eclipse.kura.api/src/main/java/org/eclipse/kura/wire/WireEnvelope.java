/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire;

import java.util.List;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.service.wireadmin.BasicEnvelope;
import org.osgi.service.wireadmin.Envelope;

/**
 * The Class WireEnvelope represents a composite envelope to be used as an
 * abstract data to be transmitted between the wire emitter and the wire
 * receiver
 *
 * @see Envelope
 * @see BasicEnvelope
 */
@Immutable
@ThreadSafe
public final class WireEnvelope extends BasicEnvelope {

    /**
     * The scope as agreed by the composite producer and consumer. This remains
     * same for all the Kura Wires communications.
     */
    private static final String SCOPE = "WIRES";

    /**
     * Instantiates a new WireEnvelope.
     *
     * @param emitterPid
     *            the wire emitter PID
     * @param wireRecords
     *            the wire records
     */
    public WireEnvelope(final String emitterPid, final List<WireRecord> wireRecords) {
        super(wireRecords, emitterPid, SCOPE);
    }

    /**
     * Gets the wire emitter PID.
     *
     * @return the wire emitter PID
     */
    public String getEmitterPid() {
        return (String) this.getIdentification();
    }

    /**
     * Gets the wire records.
     *
     * @return the wire records
     */
    @SuppressWarnings("unchecked")
    public List<WireRecord> getRecords() {
        return (List<WireRecord>) this.getValue();
    }

}
