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

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class TypedValues is an utility class to quickly create different
 * {@link TypedValue}
 */
public final class TypedValues {
	
	/** Constructor */
	private TypedValues() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Creates new boolean value.
	 *
	 * @param value
	 *            the primitive boolean value
	 * @return the boolean value represented as {@link TypedValue}
	 */
	public static BooleanValue newBooleanValue(final boolean value) {
		return new BooleanValue(value);
	}

	/**
	 * Creates new byte array value.
	 *
	 * @param value
	 *            the primitive byte array value
	 * @return the byte array value represented as {@link TypedValue}
	 */
	public static ByteArrayValue newByteArrayValue(final byte[] value) {
		return new ByteArrayValue(value);
	}

	/**
	 * Creates new byte value.
	 *
	 * @param value
	 *            the primitive byte value
	 * @return the byte value represented as {@link TypedValue}
	 */
	public static ByteValue newByteValue(final byte value) {
		return new ByteValue(value);
	}

	/**
	 * Creates new double value.
	 *
	 * @param value
	 *            the primitive double value
	 * @return the double value represented as {@link TypedValue}
	 */
	public static DoubleValue newDoubleValue(final double value) {
		return new DoubleValue(value);
	}

	/**
	 * Creates new integer value.
	 *
	 * @param value
	 *            the primitive integer value
	 * @return the integer value represented as {@link TypedValue}
	 */
	public static IntegerValue newIntegerValue(final int value) {
		return new IntegerValue(value);
	}

	/**
	 * Creates new long value.
	 *
	 * @param value
	 *            the primitive long value
	 * @return the long value represented as {@link TypedValue}
	 */
	public static LongValue newLongValue(final long value) {
		return new LongValue(value);
	}

	/**
	 * Creates new short value.
	 *
	 * @param value
	 *            the primitive short value
	 * @return the short value represented as {@link TypedValue}
	 */
	public static ShortValue newShortValue(final short value) {
		return new ShortValue(value);
	}

	/**
	 * Creates new string value.
	 *
	 * @param value
	 *            the string value to be represented as {@link TypedValue}
	 * @return the string value represented as {@link TypedValue}
	 */
	public static StringValue newStringValue(@Nullable final String value) {
		return new StringValue(value);
	}

}
