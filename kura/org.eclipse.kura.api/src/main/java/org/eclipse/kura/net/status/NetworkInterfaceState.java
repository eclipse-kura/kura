/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.status;

/**
 * The state of a network interface.
 */
public enum NetworkInterfaceState {

    /** The device is in an unknown state. */
    UNKNOWN,
    /** The device is recognized but not managed. */
    UNMANAGED,
    /** The device cannot be used (carrier off, rfkill, etc). */
    UNAVAILABLE,
    /** The device is not connected. */
    DISCONNECTED,
    /** The device is preparing to connect. */
    PREPARE,
    /** The device is being configured. */
    CONFIG,
    /** The device is awaiting secrets necessary to continue connection. */
    NEED_AUTH,
    /** The IP settings of the device are being requested and configured. */
    IP_CONFIG,
    /** The device's IP connectivity ability is being determined. */
    IP_CHECK,
    /** The device is waiting for secondary connections to be activated. */
    SECONDARIES,
    /** The device is active. */
    ACTIVATED,
    /** The device's network connection is being turn down. */
    DEACTIVATING,
    /** The device is in a failure state following an attempt to activate it. */
    FAILED;

}
