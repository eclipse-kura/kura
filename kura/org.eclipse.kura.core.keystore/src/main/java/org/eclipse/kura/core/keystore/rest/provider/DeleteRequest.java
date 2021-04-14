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

public class DeleteRequest implements Validable {

    private String keystoreName;
    private String alias;

    public String getKeystoreName() {
        return this.keystoreName;
    }

    public String getAlias() {
        return this.alias;
    }

    @Override
    public String toString() {
        return "DeleteRequest [keystoreName=" + this.keystoreName + ", alias=" + this.alias + "]";
    }

    @Override
    public boolean isValid() {
        return this.keystoreName != null && this.alias != null;
    }

}
