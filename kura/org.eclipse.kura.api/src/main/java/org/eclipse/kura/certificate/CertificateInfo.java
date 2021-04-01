/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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

/**
 * Identifies a certificate alias with the corresponding keystore name
 *
 * @since 2.2
 */
public class CertificateInfo {

    private final String alias;
    private final String keystoreName;

    public CertificateInfo(String alias, String keystoreName) {
        this.alias = alias;
        this.keystoreName = keystoreName;
    }

    public String getAlias() {
        return this.alias;
    }

    public String getKeystoreName() {
        return this.keystoreName;
    }

}
