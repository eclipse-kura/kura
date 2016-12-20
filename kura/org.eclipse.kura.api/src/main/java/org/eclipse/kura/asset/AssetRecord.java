/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.asset;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class AssetRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated driver.
 */
@NotThreadSafe
public final class AssetRecord {

    /** The asset status. */
    private AssetStatus assetStatus;

    /**
     * The associated channel identifier. The channel identifier for any asset
     * must be unique.
     */
    private final long channelId;

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
     * @param channelId
     *            the channel identifier
     * @throws IllegalArgumentException
     *             if the channel identifier is less than or equal to zero
     */
    public AssetRecord(final long channelId) {
        if (channelId <= 0) {
            throw new IllegalArgumentException("Channel ID cannot be zero or less");
        }
        this.channelId = channelId;
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
        final AssetRecord other = (AssetRecord) obj;
        if (this.assetStatus != other.assetStatus) {
            return false;
        }
        if (this.channelId != other.channelId) {
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
        return true;
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
    public long getChannelId() {
        return this.channelId;
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

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.assetStatus == null) ? 0 : this.assetStatus.hashCode());
        result = (prime * result) + (int) (this.channelId ^ (this.channelId >>> 32));
        result = (prime * result) + (int) (this.timestamp ^ (this.timestamp >>> 32));
        result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
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
        return "AssetRecord [assetStatus=" + this.assetStatus + ", channelId=" + this.channelId + ", timestamp="
                + this.timestamp + ", value=" + this.value + "]";
    }

}
