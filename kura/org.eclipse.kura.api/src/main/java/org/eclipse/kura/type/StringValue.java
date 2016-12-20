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

import static org.eclipse.kura.type.DataType.STRING;
import static java.util.Objects.requireNonNull;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * This class represents a {@link Short} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class StringValue implements TypedValue<String> {

    /**
     * The actual contained value that will be represented as
     * {@link TypedValue}.
     */
    private final String value;

    /**
     * Instantiates a new string value.
     *
     * @param value
     *            the value
     */
    public StringValue(@Nullable final String value) {
        this.value = value == null ? "" : value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final TypedValue<String> otherTypedValue) {
        requireNonNull(otherTypedValue, "Typed Value cannot be null");
        return this.value.compareTo(otherTypedValue.getValue());
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
        final StringValue other = (StringValue) obj;
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public DataType getType() {
        return STRING;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "StringValue [value=" + this.value + "]";
    }

}
