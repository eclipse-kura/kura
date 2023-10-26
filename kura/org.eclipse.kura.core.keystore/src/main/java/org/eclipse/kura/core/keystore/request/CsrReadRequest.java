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

import org.eclipse.kura.core.keystore.util.CsrInfo;
import org.eclipse.kura.rest.utils.Validable;

public class CsrReadRequest extends CsrInfo implements Validable {

    public CsrReadRequest(String keystoreServicePid, String alias) {
        super(keystoreServicePid, alias);
    }

    public CsrReadRequest(final CsrInfo csrInfo) {
        super(csrInfo.getKeystoreServicePid(), csrInfo.getAlias());
        this.setSignatureAlgorithm(csrInfo.getSignatureAlgorithm());
        this.setAttributes(csrInfo.getAttributes());
    }

    @Override
    public String toString() {
        return "ReadRequest [keystoreServicePid=" + this.getKeystoreServicePid() + ", alias=" + this.getAlias()
                + ", algorithm="
                + this.getSignatureAlgorithm() + ", attributes=" + this.getAttributes() + "]";
    }

    @Override
    public boolean isValid() {
        return this.getKeystoreServicePid() != null && this.getAlias() != null && this.getSignatureAlgorithm() != null
                && this.getAttributes() != null;
    }

}
