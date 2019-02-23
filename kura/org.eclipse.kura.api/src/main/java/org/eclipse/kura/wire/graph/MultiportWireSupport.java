/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface extends {@link WireSupport} to provide multi-port support in Wires.
 * 
 * @since 1.4
 */
@ProviderType
public interface MultiportWireSupport extends WireSupport {

    /**
     * Returns the list of EmitterPorts of a Wire Component
     * @return a list of {@link EmitterPort}
     */
    public List<EmitterPort> getEmitterPorts();

    /**
     * Returns the list of ReceiverPorts associated to a Wire Component
     * @return a list of {@link ReceiverPort}
     */
    public List<ReceiverPort> getReceiverPorts();

    /**
     * This method allows to create a {@link WireEnvelope} from the list of {@link WireRecord} passed as an argument.
     * @param records a list of {@link WireRecord}s that will be wrapped into a {@link WireEnvelope}
     * @return a {@link WireEnvelope} that wraps the list of {@link WireRecord}s passed. 
     */
    public WireEnvelope createWireEnvelope(List<WireRecord> records);

}
