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
