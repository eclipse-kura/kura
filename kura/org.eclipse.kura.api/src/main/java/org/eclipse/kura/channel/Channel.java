/*******************************************************************************
 * Copyright (c) 2017, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.channel;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class Channel represents a communication channel. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write).
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class Channel {

    /** The communication channel configuration. */
    private final transient Map<String, Object> configuration;

    /** The name of the communication channel. */
    private String name;

    /** The type of the channel. (READ/WRITE/READ_WRITE) */
    private ChannelType type;

    /**
     * The data type of the value as expected from the operation
     */
    private DataType valueType;

    /**
     * The data type of the scale/offset as expected from the operation
     */
    private ScaleOffsetType scaleOffsetType = ScaleOffsetType.DEFINED_BY_VALUE_TYPE;

    /*
     * The value used to scale the value
     */
    private Number valueScale = 1.0d;

    /**
     * The value used as offset of the value
     */
    private Number valueOffset = 0.0d;

    private String unit = "";

    /**
     * Determines if this channel is enabled or not
     */
    private boolean isEnabled = true;

    /**
     * Instantiates a new channel.
     *
     * @param name
     *            the name for this channel
     * @param type
     *            the type
     * @param valueType
     *            the value type
     * @param config
     *            the configuration
     * @throws NullPointerException
     *             if any of the arguments is null
     */

    @Deprecated
    public Channel(final String name, final ChannelType type, final DataType valueType,
            final Map<String, Object> config) {

        requireNonNull(name, "Channel name cannot be null");
        requireNonNull(type, "Channel type cannot be null");
        requireNonNull(valueType, "Channel value type cannot be null");
        requireNonNull(config, "Channel configuration cannot be null");

        this.configuration = Collections.unmodifiableMap(config);
        this.name = name;
        this.type = type;
        this.valueType = valueType;
    }

    /**
     * Instantiates a new channel.
     *
     * @param name
     *            the name for this channel
     * @param type
     *            the type
     * @param valueType
     *            the value type
     * @param valueScale
     *            the value used to scale the value, must have the same {@link DataType} as valueOffset
     * @param valueOffset
     *            the value used as offset of the value, must have the same {@link DataType} as valueScale
     * @param config
     *            the configuration
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             if any of the valueScale and valueOffset have different types
     * 
     * @since 2.8
     */
    public Channel(final String name, final ChannelType type, final DataType valueType,
            final ScaleOffsetType scaleOffsetType, final Number valueScale, final Number valueOffset,
            final Map<String, Object> config) {

        requireNonNull(name, "Channel name cannot be null");
        requireNonNull(type, "Channel type cannot be null");
        requireNonNull(valueType, "Channel value type cannot be null");
        requireNonNull(config, "Channel configuration cannot be null");

        requireNonNull(scaleOffsetType, "Scale/Offset type cannot be null");
        requireNonNull(valueScale, "Channel value scale cannot be null");
        requireNonNull(valueOffset, "Channel value offset cannot be null");

        this.configuration = Collections.unmodifiableMap(config);
        this.name = name;
        this.type = type;
        this.valueType = valueType;

        this.scaleOffsetType = scaleOffsetType;
        this.valueScale = valueScale;
        this.valueOffset = valueOffset;

    }

    /**
     * Gets the configuration of the communication channel.
     *
     * @return the configuration of the communication channel
     */
    public Map<String, Object> getConfiguration() {
        return this.configuration;
    }

    /**
     * Gets the name of the communication channel.
     *
     * @return the name of the communication channel
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the type of the communication channel.
     *
     * @return the type of the communication channel
     */
    public ChannelType getType() {
        return this.type;
    }

    /**
     * Gets the value type as expected for operations.
     *
     * @return the value type
     */
    public DataType getValueType() {
        return this.valueType;
    }

    /**
     * Gets the Scale/Offset type as expected for operations.
     *
     * @return the value type
     * 
     * @since 2.8
     */
    public ScaleOffsetType getScaleOffsetType() {
        return this.scaleOffsetType;
    }

    /**
     * Returns a boolean indicating if this channel is enabled or not
     *
     * @since 1.4
     * @return a boolean indicating if this channel is enabled or not
     */

    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * Returns a double that represents the scale factor to be applied to the read value
     *
     * @return a double that represents the scale factor to be applied to the read value
     * 
     * @since 2.3
     * 
     * @deprecated Use {@link #getValueScaleAsNumber()}
     */
    @Deprecated
    public double getValueScale() {
        return this.valueScale.doubleValue();
    }

    /**
     * Returns a {@link Number} that represents the scale factor to be applied to the read
     * value
     *
     * @return a {@link Number} that represents the scale factor to be applied to the read value
     *
     * @since 2.8
     */
    public Number getValueScaleAsNumber() {
        return this.valueScale;
    }

    /**
     * Returns a double that represents the offset to be applied to the read value
     *
     * @return a double that represents the offset to be applied to the read value
     *
     * @since 2.3
     * 
     * @deprecated Use {@link #getValueOffsetAsNumber()}
     */
    @Deprecated
    public double getValueOffset() {
        return this.valueOffset.doubleValue();
    }

    /**
     * Returns a {@link TypedValue} that represents the offset factor to be applied to the read
     * value
     *
     * @return a {@link TypedValue} that represents the offset factor to be applied to the read value
     *
     * @since 2.8
     */
    public Number getValueOffsetAsNumber() {
        return this.valueOffset;
    }

    /**
     * @since 2.3
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * Sets the name.
     *
     * @param name
     *            the new name
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setName(final String name) {
        requireNonNull(name, "Channel name cannot be null");
        this.name = name;
    }

    /**
     * Sets the type.
     *
     * @param type
     *            the new type
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setType(final ChannelType type) {
        requireNonNull(type, "Channel type cannot be null");
        this.type = type;
    }

    /**
     * Sets the value type.
     *
     * @param valueType
     *            the new value type
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setValueType(final DataType valueType) {
        requireNonNull(valueType, "Channel value type cannot be null");
        this.valueType = valueType;
    }

    /**
     * Set the type of the scale/offset.
     * 
     * @param scaleOffsetType
     *            the scale/offset type
     * @throws NullPointerException
     *             if the argument is null
     * 
     * @since 2.8
     */
    public void setScaleOffsetType(ScaleOffsetType scaleOffsetType) {
        requireNonNull(valueType, "Scale/Offset value type cannot be null");
        this.scaleOffsetType = scaleOffsetType;
    }

    /**
     * Specifies if this channel is enabled or not
     *
     * @since 1.4
     * @param isEnabled
     *            a boolean indicating if this channel is enabled or not
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Specifies the scale to be applied to the channel value
     *
     * @param scale
     *            a double value that specifies the scale to be applied to the channel value
     * @since 2.3
     */
    public void setScale(double scale) {
        this.valueScale = scale;
    }

    /**
     * Specifies the scale to be applied to the channel value
     *
     * @param scale
     *            a {@link Number} value that specifies the scale to be applied to the channel value
     * @since 2.8
     */
    public void setScale(Number scale) {
        this.valueScale = scale;
    }

    /**
     * Specifies the offset to be applied to the channel value
     *
     * @param offset
     *            a double value that specifies the offset to be applied to the channel value
     * @since 2.3
     */
    public void setOffset(double offset) {
        this.valueOffset = offset;
    }

    /**
     * Specifies the offset to be applied to the channel value
     *
     * @param offset
     *            a {@link Number} value that specifies the offset to be applied to the channel value
     * @since 2.8
     */
    public void setOffset(Number offset) {
        this.valueOffset = offset;
    }

    /**
     * @since 2.3
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Channel [configuration=" + this.configuration + ", name=" + this.name + ", type=" + this.type
                + ", valueType=" + this.valueType + ", valueScale=" + this.valueScale + ", valueOffset="
                + this.valueOffset + ", valueType=" + this.valueType + ", unit=" + this.unit + "]";
    }

    /**
     * Creates a new {@link ChannelRecord} that represents a read request
     * for the value of this {@code Channel}.
     *
     * @return
     *         the {@link ChannelRecord}
     */
    public ChannelRecord createReadRecord() {
        ChannelRecord result = ChannelRecord.createReadRecord(this.name, this.valueType, this.unit);
        result.setChannelConfig(this.configuration);

        return result;
    }

    /**
     * Creates a new {@link ChannelRecord} that represents a write request for this
     * {@code Channel}.
     *
     * @param vlaue
     *            The value to be written.
     * @throws IllegalArgumentException
     *             If the {@link DataType} of the provided value differs from the data type
     *             of this channel
     * @throws NullPointerException
     *             If the provided value is null
     * @return
     *         the {@link CheannelRecord}
     */
    public ChannelRecord createWriteRecord(TypedValue<?> value) {
        requireNonNull(value, "Value cannot be null");
        if (value.getType() != this.valueType) {
            throw new IllegalArgumentException("The value type of the argument must match the channel value type");
        }
        ChannelRecord result = ChannelRecord.createWriteRecord(this.name, value);
        result.setChannelConfig(this.configuration);

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.isEnabled, this.name, this.scaleOffsetType, this.type, this.unit, this.valueOffset,
                this.valueScale, this.valueType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Channel other = (Channel) obj;
        return this.isEnabled == other.isEnabled && Objects.equals(this.name, other.name)
                && this.scaleOffsetType == other.scaleOffsetType && this.type == other.type
                && Objects.equals(this.unit, other.unit) && Objects.equals(this.valueOffset, other.valueOffset)
                && Objects.equals(this.valueScale, other.valueScale) && this.valueType == other.valueType;
    }

}
