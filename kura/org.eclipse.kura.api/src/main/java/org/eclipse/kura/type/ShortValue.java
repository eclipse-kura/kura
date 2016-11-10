/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.type;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.type.DataType.SHORT;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * This class represents a {@link Short} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class ShortValue implements TypedValue<Short> {

    /**
     * The actual contained value that will be represented as
     * {@link TypedValue}.
     */
    private final short value;

    /**
     * Instantiates a new short value.
     *
     * @param value
     *            the value
     */
    public ShortValue(final short value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TypedValue<Short> otherTypedValue) {
        checkNull(otherTypedValue, "Typed Value cannot be null");
        return Short.valueOf(this.value).compareTo(otherTypedValue.getValue());
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
        final ShortValue other = (ShortValue) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getType() {
        return SHORT;
    }

    /** {@inheritDoc} */
    @Override
    public Short getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.value;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ShortValue [value=" + this.value + "]";
    }

}
