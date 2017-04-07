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
import org.eclipse.kura.wire.WireEmitter;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class StubEmitter represents a stub Wire Emitter Component
 */
public final class StubEmitter implements WireEmitter, WireComponent, ConfigurableComponent {

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] arg0) {
        // no need
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire arg0) {
        // no need
        return null;
    }

}
