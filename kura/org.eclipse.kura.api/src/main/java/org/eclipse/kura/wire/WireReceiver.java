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
package org.eclipse.kura.wire;

import org.eclipse.kura.KuraRuntimeException;
import org.osgi.service.wireadmin.Consumer;

/**
 * The WireReceiver interface Represents a wire component which is a data
 * consumer that can receive produced or emitted values from upstream
 * {@link WireEmitter}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireReceiver extends WireComponent, Consumer {

	/**
	 * Triggers when the wire component receives a {@link WireEnvelope}
	 *
	 * @param wireEnvelope
	 *            the received wire envelope
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void onWireReceive(WireEnvelope wireEnvelope);
}
