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
import static org.eclipse.kura.type.DataType.BOOLEAN;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * This class represents a {@link Boolean} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class BooleanValue implements TypedValue<Boolean> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final boolean value;

	/**
	 * Instantiates a new boolean value.
	 *
	 * @param value
	 *            the value
	 */
	public BooleanValue(final boolean value) {
		checkNull(value, "Provided Typed Value cannot be null");
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final TypedValue<Boolean> otherTypedValue) {
		checkNull(otherTypedValue, "Typed Value cannot be null");
		return Boolean.compare(this.value, otherTypedValue.getValue());
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
		final BooleanValue other = (BooleanValue) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return BOOLEAN;
	}

	/** {@inheritDoc} */
	@Override
	public Boolean getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (this.value ? 1231 : 1237);
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "BooleanValue [value=" + this.value + "]";
	}

}
