/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.type;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface wraps a Java data type with its represented value format
 *
 * @param <T>
 *            The Java Value Type to be represented
 *
 * @see TypedValues
 * @see BooleanValue
 * @see ByteArrayValue
 * @see FloatValue
 * @see DoubleValue
 * @see IntegerValue
 * @see StringValue
 * @see DataType
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface TypedValue<T> extends Comparable<TypedValue<T>> {

    /**
     * Returns the data type of the represented value
     *
     * @return the datatype as associated
     */
    public DataType getType();

    /**
     * Returns the actual value as represented
     *
     * @return the value as associated
     */
    public T getValue();
}
