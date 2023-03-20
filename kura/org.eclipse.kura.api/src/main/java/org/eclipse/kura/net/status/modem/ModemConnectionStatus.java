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
package org.eclipse.kura.net.status.modem;

/**
 * The status of a modem.
 *
 */
public enum ModemConnectionStatus {
    /** The modem is unavailable */
    FAILED,
    /** The modem is in an unknown state. */
    UNKNOWN,
    /** The modem is being initialised. */
    INITIALIZING,
    /** The modem is locked. */
    LOCKED,
    /** The modem is disabled and powered off. */
    DISABLED,
    /** The modem is disabling. */
    DISABLING,
    /** The modem is enabling. */
    ENABLING,
    /** The modem is enabled but not registered to a network provider. */
    ENABLED,
    /** The modem is searching for a network provider. */
    SEARCHING,
    /** The modem is registered to a network provider. */
    REGISTERED,
    /** The modem is disconnecting. */
    DISCONNECTING,
    /** The modem is connecting. */
    CONNECTING,
    /** The modem is connected. */
    CONNECTED;

}
