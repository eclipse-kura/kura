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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * The WireField represents an abstract data type to be used in
 * {@link WireRecord}
 */
@Immutable
@ThreadSafe
public final class WireField implements Comparable<WireField> {

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
	public int compareTo(final WireField otherWireField) {
		return ComparisonChain.start().compare(this.name, otherWireField.getName())
				.compare(this.value, otherWireField.getValue()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof WireField) {
			final WireField wf = (WireField) obj;
			return Objects.equal(wf.getValue(), this.value) && Objects.equal(wf.getName(), this.name);
		}
		return false;
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
		return Objects.hashCode(this.value, this.name);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.name).add("value", this.value).toString();
	}
}
