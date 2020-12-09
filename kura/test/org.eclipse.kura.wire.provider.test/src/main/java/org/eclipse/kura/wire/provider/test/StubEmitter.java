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
