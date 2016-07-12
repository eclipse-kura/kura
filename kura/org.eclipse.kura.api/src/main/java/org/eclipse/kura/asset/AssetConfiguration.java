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

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;

import com.google.common.base.MoreObjects;

/**
 * The Class AssetConfiguration is responsible for storing the configuration for
 * an asset.
 *
 * @see Channel
 */
@NotThreadSafe
public final class AssetConfiguration {

	/**
	 * The list of channels associated with this asset. The association denotes
	 * channel ID and its actual object reference pair.
	 */
	private final Map<Long, Channel> channels;

	/** the asset description. */
	private String description;

	/** the driver ID as associated with this asset. */
	private final String driverId;

	/** the name of the asset. */
	private String name;

	/**
	 * Instantiates a new asset configuration.
	 *
	 * @param name
	 *            the name of the asset
	 * @param description
	 *            the description of the asset
	 * @param driverId
	 *            the driver id
	 * @param channels
	 *            the map of all channel configurations
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public AssetConfiguration(final String name, final String description, final String driverId,
			final Map<Long, Channel> channels) {
		checkNull(description, "Asset name cannot be null");
		checkNull(description, "Asset description cannot be null");
		checkNull(description, "Asset driver ID cannot be null");
		checkNull(description, "Asset channel configurations cannot be null");

		this.description = description;
		this.driverId = driverId;
		this.name = name;
		this.channels = channels;
	}

	/**
	 * Gets the channels.
	 *
	 * @return the channels
	 */
	public Map<Long, Channel> getChannels() {
		return this.channels;
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Gets the driver id.
	 *
	 * @return the driver id
	 */
	public String getDriverId() {
		return this.driverId;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the description.
	 *
	 * @param description
	 *            the new description
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setDescription(final String description) {
		checkNull(description, "Asset description cannot be null");
		this.description = description;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            the new name
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setName(final String name) {
		checkNull(this.description, "Asset name cannot be null");
		this.name = name;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.name).add("description", this.description)
				.add("driverId", this.driverId).add("channels", this.channels).toString();
	}

}
