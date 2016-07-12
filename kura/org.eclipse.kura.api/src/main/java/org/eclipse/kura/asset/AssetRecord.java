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

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class AssetRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated device driver.
 */
@NotThreadSafe
public final class AssetRecord {

	/** The asset flag. */
	private AssetFlag assetFlag;

	/**
	 * The associated channel name. The channel name for any asset must be
	 * unique.
	 */
	private String channelName;

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
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public AssetRecord(final String channelName) {
		checkNull(channelName, "Channel name cannot be null");
		this.channelName = channelName;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof AssetRecord) {
			final AssetRecord rec = (AssetRecord) obj;
			return Objects.equal(rec.getChannelName(), this.channelName) && Objects.equal(rec.getValue(), this.value)
					&& Objects.equal(rec.getAssetFlag(), this.assetFlag)
					&& Objects.equal(rec.getTimestamp(), this.timestamp);
		}
		return false;
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
	 * Gets the channel name.
	 *
	 * @return the channel name
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

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.channelName, this.value, this.assetFlag, this.timestamp);
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
	 * Sets the channel name as provided.
	 *
	 * @param channelName
	 *            the new channel name
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setChannelName(final String channelName) {
		checkNull(channelName, "Channel name cannot be null");
		this.channelName = channelName;
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
		return MoreObjects.toStringHelper(this).add("channel_name", this.channelName).add("asset_flag", this.assetFlag)
				.add("timestamp", this.timestamp).add("value", this.value).toString();
	}

}
