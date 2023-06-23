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
 * The registration status of a modem when connected to a mobile network.
 *
 */
public enum RegistrationStatus {

    IDLE,
    HOME,
    SEARCHING,
    DENIED,
    UNKNOWN,
    ROAMING,
    HOME_SMS_ONLY,
    ROAMING_SMS_ONLY,
    EMERGENCY_ONLY,
    HOME_CSFB_NOT_PREFERRED,
    ROAMING_CSFB_NOT_PREFERRED,
    ATTACHED_RLOS;

}
