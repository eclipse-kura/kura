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

/**
 * This interface wraps a Java data type with its represented value format
 *
 * @param <T>
 *            The Java Value Type to be represented
 */
@SuppressWarnings("rawtypes")
public interface TypedValue<T> extends Comparable<TypedValue> {

	/**
	 * Returns the data type of the represented value
	 */
	public DataType getType();

	/**
	 * Returns the actual value as represented
	 */
	public T getValue();
}
