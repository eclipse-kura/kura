/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.type;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.type.DataType.FLOAT;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents a {@link Float} value as a {@link TypedValue}.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class FloatValue implements TypedValue<Float> {

    /**
     * The actual contained value that will be represented as
     * {@link TypedValue}.
     */
    private final float value;

    /**
     * Instantiates a new {@link Float} value.
     *
     * @param value
     *            the value
     */
    public FloatValue(final float value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TypedValue<Float> otherTypedValue) {
        requireNonNull(otherTypedValue, "Typed Value cannot be null");
        return Float.valueOf(this.value).compareTo(otherTypedValue.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FloatValue)) {
            return false;
        }
        FloatValue other = (FloatValue) obj;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getType() {
        return FLOAT;
    }

    /** {@inheritDoc} */
    @Override
    public Float getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(value);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "FloatValue [value=" + this.value + "]";
    }
}
