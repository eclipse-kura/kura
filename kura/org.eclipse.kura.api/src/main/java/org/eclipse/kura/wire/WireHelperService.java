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

import java.sql.Timestamp;
import java.util.List;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.type.TypedValue;
import org.osgi.util.position.Position;

/**
 * The interface WireHelperService is an service utility API to provide quick
 * and necessary operations for Kura Wires topology.
 */
public interface WireHelperService {

	/**
	 * Retrieves the factory PID (service.pid) of the provided wire component
	 * PID
	 *
	 * @param wireComponentPid
	 *            the wire component pid (kura.service.pid)
	 * @return the Service PID of the provided wire component or {@code null} if
	 *         the provided Wire Component PID is not associated with any
	 *         available Wire Component in the OSGi service registry
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public String getFactoryPid(final String wireComponentPid);

	/**
	 * Retrieves the factory PID (service.pid) of the wire component
	 *
	 * @param wireComponent
	 *            the wire component
	 * @return the Service PID of the provided wire component or {@code null} if
	 *         the provided Wire Component PID is not associated with any
	 *         available Wire Component in the OSGi service registry
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public String getFactoryPid(final WireComponent wireComponent);

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
	 * Instantiates a new wire configuration.
	 *
	 * @param emitterPid
	 *            the Wire Emitter PID
	 * @param receiverPid
	 *            the Wire Receiver PID
	 * @param filter
	 *            the filter
	 * @return the Wire Configuration
	 * @throws KuraRuntimeException
	 *             if emitter and/or receiver PID is null
	 */
	public WireConfiguration newWireConfiguration(String emitterPid, String receiverPid, @Nullable String filter);

	/**
	 * Instantiates a new wire envelope.
	 *
	 * @param emitterPid
	 *            the wire emitter PID
	 * @param wireRecords
	 *            the wire records
	 * @return the Wire envelope
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireEnvelope newWireEnvelope(String emitterPid, List<WireRecord> wireRecords);

	/**
	 * Prepares new wire field.
	 *
	 * @param name
	 *            the name
	 * @param value
	 *            the value
	 * @return the wire field
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireField newWireField(String name, TypedValue<?> value);

	/**
	 * Prepares new wire record.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param fields
	 *            the wire fields
	 * @return the wire record
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireRecord newWireRecord(Timestamp timestamp, List<WireField> fields);

	/**
	 * Prepares new wire record.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param position
	 *            the position
	 * @param fields
	 *            the wire fields
	 * @return the wire record
	 * @throws KuraRuntimeException
	 *             if timestamp or fields is null
	 */
	public WireRecord newWireRecord(Timestamp timestamp, @Nullable Position position, final List<WireField> fields);

	/**
	 * Prepares new wire record.
	 *
	 * @param fields
	 *            the wire fields
	 * @return the wire record
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public WireRecord newWireRecord(WireField... fields);

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
