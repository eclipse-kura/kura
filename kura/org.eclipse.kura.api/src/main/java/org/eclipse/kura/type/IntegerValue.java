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

import static org.eclipse.kura.type.DataType.INTEGER;

import com.google.common.base.MoreObjects;

/**
 * This class represents a {@link Integer} value as a {@link TypedValue}.
 */
public final class IntegerValue implements TypedValue<Integer> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final int value;

	/**
	 * Instantiates a new integer value.
	 *
	 * @param value
	 *            the value
	 */
	public IntegerValue(final int value) {
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return INTEGER;
	}

	/** {@inheritDoc} */
	@Override
	public Integer getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("integer_value", this.value).toString();
	}
}
