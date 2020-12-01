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

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Consumer;

/**
 * The WireReceiver interface Represents a wire component which is a data
 * consumer that can receive produced or emitted values from upstream
 * {@link WireEmitter}.
 *
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
