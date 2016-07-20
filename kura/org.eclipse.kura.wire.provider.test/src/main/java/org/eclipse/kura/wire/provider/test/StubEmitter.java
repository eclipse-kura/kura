/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire.provider.test;

import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class StubEmitter represents a stub Wire Emitter Component
 */
public final class StubEmitter implements WireEmitter, WireComponent, ConfigurableComponent {

	protected synchronized void activate(final ComponentContext context) throws Exception {
		TimeUnit.SECONDS.sleep(1);
	}

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
