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

/**
 * Identifies a certificate alias with the corresponding {@link CertificateType}
 *
 * @since 2.2
 */
public class CertificateInfo {

    private final String alias;
    private final CertificateType type;

    public CertificateInfo(String alias, CertificateType type) {
        this.alias = alias;
        this.type = type;
    }

    public String getAlias() {
        return this.alias;
    }

    public CertificateType getType() {
        return this.type;
    }

}
