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

import java.security.cert.Certificate;
import java.util.Optional;

/**
 * 
 * The KuraCertificate represents a {@java.security.cert.Certificate} stored in a keystore.
 * The certificate is identified by an id made with the id of the keystore and the alias.
 *
 * @since 2.2
 */
public class KuraCertificate {

    private String certificateId;
    private String keystoreId;
    private String alias;
    private Optional<Certificate> certificate;

    public KuraCertificate(String keystoreId, String alias, Certificate certificate) {
        super();
        this.keystoreId = keystoreId;
        this.alias = alias;
        this.certificate = Optional.of(certificate);
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

    public Optional<Certificate> getCertificate() {
        return this.certificate;
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
