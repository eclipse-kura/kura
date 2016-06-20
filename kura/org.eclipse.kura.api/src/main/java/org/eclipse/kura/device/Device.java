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
package org.eclipse.kura.device;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;

/**
 * This interface provides higher level abstraction to operate or perform
 * actions on the device. This actually propagates the necessary operations on
 * the associated device driver. The device exposes the generic operations to
 * perform read, write and monitor operations. The device only specifies the
 * channel specific configuration (which is very channel descriptor specific)
 * and the associated driver is mainly responsible for performing all the lower
 * level tasks on the provided channel configuration.
 */
public interface Device {

	/**
	 * Reads the provided communication channels that corresponds to the given
	 * device records. The read result is returned by setting the value in the
	 * device record. If for some reason no value can be read the value should
	 * be set anyways. In this case the device flag needs to be specified in the
	 * device record. The flag shall best describe the reason of failure. If no
	 * value is set the default flag is
	 * {@code DeviceFlag#DEVICE_ERROR_UNSPECIFIED}. If the connection to the
	 * device is interrupted, then any necessary resources that correspond to
	 * this connection should be cleaned up and a {@code KuraException} shall be
	 * thrown.
	 *
	 * @param channelNames
	 *            the list of channel names which are to be read. The channel
	 *            names for a device must be unique for every channel.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the device then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the device was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException}. For any other internal
	 *             exception, the error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 * @return the list of device records which comprises the currently read
	 *         value in case of success or the reason of failure
	 */
	public List<DeviceRecord> read(List<String> channelNames) throws KuraException;

	/**
	 * Registers device listener for the provided channel name for a monitor
	 * operation on it.
	 *
	 * @param channelName
	 *            the channel name. The channel names for a device must be
	 *            unique for every channel.
	 * @param deviceListener
	 *            the device listener
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the device then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the device was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException} and if the channel is not
	 *             present, error code {@code KuraErrorCode#INTERNAL_ERROR}
	 *             needs to be set in the thrown {@link KuraException}. For any
	 *             other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public void registerDeviceListener(String channelName, DeviceListener deviceListener) throws KuraException;

	/**
	 * Unregisters a already registered device listener which has been
	 * registered for a monitor operation
	 *
	 * @param deviceListener
	 *            the device listener to unregister
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the device then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             For any other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	public void unregisterDeviceListener(DeviceListener deviceListener) throws KuraException;

	/**
	 * Writes the data to the provided communication channels that correspond to
	 * the given device records. The write result is returned by setting the
	 * device flag {@code DeviceFlag#WRITE_SUCCESSFUL} in the provided device
	 * records. If the connection to the device is interrupted, then any
	 * necessary resources that correspond to this connection should be cleaned
	 * up and a {@code KuraException} with a suitable error code shall be
	 * thrown.
	 *
	 * @param deviceRecords
	 *            the device records hold the information of what channels are
	 *            to be written and the values that are to be written. They will
	 *            be filled by this function with a device flag stating whether
	 *            the write process is successful or not.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the device then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException}
	 * @throws KuraException
	 *             if the connection to the device was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException} and if the channel is not
	 *             present, error code {@code KuraErrorCode#INTERNAL_ERROR}
	 *             needs to be set in the thrown {@link KuraException}. For any
	 *             other internal exception, then error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 * @return the list of device records which comprises the status of the
	 *         write operations
	 */
	public List<DeviceRecord> write(List<DeviceRecord> deviceRecords) throws KuraException;

}
