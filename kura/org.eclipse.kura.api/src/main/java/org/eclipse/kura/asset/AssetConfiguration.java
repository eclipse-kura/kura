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

import java.util.Map;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.DataType;

/**
 * The Class AssetConfiguration is responsible for storing the configuration for
 * an industrial device (also known as Asset in the context of Eclipse
 * Kura).<br/>
 * <br/>
 *
 * The properties as provided to an Asset must conform to the following
 * specifications. The properties must have the following.<br/>
 * <br/>
 *
 * <ul>
 * <li>the value associated with <b><i>driver.pid</i></b> key in the map denotes
 * the driver instance PID (kura.service.pid) to be consumed by this asset</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
 * description</li>
 * <li>x.CH.[property]</li> where x is any number denoting the channel's unique
 * ID and the {@code [property]} denotes the protocol specific properties. (Note
 * that the format includes at least two ".") denotes map object containing a
 * channel configuration</li>
 *
 * For example, 1.CH.name, 1.CH.value.type etc.<br/>
 * <br/>
 *
 * The representation in the provided properties as prepended by a number
 * signifies a single channel and it should conform to the following
 * specification.<br/>
 * <br/>
 *
 * The properties should contain the following keys
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>value.type</li>
 * <li>[more configuration]</li> as mentioned by the driver in the format which
 * begins with <b><i>DRIVER.</i></b>
 * </ul>
 *
 * For example, [more configuration] would be 1.CH.DRIVER.modbus.register,
 * 1.CH.DRIVER.modbus.unit.id etc.<br/>
 * <br/>
 *
 * The key <b><i>name</i></b> must be String.<br/>
 * <br/>
 *
 * The key <b><i>value.type</i></b> must be in one of types from
 * {@link DataType} in String representation format (case-insensitive)<br/>
 * <br/>
 *
 * The channel {@code type} should be one of the types from {@link ChannelType}
 * in String representation format (case-insensitive)<br/>
 * <br/>
 *
 * @see Channel
 * @see ChannelType
 * @see DataType
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class AssetConfiguration {

    /**
     * The list of channels associated with this asset. The association denotes
     * channel ID and its actual object reference pair.
     */
    private final Map<Long, Channel> assetChannels;

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
    public AssetConfiguration(final String description, final String driverPid, final Map<Long, Channel> channels) {
        requireNonNull(description, "Asset description cannot be null");
        requireNonNull(driverPid, "Asset driver PID cannot be null");
        requireNonNull(channels, "Asset channel configurations cannot be null");

        this.assetDescription = description;
        this.driverPid = driverPid;
        this.assetChannels = channels;
    }

    /**
     * Gets the asset channels.
     *
     * @return the asset channels
     */
    public Map<Long, Channel> getAssetChannels() {
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
