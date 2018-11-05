/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
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
