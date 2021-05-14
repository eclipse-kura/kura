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
    private long startDate;
    private long expirationDate;
    private String algorithm;
    private int size;
    private String certificate;
    private EntryType type = EntryType.TRUSTED_CERTIFICATE;

    public CertificateInfo(String keystoreName, String alias) {
        super(keystoreName, alias);
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

    public long getStartDate() {
        return this.startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getExpirationDate() {
        return this.expirationDate;
    }

    public void setExpirationDate(long expirationDate) {
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

    public EntryType getType() {
        return this.type;
    }
}
