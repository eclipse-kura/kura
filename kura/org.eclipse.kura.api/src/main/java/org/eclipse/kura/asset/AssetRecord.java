/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.asset;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class AssetRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated driver.
 */
@NotThreadSafe
public final class AssetRecord {

	/** The asset flag. */
	private AssetFlag assetFlag;

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
	 * @throws KuraRuntimeException
	 *             if the channel identifier is less than or equal to zero
	 */
	public AssetRecord(final long channelId) {
		checkCondition(channelId <= 0, "Channel ID cannot be zero or less");
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
		if (this.assetFlag != other.assetFlag) {
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
	 * Gets the asset flag.
	 *
	 * @return the asset flag
	 */
	public AssetFlag getAssetFlag() {
		return this.assetFlag;
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
		result = (prime * result) + ((this.assetFlag == null) ? 0 : this.assetFlag.hashCode());
		result = (prime * result) + (int) (this.channelId ^ (this.channelId >>> 32));
		result = (prime * result) + (int) (this.timestamp ^ (this.timestamp >>> 32));
		result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	/**
	 * Sets the asset flag as provided.
	 *
	 * @param assetFlag
	 *            the new asset flag
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setAssetFlag(final AssetFlag assetFlag) {
		checkNull(assetFlag, "Asset flag cannot be null");
		this.assetFlag = assetFlag;
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
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setValue(final TypedValue<?> value) {
		checkNull(value, "Value type cannot be null");
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "AssetRecord [assetFlag=" + this.assetFlag + ", channelId=" + this.channelId + ", timestamp="
				+ this.timestamp + ", value=" + this.value + "]";
	}

}
