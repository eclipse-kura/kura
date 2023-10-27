/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.keystore.request;

import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.rest.utils.Validable;

public class TrustedCertificateWriteRequest extends CertificateInfo implements Validable {

    public TrustedCertificateWriteRequest(String keystoreServicePid, String alias) {
        super(keystoreServicePid, alias);
    }

    public TrustedCertificateWriteRequest(final CertificateInfo other) {
        super(other.getKeystoreServicePid(), other.getAlias());
        this.setSubjectDN(other.getSubjectDN());
        this.setSubjectAN(other.getSubjectAN());
        this.setIssuer(other.getIssuer());
        this.setStartDate(other.getStartDate());
        this.setExpirationDate(other.getExpirationDate());
        this.setAlgorithm(other.getAlgorithm());
        this.setSize(other.getSize());
        this.setCertificate(other.getCertificate());
    }

    @Override
    public String toString() {
        return "WriteRequest [keystoreServicePid=" + getKeystoreServicePid() + ", alias=" + getAlias() + "]";
    }

    @Override
    public boolean isValid() {
        boolean result = true;
        if (getKeystoreServicePid() == null || getAlias() == null || getCertificate() == null) {
            result = false;
        }
        return result;
    }

}
