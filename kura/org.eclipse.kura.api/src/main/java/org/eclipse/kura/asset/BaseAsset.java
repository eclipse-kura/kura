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

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.listener.AssetListener;

/**
 * This interface BaseAsset provides higher level abstraction to operate or
 * perform actions on the industrial device. This actually propagates the
 * necessary operations to the associated device driver. The asset exposes the
 * generic operations to perform read, write and monitor operations. The asset
 * only specifies the channel specific configuration (which is very channel
 * descriptor specific) and the associated driver is mainly responsible for
 * performing all the lower level tasks on the provided channel configuration.
 */
public interface BaseAsset {

	/**
	 * Reads the provided communication channels that corresponds to the given
	 * asset records. The read result is returned by setting the value in the
	 * asset record. If for some reason no value can be read the value should be
	 * set anyways. In this case the asset flag needs to be specified in the
	 * asset record. The flag shall best describe the reason of failure. If no
	 * value is set the default flag is
	 * {@code AssetFlag#ASSET_ERROR_UNSPECIFIED}. If the connection to the asset
	 * is interrupted, then any necessary resources that correspond to this
	 * connection should be cleaned up and a {@code KuraException} shall be
	 * thrown.
	 *
	 * @param channelIds
	 *            the list of channel identifiers which are to be read. The
	 *            channel identifiers for an asset must be unique for every
	 *            channel belonging to an asset.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the asset then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the asset was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException}. For any other internal
	 *             exception, the error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 * @return the list of asset records which comprises the currently read
	 *         value in case of success or the reason of failure
	 */
	public List<AssetRecord> read(List<Long> channelIds) throws KuraException;

	/**
	 * Registers asset listener for the provided channel name for a monitor
	 * operation on it.
	 *
	 * @param channelId
	 *            the channel identifier. The channel identifier for an asset
	 *            must be unique for every channel belonging to an asset.
	 * @param assetListener
	 *            the asset listener
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the asset then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the asset was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException} and if the channel is not
	 *             present, error code {@code KuraErrorCode#INTERNAL_ERROR}
	 *             needs to be set in the thrown {@link KuraException}. For any
	 *             other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null or channel ID is less than or
	 *             equal to zero
	 */
	public void registerAssetListener(long channelId, AssetListener assetListener) throws KuraException;

	/**
	 * Unregisters a already registered asset listener which has been registered
	 * for a monitor operation
	 *
	 * @param assetListener
	 *            the asset listener to unregister
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the asset then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             For any other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public void unregisterAssetListener(AssetListener assetListener) throws KuraException;

	/**
	 * Writes the data to the provided communication channels that correspond to
	 * the given asset records. The write result is returned by setting the
	 * asset flag {@code AssetFlag#WRITE_SUCCESSFUL} in the provided asset
	 * records. If the connection to the asset is interrupted, then any
	 * necessary resources that correspond to this connection should be cleaned
	 * up and a {@code KuraException} with a suitable error code shall be
	 * thrown.
	 *
	 * @param assetRecords
	 *            the asset records hold the information of what channels are to
	 *            be written and the values that are to be written. They will be
	 *            filled by this function with a asset flag stating whether the
	 *            write process is successful or not.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the asset then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the asset was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException} and if the channel is not
	 *             present, error code {@code KuraErrorCode#INTERNAL_ERROR}
	 *             needs to be set in the thrown {@link KuraException}. For any
	 *             other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 * @return the list of asset records which comprises the status of the write
	 *         operations
	 */
	public List<AssetRecord> write(List<AssetRecord> assetRecords) throws KuraException;

}
