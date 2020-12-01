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

import org.eclipse.kura.wire.WireEnvelope;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface represents an emitter port
 *
 * @since 1.4
 */
@ProviderType
public interface EmitterPort extends Port {

    /**
     * This methods is invoked with the {@link WireEnvelope} that has to be sent to the other end of the wire.
     *
     * @param wireEnvelope
     *            the message that needs to be sent.
     */
    public void emit(WireEnvelope wireEnvelope);

}
