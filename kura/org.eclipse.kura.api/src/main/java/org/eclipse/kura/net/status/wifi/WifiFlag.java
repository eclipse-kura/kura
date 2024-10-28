/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.status.wifi;

/**
 * Flags describing the capabilities of an Access Point.
 * 
 * @since 2.8
 */
public enum WifiFlag {
    /** None */
    NONE,
    /** Supports authentication and encryption */
    PRIVACY,
    /** Supports WPS */
    WPS,
    /** Supports push-button based WPS */
    WPS_PBC,
    /** Supports PIN based WPS */
    WPS_PIN;

}
