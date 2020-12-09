/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.driver.binary;

import static java.util.Objects.requireNonNull;

public abstract class AbstractBinaryData<T> implements BinaryData<T> {

    protected final Endianness endianness;
    protected final int size;

    /**
     * Creates a new {@link BinaryData} instance.
     *
     * @param endianness
     *            the endianness of the data
     * @param size
     *            the size of the data
     */
    public AbstractBinaryData(Endianness endianness, int size) {
        requireNonNull(endianness, "Endianness cannot be null");
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.endianness = endianness;
        this.size = size;
    }

    /**
     * @return the endianness of the data
     */
    public Endianness getEndianness() {
        return this.endianness;
    }

    /**
     * @return the size of the data
     */
    public int getSize() {
        return this.size;
    }

}
