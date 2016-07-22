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
package org.eclipse.kura.wire;

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The WireField represents an ADT (abstract data type) to be used in
 * {@link WireRecord}
 */
@Immutable
@ThreadSafe
public final class WireField {

	/** The name of the field */
	private final String name;

	/** The value as contained */
	private final TypedValue<?> value;

	/**
	 * Instantiates a new wire field.
	 *
	 * @param name
	 *            the name
	 * @param value
	 *            the value
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireField(final String name, final TypedValue<?> value) {
		checkNull(name, "Wire field name cannot be null");
		checkNull(value, "Wire field value type cannot be null");

		this.name = name;
		this.value = value;
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
		final WireField other = (WireField) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		if (this.value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!this.value.equals(other.value)) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the name of the field
	 *
	 * @return the name of the field
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the contained value
	 *
	 * @return the contained value
	 */
	public TypedValue<?> getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
		result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "WireField [name=" + this.name + ", value=" + this.value + "]";
	}

}
