/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.rest.utils.Validable;

public class PrivateKeyWriteRequest extends PrivateKeyInfo implements Validable {

    public PrivateKeyWriteRequest(String keystoreServicePid, String alias) {
        super(keystoreServicePid, alias);
    }

    public PrivateKeyWriteRequest(final PrivateKeyInfo other) {
        super(other.getAlias(), other.getKeystoreServicePid());
        this.setAlgorithm(other.getAlgorithm());
        this.setSize(other.getSize());
        this.setPrivateKey(other.getPrivateKey());
        this.setCertificateChain(other.getCertificateChain());
    }

    @Override
    public boolean isValid() {
        return getKeystoreServicePid() != null && getAlias() != null && getCertificateChain() != null
                && getCertificateChain().length > 0;
    }
}
