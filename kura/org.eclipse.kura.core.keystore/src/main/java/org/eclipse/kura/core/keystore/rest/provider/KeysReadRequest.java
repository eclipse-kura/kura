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

import org.eclipse.kura.rest.utils.Validable;

public class KeysReadRequest implements Validable {

    private String keystoreServicePid;
    private String alias;

    public String getKeystoreServicePid() {
        return this.keystoreServicePid;
    }

    public String getAlias() {
        return this.alias;
    }

    @Override
    public String toString() {
        return "KeysReadRequest [keystoreServicePid=" + this.keystoreServicePid + ", alias=" + this.alias + "]";
    }

    @Override
    public boolean isValid() {
        return this.keystoreServicePid != null || this.alias != null;
    }

}
