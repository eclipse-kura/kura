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

import static org.eclipse.kura.type.DataType.BOOLEAN;

import com.google.common.base.MoreObjects;

/**
 * This class represents a {@link Boolean} value as a {@link TypedValue}.
 */
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
		this.value = value;
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
	public String toString() {
		return MoreObjects.toStringHelper(this).add("boolean_value", this.value).toString();
	}
}
