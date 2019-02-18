/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.binary;

/**
 * This class can be used to read/write a block of data in a {@link Buffer} to/from an instance of type T.
 *
 * @param <T>
 *            the type to be used for reading or writing.
 */
public interface BinaryData<T> {

    /**
     * @return the endianness of the data
     */
    public Endianness getEndianness();

    /**
     * @return the size of the data
     */
    public int getSize();

    /**
     * Writes the provided value into the provided {@link Buffer}
     *
     * @param buf
     *            a {@link Buffer} instance to be written
     * @param offset
     *            the offset at which the data will be written
     * @param value
     *            the value to be written
     */
    public abstract void write(Buffer buf, int offset, T value);

    /**
     *
     * @param buf
     *            a {@link Buffer} from which the data needs to be read
     * @param the
     *            offset from which the data will be read
     * @return the obtained value
     */
    public abstract T read(Buffer buf, int offset);

    public abstract Class<T> getValueType();
}
