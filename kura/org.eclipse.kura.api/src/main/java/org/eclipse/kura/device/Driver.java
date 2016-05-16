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
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;

/**
 * The Interface Driver is the main interface that all Kura specific
 * communication drivers have to implement and register as an OSGi service
 * instance. A driver often implements a communication protocol to interact with
 * the field devices. The configuration as provided to the driver for
 * communicating with the field device is provided by user using the
 * configurable component of the actual driver which is internally managed by
 * the OSGi Configuration Admin service
 */
public interface Driver {

	/**
	 * Attempts to connect to the given communication device using the given
	 * settings. Before performing any read/write/monitor operation on the
	 * connection, a communication channel has to be opened using this method.
	 * If the connection attempt fails it throws a {@code KuraException} with
	 * appropriate error code {@code KuraErrorCode#CONNECTION_FAILED} set.
	 *
	 * Some communication protocols are not connection oriented. That means no
	 * connection has to be build up in order to read or write data. In this
	 * case the connect function may optionally test if the device is reachable.
	 *
	 * @throws KuraException
	 *             if the connection to the device was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException}. For any other internal
	 *             exception, the error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 */
	public void connect() throws KuraException;

	/**
	 * Attempts to disconnect the already established communication channel.
	 *
	 * @throws KuraException
	 *             if the connection to the device was interrupted, then error
	 *             code {@code KuraErrorCode#CONNECTION_FAILED} needs to be set
	 *             in the thrown {@link KuraException}. For any other internal
	 *             exception, the error code
	 *             {@code KuraErrorCode#INTERNAL_ERROR} will be set.
	 */
	public void disconnect() throws KuraException;

	/**
	 * Returns the protocol specific channel descriptor.
	 *
	 * @return the channel descriptor
	 */
	public ChannelDescriptor getChannelDescriptor();

	/**
	 * Reads the communication channels that correspond to the given driver
	 * records. The read result is returned by setting the value in the driver
	 * record. If for some reason no value can be read the value should be set
	 * anyways. In this case the driver flag needs to be specified in the driver
	 * record. The flag shall best describe the reason of failure. If no value
	 * is set the default error code is
	 * {@code DriverFlag#DRIVER_ERROR_UNSPECIFIED}. If the connection to the
	 * device is interrupted, then any necessary resources that correspond to
	 * this connection should be cleaned up and a {@code KuraException} shall be
	 * thrown.
	 *
	 * @param records
	 *            the records hold the information of what channels are to be
	 *            read. They will be filled by this function with the records
	 *            read.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
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
	 */
	public void read(List<DriverRecord> records) throws KuraRuntimeException, KuraException;

	/**
	 * Registers driver listener for the provided channel configuration for a
	 * monitor operation on it.
	 *
	 * @param channelConfig
	 *            the channel configuration
	 * @param listener
	 *            the listener
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
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
	 *             if any of the argument is null
	 */
	public void registerDriverListener(Map<String, Object> channelConfig, DriverListener listener)
			throws KuraRuntimeException, KuraException;

	/**
	 * Unregisters a already registered driver listener which has been
	 * registered for a monitor operation
	 *
	 * @param listener
	 *            the listener to unregister
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
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
	 *             if argument is null
	 */
	public void unregisterDriverListener(DriverListener listener) throws KuraRuntimeException, KuraException;

	/**
	 * Writes the data channels that correspond to the given driver records. The
	 * write result is returned by setting the driver flag
	 * {@code DriverFlag#WRITE_SUCCESSFUL} in the driver records. If the
	 * connection to the device is interrupted, then any necessary resources
	 * that correspond to this connection should be cleaned up and a
	 * {@code KuraException} shall be thrown.
	 *
	 * @param records
	 *            the records hold the information of what channels are to be
	 *            written and the values that are to written. They will be
	 *            filled by this function with a driver flag stating whether the
	 *            write process was successful or not.
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
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
	 */
	public void write(List<DriverRecord> records) throws KuraRuntimeException, KuraException;

}
