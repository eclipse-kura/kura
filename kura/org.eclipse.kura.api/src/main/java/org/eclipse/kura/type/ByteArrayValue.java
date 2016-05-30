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

import static org.eclipse.kura.type.DataType.BYTE_ARRAY;

import java.util.Arrays;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.UnsignedBytes;

/**
 * This class represents a {@link Byte[]} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class ByteArrayValue implements TypedValue<byte[]> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final byte[] m_value;

	/**
	 * Instantiates a new byte array value.
	 *
	 * @param value
	 *            the value
	 */
	public ByteArrayValue(final byte[] value) {
		this.m_value = value;
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public int compareTo(final TypedValue otherTypedValue) {
		if (!(otherTypedValue instanceof ByteArrayValue)) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Typed Value is not byte array");
		}
		return ComparisonChain.start().compare(this.m_value, ((ByteArrayValue) (otherTypedValue)).getValue(),
				UnsignedBytes.lexicographicalComparator()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ByteArrayValue) {
			return Arrays.equals(((ByteArrayValue) obj).getValue(), this.m_value);
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return BYTE_ARRAY;
	}

	/** {@inheritDoc} */
	@Override
	public byte[] getValue() {
		return this.m_value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.m_value);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("byte_array_value", this.m_value).toString();
	}
}
