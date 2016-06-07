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
import static org.eclipse.kura.type.DataType.SHORT;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * This class represents a {@link Short} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class ShortValue implements TypedValue<Short> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final short value;

	/**
	 * Instantiates a new short value.
	 *
	 * @param value
	 *            the value
	 */
	public ShortValue(final short value) {
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final TypedValue<Short> otherTypedValue) {
		checkNull(otherTypedValue, "Typed Value cannot be null");
		return ComparisonChain.start().compare(this.value, otherTypedValue.getValue(), Ordering.natural()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ShortValue) {
			return Objects.equal(((ShortValue) obj).getValue(), this.value);
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return SHORT;
	}

	/** {@inheritDoc} */
	@Override
	public Short getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.value);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("short_value", this.value).toString();
	}
}
