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
package org.eclipse.kura.type;

import static org.eclipse.kura.type.DataType.BYTE;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * This class represents a {@link Byte} value as a {@link TypedValue}.
 */
public final class ByteValue implements TypedValue<Byte> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final byte m_value;

	/**
	 * Instantiates a new byte value.
	 *
	 * @param value
	 *            the value
	 */
	public ByteValue(final byte value) {
		this.m_value = value;
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public int compareTo(final TypedValue otherTypedValue) {
		if (!(otherTypedValue instanceof ByteValue)) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Typed Value is not byte");
		}
		return ComparisonChain.start()
				.compare(this.m_value, ((ByteValue) (otherTypedValue)).getValue(), Ordering.natural()).result();
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return BYTE;
	}

	/** {@inheritDoc} */
	@Override
	public Byte getValue() {
		return this.m_value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("byte_value", this.m_value).toString();
	}
}
