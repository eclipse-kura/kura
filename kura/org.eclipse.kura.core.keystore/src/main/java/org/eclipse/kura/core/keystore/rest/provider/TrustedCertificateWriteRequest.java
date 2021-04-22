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
 *******************************************************************************/
package org.eclipse.kura.core.keystore.rest.provider;

import org.eclipse.kura.core.keystore.util.CertificateInfo;
import org.eclipse.kura.rest.utils.Validable;

public class TrustedCertificateWriteRequest extends CertificateInfo implements Validable {

    public TrustedCertificateWriteRequest(String alias, String keystoreName) {
        super(alias, keystoreName);
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
