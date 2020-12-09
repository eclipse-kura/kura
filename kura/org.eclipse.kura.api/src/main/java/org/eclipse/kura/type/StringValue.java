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
import static org.eclipse.kura.type.DataType.STRING;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents a {@link String} value as a {@link TypedValue}.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class StringValue implements TypedValue<String> {

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
        result = prime * result + (this.value == null ? 0 : this.value.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "StringValue [value=" + this.value + "]";
    }

}
