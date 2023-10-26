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
 *
 *******************************************************************************/
package org.eclipse.kura.core.keystore.request;

import org.eclipse.kura.core.keystore.util.KeyPairInfo;
import org.eclipse.kura.rest.utils.Validable;

public class KeyPairWriteRequest extends KeyPairInfo implements Validable {

    public KeyPairWriteRequest(String keystoreName, String alias) {
        super(keystoreName, alias);
    }

    public KeyPairWriteRequest(final KeyPairInfo keyPairInfo) {
        super(keyPairInfo.getKeystoreServicePid(), keyPairInfo.getAlias());
        this.setAlgorithm(keyPairInfo.getAlgorithm());
        this.setAttributes(keyPairInfo.getAttributes());
        this.setSignatureAlgorithm(keyPairInfo.getSignatureAlgorithm());
        this.setSize(keyPairInfo.getSize());
    }

    @Override
    public boolean isValid() {
        if (getKeystoreServicePid() == null || getAlias() == null) {
            return false;
        }
        return !(getAlgorithm() == null || getSize() == 0 || getSignatureAlgorithm() == null
                || getAttributes() == null);
    }

}
