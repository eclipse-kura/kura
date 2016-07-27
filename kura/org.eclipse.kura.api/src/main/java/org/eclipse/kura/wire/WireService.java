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

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;

/**
 * This WireService interface provides all necessary service API methods to
 * manipulate wire mechanisms in Kura Wires topology.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireService {

	/**
	 * Creates the wire configuration for the provided wire emitter and the wire
	 * receiver
	 *
	 * @param emitterPid
	 *            the PID of the wire emitter (this PID will internally be used
	 *            to retrieve kura.service.pid property of any matching DS
	 *            component)
	 * @param receiverPid
	 *            the PID of the wire receiver (this PID will internally be used
	 *            to retrieve kura.service.pid property of any matching DS
	 *            component)
	 * @throws KuraException
	 *             if there doesn't exist any Wire Component having provided
	 *             emitter PID or any Wire Component having provided receiver
	 *             PID
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 * @return the wire instance recently created
	 */
	public WireConfiguration createWireConfiguration(String emitterPid, String receiverPid) throws KuraException;

	/**
	 * Removes the provided wire configuration for the provided wire emitter and
	 * the wire receiver
	 *
	 * @param wireConfiguration
	 *            the wire configuration to be deleted
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void deleteWireConfiguration(WireConfiguration wireConfiguration);

	/**
	 * Retrieves the list of already created Wire Configurations
	 *
	 * @return the list of wire configurations
	 */
	public List<WireConfiguration> getWireConfigurations();

}
