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

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class AssetRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated driver.
 *
 * @noextend This class is not intended to be extended by clients.
 */
@NotThreadSafe
public class AssetRecord {

    /** The asset status. */
    private AssetStatus assetStatus;

    /**
     * The associated channel identifier. The channel identifier for any asset
     * must be unique.
     */
    private final String channelName;

    /** The timestamp of the record. */
    private long timestamp;

    /**
     * Represents the value as read by the driver during a read or a monitor
     * operation. It can also represent the value which needs to be written by
     * the driver to the actual asset.
     */
    private TypedValue<?> value;

    /**
     * Instantiates a new asset record.
     *
     * @param channelName
     *            the channel name
     * @throws NullPointerException
     *             if the provided channel name is null
     * @throws IllegalArgumentException
     *             if the channel identifier is not valid
     */
    public AssetRecord(final String channelName) {
        requireNonNull(channelName, "The provided channel name cannot be null");
        if (!Channel.isValidChannelName(channelName)) {
            throw new IllegalArgumentException("The provided channel name is not valid");
        }
        this.channelName = channelName;
    }

    /**
     * Gets the asset status.
     *
     * @return the asset status
     */
    public AssetStatus getAssetStatus() {
        return this.assetStatus;
    }

    /**
     * Gets the channel identifier.
     *
     * @return the channel identifier
     */
    public String getChannelName() {
        return this.channelName;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public TypedValue<?> getValue() {
        return this.value;
    }

    /**
     * Sets the asset status as provided.
     *
     * @param assetStatus
     *            the new asset status
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setAssetStatus(final AssetStatus assetStatus) {
        requireNonNull(assetStatus, "Asset status cannot be null");
        this.assetStatus = assetStatus;
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
     * Sets the value as provided.
     *
     * @param value
     *            the new value
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setValue(final TypedValue<?> value) {
        requireNonNull(value, "Value type cannot be null");
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AssetRecord [assetStatus=" + this.assetStatus + ", channelName=" + this.channelName + ", timestamp="
                + this.timestamp + ", value=" + this.value + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assetStatus == null) ? 0 : assetStatus.hashCode());
        result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
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
        AssetRecord other = (AssetRecord) obj;
        if (assetStatus == null) {
            if (other.assetStatus != null)
                return false;
        } else if (!assetStatus.equals(other.assetStatus))
            return false;
        if (channelName == null) {
            if (other.channelName != null)
                return false;
        } else if (!channelName.equals(other.channelName))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
