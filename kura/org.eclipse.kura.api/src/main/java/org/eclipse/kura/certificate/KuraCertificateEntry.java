/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.certificate;

import java.security.KeyStore.TrustedCertificateEntry;
import java.security.cert.Certificate;

/**
 * 
 * The KuraCertificateEntry represents a {@java.security.KeyStore.TrustedCertificateEntry} stored in a keystore.
 * The entry is identified by an id made with the id of the keystore and the alias.
 *
 * @since 2.2
 */
public class KuraCertificateEntry {

    private String certificateId;
    private String keystoreId;
    private String alias;
    private TrustedCertificateEntry certificateEntry;

    public KuraCertificateEntry(String keystoreId, String alias, Certificate certificate) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.certificateEntry = new TrustedCertificateEntry(certificate);
        this.certificateId = keystoreId + ":" + alias;
    }

    public KuraCertificateEntry(String keystoreId, String alias, TrustedCertificateEntry entry) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.certificateEntry = entry;
        this.certificateId = keystoreId + ":" + alias;
    }

    public String getCertificateId() {
        return this.certificateId;
    }

    public String getKeystoreId() {
        return this.keystoreId;
    }

    public String getAlias() {
        return this.alias;
    }

    public TrustedCertificateEntry getCertificateEntry() {
        return this.certificateEntry;
    }

    public static String getKeystoreId(String id) {
        if (id != null) {
            return id.split(":")[0];
        } else {
            return "";
        }
    }

    public static String getAlias(String id) {
        if (id != null) {
            String[] fields = id.split(":");
            if (fields.length == 1) {
                return "";
            }
            return fields[fields.length - 1];
        } else {
            return "";
        }
    }
}
