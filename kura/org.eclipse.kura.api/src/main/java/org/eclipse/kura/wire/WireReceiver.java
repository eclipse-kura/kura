/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Consumer;

/**
 * The WireReceiver interface Represents a wire component which is a data
 * consumer that can receive produced or emitted values from upstream
 * {@link WireEmitter}.
 * @since 1.2
 */
@ConsumerType
public interface WireReceiver extends WireComponent, Consumer {

    /**
     * Triggers when the wire component receives a {@link WireEnvelope}
     *
     * @param wireEnvelope
     *            the received {@link WireEnvelope}
     * @throws NullPointerException
     *             if the argument is null
     */
    public void onWireReceive(WireEnvelope wireEnvelope);
}
