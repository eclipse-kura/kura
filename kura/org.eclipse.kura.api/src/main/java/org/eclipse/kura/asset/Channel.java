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
package org.eclipse.kura.asset;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class Channel represents a communication channel of an asset. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write/monitor).
 *
 * @see AssetConfiguration
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class Channel {

    /** The communication channel configuration. */
    private final Map<String, Object> configuration;

    /** The name of the communication channel. */
    private String name;

    /** The type of the channel. (READ/WRITE/READ_WRITE) */
    private ChannelType type;

    /**
     * The data type of the value as expected from the operation
     */
    private DataType valueType;

    /**
     * Instantiates a new channel.
     *
     * @param name
     *            the name
     * @param type
     *            the type
     * @param valueType
     *            the value type
     * @param config
     *            the configuration
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             if channel name is not valid
     */
    public Channel(final String name, final ChannelType type, final DataType valueType,
            final Map<String, Object> config) {
        requireNonNull(name, "Channel name cannot be null");
        requireNonNull(type, "Channel type cannot be null");
        requireNonNull(valueType, "Channel value type cannot be null");
        requireNonNull(config, "Channel configuration cannot be null");

        if (!Channel.isValidChannelName(name)) {
            throw new IllegalArgumentException("Channel name is not valid");
        }

        this.configuration = config;
        this.name = name;
        this.type = type;
        this.valueType = valueType;
        config.put(AssetConstants.NAME.value(), name);
        config.put(AssetConstants.TYPE.value(), type.toString());
        config.put(AssetConstants.VALUE_TYPE.value(), valueType);
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Channel [configuration=" + this.configuration + ", name=" + this.name + ", type=" + this.type
                + ", valueType=" + this.valueType + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
        return result;
    }

    /**
     * Determines if the provided String is suitable to be used as a channel name;
     * 
     * @param channelName
     *            The String to be validated.
     * @return
     *         the result of the validation.
     */
    public static boolean isValidChannelName(String channelName) {
        if (isNull(channelName) || (channelName = channelName.trim()).isEmpty())
            return false;

        final String prohibitedChars = AssetConstants.CHANNEL_NAME_PROHIBITED_CHARS.value();

        for (int i = 0; i < channelName.length(); i++) {
            if (prohibitedChars.indexOf(channelName.charAt(i)) != -1) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Channel other = (Channel) obj;
        if (configuration == null) {
            if (other.configuration != null)
                return false;
        } else if (!configuration.equals(other.configuration))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        if (valueType != other.valueType)
            return false;
        return true;
    }
}
