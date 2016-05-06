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

import org.osgi.service.wireadmin.Consumer;

/**
 * Represents a wire component which is a data consumer that can receive
 * produced or emitted values from {@link WireEmitter}.
 */
public interface WireReceiver extends WireComponent, Consumer {

	/**
	 * Triggers when the wire component receives an {@link WireEnvelope}
	 *
	 * @param wireEnvelope
	 *            the received wire envelope
	 */
	public void onWireReceive(WireEnvelope wireEnvelope);
}
