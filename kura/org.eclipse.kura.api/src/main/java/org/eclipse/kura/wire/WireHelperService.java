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

/**
 * The interface WireHelperService is an service utility API to provide quick
 * and necessary operations for Kura Wires topology.
 */
public interface WireHelperService {

	/**
	 * Retrieves the Kura Service PID (kura.service.pid) of the wire component
	 *
	 * @param wireComponent
	 *            the wire component
	 * @return the Service PID of the provided wire component or {@code null} if
	 *         the provided Wire Component PID is not associated with any
	 *         available Wire Component in the OSGi service registry
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public String getPid(final WireComponent wireComponent);

	/**
	 * Retrieves the OSGi Component Service PID (service.pid) of the provided
	 * wire component PID
	 *
	 * @param wireComponentPid
	 *            the wire component PID (kura.service.pid)
	 * @return the Service PID of the provided wire component or {@code null} if
	 *         the provided Wire Component PID is not associated with any
	 *         available Wire Component in the OSGi service registry
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public String getServicePid(final String wireComponentPid);

	/**
	 * Retrieves the OSGi Component Service PID (service.pid) of the wire
	 * component
	 *
	 * @param wireComponent
	 *            the wire component
	 * @return the Service PID of the provided wire component or {@code null} if
	 *         the provided Wire Component PID is not associated with any
	 *         available Wire Component in the OSGi service registry
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public String getServicePid(final WireComponent wireComponent);

	/**
	 * Returns a Wire Support instance of the provided wire component
	 *
	 * @param wireComponent
	 *            the wire component
	 * @return the wire support instance
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public WireSupport newWireSupport(WireComponent wireComponent);

}
