/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.asset;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface Asset provides higher level abstraction to operate or
 * perform actions on the industrial device. This actually propagates the
 * necessary operations to the associated device driver. The asset exposes the
 * generic operations to perform read, write and monitor operations. The asset
 * only specifies the channel specific configuration (which is very channel
 * descriptor specific) and the associated driver is mainly responsible for
 * performing all the lower level tasks on the provided channel configuration.
 *
 * @see AssetRecord
 * @see AssetConfiguration
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface Asset {

    /**
     * Gets the asset configuration.
     *
     * @return the asset configuration
     */
    public AssetConfiguration getAssetConfiguration();

    /**
     * Reads the communication channels identified by the set of channel names provided as argument.
     * The read result is returned as a list of channel records.
     * If for some reason the value of a channel cannot be read,
     * a channel record containing a proper channel flag should be returned anyways.
     * The channel flag shall best describe the reason of failure.
     * If the connection to the asset is interrupted, then any necessary resources
     * that correspond to this connection should be cleaned up and a {@code KuraException} shall be
     * thrown.
     *
     * @param channelNames
     *            the set of channel names which are to be read. The channel name
     *            must be unique for every channel belonging to an asset.
     *            Channel names are case sensitive.
     * @throws KuraRuntimeException
     *             if the method is not implemented by the asset then specific
     *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
     *             needs to be set in the thrown {@link KuraRuntimeException}
     * @throws KuraException
     *             if the connection to the asset was interrupted, then error
     *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
     *             in the thrown {@link KuraException}.
     * @throws NullPointerException
     *             if argument is null
     * @return the list of channel records which comprises the currently read
     *         value in case of success or the reason of failure
     */
    public List<ChannelRecord> read(Set<String> channelNames) throws KuraException;

    /**
     * Performs a read on all READ or READ_WRITE channels that are defined on this asset and returns
     * the result as a list of {@link ChannelRecord} instances.
     *
     * @see Asset#read(List)
     *
     * @throws KuraRuntimeException
     *             if the method is not implemented by the asset then specific
     *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
     *             needs to be set in the thrown {@link KuraRuntimeException}
     * @throws KuraException
     *             if the connection to the asset was interrupted, then error
     *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
     *             in the thrown {@link KuraException}.
     * @return the list of channel records which comprises the currently read
     *         value in case of success or the reason of failure
     */
    public List<ChannelRecord> readAllChannels() throws KuraException;

    /**
     * Registers a channel listener for the provided channel name for a monitor
     * operation on it.
     *
     * @param channelName
     *            the channel name. The channel name
     *            must be unique for every channel belonging to an asset.
     *            Channel names are case sensitive.
     * @param channelListener
     *            the channel listener
     * @throws KuraRuntimeException
     *             if the method is not implemented by the asset then specific
     *             error code {@link KuraErrorCode#OPERATION_NOT_SUPPORTED}
     *             needs to be set in the thrown {@link KuraRuntimeException}
     * @throws KuraException
     *             if the connection to the asset was interrupted, then error
     *             code {@link KuraErrorCode#CONNECTION_FAILED} needs to be set
     *             in the thrown {@link KuraException} and if the channel is not
     *             present, error code {@link KuraErrorCode#INTERNAL_ERROR}
     *             needs to be set in the thrown {@link KuraException}. For any
     *             other internal exception, then error code
     *             {@link KuraErrorCode#INTERNAL_ERROR} will be set.
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws IllegalArgumentException
     *             If the provided channel name is not present in the configuration
     *             of this asset
     */
    public void registerChannelListener(String channelName, ChannelListener channelListener) throws KuraException;

    /**
     * Unregisters a already registered channel listener which has been registered
     * for a monitor operation
     *
     * @param channelListener
     *            the channel listener to unregister
     * @throws KuraRuntimeException
     *             if the method is not implemented by the asset then specific
     *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
     *             needs to be set in the thrown {@link KuraRuntimeException}
     * @throws KuraException
     *             For any other internal exception, then error code
     *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
     * @throws NullPointerException
     *             if argument is null
     */
    public void unregisterChannelListener(ChannelListener channelListener) throws KuraException;

    /**
     * Writes the data to the provided communication channels that correspond to
     * the given channel records. The write result is returned by setting the
     * channel flag {@link ChannelFlag#SUCCESS} in the provided channel
     * records. If the connection to the asset is interrupted, then any
     * necessary resources that correspond to this connection should be cleaned
     * up and a {@link KuraException} with a suitable error code shall be
     * thrown.
     *
     * @param channelRecords
     *            the channel records hold the information of what channels are to
     *            be written and the values that are to be written. They will be
     *            filled by this function with a channel flag stating whether the
     *            write process is successful or not.
     * @throws KuraRuntimeException
     *             if the method is not implemented by the asset then specific
     *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
     *             needs to be set in the thrown {@link KuraRuntimeException}
     * @throws KuraException
     *             if the connection to the asset was interrupted, then error
     *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
     *             in the thrown {@link KuraException}
     * @throws NullPointerException
     *             if argument is null
     */
    public void write(List<ChannelRecord> channelRecords) throws KuraException;

}
