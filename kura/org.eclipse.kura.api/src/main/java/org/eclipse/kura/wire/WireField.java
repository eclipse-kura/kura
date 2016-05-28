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

import org.eclipse.kura.type.TypedValue;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

/**
 * The WireField represents an abstract data type to be used in
 * {@link WireRecord}
 */
public final class WireField implements Comparable<WireField> {

	/** The name of the field */
	private final String m_name;

	/** The value as contained */
	private final TypedValue<?> m_value;

	/**
	 * Instantiates a new wire field.
	 *
	 * @param name
	 *            the name
	 * @param value
	 *            the value
	 */
	public WireField(final String name, final TypedValue<?> value) {
		this.m_name = name;
		this.m_value = value;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final WireField otherWireField) {
		return ComparisonChain.start().compare(this.m_name, otherWireField.getName())
				.compare(this.m_value, otherWireField.getValue()).result();
	}

	/**
	 * Gets the name of the field
	 *
	 * @return the name of the field
	 */
	public String getName() {
		return this.m_name;
	}

	/**
	 * Gets the contained value
	 *
	 * @return the contained value
	 */
	public TypedValue<?> getValue() {
		return this.m_value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.m_name).add("value", this.m_value).toString();
	}
}
