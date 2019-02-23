/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  
 *******************************************************************************/
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
