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

import org.osgi.service.wireadmin.Wire;

/**
 * This interface provides all necessary service methods to manipulate wire
 * mechanisms in Kura Wires topology.
 */
public interface WireService {

	/**
	 * Creates the wire between the provided emitter and the receiver
	 *
	 * @param emitterName
	 *            the name of the Wire Emitter
	 * @param receiverName
	 *            the name of the Wire Receiver
	 * @return the wire instance recently created
	 */
	public Wire createWire(String emitterName, String receiverName);

	/**
	 * Creates the wire component.
	 *
	 * @param factoryPid
	 *            the factory pid for the Wire Component
	 * @param name
	 *            the name for the Wire Component
	 * @return the wire component name which is recently created
	 */
	public String createWireComponent(String factoryPid, String name);

	/**
	 * Removes the wire between the provided wire emitter and the receiver
	 *
	 * @param emitterName
	 *            the name of the Wire Emitter
	 * @param receiverName
	 *            the name of the Wire Receiver
	 * @return true, if removal is successful
	 */
	public boolean removeWire(String emitterName, String receiverName);

	/**
	 * Removes the wire component.
	 *
	 * @param name
	 *            the name of the Wire Component
	 * @return true, if removal is successful
	 */
	public boolean removeWireComponent(String name);

}
