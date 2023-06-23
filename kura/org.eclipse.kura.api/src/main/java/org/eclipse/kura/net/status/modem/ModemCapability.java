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
 * The generic access technologies families supported by a modem.
 *
 */
public enum ModemCapability {

    /** The modem has no capabilities. */
    NONE,
    /**
     * The modem supports the Plain Old Telephone Service (analog wired telephone
     * network).
     */
    POTS,
    /** The modem supports EVDO revision 0, A or B. */
    EVDO,
    /**
     * The modem supports at least one of GSM, GPRS, EDGE, UMTS, HSDPA, HSUPA or
     * HSPA+ technologies.
     */
    GSM_UMTS,
    /** The modem has LTE capabilities */
    LTE,
    /** The modem supports Iridium technology. */
    IRIDIUM,
    /** The modem supports 5GNR. */
    FIVE_GNR,
    /** The modem supports TDS. */
    TDS,
    /** The modem supports all capabilities. */
    ANY;

}
