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
import org.osgi.service.wireadmin.Wire;

/**
 * This interface provides all necessary service methods to manipulate wire
 * mechanisms in Kura Wires topology.
 */
public interface WireService {

	/**
	 * Creates the wire between the provided wire emitter and the wire receiver
	 *
	 * @param emitterPid
	 *            the PID of the wire emitter
	 * @param receiverPid
	 *            the PID of the wire receiver
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 * @return the wire instance recently created
	 */
	public Wire createWire(String emitterPid, String receiverPid);

	/**
	 * Creates the wire component.
	 *
	 * @param factoryPid
	 *            the factory PID for the wire component
	 * @param name
	 *            the name for the wire component
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public void createWireComponent(String factoryPid, String name);

	/**
	 * Removes the wire between the provided wire emitter and the wire receiver
	 *
	 * @param emitterPid
	 *            the PID of the wire emitter
	 * @param receiverPid
	 *            the PID of the wire receiver
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 * @return true, if removal is successful
	 */
	public boolean removeWire(String emitterPid, String receiverPid);

	/**
	 * Removes the wire component.
	 *
	 * @param pid
	 *            the PID of the wire component
	 * @throws KuraRuntimeException
	 *             if argument is null
	 * @return true, if removal is successful
	 */
	public boolean removeWireComponent(String pid);

}
