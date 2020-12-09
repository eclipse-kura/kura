/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura;

/**
 * KuraErrorCode holds the enumeration of valid error codes for the exception message. For each defined enum value, a
 * corresponding message should be defined in the properties bundle named:
 * KuraExceptionMessagesBundle.properties.
 *
 * @since 1.3
 *
 */
public enum KuraErrorCode {
    /**
     * Configuration Error: {0}
     */
    CONFIGURATION_ERROR,
    /**
     * Error updating Configuration of ConfigurableComponent {0}
     */
    CONFIGURATION_UPDATE,
    /**
     * Error rolling back to snapshot.
     */
    CONFIGURATION_ROLLBACK,
    /**
     * The configuration attribute {0} is undefined.
     */
    CONFIGURATION_ATTRIBUTE_UNDEFINED,
    /**
     * The configuration attribute {0} cannot accept value {1}: {2}.
     */
    CONFIGURATION_ATTRIBUTE_INVALID,
    /**
     * The configuration attribute {0} is required and no value has been specified.
     */
    CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
    /**
     * Configuration snapshot {0} was not found.
     */
    CONFIGURATION_SNAPSHOT_NOT_FOUND,
    /**
     * Error Taking Snapshot.
     */
    CONFIGURATION_SNAPSHOT_TAKING,
    /**
     * Error Listing Snapshots.
     */
    CONFIGURATION_SNAPSHOT_LISTING,
    /**
     * Error Loading Snapshot
     */
    CONFIGURATION_SNAPSHOT_LOADING,
    /**
     * An internal error occurred. {0}
     * 
     * @deprecated
     */
    INTERNAL_ERROR,
    /**
     * The serial port ha an invalid configuration. {0}
     */
    SERIAL_PORT_INVALID_CONFIGURATION,
    /**
     * The serial port does not exist. {0}
     */
    SERIAL_PORT_NOT_EXISTING,
    /**
     * The port is in use. {0}
     */
    PORT_IN_USE,
    /**
     * The operation succeeded only partially.
     */
    PARTIAL_SUCCESS,
    /**
     * The current subject is not authorized to perform this operation. {0}
     */
    SECURITY_EXCEPTION,
    /**
     * Not connected.
     */
    NOT_CONNECTED,
    /**
     * Timeout occurred while waiting for the operation to complete.
     */
    TIMED_OUT,
    /**
     * Connection failed. {0}
     */
    CONNECTION_FAILED,
    /**
     * Too many in-flight messages.
     */
    TOO_MANY_INFLIGHT_MESSAGES,
    /**
     * Error performing operation on store. {0}
     */
    STORE_ERROR,
    /**
     * Error encoding {0}.
     */
    ENCODE_ERROR,
    /**
     * Error decoding {0}.
     */
    DECODER_ERROR,
    /**
     * Metric {0} is invalid.
     */
    INVALID_METRIC_EXCEPTION,
    /**
     * Message or its encoding is invalid.
     */
    INVALID_MESSAGE_EXCEPTION,
    /**
     * Operation {0} not supported.
     */
    OPERATION_NOT_SUPPORTED,
    /**
     * Device {0} is unavailable.
     */
    UNAVAILABLE_DEVICE,
    /**
     * Device {0} is closed.
     */
    CLOSED_DEVICE,
    /**
     * Error accessing GPIO resource. {0}
     */
    GPIO_EXCEPTION,
    /**
     * Command {0} exited with code {1}.
     *
     * @since 1.0.8
     */
    OS_COMMAND_ERROR,
    /**
     * Invalid parameter. {0}
     * 
     * @since 1.0.8
     */
    INVALID_PARAMETER,
    /**
     * Unable to execute system process {0}
     *
     * @since 1.2
     */
    PROCESS_EXECUTION_ERROR,
    /**
     * Error processing subscription for {0}
     *
     * @since 1.2
     */
    SUBSCRIPTION_ERROR,
    /**
     * Error during BLE notification.
     *
     * @since 1.3
     */
    BLE_NOTIFICATION_ERROR,
    /**
     * Error during BLE connection.
     * 
     * @since 1.3
     */
    BLE_CONNECTION_ERROR,
    /**
     * Error during BLE pairing.
     * 
     * @since 1.3
     */
    BLE_PAIR_ERROR,
    /**
     * BLE resource not found.
     * 
     * @since 1.3
     */
    BLE_RESOURCE_NOT_FOUND,
    /**
     * Error during BLE IO activity.
     * 
     * @since 1.3
     */
    BLE_IO_ERROR,
    /**
     * Error executing {0} command.
     * 
     * @since 1.3
     */
    BLE_COMMAND_ERROR,
    /**
     * Error during discovery procedure.
     * 
     * @since 1.3
     */
    BLE_DISCOVERY_ERROR,
    /**
     * Error during device remove.
     * 
     * @since 2.0
     */
    BLE_REMOVE_ERROR,
    /**
     * Bad request.
     * 
     * @since 2.0
     */
    BAD_REQUEST,
    /**
     * Not found.
     * 
     * @since 2.0
     */
    NOT_FOUND,
    /**
     * Service unavailable. {0}.
     * 
     * @since 2.0
     */
    SERVICE_UNAVAILABLE,
    /**
     * Disconnection failed.
     * 
     * @since 2.0
     */
    DISCONNECTION_FAILED;
}
