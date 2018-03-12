/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import java.util.Collections;
import java.util.List;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.BasicEnvelope;
import org.osgi.service.wireadmin.Envelope;

/**
 * The Class WireEnvelope represents a composite envelope to be used as an
 * abstract data to be transmitted between the wire emitter and the wire
 * receiver
 *
 * @see Envelope
 * @see BasicEnvelope
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class WireEnvelope extends BasicEnvelope {

    /**
     * The scope as agreed by the composite producer and consumer. This remains same
     * for all the Kura Wires communications.
     */
    private static final String SCOPE = "WIRES";

    /**
     * Instantiates a new WireEnvelope.
     *
     * @param emitterPid
     *            the wire emitter PID
     * @param wireRecords
     *            the {@link WireRecord}s
     */
    public WireEnvelope(final String emitterPid, final List<WireRecord> wireRecords) {
        super(Collections.unmodifiableList(wireRecords), emitterPid, SCOPE);
    }

    /**
     * Gets the wire emitter PID.
     *
     * @return the wire emitter PID
     */
    public String getEmitterPid() {
        return (String) getIdentification();
    }

    /**
     * Gets the {@link WireRecord}s.
     *
     * @return the {@link WireRecord}s
     */
    @SuppressWarnings("unchecked")
    public List<WireRecord> getRecords() {
        return (List<WireRecord>) getValue();
    }
}
