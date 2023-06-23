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
package org.eclipse.kura.net.status.wifi;

/**
 * Modes of operation for WiFi interfaces
 */
public enum WifiMode {
    /** Mode is unknown. */
    UNKNOWN,
    /** Uncoordinated network without central infrastructure. */
    ADHOC,
    /** Client mode - Coordinated network with one or more central controllers. */
    INFRA,
    /**
     * Access Point Mode - Coordinated network with one or more central controllers.
     */
    MASTER,
    /** IEEE 802.11s mesh network. */
    MESH;

}
