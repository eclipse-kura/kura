/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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
