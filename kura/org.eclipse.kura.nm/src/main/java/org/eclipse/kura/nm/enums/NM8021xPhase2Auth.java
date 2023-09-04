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
 *******************************************************************************/
package org.eclipse.kura.nm.enums;

public enum NM8021xPhase2Auth {

    EAP("eap"),
    MSCHAPV2("mschapv2"),
    GTC("gtc"),
    OTP("otp"),
    MD5("md5"),
    TLS("tls");

    private String value;

    private NM8021xPhase2Auth(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
