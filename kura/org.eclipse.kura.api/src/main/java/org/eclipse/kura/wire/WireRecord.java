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
import java.util.Date;
import java.util.List;

import org.osgi.util.position.Position;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;

/**
 * The Class WireRecord represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver
 */
public final class WireRecord implements Comparable<WireRecord> {

	/** The wire fields. */
	private final List<WireField> m_fields;

	/** The position. */
	private final Position m_position;

	/** The timestamp. */
	private final Timestamp m_timestamp;

	/**
	 * Instantiates a new wire record.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param fields
	 *            the fields
	 */
	public WireRecord(final Timestamp timestamp, final List<WireField> fields) {
		this.m_timestamp = timestamp;
		this.m_position = null;
		this.m_fields = ImmutableList.copyOf(fields);
	}

	/**
	 * Instantiates a new wire record.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param position
	 *            the position
	 * @param fields
	 *            the fields
	 */
	public WireRecord(final Timestamp timestamp, final Position position, final List<WireField> fields) {
		this.m_timestamp = timestamp;
		this.m_position = position;
		this.m_fields = ImmutableList.copyOf(fields);
	}

	/**
	 * Instantiates a new wire record.
	 *
	 * @param dataFields
	 *            the wire fields
	 */
	public WireRecord(final WireField... dataFields) {
		this.m_timestamp = new Timestamp(new Date().getTime());
		this.m_position = null;
		this.m_fields = ImmutableList.copyOf(dataFields);
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final WireRecord otherWireRecord) {
		return ComparisonChain.start().compare(this.m_timestamp, otherWireRecord.getTimestamp()).result();
	}

	/**
	 * Gets the associated fields.
	 *
	 * @return the fields
	 */
	public List<WireField> getFields() {
		return this.m_fields;
	}

	/**
	 * Gets the position.
	 *
	 * @return the position
	 */
	public Position getPosition() {
		return this.m_position;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return this.m_timestamp;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("wire_fields", this.m_fields).add("position", this.m_position)
				.add("timestamp", this.m_timestamp).toString();
	}
}
