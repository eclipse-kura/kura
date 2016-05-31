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
package org.eclipse.kura.wire.util;

import java.sql.Timestamp;
import java.util.List;

import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.util.position.Position;

/**
 * The Class Wires is an utility class to provide quick operations for Kura
 * Wires.
 */
public final class Wires {

	/**
	 * Prepares new wire field.
	 *
	 * @param name
	 *            the name
	 * @param value
	 *            the value
	 * @return the wire field
	 */
	public static WireField newWireField(final String name, final TypedValue<?> value) {
		return new WireField(name, value);
	}

	/**
	 * Prepares new wire record.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param fields
	 *            the wire fields
	 * @return the wire record
	 */
	public static WireRecord newWireRecord(final Timestamp timestamp, final List<WireField> fields) {
		return new WireRecord(timestamp, fields);
	}

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
	 */
	public static WireRecord newWireRecord(final Timestamp timestamp, final Position position,
			final List<WireField> fields) {
		return new WireRecord(timestamp, position, fields);
	}

	/**
	 * Prepares new wire record.
	 *
	 * @param fields
	 *            the wire fields
	 * @return the wire record
	 */
	public static WireRecord newWireRecord(final WireField... fields) {
		return new WireRecord(fields);
	}

	/**
	 * Returns a Wire Support instance of the provided wire component
	 *
	 * @param wireComponent
	 *            the wire component
	 * @return the wire support instance
	 */
	public static WireSupport newWireSupport(final WireComponent wireComponent) {
		return new WireSupport(wireComponent);
	}

	/**
	 * Constructor.
	 */
	private Wires() {
		// Static Factory Methods container. No need to instantiate.
	}

}
