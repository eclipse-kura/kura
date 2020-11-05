/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.certificate;

public enum CertificateType {

    DM,
    BUNDLE,
    SSL,
    LOGIN;

    public static CertificateType getCertificateType(String stringCertificateType) {
        if (DM.name().equalsIgnoreCase(stringCertificateType)) {
            return DM;
        }
        if (BUNDLE.name().equalsIgnoreCase(stringCertificateType)) {
            return BUNDLE;
        }
        if (SSL.name().equalsIgnoreCase(stringCertificateType)) {
            return SSL;
        }
        if (LOGIN.name().equalsIgnoreCase(stringCertificateType)) {
            return LOGIN;
        }

        throw new IllegalArgumentException("Cannot convert to CertificateType");
    }

}
