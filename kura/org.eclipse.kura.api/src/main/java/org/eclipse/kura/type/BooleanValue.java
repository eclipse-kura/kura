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

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.type.DataType.BOOLEAN;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents a {@link Boolean} value as a {@link TypedValue}.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class BooleanValue implements TypedValue<Boolean> {

    /**
     * The actual contained value that will be represented as
     * {@link TypedValue}.
     */
    private final boolean value;

    /**
     * Instantiates a new boolean value.
     *
     * @param value
     *            the value
     */
    public BooleanValue(final boolean value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TypedValue<Boolean> otherTypedValue) {
        requireNonNull(otherTypedValue, "Typed Value cannot be null");
        return (this.value == otherTypedValue.getValue()) ? 0 : (this.value ? 1 : -1);
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
        final BooleanValue other = (BooleanValue) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getType() {
        return BOOLEAN;
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (this.value ? 1231 : 1237);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "BooleanValue [value=" + this.value + "]";
    }

}
