/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.TypedValue;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class ChannelRecord contains the information needed for performing a read or write
 * operation on a specific channel.<br/>
 * <br/>
 *
 * The possible cases for a channel record are the following:
 * <ul>
 *
 * <li>
 * Describing a read request: in this case the channel record must contain the channel
 * name and the data type to be read.
 * A channel record suitable for this use case can be created using the
 * {@link ChannelRecord#createReadRecord(String, DataType)},
 * {@link ChannelRecord#createReadRecord(String, DataType, double, double)} or {@link Channel#createReadRecord()}
 * methods.
 * </li>
 *
 * <li>
 * Describing a write request: in this case the channel record must contain the channel
 * name, the value to be written and its data type
 * A channel record suitable for this use case can be created using the
 * {@link ChannelRecord#createWriteRecord(String, TypedValue)} or {@link Channel#createWriteRecord(TypedValue)} methods.
 * </li>
 *
 * <li>
 * Reporting a status: in this case the channel record must contain the channel
 * name and a {@link ChannelStatus} instance.
 * The status contains a flag, an exception message and an exception
 * instance.
 * A channel record suitable for this use case can be created using the
 * {@link ChannelRecord#createStatusRecord(String, ChannelStatus)} method.
 * </li>
 *
 * </ul>
 *
 * A channel record might also contain an user defined configuration, specified
 * as a {@code Map<String, Object>} instance.
 * This configuration can be used to provide additional information concerning the
 * operation to be performed.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@ProviderType
@NotThreadSafe
public class ChannelRecord {

    private static final String VALUE_TYPE_CANNOT_BE_NULL = "Value Type cannot be null";

    private static final String CHANNEL_NAME_CANNOT_BE_NULL = "Channel Name cannot be null";

    /**
     * Provided channel configuration to perform read or write
     * operation.
     */
    private transient Map<String, Object> channelConfiguration;

    /**
     * Represents a channel specific status which signifies the status
     * of an operation performed on a channel.
     */
    private ChannelStatus channelStatus;

    /**
     * Represents the name of this channel
     */
    private String name;

    /**
     * Represents the type of the value involved in the read/write operation.
     */
    private DataType valueType;

    /**
     * Represents the value obtained/to be written from/to the channel
     */
    private TypedValue<?> value;

    private double valueScale;

    private double valueOffset;

    /** Represents the timestamp of the operation performed. */
    private long timestamp;

    private ChannelRecord() {
    }

    /**
     * Creates a channel record that represents a read request.
     *
     * @param channelName
     *            The name of the channel
     * @param valueType
     *            The type of the value to be read
     * @throws NullPointerException
     *             If any of the provided arguments is null
     * @return the channel record
     */
    public static ChannelRecord createReadRecord(final String channelName, final DataType valueType) {
        requireNonNull(channelName, CHANNEL_NAME_CANNOT_BE_NULL);
        requireNonNull(valueType, VALUE_TYPE_CANNOT_BE_NULL);

        ChannelRecord result = new ChannelRecord();
        result.name = channelName;
        result.valueType = valueType;
        result.valueScale = 1.0d;

        return result;
    }

    /**
     * Creates a channel record that represents a read request.
     *
     * @param channelName
     *            The name of the channel
     * @param valueType
     *            The type of the value to be read
     * @param valueScale
     *            The scaling factor to be applied to the value
     * @param valueOffset
     *            The offset to be applied to the value
     * @throws NullPointerException
     *             If any of the provided arguments is null
     * @return the channel record
     * @since 2.3
     */
    public static ChannelRecord createReadRecord(final String channelName, final DataType valueType,
            final double valueScale, final double valueOffset) {
        requireNonNull(channelName, CHANNEL_NAME_CANNOT_BE_NULL);
        requireNonNull(valueType, VALUE_TYPE_CANNOT_BE_NULL);

        ChannelRecord result = new ChannelRecord();
        result.name = channelName;
        result.valueType = valueType;
        result.valueScale = valueScale;
        result.valueOffset = valueOffset;

        return result;
    }

    /**
     * Creates a channel record that represents a write request.
     *
     * @param channelName
     *            The name of the channel
     * @param value
     *            The value to be written
     * @throws NullPointerException
     *             If any of the provided arguments is null
     * @return the channel record
     */
    public static ChannelRecord createWriteRecord(final String channelName, final TypedValue<?> value) {
        requireNonNull(channelName, CHANNEL_NAME_CANNOT_BE_NULL);
        requireNonNull(value, "Value cannot be null");

        ChannelRecord result = new ChannelRecord();
        result.name = channelName;
        result.valueType = value.getType();
        result.value = value;

        return result;
    }

    /**
     * Creates a channel record that describes the status of an operation.
     *
     * @param channelName
     *            The name of the channel
     * @param status
     *            The status
     * @throws NullPointerException
     *             If any of the provided arguments is null
     * @return the channel record
     */
    public static ChannelRecord createStatusRecord(final String channelName, final ChannelStatus status) {
        requireNonNull(channelName, CHANNEL_NAME_CANNOT_BE_NULL);
        requireNonNull(status, "Status cannot be null");

        ChannelRecord result = new ChannelRecord();
        result.name = channelName;
        result.channelStatus = status;

        return result;
    }

    /**
     * Returns the channel configuration as provided.
     *
     * @return the channel configuration
     */
    public Map<String, Object> getChannelConfig() {
        return this.channelConfiguration;
    }

    /**
     * Returns the channel operation status.
     *
     * @return the driver status
     */
    public ChannelStatus getChannelStatus() {
        return this.channelStatus;
    }

    /**
     * Returns the associated timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Sets the channel configuration as provided.
     *
     * @param channelConfig
     *            the channel configuration
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setChannelConfig(final Map<String, Object> channelConfig) {
        requireNonNull(channelConfig, "Channel configuration cannot be null");
        this.channelConfiguration = new HashMap<>(channelConfig);
    }

    /**
     * Sets the status.
     *
     * @param status
     *            the new driver status
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setChannelStatus(final ChannelStatus status) {
        requireNonNull(status, "Channel Status cannot be null");
        this.channelStatus = status;
    }

    /**
     * Sets the timestamp as provided.
     *
     * @param timestamp
     *            the new timestamp
     */
    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the name of the channel associated to the operation represented by this channel record
     *
     * @return the channel name
     */
    public String getChannelName() {
        return this.name;
    }

    /**
     * Returns the type of the value associated to the operation represented by this channel record
     *
     * @return the value type
     */
    public DataType getValueType() {
        return this.valueType;
    }

    /**
     * Returns the value associated to the operation represented by this channel record
     *
     * @return the value
     */
    public TypedValue<?> getValue() {
        if (this.valueType.equals(DataType.DOUBLE)) {
            return new DoubleValue((double) this.value.getValue() * this.valueScale + this.valueOffset);
        } else if (this.valueType.equals(DataType.FLOAT)) {
            return new FloatValue((float) this.value.getValue() * (float) this.valueScale + (float) this.valueOffset);
        } else if (this.valueType.equals(DataType.INTEGER)) {
            return new IntegerValue((int) this.value.getValue() * (int) this.valueScale + (int) this.valueOffset);
        } else if (this.valueType.equals(DataType.LONG)) {
            return new LongValue((long) this.value.getValue() * (long) this.valueScale + (long) this.valueOffset);
        }

        return this.value;
    }

    /**
     * Sets the value associated to the operation represented by this channel record
     *
     * @param value
     *            the value to be set
     * @throws NullPointerException
     *             if the provided value is null
     */
    public void setValue(TypedValue<?> value) {
        requireNonNull(value, "Value cannot be null");
        this.value = value;
    }

    @Override
    public String toString() {
        return "ChannelRecord [channelConfiguration=" + this.channelConfiguration + ", channelStatus="
                + this.channelStatus + ", name=" + this.name + ", valueType=" + this.valueType + ", value=" + this.value
                + ", valueScale=" + this.valueScale + ", valueOffset=" + this.valueOffset + ", timestamp="
                + this.timestamp + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.channelStatus == null ? 0 : this.channelStatus.hashCode());
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (int) (this.timestamp ^ this.timestamp >>> 32);
        result = prime * result + (this.value == null ? 0 : this.value.hashCode());
        long temp;
        temp = Double.doubleToLongBits(this.valueOffset);
        result = prime * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.valueScale);
        result = prime * result + (int) (temp ^ temp >>> 32);
        result = prime * result + (this.valueType == null ? 0 : this.valueType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ChannelRecord other = (ChannelRecord) obj;
        if (this.channelStatus == null) {
            if (other.channelStatus != null) {
                return false;
            }
        } else if (!this.channelStatus.equals(other.channelStatus)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.timestamp != other.timestamp) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        if (Double.doubleToLongBits(this.valueOffset) != Double.doubleToLongBits(other.valueOffset)
                || Double.doubleToLongBits(this.valueScale) != Double.doubleToLongBits(other.valueScale)
                || this.valueType != other.valueType) {
            return false;
        }
        return true;
    }

}
