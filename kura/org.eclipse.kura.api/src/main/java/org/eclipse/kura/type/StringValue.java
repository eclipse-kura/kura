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

import static org.eclipse.kura.Preconditions.checkNonInstance;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.type.DataType.STRING;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
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
	private final String value;

	/**
	 * Instantiates a new string value.
	 *
	 * @param value
	 *            the value
	 */
	public StringValue(@Nullable final String value) {
		this.value = Strings.nullToEmpty(value);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("rawtypes")
	public int compareTo(final TypedValue otherTypedValue) {
		checkNull(otherTypedValue, "Typed Value cannot be null");
		checkNonInstance(otherTypedValue, StringValue.class, "Typed Value is not string");

		return ComparisonChain.start().compare(this.value, ((StringValue) (otherTypedValue)).getValue()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof StringValue) {
			return Objects.equal(((StringValue) obj).getValue(), this.value);
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
		return MoreObjects.toStringHelper(this).add("string_value", this.value).toString();
	}
}
