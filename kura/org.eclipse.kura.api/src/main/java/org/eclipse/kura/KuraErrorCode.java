/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura;

/**
 * KuraErrorCode holds the enumeration of valid error codes for the exception message. For each defined enum value, a corresponding message should be defined in the properties bundle named:
 * KuraExceptionMessagesBundle.properties.
 * 
 */
public enum KuraErrorCode {
	CONFIGURATION_ERROR,
	CONFIGURATION_UPDATE,
	CONFIGURATION_ROLLBACK,
	CONFIGURATION_ATTRIBUTE_UNDEFINED,
	CONFIGURATION_ATTRIBUTE_INVALID,
	CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
	CONFIGURATION_SNAPSHOT_NOT_FOUND,
	CONFIGURATION_SNAPSHOT_TAKING,
	CONFIGURATION_SNAPSHOT_LISTING,
	CONFIGURATION_SNAPSHOT_LOADING,
	INTERNAL_ERROR,
	SERIAL_PORT_INVALID_CONFIGURATION,
	SERIAL_PORT_NOT_EXISTING,
	PORT_IN_USE,
	PARTIAL_SUCCESS,
	SECURITY_EXCEPTION,
	NOT_CONNECTED,
	TIMED_OUT,
	CONNECTION_FAILED,
	TOO_MANY_INFLIGHT_MESSAGES,
	STORE_ERROR,
	ENCODE_ERROR,
	DECODER_ERROR,
	INVALID_METRIC_EXCEPTION,
	INVALID_MESSAGE_EXCEPTION,
	OPERATION_NOT_SUPPORTED,
	UNAVAILABLE_DEVICE,
	CLOSED_DEVICE,
	GPIO_EXCEPTION,
	/**
	 * Error executing an OS command.
	 * @since package org.eclipse.kura 1.1.0
	 */
	OS_COMMAND_ERROR;
}
