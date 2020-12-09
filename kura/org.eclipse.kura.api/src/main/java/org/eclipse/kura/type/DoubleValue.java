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

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.type.DataType.DOUBLE;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents a {@link Double} value as a {@link TypedValue}.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class DoubleValue implements TypedValue<Double> {

    /**
     * The actual contained value that will be represented as
     * {@link TypedValue}.
     */
    private final double value;

    /**
     * Instantiates a new double value.
     *
     * @param value
     *            the value
     */
    public DoubleValue(final double value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TypedValue<Double> otherTypedValue) {
        requireNonNull(otherTypedValue, "Typed Value cannot be null");
        return Double.valueOf(this.value).compareTo(otherTypedValue.getValue());
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
        final DoubleValue other = (DoubleValue) obj;
        if (Double.doubleToLongBits(this.value) != Double.doubleToLongBits(other.value)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getType() {
        return DOUBLE;
    }

    /** {@inheritDoc} */
    @Override
    public Double getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(this.value);
        result = prime * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DoubleValue [value=" + this.value + "]";
    }

}
