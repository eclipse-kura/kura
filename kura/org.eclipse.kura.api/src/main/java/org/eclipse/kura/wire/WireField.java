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
package org.eclipse.kura.wire;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.SeverityLevel.CONFIG;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

/**
 * The {@code WireField} represents an associated type to be contained in {@link WireRecord}. Every
 * {@code WireField} represents an association of a name and a value. Every {@code WireField} is
 * also capable of denoting the {@code SeverityLevel} of this association.
 * <br/>
 * <br/>
 * Using such {@code WireField}, any type of primitive data (see {@link DataType}) can be represented.
 * For instance, a data retrieved from a measurement point from a device can be represented as the
 * following.
 * <br/>
 *
 * <pre>
 * <code>
 * name = LED
 * value = true
 * level = INFO
 * </pre>
 * </code>
 * <br/>
 * Clients are hence intended to create instances of {@code WireField} to represent their data.
 * <br/>
 * <br/>
 * <b>N.B:</b> For optimized performance in Kura Wires, every {@link WireField} name is internally
 * suffixed with {@code .v} that represents associated value if and only if the {@code WireField}
 * associates {@link SeverityLevel#INFO} or {@link SeverityLevel#CONFIG}.
 * <br/>
 * <br/>
 * Otherwise (in case of {@link SeverityLevel#SEVERE} and {@link SeverityLevel#ERROR}), name is
 * suffixed with {@code .e}
 * <br/>
 * <br/>
 * For aforementioned example, the name would then be {@code LED.v} as the associated severity level
 * is {@link SeverityLevel#INFO}
 * <br/>
 * <br/>
 *
 *
 *
 * @see {@link SeverityLevel}
 * @See {@link DataType}
 * @see {@link TypedValue}
 * @see {@link TypedValues}
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireField {

    /** Suffix for Wire Field name of ERROR or SEVERE Severity Level */
    public static final String ERROR_SUFFIX = ".e";

    /** Suffix for Wire Field name of INFO or CONFIG Severity Level */
    public static final String INFO_SUFFIX = ".v";

    /** The severity level of the field */
    private final SeverityLevel level;

    /** The name of the field */
    private final String name;

    /** The value as contained */
    private final TypedValue<?> value;

    /**
     * Instantiates a new wire field.
     *
     * @param name
     *            the name
     * @param value
     *            the value
     * @param level
     *            the severity level
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public WireField(final String name, final TypedValue<?> value, final SeverityLevel level) {
        requireNonNull(name, "Wire field name cannot be null");
        requireNonNull(value, "Wire field value type cannot be null");
        requireNonNull(level, "Wire field severity level cannot be null");

        this.level = level;
        this.name = suffixName(name, level);
        this.value = value;
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
        final WireField other = (WireField) obj;
        if (this.level != other.level) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the name of the field
     *
     * @return the name of the field
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the severity level of the field
     *
     * @return the severity level of the field
     */
    public SeverityLevel getSeverityLevel() {
        return this.level;
    }

    /**
     * Gets the contained value
     *
     * @return the contained value
     */
    public TypedValue<?> getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.level == null) ? 0 : this.level.hashCode());
        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
        result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    /**
     * Suffixes the name with .v in case of INFO or CONFIG severity level, otherwise .e
     *
     * @param name
     *            the name to be suffixed with
     * @param level
     *            the level to check
     * @return suffixed string representation
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private String suffixName(final String name, final SeverityLevel level) {
        requireNonNull(name, "Wire field name cannot be null");
        requireNonNull(level, "Wire field severity level cannot be null");

        if ((level == INFO) || (level == CONFIG)) {
            return name + INFO_SUFFIX;
        }
        return name + ERROR_SUFFIX;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireField [level=" + this.level + ", name=" + this.name + ", value=" + this.value + "]";
    }

}
