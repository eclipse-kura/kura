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

import static org.eclipse.kura.type.DataType.LONG;

import com.google.common.base.MoreObjects;

/**
 * This class represents a {@link Long} value as a {@link TypedValue}.
 */
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
	public String toString() {
		return MoreObjects.toStringHelper(this).add("long_value", this.value).toString();
	}
}
