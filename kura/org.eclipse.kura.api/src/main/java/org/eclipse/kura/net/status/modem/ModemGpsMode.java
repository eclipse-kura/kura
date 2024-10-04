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

package org.eclipse.kura.net.status.modem;

/*
 * The GPS mode supported by the modem.
 */
public enum ModemGpsMode {
    /*
     * Unmanaged GPS mode. In this mode the GPS device of the modem will be setup but not directly managed, therefore
     * freeing the serial port for other services to use.
     */
    UNMANAGED,
    /*
     * Managed GPS mode. In this mode the GPS device of the modem will be setup and directly managed (typically by
     * ModemManager) therefore the serial port won't be available for other services to use.
     */
    MANAGED_GPS
}
