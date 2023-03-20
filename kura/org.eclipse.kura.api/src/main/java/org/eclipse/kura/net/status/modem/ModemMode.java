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
 * The generic access mode a modem supports.
 *
 */
public enum ModemMode {
    NONE,
    CS,
    MODE_2G,
    MODE_3G,
    MODE_4G,
    MODE_5G,
    ANY;
}
