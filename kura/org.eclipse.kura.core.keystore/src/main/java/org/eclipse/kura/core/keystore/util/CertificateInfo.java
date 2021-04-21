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
package org.eclipse.kura.core.keystore.util;

import java.util.Collection;
import java.util.List;

/**
 * Identifies a certificate with its alias and the corresponding keystore name.
 * Further certificate information can be added (i.e. subjectDN, issuer, ecc.)
 *
 * @since 2.2
 */
public class CertificateInfo extends EntryInfo {

    private String subjectDN;
    private Collection<List<?>> subjectAN;
    private String issuer;
    private String startDate;
    private String expirationDate;
    private String algorithm;
    private int size;
    private String certificate;

    public CertificateInfo(String alias, String keystoreName) {
        super(alias, keystoreName);
        this.setType(EntryType.TRUSTED_CERTIFICATE);
    }

    public String getSubjectDN() {
        return this.subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public Collection<List<?>> getSubjectAN() {
        return this.subjectAN;
    }

    public void setSubjectAN(Collection<List<?>> subjectAN) {
        this.subjectAN = subjectAN;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getStartDate() {
        return this.startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getExpirationDate() {
        return this.expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getCertificate() {
        return this.certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
