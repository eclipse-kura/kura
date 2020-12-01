/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.wire;

import java.util.Collections;
import java.util.List;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.BasicEnvelope;

/**
 * The Class WireEnvelope represents a composite envelope to be used as an
 * abstract data to be transmitted between the wire emitter and the wire
 * receiver
 *
 * @see org.osgi.service.wireadmin.Envelope
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
