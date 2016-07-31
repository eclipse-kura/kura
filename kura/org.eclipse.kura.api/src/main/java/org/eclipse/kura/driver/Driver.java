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
package org.eclipse.kura.driver;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.listener.DriverListener;

/**
 * The Interface Driver is the main interface that all Kura specific
 * communication drivers have to implement and register as an OSGi service
 * instance. A driver often implements a communication protocol to interact with
 * the field devices (assets). The configuration as provided to the driver for
 * communicating with the field device (asset) is provided by user using the
 * configurable component of the actual driver which is internally managed by
 * the OSGi Configuration Admin service
 *
 * @see DriverRecord
 * @see ChannelDescriptor
 */
public interface Driver {

	/**
	 * The Class ConnectionException is a driver specific exception which is
	 * essentially used to raise exception related to driver connection
	 */
	public final class ConnectionException extends Exception {

		/** The Constant serial version UID. */
		private static final long serialVersionUID = 3050873377900124238L;

		/**
		 * Instantiates a new connection exception.
		 */
		public ConnectionException() {
			super();
		}

		/**
		 * Instantiates a new connection exception.
		 *
		 * @param messsage
		 *            the exception message
		 */
		public ConnectionException(final String messsage) {
			super(messsage);
		}

		/**
		 * Instantiates a new connection exception.
		 *
		 * @param message
		 *            the exception message
		 * @param cause
		 *            the exception cause
		 */
		public ConnectionException(final String message, final Throwable cause) {
			super(message, cause);
		}

		/**
		 * Instantiates a new connection exception.
		 *
		 * @param cause
		 *            the exception cause
		 */
		public ConnectionException(final Throwable cause) {
			super(cause);
		}

	}

	/**
	 * Attempts to connect to the asset. Before performing any
	 * read/write/monitor operation on the connection, a communication channel
	 * has to be opened using this method. If the connection attempt fails it
	 * throws a {@code ConnectionException}
	 *
	 * Some communication protocols are not connection oriented. That means no
	 * connection has to be built up in order to read or write data. In this
	 * case the connect function may optionally test if the asset is reachable.
	 *
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 */
	public void connect() throws ConnectionException;

	/**
	 * Attempts to disconnect the already established communication channel.
	 *
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 */
	public void disconnect() throws ConnectionException;

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
	 * asset is interrupted, then any necessary resources that correspond to
	 * this connection should be cleaned up and a {@code ConnectionException}
	 * shall be thrown.
	 *
	 * @param records
	 *            the records hold the information of what channels are to be
	 *            read. They will be filled by this function with the records
	 *            already read.
	 * @return the list of driver records which comprises the currently read
	 *         value in case of success or the reason of failure
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 */
	public List<DriverRecord> read(List<DriverRecord> records) throws ConnectionException;

	/**
	 * Registers driver listener for the provided channel configuration for a
	 * monitor operation on it.
	 *
	 * @param channelConfig
	 *            the channel configuration
	 * @param listener
	 *            the listener
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException} or
	 *             any of the arguments is null
	 */
	public void registerDriverListener(Map<String, Object> channelConfig, DriverListener listener)
			throws ConnectionException;

	/**
	 * Unregisters a already registered driver listener which has been
	 * registered for a monitor operation.
	 *
	 * @param listener
	 *            the listener to unregister
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException} or
	 *             the provided argument is null
	 */
	public void unregisterDriverListener(DriverListener listener) throws ConnectionException;

	/**
	 * Writes the data channels that correspond to the given driver records. The
	 * write result is returned by setting the driver flag
	 * {@code DriverFlag#WRITE_SUCCESSFUL} in the driver records. If the
	 * connection to the asset is interrupted, then any necessary resources that
	 * correspond to this connection should be cleaned up and a
	 * {@code ConnectionException} shall be thrown.
	 *
	 * @param records
	 *            the records hold the information of what channels are to be
	 *            written and the values that are to written. They will be
	 *            filled by this function with a driver flag stating whether the
	 *            write process was successful or not.
	 * @return the list of driver records which comprises the status of the
	 *         write operations
	 * @throws ConnectionException
	 *             if the connection to the field device is interrupted
	 * @throws KuraRuntimeException
	 *             if the method is not implemented by the driver then specific
	 *             error code {@code KuraErrorCode#OPERATION_NOT_SUPPORTED}
	 *             needs to be set in the thrown {@link KuraRuntimeException} or
	 *             the provided argument is null or empty
	 */
	public List<DriverRecord> write(List<DriverRecord> records) throws ConnectionException;

}
