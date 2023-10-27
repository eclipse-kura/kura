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

import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.rest.utils.Validable;

public class EntryRequest extends EntryInfo implements Validable {

    public EntryRequest(String keystoreServicePid, String alias) {
        super(keystoreServicePid, alias);
    }

    public EntryRequest(final EntryInfo entryInfo) {
        super(entryInfo.getKeystoreServicePid(), entryInfo.getAlias());
    }

    @Override
    public String toString() {
        return "DeleteRequest [keystoreServicePid=" + this.getKeystoreServicePid() + ", alias=" + this.getAlias() + "]";
    }

    @Override
    public boolean isValid() {
        return this.getKeystoreServicePid() != null && this.getAlias() != null;
    }

}
