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
 *******************************************************************************/
package org.eclipse.kura.wire.provider.test;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class StubReceiver represents a stub Wire Receiver Component
 */
public final class StubReceiver implements ConfigurableComponent, WireComponent, WireReceiver {

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        // no need
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] arg0) {
        // no need
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire arg0, final Object arg1) {
        // no need
    }

}
