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

import java.util.Collections;
import java.util.Map;

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
     * Returns a boolean indicating if this channel is enabled or not
     *
     * @since 1.4
     * @return a boolean indicating if this channel is enabled or not
     */

    public boolean isEnabled() {
        return this.isEnabled;
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
     * Specifies if this channel is enabled or not
     *
     * @since 1.4
     * @param isEnabled
     *            a boolean indicating if this channel is enabled or not
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Channel [configuration=" + this.configuration + ", name=" + this.name + ", type=" + this.type
                + ", valueType=" + this.valueType + "]";
    }

    /**
     * Creates a new {@link ChannelRecord} that represents a read request
     * for the value of this {@code Channel}.
     *
     * @return
     *         the {@link ChannelRecord}
     */
    public ChannelRecord createReadRecord() {
        ChannelRecord result = ChannelRecord.createReadRecord(this.name, this.valueType);
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
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.configuration == null ? 0 : this.configuration.hashCode());
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());
        result = prime * result + (this.valueType == null ? 0 : this.valueType.hashCode());
        return result;
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
        if (this.configuration == null) {
            if (other.configuration != null) {
                return false;
            }
        } else if (!this.configuration.equals(other.configuration)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (this.valueType != other.valueType) {
            return false;
        }
        return true;
    }
}
