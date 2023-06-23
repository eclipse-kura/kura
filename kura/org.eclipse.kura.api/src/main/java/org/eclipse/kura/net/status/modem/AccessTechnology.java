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
 * The specific technology types used when a modem is connected or registered to
 * a network.
 *
 */
public enum AccessTechnology {

    UNKNOWN,
    POTS,
    GSM,
    GSM_COMPACT,
    GPRS,
    EDGE,
    UMTS,
    HSDPA,
    HSUPA,
    HSPA,
    HSPA_PLUS,
    ONEXRTT,
    EVDO0,
    EVDOA,
    EVDOB,
    LTE,
    FIVEGNR,
    LTE_CAT_M,
    LTE_NB_IOT,
    ANY;
}
