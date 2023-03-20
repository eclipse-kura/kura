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
 * The type of Bearer or Context associated to a modem connection.
 *
 */
public enum BearerIpType {

    NONE,
    IPV4,
    IPV6,
    IPV4V6,
    NON_IP,
    ANY;
}
