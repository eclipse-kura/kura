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

import java.util.Collections;
import java.util.Map;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.type.DataType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class AssetConfiguration is responsible for storing the configuration for
 * an industrial device (also known as Asset in the context of Eclipse
 * Kura).<br>
 * <br>
 *
 * @see Channel
 * @see ChannelType
 * @see DataType
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class AssetConfiguration {

    /**
     * The list of channels associated with this asset. The association denotes
     * channel name and its actual object reference pair.
     */
    private final Map<String, Channel> assetChannels;

    /** the asset description. */
    private String assetDescription;

    /** the driver PID as associated with this asset. */
    private final String driverPid;

    /**
     * Instantiates a new asset configuration.
     *
     * @param description
     *            the description of the asset
     * @param driverPid
     *            the driver PID
     * @param channels
     *            the map of all channel configurations
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public AssetConfiguration(final String description, final String driverPid, final Map<String, Channel> channels) {
        requireNonNull(description, "Asset description cannot be null");
        requireNonNull(driverPid, "Asset driver PID cannot be null");
        requireNonNull(channels, "Asset channel configurations cannot be null");

        this.assetDescription = description;
        this.driverPid = driverPid;
        this.assetChannels = Collections.unmodifiableMap(channels);
    }

    /**
     * Gets the asset channels.
     *
     * @return the asset channels
     */
    public Map<String, Channel> getAssetChannels() {
        return this.assetChannels;
    }

    /**
     * Gets the asset description.
     *
     * @return the asset description
     */
    public String getAssetDescription() {
        return this.assetDescription;
    }

    /**
     * Gets the driver PID.
     *
     * @return the driver PID
     */
    public String getDriverPid() {
        return this.driverPid;
    }

    /**
     * Sets the asset description.
     *
     * @param description
     *            the new asset description
     * @throws NullPointerException
     *             if the argument is null
     */
    public void setAssetDescription(final String description) {
        requireNonNull(description, "Asset description cannot be null");
        this.assetDescription = description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AssetConfiguration [channels=" + this.assetChannels + ", description=" + this.assetDescription
                + ", driverPid=" + this.driverPid + "]";
    }
}
