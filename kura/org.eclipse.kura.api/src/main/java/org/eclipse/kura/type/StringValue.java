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

import static org.eclipse.kura.type.DataType.STRING;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * This class represents a {@link Short} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class StringValue implements TypedValue<String> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final String m_value;

	/**
	 * Instantiates a new string value.
	 *
	 * @param value
	 *            the value
	 */
	public StringValue(final String value) {
		this.m_value = value;
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public int compareTo(final TypedValue otherTypedValue) {
		if (!(otherTypedValue instanceof StringValue)) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, "Typed Value is not string");
		}
		return ComparisonChain.start().compare(this.m_value, ((StringValue) (otherTypedValue)).getValue()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof StringValue) {
			return Objects.equal(((StringValue) obj).getValue(), this.m_value);
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return STRING;
	}

	/** {@inheritDoc} */
	@Override
	public String getValue() {
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
		return MoreObjects.toStringHelper(this).add("string_value", this.m_value).toString();
	}
}
