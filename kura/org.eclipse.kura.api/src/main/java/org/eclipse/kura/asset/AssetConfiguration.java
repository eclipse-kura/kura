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
import org.eclipse.kura.type.DataType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class AssetConfiguration is responsible for storing the configuration for
 * an industrial device (also known as Asset in the context of Eclipse
 * Kura).<br>
 * <br>
 *
 * The properties as provided to an Asset must conform to the following
 * specifications.<br>
 * <br>
 *
 * <ul>
 * <li>The value associated with <b><i>driver.pid</i></b> key in the map denotes
 * the driver instance PID (kura.service.pid) to be consumed by this asset</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
 * description</li>
 * <li>[name#property]</li> where name is a string denoting the channel's unique
 * name and the {@code [property]} denotes the protocol specific properties.
 * The name of a channel must be unique in the channels configurations of an Asset, and is not
 * allowed to contain spaces or any of the following characters: <b>#</b>, <b>_</b>.
 * </ul>
 *
 * The configuration properties of a channel belong to one of this two groups: generic channel properties and
 * driver specific properties.
 * <br>
 * Generic channel properties begin with the '+' character, and are driver independent.
 * The following generic channel properties must always be present in the channel configuration:
 * <ul>
 * <li>{@code +type} identifies the channel type (READ, WRITE or READ_WRITE) as specified by {@link ChannelType}</li>
 * <li>{@code +value.type} identifies the {@link DataType} of the channel.</li>
 * </ul>
 * For example, the property keys above for a channel named channel1 would be encoded as channel1#+type and
 * channel1#+value.type<br>
 * 
 * The values of the <b>+value.type</b> and <b>+type</b> properties must me mappable
 * respectively to a {@link DataType} and {@link ChannelType} instance.
 * <br>
 * The value of these property can be either an instance of the corresponding type,
 * or a string representation that equals the value returned by calling the {@code toString()} method
 * on one of the enum variants of that type.
 * 
 * <br>
 * Driver specific properties are defined by the driver, their keys cannot begin with a '+' character.
 * For example, valid driver specific properties can be channel1#modbus.register,
 * channel1#modbus.unit.id etc.<br>
 * <br>
 *
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
