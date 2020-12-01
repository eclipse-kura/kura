/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.certificate;

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
