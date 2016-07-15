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

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.type.DataType.LONG;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * This class represents a {@link Long} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class LongValue implements TypedValue<Long> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final long value;

	/**
	 * Instantiates a new long value.
	 *
	 * @param value
	 *            the value
	 */
	public LongValue(final long value) {
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final TypedValue<Long> otherTypedValue) {
		checkNull(otherTypedValue, "Typed Value cannot be null");
		return Long.compare(this.value, otherTypedValue.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final LongValue other = (LongValue) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return LONG;
	}

	/** {@inheritDoc} */
	@Override
	public Long getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (int) (this.value ^ (this.value >>> 32));
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "LongValue [value=" + this.value + "]";
	}

}
